"""appgrove — Pre-Token-Generation Lambda (UC 0016, #02 9/10/11).

Cognito invoca questa funzione a OGNI emissione token. Legge l'appartenenza
della persona dallo schema `platform` (via RDS Proxy) e inietta i claim
`tenant_id` (stringa) + `roles` (array) nell'ACCESS token — il meccanismo che
rende vera l'invariante "tenant_id solo dal JWT verificato".

Dopo UC 0116 la lettura è identità (`platform.identity`) ⋈ appartenenze
(`platform.membership`): la persona è unica sulla piattaforma, le sue
appartenenze possono essere più di una. Con UC 0117 quale appartenenza
diventa l'account della sessione non è più un ripiego («la più antica») ma
una regola scritta, applicata al riferimento conservato in
`identity.active_membership_id`:

  | Appartenenze attive | Valore conservato          | Esito                  |
  |---------------------|----------------------------|------------------------|
  | nessuna             | qualunque                  | nessun claim           |
  | una sola            | qualunque, anche assente   | quella (ignora il valore) |
  | più di una          | corrisponde a una di esse  | quella                 |
  | più di una          | assente o non corrisponde  | nessun claim           |

**Il valore conservato NON è creduto**: vale solo se corrisponde a
un'appartenenza attiva trovata adesso. È la riga che impedisce che una
manomissione di quella colonna diventi un varco fra due aziende — l'invariante
«account solo dal token verificato» resta intatta, perché cambia la funzione
che CALCOLA il claim, non chi se ne fida.

Con UC 0099 il claim `roles` porta SOLO il ruolo di piattaforma (`owner` o
`member`, più `platform-admin` per chi amministra la piattaforma): niente ruoli
per applicazione, che si leggono dal core (`/me/app-access`) attraverso il varco
condiviso dei servizi. Il valore `admin`, ritirato da UC 0098, viene convertito
in `member` mentre si compone il claim (vedi `_claim_role`): è la tolleranza dei
dati non ancora convertiti, e la ritira UC 0113.

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
`token_use=access`), stessa regola `platform-admin` (allow-list di `sub`) e
stessa tabella di casi per la scelta dell'account attivo, così i servizi
hanno un unico percorso di codice in locale e in cloud. La regola è attuata
DUE volte — qui in Python e in Java
(services/commons/src/main/java/app/appgrove/commons/membership/ActiveAccount.java,
usata dal provider locale) — perché questa funzione gira dentro
l'infrastruttura e non può chiamare il codice dei servizi. Due attuazioni
della stessa regola sono un debito: si tiene onesto con la stessa tabella di
casi eseguita dai collaudi di entrambe (`test_handler.py` e
`ActiveAccountTest`). Se una cambia, l'altra cambia con essa.

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


# Identità ⋈ appartenenze ATTIVE + riferimento all'account attivo (UC 0116/0117).
# PARITÀ con il fornitore locale
# (services/auth/src/main/java/app/appgrove/auth/local/UserDirectory.java): stessa
# condizione, stesso ordine, stessa tabella di casi. Si leggono TUTTE le appartenenze
# attive (non più `LIMIT 1`): è la scelta dell'account attivo a stabilire quale vale,
# e per riverificarla servono tutte.
_MEMBERSHIP_SQL = (
    "SELECT m.id, m.tenant_id, m.role, i.active_membership_id "
    "FROM platform.identity i "
    "JOIN platform.membership m ON m.identity_id = i.id "
    "WHERE i.cognito_sub = :sub "
    "AND i.status = 'active' AND i.deleted_at IS NULL "
    "AND m.status = 'active' AND m.deleted_at IS NULL "
    "ORDER BY m.created_at, m.id"
)

# Esiti della scelta dell'account attivo: tre, mai un quarto implicito. Sono gli stessi
# tre di ActiveAccount.Choice in Java (None / Chosen / MustChoose).
CHOICE_NONE = "none"
CHOICE_CHOSEN = "chosen"
CHOICE_MUST_CHOOSE = "must_choose"


def choose_active_account(memberships, stored_membership_id):
    """La tabella dei casi di UC 0117 §4.2. Funzione PURA: nessun accesso alla banca dati.

    `memberships` è la lista delle appartenenze ATTIVE, in ordine deterministico
    (anzianità), ognuna come (membership_id, tenant_id, role). `stored_membership_id`
    è il valore conservato, eventualmente None o non più valido.

    Ritorna (esito, appartenenza) dove l'appartenenza è valorizzata solo con
    CHOICE_CHOSEN. Gemella di ActiveAccount.choose (Java): se una cambia, l'altra
    cambia con essa.
    """
    if not memberships:
        return (CHOICE_NONE, None)
    if len(memberships) == 1:
        # Il caso di tutti gli utenti di oggi: il valore conservato è IRRILEVANTE, anche
        # se manomesso. Deve restare a costo zero e senza modi di sbagliare.
        return (CHOICE_CHOSEN, memberships[0])
    if stored_membership_id is not None:
        for membership in memberships:
            if str(membership[0]) == str(stored_membership_id):
                return (CHOICE_CHOSEN, membership)
    # Più appartenenze e nessuna scelta valida: nessun claim. Nessuno può decidere al
    # posto della persona per conto di chi sta agendo.
    return (CHOICE_MUST_CHOOSE, None)


def _lookup_active_account(sub):
    """Ritorna (esito, (tenant_id, role)) per la persona, riverificando le appartenenze.

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
    memberships = [(r[0], r[1], r[2]) for r in rows]
    stored = rows[0][3] if rows else None
    outcome, chosen = choose_active_account(memberships, stored)
    if outcome != CHOICE_CHOSEN:
        return (outcome, None)
    return (outcome, (chosen[1], chosen[2]))


# Valore RITIRATO dal ruolo di piattaforma (UC 0098): l'appartenenza ammette due soli
# valori, `owner` e `member`. Un ambiente i cui dati non sono ancora stati convertiti
# (la conversione è di UC 0113) può però avere righe che valgono ancora `admin`.
_RETIRED_PLATFORM_ROLE = "admin"


def _claim_role(role):
    """Il ruolo di piattaforma come va scritto nel claim (UC 0099): `admin` → `member`.

    Si converte QUI, nel momento in cui si compone il claim, e non nel modello: una
    persona di un ambiente non convertito accede così con il potere MINORE invece che
    con NESSUN potere — che è il comportamento giusto, perché il valore vecchio
    significava «membro con poteri in più», non «persona sconosciuta».

    DA TOGLIERE con UC 0113, insieme alla conversione dei dati reali e al ritiro della
    tolleranza dei token già emessi (`Roles.ADMIN` nei servizi): a quel punto nessuna
    riga vale più `admin` e questa funzione diventa codice morto. Prima di quel giorno,
    toglierla chiude fuori qualcuno.

    PARITÀ con il gemello Java
    (services/commons/src/main/java/app/appgrove/commons/membership/PlatformRoles.java,
    usato dal fornitore di identità locale): stessa regola, attuata due volte perché
    questa funzione gira dentro l'infrastruttura e non può chiamare il codice dei
    servizi. Se una cambia, l'altra cambia con essa — altrimenti i collaudi locali
    dicono una cosa e l'ambiente reale un'altra.
    """
    return "member" if role == _RETIRED_PLATFORM_ROLE else role


def _roles_for(sub, role):
    """Ruoli del claim: ruolo di piattaforma (normalizzato) + platform-admin se il `sub`
    è in allow-list (stessa regola del provider locale, TokenService.groupsFor).

    Il claim NON porta i ruoli per applicazione, ed è la decisione centrale di UC 0099:
    nel token un cambio di ruolo avrebbe effetto solo al rinnovo, e un account con dieci
    applicazioni gonfierebbe ogni richiesta. Quei ruoli si leggono dal core
    (`/me/app-access`) attraverso il varco condiviso dei servizi.
    """
    roles = [_claim_role(role)]
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

    outcome, membership = _lookup_active_account(sub)
    if outcome == CHOICE_NONE:
        # Fail-closed: persona senza appartenenza attiva → nessun claim (#02 10).
        _log("WARN", "pre-token-gen: nessuna appartenenza attiva (fail-closed)", user_id=sub)
        return event
    if outcome == CHOICE_MUST_CHOOSE:
        # Fail-closed: più appartenenze attive e nessun account attivo valido (UC 0117).
        # Distinto dal caso precedente perché la persona PUÒ lavorare: manca solo la scelta.
        _log(
            "WARN",
            "pre-token-gen: più appartenenze attive e nessun account attivo scelto (fail-closed)",
            user_id=sub,
        )
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
