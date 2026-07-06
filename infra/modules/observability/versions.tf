# Modulo `observability` — il "cruscotto" dell'ambiente (UC 0006, #08):
# dashboard CloudWatch unica per-env (overview + sezione per-servizio generata
# da microsaas_app + auth/sicurezza), allarmi sulle risorse condivise (Aurora,
# ingest errori, task ECS che non parte) e query Logs Insights pre-salvate per
# il drill-down per-tenant/correlazione (#08 30-32: regola dei due piani).
# Vive A VALLE delle istanze microsaas_app: ne consuma l'output `observability`.
terraform {
  required_version = ">= 1.9"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}

data "aws_region" "current" {}
