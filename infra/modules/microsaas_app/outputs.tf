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

output "gdpr_export_dlq_arn" {
  description = "ARN della DLQ export GDPR (allarmi DLQ, UC 0006)."
  value       = aws_sqs_queue.gdpr_export_dlq.arn
}

output "tenant_purge_dlq_arn" {
  description = "ARN della DLQ purge per-tenant (allarmi DLQ, UC 0006)."
  value       = aws_sqs_queue.tenant_purge_dlq.arn
}

output "log_group_arn" {
  description = "ARN del log group del servizio."
  value       = aws_cloudwatch_log_group.this.arn
}

output "log_level_parameter_name" {
  description = "Parametro SSM del livello di log (#08 6): DEBUG a runtime senza rebuild."
  value       = aws_ssm_parameter.log_level.name
}

output "observability" {
  description = "Sezione osservabilità del servizio per la dashboard d'ambiente (modulo observability, UC 0006)."
  value = {
    app_id         = var.app_id
    log_group_name = aws_cloudwatch_log_group.this.name
    widgets        = local.dashboard_widgets
  }
}

output "task_definition_family" {
  description = "Family della task definition: i task one-shot della pipeline (Flyway migrate / sync-pricing, UC 0005) la lanciano via `aws ecs run-task` con command override (ultima revision ACTIVE)."
  value       = aws_ecs_task_definition.this.family
}

output "security_group_id" {
  description = "Security group del service (riusato dai task one-shot della pipeline per raggiungere Aurora in VPC, UC 0005)."
  value       = aws_security_group.service.id
}
