# ─────────────────────────────────────────────────────────────────────────────
# Ambiente PROD (#12 1). State: s3://<state-bucket>/envs/prod/terraform.tfstate,
# separato da test (#06 5).
#
# Guardrail (#06 24/25, applicati dagli script wrapper e dalle risorse):
#   • `up prod` / `down prod` richiedono conferma digitata (mai auto-approve);
#   • i bucket NON si svuotano da soli (force_destroy=false);
#   • dati stateful protetti: deletion protection + snapshot finale sul
#     cluster Aurora (modulo platform_shared, UC 0055).
# ─────────────────────────────────────────────────────────────────────────────

terraform {
  required_version = ">= 1.9"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }

  backend "s3" {
    key = "envs/prod/terraform.tfstate" # bucket/lock via -backend-config (scripts/_lib.sh)
  }
}

provider "aws" {
  region = var.region
  default_tags {
    tags = {
      project    = "appgrove"
      env        = "prod"
      managed_by = "terraform"
    }
  }
}

# CloudFront accetta SOLO certificati emessi in us-east-1 (vincolo AWS, #06 17):
# alias richiesto dal modulo platform_shared per risolverli.
provider "aws" {
  alias  = "us_east_1"
  region = "us-east-1"
  default_tags {
    tags = {
      project    = "appgrove"
      env        = "prod"
      managed_by = "terraform"
    }
  }
}

variable "region" {
  description = "Regione AWS dell'ambiente (#06 6: eu-west-1)."
  type        = string
  default     = "eu-west-1"
}

variable "image_tag" {
  description = "Tag delle immagini dei servizi su ECR (UC 0005: per-SHA, `TF_VAR_image_tag` dalla pipeline; `latest` solo come default fuori CI)."
  type        = string
  default     = "latest"
}

variable "auth_lambda_s3_key" {
  description = "Chiave S3 del function.zip della Lambda BFF auth (UC 0015: per-SHA, `TF_VAR_auth_lambda_s3_key` dalla pipeline). Vuota = Lambda e route /api/auth/* non create (attivazione a fasi)."
  type        = string
  default     = ""
}

module "baseline" {
  source = "../../modules/env_baseline"

  env      = "prod"
  vpc_cidr = "10.1.0.0/16" # distinto da test (10.0.0.0/16)

  # Prod: nessuno svuotamento automatico — un destroy che incontra bucket pieni
  # si ferma, e svuotarli è un atto esplicito (#06 24).
  force_destroy_buckets = false
}

# Risorse condivise per-ambiente (UC 0055): Aurora SsV2 + RDS Proxy, cluster
# ECS, API GW HTTP + VPC Link + Cloud Map, bus EventBridge, 2 CloudFront SPA.
# Il modulo per-app `microsaas_app` (UC 0004) vi si aggancia via output.
module "platform_shared" {
  source = "../../modules/platform_shared"
  providers = {
    aws           = aws
    aws.us_east_1 = aws.us_east_1
  }

  env        = "prod"
  vpc_id     = module.baseline.vpc_id
  vpc_cidr   = "10.1.0.0/16"
  subnet_ids = module.baseline.public_subnet_ids

  # Prod: dati stateful protetti (deletion protection + snapshot finale Aurora,
  # bucket non svuotati da soli, #06 16/24) e capacità on-demand (#06 10).
  deletion_protection   = true
  force_destroy_buckets = false
  use_fargate_spot      = false

  alert_email = var.alert_email

  # BFF auth (UC 0015): artefatto Lambda per-SHA pubblicato dalla CI.
  auth_lambda_s3_key = var.auth_lambda_s3_key
}

variable "alert_email" {
  description = "Destinatario email di allarmi SNS (#08 15); la subscription va confermata via mail."
  type        = string
  default     = "marcosindoni@gmail.com"
}

# ─────────────────────────────────────────────────────────────────────────────
# Microservizi (UC 0004, invariante #3): un blocco `module` per servizio,
# generato/rimosso da ./infra/scripts/service-add|service-remove (il flusso
# normale è PR→CI, #07 18; su prod l'apply è gated). I marker
# `service-add:begin|end` delimitano i blocchi per gli script: non rimuoverli.
# ─────────────────────────────────────────────────────────────────────────────

locals {
  # Punti di aggancio condivisi consumati da ogni istanza `microsaas_app`.
  microsaas_shared = {
    vpc_id                        = module.baseline.vpc_id
    vpc_cidr                      = "10.1.0.0/16"
    subnet_ids                    = module.baseline.public_subnet_ids
    ecs_cluster_arn               = module.platform_shared.ecs_cluster_arn
    cloud_map_namespace_id        = module.platform_shared.cloud_map_namespace_id
    api_id                        = module.platform_shared.api_id
    authorizer_id                 = module.platform_shared.authorizer_id
    vpc_link_id                   = module.platform_shared.vpc_link_id
    vpc_link_security_group_id    = module.platform_shared.vpc_link_security_group_id
    event_bus_name                = module.platform_shared.event_bus_name
    event_bus_arn                 = module.platform_shared.event_bus_arn
    aurora_endpoint               = module.platform_shared.aurora_endpoint
    aurora_port                   = module.platform_shared.aurora_port
    aurora_database_name          = module.platform_shared.aurora_database_name
    db_bootstrap_lambda_name      = module.platform_shared.db_bootstrap_lambda_name
    sqs_queue_prefix              = module.platform_shared.sqs_queue_prefix
    gdpr_export_results_queue_arn = module.platform_shared.gdpr_export_results_queue_arn
    gdpr_export_bucket            = module.baseline.gdpr_export_bucket
    gdpr_export_bucket_arn        = module.baseline.gdpr_export_bucket_arn
    alarm_topic_critical_arn      = module.platform_shared.alarm_topic_critical_arn
    alarm_topic_warning_arn       = module.platform_shared.alarm_topic_warning_arn
    audit_firehose_arn            = module.platform_shared.audit_firehose_arn
    logs_to_firehose_role_arn     = module.platform_shared.logs_to_firehose_role_arn
    cognito_issuer                = module.platform_shared.cognito_issuer
    cognito_jwks_url              = module.platform_shared.cognito_jwks_url
    cognito_client_id             = module.platform_shared.cognito_client_id
  }
}

# ── service-add:begin platform ────────────────────────────────────────────────
# Il core della piattaforma: schema `platform` (non app_*, #05 11) e ruolo di
# orchestratore GDPR (dispatch export, consumo risultati, eventi sul bus).
module "app_platform" {
  source = "../../modules/microsaas_app"

  env              = "prod"
  app_id           = "platform"
  container_port   = 8080
  db_schema        = "platform"
  is_platform_core = true
  image_tag        = var.image_tag

  use_fargate_spot = false # prod: on-demand (#06 10)
  force_destroy    = false # prod: nessuno svuotamento automatico (#06 24)

  # Eccezione all'authorizer (UC 0014): Paddle chiama senza access token e si
  # autentica con la firma HMAC del messaggio (PaddleSignature, UC 0025). Rotta
  # più specifica del proxy generico, quindi vince il routing di API GW.
  public_routes = ["POST /api/platform/v1/webhooks/paddle"]

  shared = local.microsaas_shared
}
# ── service-add:end platform ──────────────────────────────────────────────────

# ── service-add:begin fatture ─────────────────────────────────────────────────
module "app_fatture" {
  source = "../../modules/microsaas_app"

  env            = "prod"
  app_id         = "fatture"
  container_port = 8081
  image_tag      = var.image_tag

  use_fargate_spot = false # prod: on-demand (#06 10)
  force_destroy    = false # prod: nessuno svuotamento automatico (#06 24)

  shared = local.microsaas_shared
}
# ── service-add:end fatture ───────────────────────────────────────────────────


# ─────────────────────────────────────────────────────────────────────────────
# Osservabilità dell'ambiente (UC 0006): dashboard unica, allarmi condivisi,
# query salvate. La lista `services` è mantenuta dagli script service-add e
# service-remove (marker obs-services): non rimuovere i marker.
# ─────────────────────────────────────────────────────────────────────────────

module "observability" {
  source = "../../modules/observability"

  env = "prod"

  services = [
    # ── obs-services:begin ──
    module.app_platform.observability,
    module.app_fatture.observability,
    # ── obs-services:end ──
  ]

  api_id                      = module.platform_shared.api_id
  aurora_cluster_identifier   = module.platform_shared.aurora_cluster_identifier
  ecs_cluster_arn             = module.platform_shared.ecs_cluster_arn
  alarm_topic_critical_arn    = module.platform_shared.alarm_topic_critical_arn
  alarm_topic_warning_arn     = module.platform_shared.alarm_topic_warning_arn
  error_ingest_lambda_name    = module.platform_shared.error_ingest_lambda_name
  error_ingest_log_group_name = module.platform_shared.error_ingest_log_group_name
  auth_lambda_name            = module.platform_shared.auth_lambda_name
  pre_token_gen_lambda_name   = module.platform_shared.pre_token_gen_lambda_name
}

# ─────────────────────────────────────────────────────────────────────────────
# Ruolo DB dedicato delle Lambda auth (UC 0016, opzione B di E23): crea il ruolo
# Postgres `auth_lambdas` e i grant least-privilege sullo schema `platform`.
# Gira DOPO app_platform (che crea lo schema) e dopo che Flyway ha applicato le
# migrazioni (CI, prima dell'apply). L'input include `image_tag` così i grant si
# riallineano a ogni deploy, coprendo le tabelle di nuove migrazioni.
# ─────────────────────────────────────────────────────────────────────────────
resource "aws_lambda_invocation" "auth_lambdas_grants" {
  function_name = module.platform_shared.db_bootstrap_lambda_name

  input = jsonencode({
    mode       = "grant"
    role_name  = "auth_lambdas"
    secret_arn = module.platform_shared.auth_lambdas_db_secret_arn
    grants = {
      schema       = "platform"
      owner_role   = "platform"
      select_all   = true
      write_tables = ["accounts", "users", "invitations"]
    }
    # Solo per ri-innescare l'invocazione (la Lambda li ignora): la password e
    # ogni deploy (nuove tabelle) devono riconciliare i grant.
    secret_version_id = module.platform_shared.auth_lambdas_db_secret_version_id
    image_tag         = var.image_tag
  })

  depends_on = [module.app_platform]
}
