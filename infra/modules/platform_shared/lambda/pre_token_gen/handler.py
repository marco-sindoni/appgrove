"""appgrove — Pre-Token-Generation Lambda (UC 0016, #02 9/10/11).

Cognito invoca questa funzione a OGNI emissione token. Legge l'appartenenza
della persona dallo schema `platform` (via RDS Proxy) e inietta i claim
`tenant_id` (stringa) + `roles` (array) nell'ACCESS token — il meccanismo che
rende vera l'invariante "tenant_id solo dal JWT verificato".

Dopo UC 0116 la lettura è identità (`platform.identity`) ⋈ appartenenza
(`platform.membership`): la persona è unica sulla piattaforma, le sue
appartenenze possono essere più di una. Con UNA sola appartenenza attiva —
il caso di tutti gli utenti di oggi — il comportamento è identico a prima.
Con PIÙ appartenenze attive si prende la più ANTICA, in modo deterministico:
è un ripiego dichiarato, non una scelta di prodotto. Quale account è attivo
in una sessione, e come si cambia, è materia di UC 0117: scegliere qui senza
un criterio scritto significherebbe scegliere male.

**Fail-closed**: se la persona non ha un'appartenenza attiva, NON viene iniettato
alcun claim → il token esce senza `tenant_id`/`roles` e i servizi lo rifiutano
(TenantResolver fail-closed, UC 0012). Non solleviamo eccezioni per non
trasformare un "utente senza tenant" in un errore di login: restituiamo
l'evento inalterato. Una persona che è uscita dal suo ultimo account resta
un'identità senza appartenenze: stato non proibito ma INUTILIZZABILE, e va
conservato tale.

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
# UC 0010): platform-admin NON è derivabile dall'appartenenza (ruolo di account).
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


# Identità ⋈ appartenenza (UC 0116). PARITÀ con il fornitore locale
# (services/auth/src/main/java/app/appgrove/auth/local/UserDirectory.java): stessa
# condizione, stesso ordine, stesso ripiego sull'appartenenza più antica. Se una
# delle due cambia, l'altra cambia con essa — altrimenti i collaudi locali dicono
# una cosa e l'ambiente reale un'altra.
_MEMBERSHIP_SQL = (
    "SELECT m.tenant_id, m.role FROM platform.identity i "
    "JOIN platform.membership m ON m.identity_id = i.id "
    "WHERE i.cognito_sub = :sub "
    "AND i.status = 'active' AND i.deleted_at IS NULL "
    "AND m.status = 'active' AND m.deleted_at IS NULL "
    "ORDER BY m.created_at, m.id LIMIT 1"
)


def _lookup_membership(sub):
    """Ritorna (tenant_id, role) dell'appartenenza attiva più antica della persona, o None.

    Entrambi gli stati devono essere attivi: la persona (leva del titolare, art. 18)
    e l'appartenenza (leva dell'owner dell'account). A chiusura in caso di dubbio.
    """
    conn = _connect()
    try:
        rows = conn.run(_MEMBERSHIP_SQL, sub=sub)
    except Exception:
        # La connessione in cache può essere morta: azzera e riprova una volta.
        global _connection
        _connection = None
        conn = _connect()
        rows = conn.run(_MEMBERSHIP_SQL, sub=sub)
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
        # Fail-closed: persona senza appartenenza attiva → nessun claim (#02 10).
        _log("WARN", "pre-token-gen: nessuna appartenenza attiva (fail-closed)", user_id=sub)
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
