variable "env" {
  description = "Nome dell'ambiente (test | prod); entra nei nomi delle risorse."
  type        = string

  validation {
    condition     = contains(["test", "prod"], var.env)
    error_message = "env deve essere \"test\" o \"prod\" (local non usa AWS, #12 1/3)."
  }
}

variable "alarms_enabled" {
  description = <<-EOT
    Azioni di allarme attive (#08 18): pieni in prod, SILENZIATI in test
    (scale-to-0/spegnimento notturno senza falsi allarmi). Default: derivato
    dall'ambiente (prod=true).
  EOT
  type        = bool
  default     = null
}

variable "services" {
  description = "Sezioni per-servizio (output `observability` delle istanze microsaas_app): alimentano dashboard e query salvate."
  type = list(object({
    app_id         = string
    log_group_name = string
    widgets        = list(any)
  }))
}

variable "api_id" {
  description = "ID della HTTP API condivisa (widget overview)."
  type        = string
}

variable "aurora_cluster_identifier" {
  description = "Identificatore del cluster Aurora (widget e allarmi #08 16)."
  type        = string
}

variable "ecs_cluster_arn" {
  description = "ARN del cluster ECS: la regola EventBridge \"task che non parte\" filtra su questo cluster."
  type        = string
}

variable "alarm_topic_critical_arn" {
  description = "Topic SNS critical (output di platform_shared)."
  type        = string
}

variable "alarm_topic_warning_arn" {
  description = "Topic SNS warning (output di platform_shared)."
  type        = string
}

variable "error_ingest_lambda_name" {
  description = "Nome della Lambda di ingest errori frontend (allarme errori, widget)."
  type        = string
}

variable "error_ingest_log_group_name" {
  description = "Log group degli errori frontend normalizzati (widget auth/sicurezza)."
  type        = string
}

variable "auth_lambda_name" {
  description = "Nome della Lambda BFF auth (UC 0015): allarme errori sui fallimenti di login/signup."
  type        = string
}
