"""appgrove — db-bootstrap (UC 0004, #06 23, #05 11).

Crea (in modo idempotente) il ruolo Postgres e lo schema vuoto di un servizio
sul cluster Aurora dell'ambiente, con privilegi limitati al proprio schema
(least-privilege). Viene invocata da Terraform (`aws_lambda_invocation`) a ogni
`apply` dell'istanza `microsaas_app`; le tabelle NON si creano qui (Flyway, CI).

Usa la Data API di Aurora (rds-data): nessuna connessione di rete diretta al
database, nessun driver Postgres da impacchettare. Se il cluster è in pausa
(scale-to-0, #06 14) la prima chiamata lo risveglia: si riprova finché non
risponde (cold-start ~10-15s).

Input (JSON): {"role_name": "...", "schema_name": "...", "secret_arn": "..."}
  - role_name / schema_name: identificatori [a-z][a-z0-9_]* (coincidono per
    convenzione: il ruolo possiede il proprio schema);
  - secret_arn: segreto Secrets Manager per-app con chiave "password".
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


def handler(event, _context):
    role = event["role_name"]
    schema = event["schema_name"]
    secret_arn = event["secret_arn"]

    for name in (role, schema):
        if not IDENTIFIER.match(name):
            raise ValueError(f"identificatore non valido: {name!r}")

    password = json.loads(
        secrets.get_secret_value(SecretId=secret_arn)["SecretString"]
    )["password"]
    pw = _quote_literal(password)

    # Ruolo di servizio: login, nessun privilegio globale; se esiste già si
    # riallinea solo la password (idempotente, ri-eseguibile a ogni apply).
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

    # Schema vuoto di proprietà del ruolo (#06 23): i privilegi restano confinati
    # al proprio schema (#05 11); le tabelle le creerà Flyway (UC 0005/0012).
    _execute(f"CREATE SCHEMA IF NOT EXISTS {schema} AUTHORIZATION {role}")

    # Difesa in profondità: nessun diritto di creare oggetti nello schema public.
    _execute(f"REVOKE CREATE ON SCHEMA public FROM {role}")

    return {"role": role, "schema": schema, "status": "ok"}
