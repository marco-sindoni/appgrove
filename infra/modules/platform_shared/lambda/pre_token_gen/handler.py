"""appgrove — Pre-Token-Generation Lambda (UC 0016, #02 9/10/11).

Cognito invoca questa funzione a OGNI emissione token. Legge la membership
dell'utente dallo schema `platform` (via RDS Proxy) e inietta i claim
`tenant_id` (stringa) + `roles` (array) nell'ACCESS token — il meccanismo che
rende vera l'invariante "tenant_id solo dal JWT verificato".

**Fail-closed**: se l'utente non ha una membership attiva, NON viene iniettato
alcun claim → il token esce senza `tenant_id`/`roles` e i servizi lo rifiutano
(TenantResolver fail-closed, UC 0012). Non solleviamo eccezioni per non
trasformare un "utente senza tenant" in un errore di login: restituiamo
l'evento inalterato.

Runtime Python (come db_bootstrap/error_ingest), ma — a differenza di quelle —
si connette a Postgres via **RDS Proxy** dentro la VPC (#05 dec.3): pooling
delle connessioni per un componente effimero sul percorso caldo del login.
Driver `pg8000` puro-Python vendorizzato in `vendor/` (nessun binario nativo,
archive_file autocontenuto).

Parità col provider locale (UC 0010): stessi claim (`tenant_id`, `roles`,
`token_use=access`) e stessa regola `platform-admin` (allow-list di `sub`),
così i servizi hanno un unico percorso di codice in locale e in cloud.

Evento: Cognito Pre-Token-Generation **V2_0** (necessario per personalizzare
l'access token; richiede il piano funzionalità Essentials del pool).
"""

import json
import os
import ssl
import sys

# Le dipendenze pure-Python (pg8000 + scramp + asn1crypto) sono vendorizzate
# accanto al sorgente: le rendiamo importabili senza pipeline di build.
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "vendor"))

import pg8000.native  # noqa: E402  (vendorizzato)

DB_HOST = os.environ["DB_PROXY_HOST"]
DB_PORT = int(os.environ.get("DB_PORT", "5432"))
DB_NAME = os.environ["DB_NAME"]
DB_SECRET_ARN = os.environ["DB_SECRET_ARN"]
# Allow-list dei `sub` platform-admin (parità con auth.local.platform-admin-subjects,
# UC 0010): platform-admin NON è derivabile da platform.users (ruolo tenant-level).
PLATFORM_ADMIN_SUBS = frozenset(
    s for s in os.environ.get("PLATFORM_ADMIN_SUBS", "").replace(",", " ").split() if s
)

# Riuso tra invocazioni a caldo (stesso container): credenziali + connessione.
_credentials = None
_connection = None


def _load_credentials():
    global _credentials
    if _credentials is None:
        import boto3  # import pigro: fornito dal runtime Lambda (fuori dai test unitari)

        secret = json.loads(
            boto3.client("secretsmanager").get_secret_value(SecretId=DB_SECRET_ARN)[
                "SecretString"
            ]
        )
        _credentials = (secret["username"], secret["password"])
    return _credentials


def _connect():
    """Connessione al proxy RDS con TLS (obbligatorio, require_tls). La
    connessione è riusata finché resta viva; su errore si riprova una volta."""
    global _connection
    if _connection is not None:
        return _connection
    username, password = _load_credentials()
    ssl_context = ssl.create_default_context()
    _connection = pg8000.native.Connection(
        user=username,
        password=password,
        host=DB_HOST,
        port=DB_PORT,
        database=DB_NAME,
        ssl_context=ssl_context,
    )
    return _connection


def _lookup_membership(sub):
    """Ritorna (tenant_id, role) per un `sub` con membership attiva, o None."""
    conn = _connect()
    try:
        rows = conn.run(
            "SELECT tenant_id, role FROM platform.users "
            "WHERE cognito_sub = :sub AND status = 'active' AND deleted_at IS NULL",
            sub=sub,
        )
    except Exception:
        # La connessione in cache può essere morta: azzera e riprova una volta.
        global _connection
        _connection = None
        conn = _connect()
        rows = conn.run(
            "SELECT tenant_id, role FROM platform.users "
            "WHERE cognito_sub = :sub AND status = 'active' AND deleted_at IS NULL",
            sub=sub,
        )
    if not rows:
        return None
    tenant_id, role = rows[0][0], rows[0][1]
    return (tenant_id, role)


def _roles_for(sub, role):
    """Ruoli del claim: ruolo tenant + platform-admin se il `sub` è in allow-list
    (stessa regola del provider locale, TokenService.groupsFor)."""
    roles = [role]
    if sub in PLATFORM_ADMIN_SUBS:
        roles.append("platform-admin")
    return roles


def _log(level, message, **fields):
    """Log strutturato JSON (invariante #4): niente credenziali/token nei log."""
    print(json.dumps({"level": level, "msg": message, **fields}))


def handler(event, _context):
    sub = (event.get("request", {}).get("userAttributes", {}) or {}).get("sub")

    if not sub:
        _log("WARN", "pre-token-gen: sub assente nell'evento (fail-closed)")
        return event  # nessun claim iniettato

    membership = _lookup_membership(sub)
    if membership is None:
        # Fail-closed: utente senza membership attiva → nessun claim (#02 10).
        _log("WARN", "pre-token-gen: nessuna membership attiva (fail-closed)", user_id=sub)
        return event

    tenant_id, role = membership
    roles = _roles_for(sub, role)

    event.setdefault("response", {})["claimsAndScopeOverrideDetails"] = {
        "accessTokenGeneration": {
            "claimsToAddOrOverride": {
                "tenant_id": tenant_id,
                "roles": roles,
            }
        }
    }
    _log("INFO", "pre-token-gen: claim iniettati", user_id=sub, tenant_id=tenant_id)
    return event
