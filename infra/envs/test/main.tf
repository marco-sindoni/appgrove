# ─────────────────────────────────────────────────────────────────────────────
# Ambiente TEST (#12 1: local | test | prod; local non usa AWS).
# State: s3://<state-bucket>/envs/test/terraform.tfstate — separato da prod:
# un `down test` non può toccare prod (#06 5).
#
# Qui vivono le fondamenta dell'ambiente (modulo env_baseline) e, dagli use case
# successivi, le istanze del modulo `microsaas_app` (una per microservizio,
# UC 0004) e le altre risorse per-ambiente (Aurora, Cognito, edge, …).
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
    key = "envs/test/terraform.tfstate" # bucket/lock via -backend-config (scripts/_lib.sh)
  }
}

provider "aws" {
  region = var.region
  default_tags {
    tags = {
      project    = "appgrove"
      env        = "test"
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
      env        = "test"
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

  env      = "test"
  vpc_cidr = "10.0.0.0/16"

  # Test: teardown libero e pulito (#06 16/24) — niente protezioni, bucket
  # svuotati automaticamente dal destroy.
  force_destroy_buckets = true
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

  env        = "test"
  vpc_id     = module.baseline.vpc_id
  vpc_cidr   = "10.0.0.0/16"
  subnet_ids = module.baseline.public_subnet_ids

  # Test: destroy libero (#06 16/24) e capacità Fargate SPOT (#06 10).
  deletion_protection   = false
  force_destroy_buckets = true
  use_fargate_spot      = true

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
# normale è PR→CI, #07 18). I marker `service-add:begin|end` delimitano i
# blocchi per gli script: non rimuoverli.
# ─────────────────────────────────────────────────────────────────────────────

locals {
  # Punti di aggancio condivisi consumati da ogni istanza `microsaas_app`.
  microsaas_shared = {
    vpc_id                        = module.baseline.vpc_id
    vpc_cidr                      = "10.0.0.0/16"
    subnet_ids                    = module.baseline.public_subnet_ids
    ecs_cluster_arn               = module.platform_shared.ecs_cluster_arn
    cloud_map_namespace_id        = module.platform_shared.cloud_map_namespace_id
    api_id                        = module.platform_shared.api_id
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
  }
}

# ── service-add:begin platform ────────────────────────────────────────────────
# Il core della piattaforma: schema `platform` (non app_*, #05 11) e ruolo di
# orchestratore GDPR (dispatch export, consumo risultati, eventi sul bus).
module "app_platform" {
  source = "../../modules/microsaas_app"

  env              = "test"
  app_id           = "platform"
  container_port   = 8080
  db_schema        = "platform"
  is_platform_core = true
  image_tag        = var.image_tag

  use_fargate_spot = true # test: Spot (#06 10)
  force_destroy    = true # test: teardown libero (#06 24)

  shared = local.microsaas_shared
}
# ── service-add:end platform ──────────────────────────────────────────────────

# ── service-add:begin fatture ─────────────────────────────────────────────────
module "app_fatture" {
  source = "../../modules/microsaas_app"

  env            = "test"
  app_id         = "fatture"
  container_port = 8081
  image_tag      = var.image_tag

  use_fargate_spot = true # test: Spot (#06 10)
  force_destroy    = true # test: teardown libero (#06 24)

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

  env = "test"

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
}
