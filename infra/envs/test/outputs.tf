output "vpc_id" {
  value       = module.baseline.vpc_id
  description = "VPC dell'ambiente test."
}

output "public_subnet_ids" {
  value       = module.baseline.public_subnet_ids
  description = "Subnet pubbliche (2 AZ) dell'ambiente test."
}

output "vpc_endpoints_security_group_id" {
  value       = module.baseline.vpc_endpoints_security_group_id
  description = "Security group degli endpoint VPC."
}

output "gdpr_export_bucket" {
  value       = module.baseline.gdpr_export_bucket
  description = "Bucket export GDPR (pubblicato anche su SSM: /appgrove/test/gdpr/export-bucket)."
}
