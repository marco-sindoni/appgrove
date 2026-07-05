output "ecr_repository_url" {
  description = "URL del repo ECR del servizio (destinazione della pipeline di build, UC 0005)."
  value       = aws_ecr_repository.this.repository_url
}

output "ecs_service_name" {
  description = "Nome del service ECS (per deploy/scale mirati: pipeline UC 0005, test-start/stop)."
  value       = aws_ecs_service.this.name
}

output "log_group_name" {
  description = "Log group del servizio (widget/allarmi in UC 0006)."
  value       = aws_cloudwatch_log_group.this.name
}

output "db_secret_arn" {
  description = "Segreto Secrets Manager con le credenziali DB del servizio."
  value       = aws_secretsmanager_secret.db.arn
}

output "db_schema" {
  description = "Schema (= ruolo) Postgres del servizio; le tabelle le crea Flyway (UC 0005/0012)."
  value       = local.db_schema
}

output "gdpr_export_queue_name" {
  description = "Nome fisico della coda export GDPR del servizio (prefisso ambiente + nome logico)."
  value       = aws_sqs_queue.gdpr_export.name
}

output "tenant_purge_queue_name" {
  description = "Nome fisico della coda purge per-tenant del servizio."
  value       = aws_sqs_queue.tenant_purge.name
}

output "gdpr_export_queue_arn" {
  description = "ARN della coda export GDPR del servizio."
  value       = aws_sqs_queue.gdpr_export.arn
}

output "tenant_purge_queue_arn" {
  description = "ARN della coda purge per-tenant del servizio."
  value       = aws_sqs_queue.tenant_purge.arn
}

output "api_route" {
  description = "Route del servizio sull'API HTTP condivisa."
  value       = aws_apigatewayv2_route.this.route_key
}
