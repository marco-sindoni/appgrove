"""appgrove — db-bootstrap (UC 0004, #06 23, #05 11).

Crea (in modo idempotente) il ruolo Postgres e lo schema vuoto di un servizio
sul cluster Aurora dell'ambiente, con privilegi limitati al proprio schema
(least-privilege). Viene invocata da Terraform (`aws_lambda_invocation`) a ogni
`apply` dell'istanza `microsaas_app`; le tabelle NON si creano qui (Flyway, CI).

Usa la Data API di Aurora (rds-data): nessuna connessione di rete diretta al
database, nessun driver Postgres da impacchettare. Se il cluster è in pausa
(scale-to-0, #06 14) la prima chiamata lo risveglia: si riprova finché non
risponde (cold-start ~10-15s).

Due modalità:

1. **ruolo+schema di servizio** (default, UC 0004) — input:
   {"role_name": "...", "schema_name": "...", "secret_arn": "..."}
   crea il ruolo login e lo schema di sua proprietà (least-privilege sul solo
   schema). role_name/schema_name coincidono per convenzione.

2. **grant su schema altrui** (UC 0016, opzione B di E23) — input:
   {"mode": "grant", "role_name": "auth_lambdas", "secret_arn": "...",
    "grants": {"schema": "platform", "owner_role": "platform",
               "select_all": true, "write_tables": ["accounts", "users", ...]}}
   crea il ruolo login delle Lambda auth (NON possiede schema) e gli concede i
   privilegi minimi su uno schema di proprietà di un ALTRO ruolo. I grant sono
   idempotenti e ri-eseguiti a ogni apply (input legato a image_tag) così da
   coprire le tabelle aggiunte da nuove migrazioni Flyway.

secret_arn: segreto Secrets Manager con chiavi "username"/"password".
"""

import json
import os
import re
import time

import boto3

IDENTIFIER = re.compile(r"^[a-z][a-z0-9_]{0,62}$")

CLUSTER_ARN = os.environ["CLUSTER_ARN"]
MASTER_SECRET_ARN = os.environ["MASTER_SECRET_ARN"]
DB_NAME = os.environ["DB_NAME"]

rds_data = boto3.client("rds-data")
secrets = boto3.client("secretsmanager")


def _execute(sql):
    """Esegue una statement via Data API, riprovando mentre Aurora si risveglia."""
    last_error = None
    for _ in range(24):  # ~2 minuti: ampiamente oltre il cold-start atteso
        try:
            return rds_data.execute_statement(
                resourceArn=CLUSTER_ARN,
                secretArn=MASTER_SECRET_ARN,
                database=DB_NAME,
                sql=sql,
            )
        except rds_data.exceptions.DatabaseResumingException as e:
            last_error = e
        except rds_data.exceptions.BadRequestException as e:
            # La Data API segnala il risveglio anche come BadRequest generica.
            if "resum" not in str(e).lower():
                raise
            last_error = e
        time.sleep(5)
    raise last_error


def _quote_literal(value):
    """Quota un letterale SQL (la Data API non supporta parametri nei DDL)."""
    return "'" + value.replace("'", "''") + "'"


def _check_identifier(name):
    if not IDENTIFIER.match(name):
        raise ValueError(f"identificatore non valido: {name!r}")


def _read_password(secret_arn):
    return json.loads(
        secrets.get_secret_value(SecretId=secret_arn)["SecretString"]
    )["password"]


def _upsert_login_role(role, pw):
    """Crea il ruolo login (idempotente); se esiste, riallinea solo la password."""
    _execute(
        f"""
        DO $$
        BEGIN
          CREATE ROLE {role} LOGIN PASSWORD {pw};
        EXCEPTION WHEN duplicate_object THEN
          ALTER ROLE {role} WITH LOGIN PASSWORD {pw};
        END
        $$
        """
    )


def _handle_service(event):
    """Modalità 1: ruolo login + schema di sua proprietà (least-privilege)."""
    role = event["role_name"]
    schema = event["schema_name"]
    _check_identifier(role)
    _check_identifier(schema)

    pw = _quote_literal(_read_password(event["secret_arn"]))
    _upsert_login_role(role, pw)

    # Schema vuoto di proprietà del ruolo (#06 23): i privilegi restano confinati
    # al proprio schema (#05 11); le tabelle le creerà Flyway (UC 0005/0012).
    _execute(f"CREATE SCHEMA IF NOT EXISTS {schema} AUTHORIZATION {role}")

    # Difesa in profondità: nessun diritto di creare oggetti nello schema public.
    _execute(f"REVOKE CREATE ON SCHEMA public FROM {role}")

    return {"role": role, "schema": schema, "status": "ok"}


def _handle_grant(event):
    """Modalità 2 (UC 0016, E23-B): ruolo login delle Lambda auth + privilegi
    minimi su uno schema di proprietà di un ALTRO ruolo. Il ruolo NON possiede
    schemi: solo USAGE + SELECT (lettura pre-token-gen) e, sulle tabelle scritte
    dal BFF, anche INSERT/UPDATE. Idempotente e ri-eseguibile."""
    role = event["role_name"]
    grants = event["grants"]
    schema = grants["schema"]
    owner = grants["owner_role"]
    write_tables = grants.get("write_tables", [])

    _check_identifier(role)
    _check_identifier(schema)
    _check_identifier(owner)
    for table in write_tables:
        _check_identifier(table)

    pw = _quote_literal(_read_password(event["secret_arn"]))
    _upsert_login_role(role, pw)

    # Niente diritto di creare oggetti nello schema public (difesa in profondità).
    _execute(f"REVOKE CREATE ON SCHEMA public FROM {role}")

    _execute(f"GRANT USAGE ON SCHEMA {schema} TO {role}")
    if grants.get("select_all"):
        # Lettura di tutte le tabelle (pre-token-gen). Ri-concessa a ogni apply:
        # copre le tabelle aggiunte da nuove migrazioni (input legato a image_tag).
        _execute(f"GRANT SELECT ON ALL TABLES IN SCHEMA {schema} TO {role}")
    for table in write_tables:
        # Scritture del BFF (signup/accept invito): solo le tabelle necessarie.
        _execute(
            f"GRANT SELECT, INSERT, UPDATE ON {schema}.{table} TO {role}"
        )

    return {"role": role, "granted_on": schema, "status": "ok"}


def handler(event, _context):
    if event.get("mode") == "grant":
        return _handle_grant(event)
    return _handle_service(event)
