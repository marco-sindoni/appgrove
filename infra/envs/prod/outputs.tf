output "vpc_id" {
  value       = module.baseline.vpc_id
  description = "VPC dell'ambiente prod."
}

output "public_subnet_ids" {
  value       = module.baseline.public_subnet_ids
  description = "Subnet pubbliche (2 AZ) dell'ambiente prod."
}

output "vpc_endpoints_security_group_id" {
  value       = module.baseline.vpc_endpoints_security_group_id
  description = "Security group degli endpoint VPC."
}

output "gdpr_export_bucket" {
  value       = module.baseline.gdpr_export_bucket
  description = "Bucket export GDPR (pubblicato anche su SSM: /appgrove/prod/gdpr/export-bucket)."
}

# ── Risorse condivise per-ambiente (platform_shared, UC 0055) ────────────────
# I punti di aggancio fini (ARN, SG, segreti) restano output del modulo:
# qui si espone l'essenziale per operare con `./infra/scripts/output`.

output "spa_urls" {
  value       = module.platform_shared.spa_urls
  description = "URL pubblici delle 2 SPA (backoffice/admin)."
}

output "api_url" {
  value       = module.platform_shared.api_url
  description = "URL pubblico dell'API dell'ambiente."
}

output "spa_bucket_names" {
  value       = module.platform_shared.spa_bucket_names
  description = "Bucket dei bundle SPA (destinazione della pipeline FE, UC 0005)."
}

output "spa_distribution_ids" {
  value       = module.platform_shared.spa_distribution_ids
  description = "ID delle distribuzioni CloudFront (per l'invalidation, UC 0005)."
}

output "ecs_cluster_name" {
  value       = module.platform_shared.ecs_cluster_name
  description = "Cluster ECS dell'ambiente (i service arrivano con UC 0004)."
}

output "aurora_endpoint" {
  value       = module.platform_shared.aurora_endpoint
  description = "Endpoint writer Aurora (connessione diretta dei task, #05 dec.3)."
}

output "rds_proxy_endpoint" {
  value       = module.platform_shared.rds_proxy_endpoint
  description = "Endpoint RDS Proxy (solo Lambda auth, UC 0014)."
}

output "event_bus_name" {
  value       = module.platform_shared.event_bus_name
  description = "Bus EventBridge dell'ambiente (regole purge per-app, UC 0004)."
}

# ── Pipeline CI/CD (UC 0005) ─────────────────────────────────────────────────

output "spa_config" {
  description = "config.json runtime per-SPA generato dall'infra (#07 12: unica fonte di verità, zero valori hardcoded). Cognito placeholder fino a UC 0015; errorIngestUrl = rotta di error_ingest.tf (UC 0006)."
  value = {
    for app in ["backoffice", "admin"] : app => {
      env            = "prod"
      authBaseUrl    = module.platform_shared.api_url
      coreBaseUrl    = module.platform_shared.api_url
      cognito        = { userPoolId = "", clientId = "" }
      errorIngestUrl = "${module.platform_shared.api_url}/ingest/errors"
    }
  }
}

output "ci_deploy" {
  description = "Aggancio per la pipeline (UC 0005): cluster, rete e — per servizio — task family/service/ECR/SG per i task one-shot (migrate/sync-pricing) e il deploy. Le righe per-servizio sono mantenute da service-add/service-remove (marker ci-services)."
  value = {
    ecs_cluster_name = module.platform_shared.ecs_cluster_name
    subnet_ids       = module.baseline.public_subnet_ids
    services = {
      # ── ci-services:begin ──
      platform = { task_definition_family = module.app_platform.task_definition_family, ecs_service_name = module.app_platform.ecs_service_name, ecr_repository_url = module.app_platform.ecr_repository_url, security_group_id = module.app_platform.security_group_id }
      fatture  = { task_definition_family = module.app_fatture.task_definition_family, ecs_service_name = module.app_fatture.ecs_service_name, ecr_repository_url = module.app_fatture.ecr_repository_url, security_group_id = module.app_fatture.security_group_id }
      # ── ci-services:end ──
    }
  }
}
