data "aws_caller_identity" "current" {}

data "aws_region" "current" {}

locals {
  # Nome canonico delle risorse per-servizio (stessa convenzione di platform_shared).
  name = "appgrove-${var.env}-${var.app_id}"

  # Schema (= ruolo) Postgres: app_<app_id> di default, "platform" per il core (#05 11).
  db_schema = coalesce(var.db_schema, "app_${var.app_id}")

  # Nomi FISICI delle code = prefisso d'ambiente + nome logico (= GdprQueues/locale).
  export_queue_name = "${var.shared.sqs_queue_prefix}gdpr-export-${var.app_id}"
  purge_queue_name  = "${var.shared.sqs_queue_prefix}tenant-purge-${var.app_id}"

  # Retention log esplicita (#08 26): mai "never expire".
  log_retention_days = var.env == "prod" ? 30 : 7

  sqs_arn_prefix = "arn:aws:sqs:${data.aws_region.current.region}:${data.aws_caller_identity.current.account_id}"
}
