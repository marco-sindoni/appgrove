"""appgrove — Custom Message Lambda (UC 0018, #02 6, #06 26).

Cognito invoca questa funzione **prima di spedire** un'email di autenticazione e ne usa il
messaggio restituito. Serve a due cose: dare alle email il nostro testo e la nostra grafica, e
sceglierne la **lingua** (inglese/italiano).

Da dove viene la lingua
-----------------------
Da `request.clientMetadata`, cioè da un **parametro della chiamata** che il servizio auth fa a
Cognito (registrazione, rinvio codice, password dimenticata). NON dal database: farlo leggere qui
significherebbe mettere questa funzione in rete privata, con connessione a Postgres e avvio a
freddo, per scegliere fra due lingue. Cognito non memorizza quei valori — se un messaggio venisse
generato senza una nostra chiamata, la lingua mancherebbe e si ripiega sull'inglese.

Il vincolo che determina la forma del collegamento
--------------------------------------------------
Quando questa funzione viene invocata **il codice non esiste ancora**: Cognito passa il segnaposto
`{####}` (in `request.codeParameter`) e lo sostituisce col codice vero DOPO che abbiamo restituito
il messaggio. Quindi il segnaposto deve comparire nel messaggio **in chiaro**: non può finire dentro
una codifica. È il motivo per cui il collegamento porta due parametri distinti
(`?email=…&code={####}`) invece di un token unico, e per cui gli endpoint di verifica e
reimpostazione del servizio auth accettano anche quella forma.

I testi vengono da `shared/email-templates`, la **stessa** cartella resa dal servizio Java
(`EmailTemplates.java`): due passaggi di sostituzione identici. Terraform la inserisce
nell'archivio della funzione (`custom_message.tf`).
"""

import json
import os
import re
from urllib.parse import quote

APP_BASE_URL = os.environ["APP_BASE_URL"].rstrip("/")

# Cartella dei template: dentro l'archivio stanno accanto al sorgente; i test unitari la
# ridirigono sulla sorgente condivisa del repo.
TEMPLATES_DIR = os.environ.get(
    "EMAIL_TEMPLATES_DIR", os.path.join(os.path.dirname(__file__), "templates")
)

DEFAULT_LOCALE = "en"
SUPPORTED_LOCALES = ("en", "it")

# Buchi dell'impaginazione riempiti dalle stringhe della lingua (parità con EmailTemplates.java).
SLOTS = ("heading", "intro", "actionLabel", "fallback", "footer")

PLACEHOLDER = re.compile(r"\{\{([a-zA-Z][a-zA-Z0-9_]*)}}")

# Quale messaggio serve ogni evento di Cognito. Gli eventi non elencati (es. autenticazione con
# codice, verifica di un attributo aggiornato) non sono nostri flussi: li lasciamo ai testi di
# default invece di inventare un messaggio.
MESSAGE_BY_TRIGGER = {
    "CustomMessage_SignUp": ("verify", "/verify"),
    "CustomMessage_ResendCode": ("verify", "/verify"),
    "CustomMessage_ForgotPassword": ("reset", "/reset"),
}

_templates = None


def _read(name):
    with open(os.path.join(TEMPLATES_DIR, name), encoding="utf-8") as f:
        return f.read()


def _load():
    """Carica impaginazioni e cataloghi una volta sola (riuso tra invocazioni a caldo)."""
    global _templates
    if _templates is None:
        _templates = {
            "layout_html": _read("layout.html"),
            "layout_text": _read("layout.txt"),
            "catalogs": {loc: json.loads(_read(f"{loc}.json")) for loc in SUPPORTED_LOCALES},
        }
    return _templates


def normalize_locale(raw):
    """Riconduce una lingua qualsiasi a una supportata (parità con Locales.java)."""
    if not raw or not str(raw).strip():
        return DEFAULT_LOCALE
    language = re.split(r"[-_]", str(raw).strip().lower(), maxsplit=1)[0]
    return language if language in SUPPORTED_LOCALES else DEFAULT_LOCALE


def escape_html(value):
    """Escape dei valori inseriti nella versione grafica (stesse sostituzioni di EmailTemplates.java).

    Non è formalismo: il collegamento contiene `&` fra i parametri e senza escape arriverebbe rotto.
    """
    return (
        value.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace('"', "&quot;")
        .replace("'", "&#39;")
    )


def substitute(template, values):
    """Sostituisce i segnaposto noti; quelli ignoti restano intatti (li intercetta la guardia)."""
    return PLACEHOLDER.sub(lambda m: values.get(m.group(1), m.group(0)), template)


def render(locale, message_key, values):
    """Rende (oggetto, corpo grafico) del messaggio: stessi due passaggi del servizio Java."""
    templates = _load()
    catalog = templates["catalogs"][normalize_locale(locale)]
    message = catalog["messages"][message_key]

    slots = {"brand": catalog.get("brand", "appgrove")}
    for slot in SLOTS:
        slots[slot] = substitute(message.get(slot, ""), values)
    slots["actionUrl"] = values.get("actionUrl", "")

    subject = substitute(message.get("subject", ""), values)
    html = substitute(templates["layout_html"], {k: escape_html(v) for k, v in slots.items()})

    if PLACEHOLDER.search(subject) or PLACEHOLDER.search(html):
        raise ValueError(f"segnaposto non risolto nel messaggio '{message_key}' (lingua {locale})")
    return subject, html


def action_url(path, email, code_parameter):
    """Collegamento del messaggio: indirizzo codificato, segnaposto del codice **in chiaro**.

    Il segnaposto non va codificato per nessun motivo: Cognito lo sostituisce cercando esattamente
    `{####}` nel testo finale, e un `%7B%23%23%23%23%7D` resterebbe lì, con l'utente davanti a un
    collegamento che non verifica niente.
    """
    return f"{APP_BASE_URL}{path}?email={quote(email)}&code={code_parameter}"


def handler(event, context=None):
    trigger = event.get("triggerSource", "")
    mapping = MESSAGE_BY_TRIGGER.get(trigger)
    if not mapping:
        return event  # evento non nostro: restano i testi di default di Cognito

    request = event.get("request", {})
    locale = normalize_locale((request.get("clientMetadata") or {}).get("locale"))
    email = (request.get("userAttributes") or {}).get("email")
    code_parameter = request.get("codeParameter")

    if not email or not code_parameter:
        # Senza uno dei due non possiamo comporre un collegamento funzionante: meglio l'email di
        # default di Cognito (che il codice ce l'ha) che un messaggio nostro con un link rotto.
        print(json.dumps({"level": "WARN", "event": "custom_message_skipped",
                          "trigger": trigger, "reason": "email o codeParameter assenti"}))
        return event

    message_key, path = mapping
    try:
        subject, html = render(locale, message_key, {"actionUrl": action_url(path, email, code_parameter)})
    except Exception as exc:  # noqa: BLE001 — vedi commento
        # Fallire qui farebbe fallire la REGISTRAZIONE dell'utente, non solo l'email: il trigger è
        # sincrono. Si degrada quindi al messaggio di default di Cognito, che porta comunque il
        # codice, e si lascia una traccia esplicita nei log invece di un silenzio.
        print(json.dumps({"level": "ERROR", "event": "custom_message_render_failed",
                          "trigger": trigger, "locale": locale, "error": str(exc)}))
        return event

    event.setdefault("response", {})
    event["response"]["emailSubject"] = subject
    event["response"]["emailMessage"] = html

    # Log strutturato senza dati personali: mai l'indirizzo, mai il codice.
    print(json.dumps({"level": "INFO", "event": "custom_message_rendered",
                      "trigger": trigger, "message": message_key, "locale": locale}))
    return event
