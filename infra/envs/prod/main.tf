# ─────────────────────────────────────────────────────────────────────────────
# Ambiente PROD (#12 1). State: s3://<state-bucket>/envs/prod/terraform.tfstate,
# separato da test (#06 5).
#
# Guardrail (#06 24/25, applicati dagli script wrapper e dalle risorse):
#   • `up prod` / `down prod` richiedono conferma digitata (mai auto-approve);
#   • i bucket NON si svuotano da soli (force_destroy=false);
#   • le protezioni sui dati stateful (deletion protection + snapshot finale
#     Aurora) nascono con il cluster nel suo use case.
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
