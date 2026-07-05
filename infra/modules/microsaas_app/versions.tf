# Modulo `microsaas_app` — il mattone per-servizio (UC 0004, invariante #3):
# una nuova app backend = UN blocco `module` in envs/<env>/main.tf, MAI infra
# su misura. Un'istanza crea: ECR repo, service/task ECS Fargate registrato su
# Cloud Map, route `/api/<app_id>/v1/*` sull'API condivisa, ruolo DB + schema
# vuoto (via Lambda db-bootstrap), code SQS GDPR per-app + regola EventBridge
# `tenant.offboarded`, segreto credenziali DB, log group con retention.
# Si aggancia alle risorse condivise di `platform_shared` (input `shared`).
terraform {
  required_version = ">= 1.9"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.7"
    }
  }
}
