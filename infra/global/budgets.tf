# ─────────────────────────────────────────────────────────────────────────────
# AWS Budgets (#08 17, UC 0006): tetto mensile $100 (= massimo atteso, per non
# generare allarmi fisiologici; abbassarlo a regime è una modifica banale).
# Soglie 75/90/100% sullo SPESO REALE + early-warning sul FORECAST >100%.
# A livello account (i budget non sono per-ambiente): vive nello stack global.
# ─────────────────────────────────────────────────────────────────────────────

variable "budget_alert_email" {
  description = "Destinatario delle notifiche di budget (#08 17)."
  type        = string
  default     = "marcosindoni@gmail.com"
}

resource "aws_budgets_budget" "monthly" {
  name        = "appgrove-monthly"
  budget_type = "COST"

  limit_amount = "100"
  limit_unit   = "USD"
  time_unit    = "MONTHLY"

  # Speso reale: 75% / 90% / 100% (#08 17).
  dynamic "notification" {
    for_each = [75, 90, 100]

    content {
      comparison_operator        = "GREATER_THAN"
      notification_type          = "ACTUAL"
      threshold                  = notification.value
      threshold_type             = "PERCENTAGE"
      subscriber_email_addresses = [var.budget_alert_email]
    }
  }

  # Early warning: la proiezione di fine mese supera il tetto.
  notification {
    comparison_operator        = "GREATER_THAN"
    notification_type          = "FORECASTED"
    threshold                  = 100
    threshold_type             = "PERCENTAGE"
    subscriber_email_addresses = [var.budget_alert_email]
  }
}
