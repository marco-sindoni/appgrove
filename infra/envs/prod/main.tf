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
}
