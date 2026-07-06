# Output = punti di aggancio per il modulo `microsaas_app` (UC 0004) e per la
# pipeline (UC 0005): il per-app REFERENZIA queste risorse, non le crea.

output "ecs_cluster_arn" {
  description = "ARN del cluster ECS dell'ambiente (i service vi si registrano, UC 0004)."
  value       = aws_ecs_cluster.this.arn
}

output "ecs_cluster_name" {
  description = "Nome del cluster ECS dell'ambiente."
  value       = aws_ecs_cluster.this.name
}

output "cloud_map_namespace_id" {
  description = "ID del namespace Cloud Map (i service discovery per-app nascono qui, UC 0004)."
  value       = aws_service_discovery_private_dns_namespace.this.id
}

output "cloud_map_namespace_arn" {
  description = "ARN del namespace Cloud Map (per le integrazioni API GW, UC 0004)."
  value       = aws_service_discovery_private_dns_namespace.this.arn
}

output "cloud_map_namespace_name" {
  description = "Nome DNS del namespace Cloud Map (appgrove-<env>.internal)."
  value       = aws_service_discovery_private_dns_namespace.this.name
}

output "api_id" {
  description = "ID della HTTP API condivisa (le route per-app vi si agganciano, UC 0004)."
  value       = aws_apigatewayv2_api.this.id
}

output "api_execution_arn" {
  description = "Execution ARN della HTTP API (per i permessi dell'authorizer Lambda, UC 0014)."
  value       = aws_apigatewayv2_api.this.execution_arn
}

output "api_endpoint" {
  description = "Endpoint di servizio della HTTP API (il dominio pubblico è api.<env-prefix><domain>)."
  value       = aws_apigatewayv2_api.this.api_endpoint
}

output "vpc_link_id" {
  description = "ID del VPC Link (le integrazioni per-app lo attraversano, UC 0004)."
  value       = aws_apigatewayv2_vpc_link.this.id
}

output "vpc_link_security_group_id" {
  description = "Security group del VPC Link: i SG dei servizi (UC 0004) consentono l'ingresso da qui."
  value       = aws_security_group.vpc_link.id
}

output "event_bus_name" {
  description = "Nome del bus EventBridge dell'ambiente (regole purge per-app, UC 0004)."
  value       = aws_cloudwatch_event_bus.this.name
}

output "event_bus_arn" {
  description = "ARN del bus EventBridge dell'ambiente."
  value       = aws_cloudwatch_event_bus.this.arn
}

output "aurora_cluster_arn" {
  description = "ARN del cluster Aurora dell'ambiente."
  value       = aws_rds_cluster.this.arn
}

output "aurora_cluster_identifier" {
  description = "Identificatore del cluster Aurora (per il bootstrap schema/ruoli, UC 0004)."
  value       = aws_rds_cluster.this.cluster_identifier
}

output "aurora_endpoint" {
  description = "Endpoint writer del cluster Aurora: connessione DIRETTA dei task Fargate (#05 dec.3)."
  value       = aws_rds_cluster.this.endpoint
}

output "aurora_port" {
  description = "Porta Postgres del cluster."
  value       = aws_rds_cluster.this.port
}

output "aurora_database_name" {
  description = "Nome del database (gli schemi per-app nascono con UC 0004)."
  value       = aws_rds_cluster.this.database_name
}

output "aurora_master_secret_arn" {
  description = "ARN del segreto Secrets Manager con le credenziali master (per il bootstrap ruoli, UC 0004)."
  value       = aws_rds_cluster.this.master_user_secret[0].secret_arn
}

output "aurora_security_group_id" {
  description = "Security group del cluster Aurora."
  value       = aws_security_group.aurora.id
}

output "rds_proxy_endpoint" {
  description = "Endpoint del RDS Proxy: connessione delle SOLE Lambda auth (#05 dec.3, UC 0014)."
  value       = aws_db_proxy.this.endpoint
}

output "rds_proxy_arn" {
  description = "ARN del RDS Proxy."
  value       = aws_db_proxy.this.arn
}

output "db_bootstrap_lambda_name" {
  description = "Nome della Lambda db-bootstrap (invocata da microsaas_app per ruolo+schema, UC 0004)."
  value       = aws_lambda_function.db_bootstrap.function_name
}

output "sqs_queue_prefix" {
  description = "Prefisso dei nomi fisici delle code SQS dell'ambiente (i nomi logici restano quelli locali)."
  value       = local.sqs_queue_prefix
}

output "gdpr_export_results_queue_name" {
  description = "Nome fisico della coda condivisa dei risultati export GDPR (consumata dal core, UC 0032)."
  value       = aws_sqs_queue.gdpr_export_results.name
}

output "gdpr_export_results_queue_arn" {
  description = "ARN della coda condivisa dei risultati export GDPR."
  value       = aws_sqs_queue.gdpr_export_results.arn
}

output "spa_bucket_names" {
  description = "Bucket dei bundle SPA per distribuzione (backoffice/admin): destinazione della pipeline FE (UC 0005)."
  value       = { for k, b in aws_s3_bucket.spa : k => b.bucket }
}

output "spa_distribution_ids" {
  description = "ID delle distribuzioni CloudFront (per l'invalidation della pipeline FE, UC 0005)."
  value       = { for k, d in aws_cloudfront_distribution.spa : k => d.id }
}

output "spa_urls" {
  description = "URL pubblici delle 2 SPA."
  value       = { for k, host in local.spa_hosts : k => "https://${host}" }
}

output "api_url" {
  description = "URL pubblico dell'API dell'ambiente."
  value       = "https://${local.api_host}"
}

output "alarm_topic_critical_arn" {
  description = "Topic SNS degli allarmi critici (#08 15): destinazione delle alarm action."
  value       = aws_sns_topic.alarms["critical"].arn
}

output "alarm_topic_warning_arn" {
  description = "Topic SNS degli allarmi warning (#08 15)."
  value       = aws_sns_topic.alarms["warning"].arn
}

output "audit_firehose_arn" {
  description = "Firehose dell'archivio audit (#08 28): destinazione dei subscription filter per-servizio (microsaas_app)."
  value       = aws_kinesis_firehose_delivery_stream.audit_archive.arn
}

output "logs_to_firehose_role_arn" {
  description = "Ruolo che CloudWatch Logs assume per consegnare l'audit a Firehose (usato dai subscription filter di microsaas_app)."
  value       = aws_iam_role.logs_to_firehose.arn
}

output "error_ingest_lambda_name" {
  description = "Nome della Lambda di ingest errori frontend (allarmi nel modulo observability)."
  value       = aws_lambda_function.error_ingest.function_name
}

output "error_ingest_log_group_name" {
  description = "Log group con gli errori frontend normalizzati (widget dashboard, UC 0006)."
  value       = aws_cloudwatch_log_group.error_ingest.name
}

output "apigw_access_log_group_name" {
  description = "Log group degli access log dell'API condivisa (UC 0006)."
  value       = aws_cloudwatch_log_group.apigw_access.name
}
