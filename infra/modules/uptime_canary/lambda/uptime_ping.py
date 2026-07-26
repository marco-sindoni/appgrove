"""Canary di uptime cross-region (UC 0007, #08 G22).

Gira in eu-central-1 (Francoforte), separato da eu-west-1: un EventBridge
schedulato invoca questa Lambda ogni minuto. La Lambda fa un GET del prodotto
pubblico (la SPA su CloudFront, Opzione A) e pubblica UNA metrica CloudWatch:

    namespace  appgrove/uptime
    metrica    Healthy   (1 = risposta 2xx/3xx, 0 = altro codice / errore / timeout)
    dimensione Target = <etichetta del bersaglio>

Un allarme su questa metrica (Minimum < 1 per N periodi, dato mancante = down)
notifica un topic SNS che vive ANCH'ESSO in eu-central-1: così l'avviso
sopravvive a un outage regionale di eu-west-1.

Nessuna dipendenza esterna: solo stdlib per il ping (`urllib`); `boto3` (già nel
runtime Lambda) solo per pubblicare la metrica. La logica pura (classificazione
e payload della metrica) è separata dagli effetti collaterali, così i test
girano offline senza rete né boto3.
"""

import os
import urllib.request

TARGET_URL = os.environ.get("CANARY_TARGET_URL", "https://app.appgrove.app/")
TARGET_LABEL = os.environ.get("CANARY_TARGET_LABEL", "app")
METRIC_NAMESPACE = os.environ.get("CANARY_METRIC_NAMESPACE", "appgrove/uptime")
METRIC_NAME = "Healthy"
TIMEOUT_SECONDS = float(os.environ.get("CANARY_TIMEOUT_SECONDS", "5"))


def classify(status):
    """Sano = risposta HTTP 2xx/3xx. Tutto il resto (4xx/5xx) = non sano."""
    return 1 if status is not None and 200 <= status < 400 else 0


def probe(url, timeout=TIMEOUT_SECONDS):
    """Esegue il GET e ritorna il codice HTTP; None su qualsiasi errore/timeout.

    Un GET semplice basta: cerchiamo raggiungibilità, non contenuto. Nessun dato
    personale è coinvolto (si pinga solo la home pubblica).
    """
    req = urllib.request.Request(url, method="GET", headers={"User-Agent": "appgrove-uptime-canary"})
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:  # noqa: S310 (URL fidato, da config)
            return resp.status
    except urllib.error.HTTPError as exc:
        # Il server ha risposto con un codice di errore: è un segnale valido.
        return exc.code
    except Exception:
        # DNS, connessione, TLS, timeout, ecc.: bersaglio irraggiungibile.
        return None


def evaluate(url, timeout=TIMEOUT_SECONDS):
    """1 se il bersaglio è sano, 0 altrimenti."""
    return classify(probe(url, timeout))


def build_metric_data(healthy):
    """Payload per cloudwatch:PutMetricData (puro: testabile senza boto3)."""
    return {
        "Namespace": METRIC_NAMESPACE,
        "MetricData": [
            {
                "MetricName": METRIC_NAME,
                "Dimensions": [{"Name": "Target", "Value": TARGET_LABEL}],
                "Value": float(healthy),
                "Unit": "None",
            }
        ],
    }


def _publish_metric(healthy):
    import boto3

    boto3.client("cloudwatch").put_metric_data(**build_metric_data(healthy))


def handler(event, context):
    healthy = evaluate(TARGET_URL)
    _publish_metric(healthy)
    # Log strutturato (una riga): utile nel triage, nessun dato personale.
    print(f'{{"log_type":"uptime_canary","target":"{TARGET_LABEL}","url":"{TARGET_URL}","healthy":{healthy}}}')
    return {"healthy": healthy, "target": TARGET_LABEL}
