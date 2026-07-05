# ─────────────────────────────────────────────────────────────────────────────
# Bus EventBridge dedicato dell'ambiente (#06 19): riceve `tenant.offboarded`
# e lo instrada alle code SQS di purge per-servizio. Le REGOLE e i TARGET
# per-servizio (una coda per app) li crea il modulo `microsaas_app` (UC 0004);
# il flusso di offboarding che pubblica l'evento è di UC 0032/0035.
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_cloudwatch_event_bus" "this" {
  name = "appgrove-${var.env}"

  tags = {
    Name = "appgrove-${var.env}"
  }
}
