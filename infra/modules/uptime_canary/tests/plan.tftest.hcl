# ─────────────────────────────────────────────────────────────────────────────
# terraform test del modulo uptime_canary (UC 0007): provider AWS FINTO
# (mock_provider), gira offline — lanciato da infra/scripts/check.
# ─────────────────────────────────────────────────────────────────────────────

mock_provider "aws" {}

variables {
  env         = "prod"
  alert_email = "ops@esempio.test"
}

run "canary_cablato_correttamente" {
  command = plan

  # Ping schedulato ogni minuto sulla SPA pubblica (Opzione A, UC 0007).
  assert {
    condition     = aws_cloudwatch_event_rule.schedule.schedule_expression == "rate(1 minute)"
    error_message = "La schedule del canary deve essere rate(1 minute)."
  }
  assert {
    condition     = aws_lambda_function.ping.environment[0].variables["CANARY_TARGET_URL"] == "https://app.appgrove.app/"
    error_message = "Il bersaglio del ping deve essere la SPA pubblica app.appgrove.app (Opzione A)."
  }
  assert {
    condition     = aws_lambda_function.ping.runtime == "python3.13"
    error_message = "La Lambda di ping gira su python3.13 (coerenza con le altre Lambda)."
  }

  # Metrica e allarme: Healthy < 1, dato mancante = down (fail-safe).
  assert {
    condition     = aws_cloudwatch_metric_alarm.uptime.metric_name == "Healthy" && aws_cloudwatch_metric_alarm.uptime.namespace == "appgrove/uptime"
    error_message = "L'allarme deve poggiare sulla metrica appgrove/uptime/Healthy."
  }
  assert {
    condition     = aws_cloudwatch_metric_alarm.uptime.comparison_operator == "LessThanThreshold" && aws_cloudwatch_metric_alarm.uptime.threshold == 1
    error_message = "L'allarme scatta quando Healthy < 1."
  }
  assert {
    condition     = aws_cloudwatch_metric_alarm.uptime.treat_missing_data == "breaching"
    error_message = "Dato mancante = down (fail-safe): un canary muto non deve sembrare sano."
  }
  assert {
    condition     = aws_cloudwatch_metric_alarm.uptime.evaluation_periods == 3
    error_message = "Anti-rumore: 3 periodi consecutivi da 60s prima di allertare."
  }

  # SNS dedicato in questa regione (l'istanza lo colloca in eu-central-1).
  assert {
    condition     = aws_sns_topic.alarms.name == "appgrove-prod-uptime-alarms"
    error_message = "Il topic SNS del canary deve chiamarsi appgrove-<env>-uptime-alarms."
  }
  # L'ARN del topic non è noto a plan col provider finto: si verifica che l'allarme
  # notifichi esattamente un'azione sia in ALARM sia nel rientro OK.
  assert {
    condition     = length(aws_cloudwatch_metric_alarm.uptime.alarm_actions) == 1 && length(aws_cloudwatch_metric_alarm.uptime.ok_actions) == 1
    error_message = "L'allarme deve notificare esattamente un topic SNS in ALARM e in OK."
  }

  # Least-privilege: la Lambda può pubblicare SOLO nel namespace di uptime.
  assert {
    condition     = strcontains(aws_iam_role_policy.ping_metric.policy, "appgrove/uptime")
    error_message = "La policy della Lambda deve limitare PutMetricData al namespace appgrove/uptime."
  }
}
