# ─────────────────────────────────────────────────────────────────────────────
# Osservabilità per-servizio (UC 0006): il modulo GENERA filtri, allarmi e
# widget per ogni app (#08 13/15/16/19/28) — invariante #3: niente cablaggi a
# mano per servizio.
#   • metric filter: ERROR nei log JSON → metrica → allarme (#08 19);
#   • allarmi: 5xx e latenza p95 della route (metriche per-rotta dell'API GW),
#     DLQ non vuote (#08 16) — azioni piene in prod, silenziate in test (#08 18);
#   • subscription filter: SOLO gli eventi audit (log_type=audit) → Firehose →
#     archivio 12 mesi (#08 28/29): i log operativi NON si archiviano;
#   • widget per la dashboard d'ambiente (modulo observability).
# ─────────────────────────────────────────────────────────────────────────────

# ── Errori applicativi: log → metrica → allarme (#08 19) ─────────────────────

# I log sono JSON (quarkus-logging-json): livello top-level `level`, campi MDC
# annidati sotto `mdc` (tenant_id/app_id/user_id/correlation_id).
resource "aws_cloudwatch_log_metric_filter" "errors" {
  name           = "${local.name}-errors"
  log_group_name = aws_cloudwatch_log_group.this.name
  pattern        = "{ $.level = \"ERROR\" }"

  metric_transformation {
    namespace     = local.metric_namespace
    name          = local.error_metric_name
    value         = "1"
    default_value = "0"
    unit          = "Count"
  }
}

resource "aws_cloudwatch_metric_alarm" "errors" {
  alarm_name        = "${local.name}-errors"
  alarm_description = "ERROR applicativi ripetuti su ${var.app_id} (${var.env}): triage via Logs Insights (correlation_id/tenant_id)"

  namespace   = local.metric_namespace
  metric_name = local.error_metric_name
  statistic   = "Sum"

  # Con durata, non spike (#08 16): due finestre consecutive sopra soglia.
  period              = 300
  evaluation_periods  = 2
  datapoints_to_alarm = 2
  threshold           = 5
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching" # scale-to-0: nessun dato ≠ guasto (#08 18)

  actions_enabled = local.alarms_enabled
  alarm_actions   = [var.shared.alarm_topic_warning_arn]
  ok_actions      = [var.shared.alarm_topic_warning_arn]

  tags = {
    Name = "${local.name}-errors"
  }
}

# ── 5xx e latenza della route del servizio (#08 16) ──────────────────────────
# Metriche per-rotta dell'API HTTP (detailed metrics, stage di platform_shared).

resource "aws_cloudwatch_metric_alarm" "route_5xx" {
  alarm_name        = "${local.name}-5xx"
  alarm_description = "5xx sulla route ${aws_apigatewayv2_route.this.route_key} (${var.env})"

  namespace   = "AWS/ApiGateway"
  metric_name = "5xx"
  statistic   = "Sum"

  dimensions = {
    ApiId = var.shared.api_id
    Stage = "$default"
    Route = aws_apigatewayv2_route.this.route_key
  }

  period              = 300
  evaluation_periods  = 2
  datapoints_to_alarm = 2
  threshold           = 5
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching"

  actions_enabled = local.alarms_enabled
  alarm_actions   = [var.shared.alarm_topic_critical_arn]
  ok_actions      = [var.shared.alarm_topic_critical_arn]

  tags = {
    Name = "${local.name}-5xx"
  }
}

resource "aws_cloudwatch_metric_alarm" "route_latency_p95" {
  alarm_name        = "${local.name}-latency-p95"
  alarm_description = "Latenza p95 alta sulla route ${aws_apigatewayv2_route.this.route_key} (${var.env})"

  namespace          = "AWS/ApiGateway"
  metric_name        = "Latency"
  extended_statistic = "p95"

  dimensions = {
    ApiId = var.shared.api_id
    Stage = "$default"
    Route = aws_apigatewayv2_route.this.route_key
  }

  period              = 300
  evaluation_periods  = 3
  datapoints_to_alarm = 3
  threshold           = 2000 # ms
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"

  actions_enabled = local.alarms_enabled
  alarm_actions   = [var.shared.alarm_topic_warning_arn]
  ok_actions      = [var.shared.alarm_topic_warning_arn]

  tags = {
    Name = "${local.name}-latency-p95"
  }
}

# ── DLQ non vuote (#08 16): un messaggio in DLQ è SEMPRE azionabile ──────────

resource "aws_cloudwatch_metric_alarm" "dlq_not_empty" {
  for_each = {
    gdpr-export  = aws_sqs_queue.gdpr_export_dlq.name
    tenant-purge = aws_sqs_queue.tenant_purge_dlq.name
  }

  alarm_name        = "${local.name}-dlq-${each.key}"
  alarm_description = "Messaggi nella DLQ ${each.value} (${var.env}): elaborazione GDPR fallita dopo i retry"

  namespace   = "AWS/SQS"
  metric_name = "ApproximateNumberOfMessagesVisible"
  statistic   = "Maximum"

  dimensions = {
    QueueName = each.value
  }

  period              = 300
  evaluation_periods  = 1
  threshold           = 0
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"

  actions_enabled = local.alarms_enabled
  alarm_actions   = [var.shared.alarm_topic_critical_arn]
  ok_actions      = [var.shared.alarm_topic_critical_arn]

  tags = {
    Name = "${local.name}-dlq-${each.key}"
  }
}

# ── Archivio audit (#08 28/29) ───────────────────────────────────────────────
# SOLO gli eventi con log_type=audit (AuditLogger di services/commons) vanno a
# Firehose→S3→Glacier: separazione "logica" via pattern, la copia operativa
# scade con la retention breve del log group (minimizzazione GDPR).

resource "aws_cloudwatch_log_subscription_filter" "audit" {
  name            = "${local.name}-audit-archive"
  log_group_name  = aws_cloudwatch_log_group.this.name
  filter_pattern  = "{ $.mdc.log_type = \"audit\" }"
  destination_arn = var.shared.audit_firehose_arn
  role_arn        = var.shared.logs_to_firehose_role_arn
}

# ── Widget per la dashboard d'ambiente (#08 13) ──────────────────────────────
# Una riga di 3 widget per servizio; le posizioni le assegna il flusso
# automatico della dashboard (modulo observability, overview per-env).

locals {
  dashboard_widgets = [
    {
      type   = "metric"
      width  = 8
      height = 6
      properties = {
        title  = "${var.app_id} — CPU/Memoria (ECS)"
        region = data.aws_region.current.region
        metrics = [
          ["AWS/ECS", "CPUUtilization", "ClusterName", local.ecs_cluster_name, "ServiceName", aws_ecs_service.this.name],
          ["AWS/ECS", "MemoryUtilization", "ClusterName", local.ecs_cluster_name, "ServiceName", aws_ecs_service.this.name],
        ]
        stat   = "Average"
        period = 300
      }
    },
    {
      type   = "metric"
      width  = 8
      height = 6
      properties = {
        title  = "${var.app_id} — errori (log) e 5xx (route)"
        region = data.aws_region.current.region
        metrics = [
          [local.metric_namespace, local.error_metric_name],
          ["AWS/ApiGateway", "5xx", "ApiId", var.shared.api_id, "Stage", "$default", "Route", aws_apigatewayv2_route.this.route_key],
        ]
        stat   = "Sum"
        period = 300
      }
    },
    {
      type   = "metric"
      width  = 8
      height = 6
      properties = {
        title  = "${var.app_id} — code GDPR e DLQ"
        region = data.aws_region.current.region
        metrics = [
          ["AWS/SQS", "ApproximateNumberOfMessagesVisible", "QueueName", aws_sqs_queue.gdpr_export.name],
          ["AWS/SQS", "ApproximateNumberOfMessagesVisible", "QueueName", aws_sqs_queue.tenant_purge.name],
          ["AWS/SQS", "ApproximateNumberOfMessagesVisible", "QueueName", aws_sqs_queue.gdpr_export_dlq.name],
          ["AWS/SQS", "ApproximateNumberOfMessagesVisible", "QueueName", aws_sqs_queue.tenant_purge_dlq.name],
        ]
        stat   = "Maximum"
        period = 300
      }
    },
  ]
}
