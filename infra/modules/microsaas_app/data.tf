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

  # Coda di invalidazione degli entitlement (UC 0046): il core vi pubblica "i diritti
  # del tenant T sono cambiati", l'app marca la propria proiezione da rinfrescare.
  # Il core NON ne ha una propria (è il produttore, non un consumatore).
  entitlement_queue_name = "${var.shared.sqs_queue_prefix}entitlement-${var.app_id}"
  has_entitlement_queue  = !var.is_platform_core

  # Retention log esplicita (#08 26): mai "never expire".
  log_retention_days = var.env == "prod" ? 30 : 7

  sqs_arn_prefix = "arn:aws:sqs:${data.aws_region.current.region}:${data.aws_caller_identity.current.account_id}"
}

locals {
  # Azioni di allarme (#08 18): pieni in prod, silenziati in test (override
  # possibile via var.alarms_enabled, es. per prove mirate in test).
  alarms_enabled = coalesce(var.alarms_enabled, var.env == "prod")

  # Namespace/nome della metrica errori estratta dai log (#08 19).
  metric_namespace  = "Appgrove/${var.env}"
  error_metric_name = "log-errors-${var.app_id}"

  # Nome del cluster ECS dal suo ARN (…:cluster/<nome>): serve alle dimensioni
  # delle metriche AWS/ECS (widget e allarmi).
  ecs_cluster_name = element(split("/", var.shared.ecs_cluster_arn), 1)
}
