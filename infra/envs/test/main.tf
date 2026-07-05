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
