# ─────────────────────────────────────────────────────────────────────────────
# Allarmi sulle risorse condivise (#08 16, set minimo anti-rumore: con durata,
# non spike). Azioni piene in prod, silenziate in test (#08 18): gli allarmi
# esistono ovunque (visibili in console) ma notificano solo dove ha senso.
# Gli allarmi per-servizio (ERROR nei log, 5xx/latenza della route, DLQ) li
# genera microsaas_app.
# ─────────────────────────────────────────────────────────────────────────────

locals {
  alarms_enabled = coalesce(var.alarms_enabled, var.env == "prod")
  name_prefix    = "appgrove-${var.env}"
}

# ── Aurora (#08 16: connessioni/ACU/storage) ─────────────────────────────────

resource "aws_cloudwatch_metric_alarm" "aurora" {
  for_each = {
    acu = {
      metric      = "ACUUtilization"
      statistic   = "Average"
      threshold   = 90
      periods     = 3
      description = "Aurora vicino al tetto di capacità (ACU): valutare max_capacity"
    }
    connections = {
      metric      = "DatabaseConnections"
      statistic   = "Maximum"
      threshold   = 80
      periods     = 2
      description = "Connessioni Aurora anomale (i servizi usano pool piccoli: possibile leak)"
    }
    storage = {
      metric      = "VolumeBytesUsed"
      statistic   = "Maximum"
      threshold   = 21474836480 # 20 GiB: ben oltre il fisiologico attuale, segnala crescita anomala
      periods     = 1
      description = "Storage Aurora oltre i 20 GiB: verificare crescita dati"
    }
  }

  alarm_name        = "${local.name_prefix}-aurora-${each.key}"
  alarm_description = "${each.value.description} (${var.env})"

  namespace   = "AWS/RDS"
  metric_name = each.value.metric
  statistic   = each.value.statistic

  dimensions = {
    DBClusterIdentifier = var.aurora_cluster_identifier
  }

  period              = 300
  evaluation_periods  = each.value.periods
  datapoints_to_alarm = each.value.periods
  threshold           = each.value.threshold
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching" # cluster in pausa (scale-to-0) ≠ guasto

  actions_enabled = local.alarms_enabled
  alarm_actions   = [var.alarm_topic_warning_arn]
  ok_actions      = [var.alarm_topic_warning_arn]

  tags = {
    Name = "${local.name_prefix}-aurora-${each.key}"
  }
}

# ── Ingest errori frontend: la Lambda stessa non deve fallire ────────────────

resource "aws_cloudwatch_metric_alarm" "error_ingest_errors" {
  alarm_name        = "${local.name_prefix}-error-ingest-errors"
  alarm_description = "Errori della Lambda di ingest errori frontend (${var.env}): si stanno perdendo segnalazioni"

  namespace   = "AWS/Lambda"
  metric_name = "Errors"
  statistic   = "Sum"

  dimensions = {
    FunctionName = var.error_ingest_lambda_name
  }

  period              = 300
  evaluation_periods  = 1
  threshold           = 0
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"

  actions_enabled = local.alarms_enabled
  alarm_actions   = [var.alarm_topic_warning_arn]
  ok_actions      = [var.alarm_topic_warning_arn]

  tags = {
    Name = "${local.name_prefix}-error-ingest-errors"
  }
}

# ── Task ECS che non parte (#08 16) ──────────────────────────────────────────
# Gli eventi ECS viaggiano sul bus DEFAULT (non sul bus applicativo): regola →
# SNS critical. In test la regola è spenta (#08 18): il ciclo start/stop
# notturno e lo Spot genererebbero solo rumore.

resource "aws_cloudwatch_event_rule" "ecs_task_failed" {
  name        = "${local.name_prefix}-ecs-task-failed"
  description = "Task ECS che non riesce a partire nel cluster ${local.name_prefix} (UC 0006, #08 16)"
  state       = local.alarms_enabled ? "ENABLED" : "DISABLED"

  event_pattern = jsonencode({
    source      = ["aws.ecs"]
    detail-type = ["ECS Task State Change"]
    detail = {
      clusterArn = [var.ecs_cluster_arn]
      lastStatus = ["STOPPED"]
      stopCode   = ["TaskFailedToStart"]
    }
  })

  tags = {
    Name = "${local.name_prefix}-ecs-task-failed"
  }
}

resource "aws_cloudwatch_event_target" "ecs_task_failed_sns" {
  rule = aws_cloudwatch_event_rule.ecs_task_failed.name
  arn  = var.alarm_topic_critical_arn
}

# ── BFF auth: fallimenti della Lambda = utenti che non entrano (UC 0015) ─────
# La metrica Errors conta solo i crash dell'invocazione (le 401/400 applicative
# sono risposte regolari): qualunque valore > 0 merita attenzione.

resource "aws_cloudwatch_metric_alarm" "auth_lambda_errors" {
  alarm_name        = "${local.name_prefix}-auth-errors"
  alarm_description = "Errori della Lambda BFF auth (${var.env}): login/signup potrebbero essere fuori uso"

  namespace   = "AWS/Lambda"
  metric_name = "Errors"
  statistic   = "Sum"

  dimensions = {
    FunctionName = var.auth_lambda_name
  }

  period              = 300
  evaluation_periods  = 1
  threshold           = 0
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"

  actions_enabled = local.alarms_enabled
  alarm_actions   = [var.alarm_topic_warning_arn]
  ok_actions      = [var.alarm_topic_warning_arn]

  tags = {
    Name = "${local.name_prefix}-auth-errors"
  }
}
