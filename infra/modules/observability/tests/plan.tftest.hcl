# ─────────────────────────────────────────────────────────────────────────────
# terraform test del modulo observability (#10 29): provider AWS FINTO
# (mock_provider), gira offline — lanciato da infra/scripts/check.
# ─────────────────────────────────────────────────────────────────────────────

mock_provider "aws" {}

variables {
  env = "test"

  services = [
    {
      app_id         = "demo"
      log_group_name = "/appgrove/test/demo"
      widgets        = []
    },
    {
      app_id         = "altra"
      log_group_name = "/appgrove/test/altra"
      widgets        = []
    },
  ]

  api_id                      = "api00000"
  aurora_cluster_identifier   = "appgrove-test"
  ecs_cluster_arn             = "arn:aws:ecs:eu-west-1:123456789012:cluster/appgrove-test"
  alarm_topic_critical_arn    = "arn:aws:sns:eu-west-1:123456789012:appgrove-test-alarms-critical"
  alarm_topic_warning_arn     = "arn:aws:sns:eu-west-1:123456789012:appgrove-test-alarms-warning"
  error_ingest_lambda_name    = "appgrove-test-error-ingest"
  error_ingest_log_group_name = "/aws/lambda/appgrove-test-error-ingest"
}

run "ambiente_test_silenziato" {
  command = plan

  # Dashboard UNICA per ambiente (#08 13/14): resta nel piano gratuito.
  assert {
    condition     = aws_cloudwatch_dashboard.this.dashboard_name == "appgrove-test"
    error_message = "La dashboard deve chiamarsi appgrove-<env> (una sola per ambiente, #08 14)."
  }

  # La sezione per-servizio è GENERATA (invariante #3): ogni app della lista
  # compare nella dashboard.
  assert {
    condition     = strcontains(aws_cloudwatch_dashboard.this.dashboard_body, "Servizio `demo`") && strcontains(aws_cloudwatch_dashboard.this.dashboard_body, "Servizio `altra`")
    error_message = "Ogni servizio della lista deve avere la sua sezione in dashboard (#08 13)."
  }

  # Allarmi silenziati in test (#08 18): niente notifiche, niente falsi allarmi
  # dallo scale-to-0/spegnimento notturno.
  assert {
    condition     = aws_cloudwatch_metric_alarm.aurora["acu"].actions_enabled == false
    error_message = "In test gli allarmi Aurora non devono notificare (#08 18)."
  }
  assert {
    condition     = aws_cloudwatch_event_rule.ecs_task_failed.state == "DISABLED"
    error_message = "In test la regola 'task che non parte' è spenta (#08 18: start/stop notturno e Spot = rumore)."
  }
  assert {
    condition     = aws_cloudwatch_metric_alarm.aurora["acu"].treat_missing_data == "notBreaching"
    error_message = "Cluster in pausa (scale-to-0) ≠ guasto: notBreaching (#08 18)."
  }

  # Drill-down per-tenant A QUERY (#08 30/32), mai widget per-tenant.
  assert {
    condition     = length(aws_cloudwatch_query_definition.per_tenant.log_group_names) == 2
    error_message = "La query per-tenant deve coprire i log group di TUTTI i servizi."
  }
}

run "ambiente_prod_pieno" {
  command = plan

  variables {
    env = "prod"
  }

  # Allarmi pieni in prod (#08 18) e recapito sui topic giusti (#08 15).
  assert {
    condition     = aws_cloudwatch_metric_alarm.aurora["connections"].actions_enabled == true
    error_message = "In prod gli allarmi devono notificare (#08 18)."
  }
  assert {
    condition     = aws_cloudwatch_event_rule.ecs_task_failed.state == "ENABLED"
    error_message = "In prod la regola 'task che non parte' è attiva (#08 16)."
  }
  assert {
    condition     = contains(aws_cloudwatch_metric_alarm.error_ingest_errors.alarm_actions, var.alarm_topic_warning_arn)
    error_message = "Gli errori della Lambda di ingest notificano sul topic warning."
  }
}
