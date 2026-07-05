# ─────────────────────────────────────────────────────────────────────────────
# Coda condivisa dei risultati di export GDPR (UC 0032, #13 D22): i worker
# per-app vi pubblicano l'esito dei frammenti, il core la consuma e aggrega.
# Le code PER-APP (gdpr-export-<app_id>, tenant-purge-<app_id>) le crea il
# modulo `microsaas_app` (UC 0004).
#
# Naming: il nome LOGICO (`gdpr-export-results`, = GdprQueues in commons e
# dev/elasticmq.conf) è preceduto dal prefisso `appgrove-<env>-` perché test e
# prod convivono nello stesso account/regione e i nomi SQS devono essere unici;
# i servizi ricevono il prefisso a runtime (env var APPGROVE_SQS_QUEUE_PREFIX,
# vuoto in locale).
# ─────────────────────────────────────────────────────────────────────────────

locals {
  # Prefisso dei nomi fisici delle code dell'ambiente (vedi testata).
  sqs_queue_prefix = "appgrove-${var.env}-"
}

resource "aws_sqs_queue" "gdpr_export_results_dlq" {
  name                    = "${local.sqs_queue_prefix}gdpr-export-results-dlq"
  sqs_managed_sse_enabled = true # cifratura at rest (#06 §20bis), chiavi gestite SQS

  tags = {
    Name = "${local.sqs_queue_prefix}gdpr-export-results-dlq"
  }
}

resource "aws_sqs_queue" "gdpr_export_results" {
  name                    = "${local.sqs_queue_prefix}gdpr-export-results"
  sqs_managed_sse_enabled = true

  # Come in locale (dev/elasticmq.conf): 5 tentativi, poi DLQ.
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.gdpr_export_results_dlq.arn
    maxReceiveCount     = 5
  })

  tags = {
    Name = "${local.sqs_queue_prefix}gdpr-export-results"
  }
}
