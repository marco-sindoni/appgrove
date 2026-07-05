output "vpc_id" {
  description = "ID della VPC dell'ambiente (per il modulo microsaas_app, UC 0004)."
  value       = aws_vpc.this.id
}

output "public_subnet_ids" {
  description = "ID delle subnet pubbliche (2 AZ), per ECS/Aurora/endpoint."
  value       = aws_subnet.public[*].id
}

output "vpc_endpoints_security_group_id" {
  description = "Security group degli endpoint VPC (da cui consentire l'accesso alle Lambda auth)."
  value       = aws_security_group.vpc_endpoints.id
}

output "gdpr_export_bucket" {
  description = "Nome del bucket S3 degli export GDPR dell'ambiente."
  value       = aws_s3_bucket.gdpr_export.bucket
}

output "gdpr_export_bucket_arn" {
  description = "ARN del bucket export GDPR (per le policy del ruolo servizi, UC 0004)."
  value       = aws_s3_bucket.gdpr_export.arn
}
