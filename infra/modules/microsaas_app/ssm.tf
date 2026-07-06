# ─────────────────────────────────────────────────────────────────────────────
# Livello di log a runtime (#08 6): INFO in test/prod, DEBUG attivabile SENZA
# rebuild — si cambia il parametro e si forza un nuovo deployment del service
# (aws ecs update-service --force-new-deployment). Terraform non riporta il
# valore a INFO (ignore_changes): il toggle è un atto operativo, non di codice.
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_ssm_parameter" "log_level" {
  name  = "/appgrove/${var.env}/${var.app_id}/log-level"
  type  = "String"
  value = "INFO"

  lifecycle {
    ignore_changes = [value]
  }

  #checkov:skip=CKV_AWS_337:Il livello di log non è un segreto: String è corretto, SecureString andrebbe oltre lo scopo

  tags = {
    Name = "${local.name}-log-level"
  }
}
