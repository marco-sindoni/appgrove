variable "env" {
  description = "Ambiente (solo prod usa il canary; presente per nominare/taggare le risorse in modo coerente)."
  type        = string
}

variable "target_url" {
  description = "URL pubblico da pingare (UC 0007, Opzione A): la SPA backoffice su CloudFront. Un GET 2xx/3xx = servizio raggiungibile."
  type        = string
  default     = "https://app.appgrove.app/"
}

variable "target_label" {
  description = "Etichetta breve del bersaglio: dimensione della metrica e suffisso dei nomi (es. 'app')."
  type        = string
  default     = "app"
}

variable "alert_email" {
  description = "Destinatario email dell'allarme di uptime (la subscription SNS va confermata via mail)."
  type        = string
}

variable "schedule_expression" {
  description = "Cadenza del ping (EventBridge). Default 1 minuto: rilevazione reattiva, costo ~$0 (piano gratuito Lambda)."
  type        = string
  default     = "rate(1 minute)"
}

variable "evaluation_periods" {
  description = "Periodi da 60s consecutivi sotto soglia prima dell'allarme (anti-rumore: non un singolo blip)."
  type        = number
  default     = 3
}

variable "log_retention_days" {
  description = "Retention dei log della Lambda di ping (giorni). Non sono log di audit: retention breve (cost-min)."
  type        = number
  default     = 30
}

variable "metric_namespace" {
  description = "Namespace CloudWatch della metrica di uptime."
  type        = string
  default     = "appgrove/uptime"
}
