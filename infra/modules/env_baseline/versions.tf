# Modulo `env_baseline` — fondamenta condivise di UN ambiente (test o prod):
# VPC senza NAT, endpoint VPC, bucket export GDPR, baseline SSM.
# Le risorse per-servizio sono del modulo `microsaas_app` (UC 0004).
terraform {
  required_version = ">= 1.9"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}
