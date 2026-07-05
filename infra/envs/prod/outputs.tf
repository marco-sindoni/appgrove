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
