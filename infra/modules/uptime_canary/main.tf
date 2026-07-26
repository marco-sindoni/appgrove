# ─────────────────────────────────────────────────────────────────────────────
# Canary di uptime cross-region (UC 0007, #08 G22). Tutte le risorse vivono nella
# regione del provider passato dall'istanza (envs/prod → eu-central-1), separata
# da eu-west-1: EventBridge schedulato → Lambda di ping → metrica → allarme → SNS.
# Il topic SNS è QUI (stessa regione dell'allarme): un allarme CloudWatch può
# notificare solo un topic della propria regione — è ciò che rende l'avviso
# resiliente a un outage di eu-west-1.
# ─────────────────────────────────────────────────────────────────────────────

locals {
  name_prefix = "appgrove-${var.env}-uptime"
}

data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

# ── Lambda di ping ───────────────────────────────────────────────────────────

data "archive_file" "ping" {
  type        = "zip"
  source_file = "${path.module}/lambda/uptime_ping.py"
  output_path = "${path.module}/lambda/uptime_ping.zip"
}

resource "aws_iam_role" "ping" {
  name = local.name_prefix

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "lambda.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })

  tags = { Name = local.name_prefix }
}

resource "aws_iam_role_policy_attachment" "ping_logs" {
  role       = aws_iam_role.ping.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

# Least-privilege: pubblicare la sola metrica di uptime (namespace ristretto via
# condition; PutMetricData non supporta resource-level, si limita col namespace).
resource "aws_iam_role_policy" "ping_metric" {
  name = "put-uptime-metric"
  role = aws_iam_role.ping.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid      = "PutUptimeMetric"
      Effect   = "Allow"
      Action   = "cloudwatch:PutMetricData"
      Resource = "*"
      Condition = {
        StringEquals = { "cloudwatch:namespace" = var.metric_namespace }
      }
    }]
  })
}

resource "aws_cloudwatch_log_group" "ping" {
  name              = "/aws/lambda/${local.name_prefix}"
  retention_in_days = var.log_retention_days

  #checkov:skip=CKV_AWS_158:Cifratura at rest di default (chiavi gestite CloudWatch); CMK solo se servirà (#06 §20bis)
  #checkov:skip=CKV_AWS_338:Retention breve by-design (cost-min): non sono log di audit

  tags = { Name = local.name_prefix }
}

resource "aws_lambda_function" "ping" {
  function_name = local.name_prefix
  description   = "Canary di uptime: ping ${var.target_url} → metrica ${var.metric_namespace}/Healthy (UC 0007)"
  role          = aws_iam_role.ping.arn

  filename         = data.archive_file.ping.output_path
  source_code_hash = data.archive_file.ping.output_base64sha256
  handler          = "uptime_ping.handler"
  runtime          = "python3.13"
  timeout          = 10
  memory_size      = 128

  # Un solo ping alla volta: la schedule è seriale, nessun bisogno di concorrenza.
  reserved_concurrent_executions = 1

  environment {
    variables = {
      CANARY_TARGET_URL       = var.target_url
      CANARY_TARGET_LABEL     = var.target_label
      CANARY_METRIC_NAMESPACE = var.metric_namespace
    }
  }

  #checkov:skip=CKV_AWS_117:Fuori VPC by-design: pinga un URL PUBBLICO da un vantage point esterno; dentro la VPC vanificherebbe lo scopo
  #checkov:skip=CKV_AWS_50:X-Ray non necessario per un canary a singola chiamata (cost-min)
  #checkov:skip=CKV_AWS_116:Niente DLQ: invocazione schedulata, un ping perso è irrilevante (la metrica manca → l'allarme lo tratta come down)
  #checkov:skip=CKV_AWS_173:Env var non sensibili (URL pubblico, etichette): niente da cifrare
  #checkov:skip=CKV_AWS_272:Code signing oltre lo scopo: sorgente versionato nel repo, deploy solo via Terraform

  depends_on = [aws_cloudwatch_log_group.ping]

  tags = { Name = local.name_prefix }
}

# ── Schedulazione (EventBridge) ──────────────────────────────────────────────

resource "aws_cloudwatch_event_rule" "schedule" {
  name                = local.name_prefix
  description         = "Canary di uptime: pinga ${var.target_url} ogni intervallo (UC 0007)"
  schedule_expression = var.schedule_expression

  tags = { Name = local.name_prefix }
}

resource "aws_cloudwatch_event_target" "schedule" {
  rule = aws_cloudwatch_event_rule.schedule.name
  arn  = aws_lambda_function.ping.arn
}

resource "aws_lambda_permission" "schedule" {
  statement_id  = "AllowEventBridgeInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.ping.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.schedule.arn
}

# ── Notifica (SNS in questa regione) ─────────────────────────────────────────

resource "aws_sns_topic" "alarms" {
  name = "${local.name_prefix}-alarms"

  #checkov:skip=CKV_AWS_26:Solo metadati di allarme (nessun dato personale); CMK non giustificata (#06 §20bis, cost-min)

  tags = { Name = "${local.name_prefix}-alarms" }
}

resource "aws_sns_topic_subscription" "alarms_email" {
  topic_arn = aws_sns_topic.alarms.arn
  protocol  = "email"
  endpoint  = var.alert_email # la subscription email va CONFERMATA dal destinatario (mail AWS)
}

# ── Allarme di uptime ────────────────────────────────────────────────────────
# Down = Minimum(Healthy) < 1 per N periodi da 60s. Dato mancante = breaching:
# se la Lambda non riporta (region/schedule guasti) lo trattiamo come down, non
# come "tutto ok" — è il comportamento fail-safe che ci si aspetta da un canary.

resource "aws_cloudwatch_metric_alarm" "uptime" {
  alarm_name        = "${local.name_prefix}-${var.target_label}"
  alarm_description = "Uptime canary: ${var.target_url} non raggiungibile dal vantage point eu-central-1 (UC 0007, #08 G22)"

  namespace   = var.metric_namespace
  metric_name = "Healthy"
  statistic   = "Minimum"

  dimensions = {
    Target = var.target_label
  }

  period              = 60
  evaluation_periods  = var.evaluation_periods
  datapoints_to_alarm = var.evaluation_periods
  threshold           = 1
  comparison_operator = "LessThanThreshold"
  treat_missing_data  = "breaching"

  alarm_actions = [aws_sns_topic.alarms.arn]
  ok_actions    = [aws_sns_topic.alarms.arn]

  tags = { Name = "${local.name_prefix}-${var.target_label}" }
}
