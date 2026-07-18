variable "env" {
  description = "Nome dell'ambiente (test | prod); entra nei nomi delle risorse."
  type        = string

  validation {
    condition     = contains(["test", "prod"], var.env)
    error_message = "env deve essere \"test\" o \"prod\" (local non usa AWS, #12 1/3)."
  }
}

variable "app_id" {
  description = "Identificatore del servizio (es. \"fatture\"; il core è \"platform\"): entra in nomi risorse, route API, code e schema DB."
  type        = string

  validation {
    condition     = can(regex("^[a-z][a-z0-9_]{0,30}$", var.app_id))
    error_message = "app_id: minuscole/cifre/underscore, iniziale alfabetica (diventa parte di identificatori Postgres e nomi SQS)."
  }
}

variable "container_port" {
  description = "Porta HTTP del container (stessa del profilo %dev locale, per simmetria)."
  type        = number
  default     = 8080
}

variable "image_tag" {
  description = "Tag dell'immagine ECR referenziata dalla task definition (la pipeline UC 0005 lo pilota nei deploy)."
  type        = string
  default     = "latest"
}

variable "cpu" {
  description = "vCPU del task Fargate (#06 9: 0.25 vCPU cost-min)."
  type        = number
  default     = 256
}

variable "memory" {
  description = "Memoria del task Fargate in MiB (#06 9: 0.5 GB cost-min)."
  type        = number
  default     = 512
}

variable "use_fargate_spot" {
  description = "Capacità del service (#06 10): Fargate Spot in test (~-70%), on-demand in prod."
  type        = bool
}

variable "force_destroy" {
  description = "Teardown libero (#06 24): true su test (ECR svuotato dal destroy), false su prod."
  type        = bool
}

variable "db_schema" {
  description = "Nome dello schema (e del ruolo) Postgres del servizio. Default: app_<app_id>; il core usa \"platform\" (#05 11)."
  type        = string
  default     = null

  validation {
    condition     = var.db_schema == null || can(regex("^[a-z][a-z0-9_]{0,62}$", coalesce(var.db_schema, "x")))
    error_message = "db_schema deve essere un identificatore Postgres minuscolo ([a-z][a-z0-9_]*)."
  }
}

variable "is_platform_core" {
  description = <<-EOT
    true SOLO per l'istanza del core (`platform`): aggiunge i permessi del suo
    ruolo di orchestratore GDPR (UC 0032) — dispatch alle code export per-app,
    consumo della coda risultati condivisa, pubblicazione eventi sul bus,
    lettura/aggregazione dei frammenti export su S3.
  EOT
  type        = bool
  default     = false
}

variable "shared" {
  description = "Punti di aggancio alle risorse condivise dell'ambiente (output di platform_shared + env_baseline, vedi locals negli envs)."
  type = object({
    vpc_id                        = string
    vpc_cidr                      = string
    subnet_ids                    = list(string)
    ecs_cluster_arn               = string
    cloud_map_namespace_id        = string
    api_id                        = string
    authorizer_id                 = string
    vpc_link_id                   = string
    vpc_link_security_group_id    = string
    event_bus_name                = string
    event_bus_arn                 = string
    aurora_endpoint               = string
    aurora_port                   = number
    aurora_database_name          = string
    db_bootstrap_lambda_name      = string
    sqs_queue_prefix              = string
    gdpr_export_results_queue_arn = string
    gdpr_export_bucket            = string
    gdpr_export_bucket_arn        = string
    alarm_topic_critical_arn      = string
    alarm_topic_warning_arn       = string
    audit_firehose_arn            = string
    logs_to_firehose_role_arn     = string
    # Validazione JWT nei servizi (UC 0016): emittente/JWKS del pool Cognito +
    # app client atteso (difesa in profondità sul destinatario dell'access token).
    cognito_issuer    = string
    cognito_jwks_url  = string
    cognito_client_id = string
  })
}

variable "public_routes" {
  description = <<-EOT
    Route dell'app esposte SENZA authorizer (UC 0014): eccezione DICHIARATIVA
    per chiamanti che non hanno un access token e si autenticano in altro modo.
    Devono essere più specifiche del proxy generico e stare nel prefisso
    dell'app (`<METODO> /api/<app_id>/v1/...`, verificato da precondizione).
    Oggi l'unico caso è il webhook Paddle del core (firma HMAC, UC 0025).
    Default vuoto: un'app nasce interamente protetta.
  EOT
  type        = list(string)
  default     = []
}

variable "alarms_enabled" {
  description = <<-EOT
    Azioni di allarme attive (#08 18): pieni in prod, SILENZIATI in test (gli
    allarmi esistono ma non notificano: lo scale-to-0/spegnimento notturno non
    deve generare falsi allarmi). Default: derivato dall'ambiente (prod=true).
  EOT
  type        = bool
  default     = null
}
