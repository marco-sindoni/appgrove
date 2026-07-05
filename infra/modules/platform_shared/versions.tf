# Modulo `platform_shared` — risorse condivise di UN ambiente (UC 0055): lo
# strato tra le fondamenta (`env_baseline`, UC 0003) e il modulo per-app
# `microsaas_app` (UC 0004). Contiene ciò che le app e le SPA presuppongono
# già esistente: Aurora Serverless v2 + RDS Proxy, cluster ECS, API Gateway
# HTTP + VPC Link + Cloud Map, bus EventBridge, 2 distribuzioni CloudFront.
#
# CloudFront accetta solo certificati ACM emessi in us-east-1 (vincolo AWS,
# #06 17): il modulo richiede un provider aliasato per risolverli.
terraform {
  required_version = ">= 1.9"
  required_providers {
    aws = {
      source                = "hashicorp/aws"
      version               = "~> 6.0"
      configuration_aliases = [aws.us_east_1]
    }
    archive = {
      source  = "hashicorp/archive"
      version = "~> 2.7"
    }
  }
}
