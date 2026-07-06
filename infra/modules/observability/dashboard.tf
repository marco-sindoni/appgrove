# ─────────────────────────────────────────────────────────────────────────────
# Dashboard UNICA per ambiente (#08 13/14): overview + una riga per servizio
# (widget generati da microsaas_app) + sezione auth/sicurezza. Una sola
# dashboard per-env resta nel piano gratuito (≤3 per account) finché possibile.
# Niente x/y: il layout usa il flusso automatico di CloudWatch (le intestazioni
# markdown a larghezza piena forzano l'a-capo di riga).
# Drill-down per-tenant = query Logs Insights salvate (queries.tf), NON widget
# per-tenant (#08 32: regola dei due piani).
# ─────────────────────────────────────────────────────────────────────────────

locals {
  header = [{
    type   = "text"
    width  = 24
    height = 1
    properties = {
      markdown = "# appgrove ${var.env} — overview (drill-down per-tenant: query salvate `appgrove-${var.env}/…` in Logs Insights)"
    }
  }]

  overview_widgets = [
    {
      type   = "metric"
      width  = 8
      height = 6
      properties = {
        title  = "API — richieste ed errori"
        region = data.aws_region.current.region
        metrics = [
          ["AWS/ApiGateway", "Count", "ApiId", var.api_id],
          ["AWS/ApiGateway", "4xx", "ApiId", var.api_id],
          ["AWS/ApiGateway", "5xx", "ApiId", var.api_id],
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
        title  = "API — latenza p95"
        region = data.aws_region.current.region
        metrics = [
          ["AWS/ApiGateway", "Latency", "ApiId", var.api_id, { stat = "p95" }],
          ["AWS/ApiGateway", "IntegrationLatency", "ApiId", var.api_id, { stat = "p95" }],
        ]
        period = 300
      }
    },
    {
      type   = "metric"
      width  = 8
      height = 6
      properties = {
        title  = "Aurora — capacità e connessioni"
        region = data.aws_region.current.region
        metrics = [
          ["AWS/RDS", "ACUUtilization", "DBClusterIdentifier", var.aurora_cluster_identifier],
          ["AWS/RDS", "ServerlessDatabaseCapacity", "DBClusterIdentifier", var.aurora_cluster_identifier],
          ["AWS/RDS", "DatabaseConnections", "DBClusterIdentifier", var.aurora_cluster_identifier],
        ]
        stat   = "Average"
        period = 300
      }
    },
  ]

  service_headers = {
    for s in var.services : s.app_id => [{
      type   = "text"
      width  = 24
      height = 1
      properties = {
        markdown = "## Servizio `${s.app_id}` — log group `${s.log_group_name}`"
      }
    }]
  }

  auth_security_widgets = [
    {
      type   = "text"
      width  = 24
      height = 1
      properties = {
        markdown = "## Auth & sicurezza — login/2FA/lockout arrivano con i flussi auth cloud (UC 0014); qui gli errori frontend (#08 23)"
      }
    },
    {
      type   = "metric"
      width  = 8
      height = 6
      properties = {
        title  = "Ingest errori frontend — invocazioni/errori"
        region = data.aws_region.current.region
        metrics = [
          ["AWS/Lambda", "Invocations", "FunctionName", var.error_ingest_lambda_name],
          ["AWS/Lambda", "Errors", "FunctionName", var.error_ingest_lambda_name],
          ["AWS/Lambda", "Throttles", "FunctionName", var.error_ingest_lambda_name],
        ]
        stat   = "Sum"
        period = 300
      }
    },
    {
      type   = "log"
      width  = 16
      height = 6
      properties = {
        title  = "Ultimi errori frontend"
        region = data.aws_region.current.region
        query  = "SOURCE '${var.error_ingest_log_group_name}' | fields @timestamp, app_id, route, message, build_sha | filter log_type = 'frontend_error' | sort @timestamp desc | limit 20"
        view   = "table"
      }
    },
  ]
}

resource "aws_cloudwatch_dashboard" "this" {
  dashboard_name = "appgrove-${var.env}"

  dashboard_body = jsonencode({
    widgets = concat(
      local.header,
      local.overview_widgets,
      flatten([for s in var.services : concat(local.service_headers[s.app_id], s.widgets)]),
      local.auth_security_widgets,
    )
  })
}
