output "state_bucket" {
  description = "Nome del bucket S3 che ospita gli state Terraform (global, envs/test, envs/prod)."
  value       = aws_s3_bucket.tfstate.bucket
}

output "lock_table" {
  description = "Nome della tabella DynamoDB usata per il lock dello state."
  value       = aws_dynamodb_table.tfstate_lock.name
}

output "region" {
  description = "Regione AWS in cui vivono state e lock."
  value       = var.region
}
