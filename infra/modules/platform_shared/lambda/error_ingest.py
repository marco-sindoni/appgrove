"""Ingest degli errori JavaScript del frontend (UC 0006, #08 23).

Riceve dal reporter delle SPA un POST fire-and-forget con il contesto
dell'errore e lo riscrive come UNA riga JSON su stdout → CloudWatch Logs
(log group della Lambda, retention breve: nessun archivio lungo).

Regole:
  • SOLO errori, nessun tracking comportamentale (base giuridica: legittimo
    interesse); identificativi opachi (user_id/tenant_id = UUID, mai email);
  • payload non fidato: allowlist dei campi, limiti di taglia, mai eco del
    body in risposta; il Content-Type può essere text/plain (simple request:
    il CORS dell'API condivisa è un punto differito di UC 0004);
  • fail-quiet: risposta 202 anche su payload scartato (il client non deve
    ritentare né ricevere dettagli).
"""

import base64
import json

MAX_BODY_BYTES = 32 * 1024
MAX_STACK_CHARS = 8 * 1024
MAX_FIELD_CHARS = 512

# Campi accettati dal reporter (frontend/packages/error-reporter): tutto il
# resto viene ignorato. Niente IP, niente user agent: minimizzazione (#13).
ALLOWED_FIELDS = {
    "app_id",
    "route",
    "build_sha",
    "message",
    "stack",
    "source",
    "line",
    "col",
    "user_id",
    "tenant_id",
    "ts",
}

ACCEPTED = {"statusCode": 202, "body": ""}


def handler(event, _context):
    body = event.get("body") or ""
    if event.get("isBase64Encoded"):
        try:
            body = base64.b64decode(body).decode("utf-8", errors="replace")
        except Exception:
            return ACCEPTED

    if len(body.encode("utf-8", errors="replace")) > MAX_BODY_BYTES:
        return ACCEPTED

    try:
        payload = json.loads(body)
    except (ValueError, TypeError):
        return ACCEPTED
    if not isinstance(payload, dict):
        return ACCEPTED

    record = {"log_type": "frontend_error"}
    for key in ALLOWED_FIELDS:
        value = payload.get(key)
        if value is None:
            continue
        if key in ("line", "col"):
            # bool è sottoclasse di int in Python: "line": true va scartato, non loggato
            if isinstance(value, int) and not isinstance(value, bool):
                record[key] = value
            continue
        if not isinstance(value, str):
            continue
        limit = MAX_STACK_CHARS if key == "stack" else MAX_FIELD_CHARS
        record[key] = value[:limit]

    # Senza app_id e message non c'è niente da correlare: si scarta in silenzio.
    if "app_id" not in record or "message" not in record:
        return ACCEPTED

    print(json.dumps(record, ensure_ascii=False))
    return ACCEPTED
