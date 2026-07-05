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
}
