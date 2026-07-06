# ─────────────────────────────────────────────────────────────────────────────
# Query Logs Insights PRE-SALVATE (#08 30/32): il drill-down per-tenant e la
# correlazione di una richiesta sono "a query" (alta cardinalità nei log, mai
# dimensioni di metrica). Runbook: allarme → dashboard → query salvata con il
# tenant_id/correlation_id sospetto → triage.
# I campi MDC dei log JSON dei servizi sono annidati sotto `mdc.` (commons).
# ─────────────────────────────────────────────────────────────────────────────

locals {
  service_log_groups = [for s in var.services : s.log_group_name]
}

resource "aws_cloudwatch_query_definition" "per_tenant" {
  name = "appgrove-${var.env}/drill-down-per-tenant"

  log_group_names = local.service_log_groups

  query_string = <<-EOT
    # Sostituire TENANT_ID con l'identificativo (opaco) del tenant da indagare.
    fields @timestamp, level, mdc.app_id, mdc.user_id, mdc.correlation_id, message
    | filter mdc.tenant_id = 'TENANT_ID'
    | sort @timestamp desc
    | limit 200
  EOT
}

resource "aws_cloudwatch_query_definition" "per_correlation" {
  name = "appgrove-${var.env}/correlazione-richiesta"

  log_group_names = local.service_log_groups

  query_string = <<-EOT
    # Sostituire CORRELATION_ID con l'id propagato dall'edge (X-Correlation-Id).
    fields @timestamp, level, mdc.app_id, mdc.tenant_id, message
    | filter mdc.correlation_id = 'CORRELATION_ID'
    | sort @timestamp asc
    | limit 200
  EOT
}

resource "aws_cloudwatch_query_definition" "errori_per_servizio" {
  name = "appgrove-${var.env}/errori-per-servizio"

  log_group_names = local.service_log_groups

  query_string = <<-EOT
    # Triage error tracking (#08 19): raggruppa gli ERROR per servizio e logger.
    fields @timestamp
    | filter level = 'ERROR'
    | stats count(*) as errori by mdc.app_id, loggerName
    | sort errori desc
  EOT
}

resource "aws_cloudwatch_query_definition" "audit" {
  name = "appgrove-${var.env}/eventi-audit"

  log_group_names = local.service_log_groups

  query_string = <<-EOT
    # Eventi audit/sicurezza (#08 29): la copia lunga (12 mesi) è su S3/Glacier.
    fields @timestamp, mdc.app_id, mdc.tenant_id, mdc.user_id, message
    | filter mdc.log_type = 'audit'
    | sort @timestamp desc
    | limit 200
  EOT
}
