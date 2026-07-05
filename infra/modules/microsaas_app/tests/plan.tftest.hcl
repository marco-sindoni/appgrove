# ─────────────────────────────────────────────────────────────────────────────
# terraform test del modulo microsaas_app (#10 29): dato un input, il plan
# produce le risorse attese. Provider AWS FINTO (mock_provider): gira offline,
# senza credenziali — è ciò che lancia infra/scripts/check (e run-tests.sh).
# ─────────────────────────────────────────────────────────────────────────────

mock_provider "aws" {}

# Input di default: un'app "demo" in test (i valori di `shared` sono finti:
# nel plan contano solo i nomi/flag che il modulo costruisce da env/app_id).
variables {
  env              = "test"
  app_id           = "demo"
  use_fargate_spot = true
  force_destroy    = true

  shared = {
    vpc_id                        = "vpc-00000000"
    vpc_cidr                      = "10.0.0.0/16"
    subnet_ids                    = ["subnet-0000000a", "subnet-0000000b"]
    ecs_cluster_arn               = "arn:aws:ecs:eu-west-1:123456789012:cluster/appgrove-test"
    cloud_map_namespace_id        = "ns-0000000000000000"
    api_id                        = "api00000"
    vpc_link_id                   = "vl-0000"
    vpc_link_security_group_id    = "sg-00000000"
    event_bus_name                = "appgrove-test"
    event_bus_arn                 = "arn:aws:events:eu-west-1:123456789012:event-bus/appgrove-test"
    aurora_endpoint               = "appgrove-test.cluster-x.eu-west-1.rds.amazonaws.com"
    aurora_port                   = 5432
    aurora_database_name          = "appgrove"
    db_bootstrap_lambda_name      = "appgrove-test-db-bootstrap"
    sqs_queue_prefix              = "appgrove-test-"
    gdpr_export_results_queue_arn = "arn:aws:sqs:eu-west-1:123456789012:appgrove-test-gdpr-export-results"
    gdpr_export_bucket            = "appgrove-test-gdpr-export"
    gdpr_export_bucket_arn        = "arn:aws:s3:::appgrove-test-gdpr-export"
  }
}

run "app_di_default_in_test" {
  command = plan

  # Code SQS: nome fisico = prefisso ambiente + nome logico locale (GdprQueues),
  # DLQ con 5 tentativi come dev/elasticmq.conf, cifratura at rest.
  assert {
    condition     = aws_sqs_queue.gdpr_export.name == "appgrove-test-gdpr-export-demo"
    error_message = "La coda export deve chiamarsi <prefisso>gdpr-export-<app_id>."
  }
  assert {
    condition     = aws_sqs_queue.tenant_purge.name == "appgrove-test-tenant-purge-demo"
    error_message = "La coda purge deve chiamarsi <prefisso>tenant-purge-<app_id>."
  }
  assert {
    condition     = aws_sqs_queue.gdpr_export_dlq.name == "appgrove-test-gdpr-export-demo-dlq"
    error_message = "La DLQ export deve avere suffisso -dlq (come in locale)."
  }
  # (il redrive a 5 tentativi non è asseribile in plan: contiene l'ARN della
  # DLQ, noto solo all'apply — resta a vista nel codice, = dev/elasticmq.conf)
  assert {
    condition     = aws_sqs_queue.gdpr_export.sqs_managed_sse_enabled == true
    error_message = "Le code devono essere cifrate at rest (#06 §20bis)."
  }

  # Log group: retention esplicita per-ambiente (#08 26): 7 giorni in test.
  assert {
    condition     = aws_cloudwatch_log_group.this.retention_in_days == 7
    error_message = "In test la retention del log group deve essere 7 giorni (#08 26)."
  }
  assert {
    condition     = aws_cloudwatch_log_group.this.name == "/appgrove/test/demo"
    error_message = "Il log group deve chiamarsi /appgrove/<env>/<app_id>."
  }

  # Route API: /api/<app_id>/v1/* sull'API condivisa (#06 22).
  assert {
    condition     = aws_apigatewayv2_route.this.route_key == "ANY /api/demo/v1/{proxy+}"
    error_message = "La route deve essere ANY /api/<app_id>/v1/{proxy+}."
  }

  # Capacità: Spot in test (#06 10), 1 task (cost-min #06 9).
  assert {
    condition     = one(aws_ecs_service.this.capacity_provider_strategy).capacity_provider == "FARGATE_SPOT"
    error_message = "Con use_fargate_spot=true la capacità deve essere FARGATE_SPOT."
  }
  assert {
    condition     = aws_ecs_service.this.desired_count == 1
    error_message = "Un servizio parte con 1 task (HA = evoluzione E3)."
  }
  assert {
    condition     = aws_ecs_task_definition.this.cpu == "256" && aws_ecs_task_definition.this.memory == "512"
    error_message = "Task cost-min: 0.25 vCPU / 0.5 GB (#06 9)."
  }

  # DB: schema/ruolo di default app_<app_id> (#06 23), segreto per-app dedicato.
  assert {
    condition     = output.db_schema == "app_demo"
    error_message = "Lo schema di default deve essere app_<app_id>."
  }
  assert {
    condition     = aws_secretsmanager_secret.db.name == "appgrove/test/demo/db"
    error_message = "Il segreto DB deve chiamarsi appgrove/<env>/<app_id>/db."
  }

  # ECR: repo per-servizio con scan on push.
  assert {
    condition     = aws_ecr_repository.this.name == "appgrove-test-demo"
    error_message = "Il repo ECR deve chiamarsi appgrove-<env>-<app_id>."
  }

  # EventBridge: regola agganciata a tenant.offboarded sul bus dell'ambiente.
  assert {
    condition     = contains(jsondecode(aws_cloudwatch_event_rule.tenant_offboarded.event_pattern)["detail-type"], "tenant.offboarded")
    error_message = "La regola deve filtrare l'evento tenant.offboarded (UC 0032)."
  }
}

run "core_in_prod" {
  command = plan

  variables {
    env              = "prod"
    app_id           = "platform"
    db_schema        = "platform"
    is_platform_core = true
    use_fargate_spot = false
    force_destroy    = false
  }

  # Il core usa lo schema `platform`, non app_platform (#05 11).
  assert {
    condition     = output.db_schema == "platform"
    error_message = "db_schema deve poter essere sovrascritto (core = platform, #05 11)."
  }

  # Prod: retention 30 giorni (#08 26) e capacità on-demand (#06 10).
  assert {
    condition     = aws_cloudwatch_log_group.this.retention_in_days == 30
    error_message = "In prod la retention del log group deve essere 30 giorni (#08 26)."
  }
  assert {
    condition     = one(aws_ecs_service.this.capacity_provider_strategy).capacity_provider == "FARGATE"
    error_message = "Con use_fargate_spot=false la capacità deve essere FARGATE on-demand."
  }

  # Prefisso d'ambiente nei nomi fisici delle code.
  assert {
    condition     = aws_sqs_queue.gdpr_export.name == "appgrove-test-gdpr-export-platform"
    error_message = "Il nome fisico usa il prefisso di shared.sqs_queue_prefix (qui finto 'appgrove-test-')."
  }
}

# Due istanze nello stesso ambiente: ogni risorsa con un nome è scopata
# sull'app_id → rimuovere un'istanza (service-remove → destroy -target) non
# può toccare le risorse dell'altra.
run "due_istanze_disgiunte" {
  command = plan

  module {
    source = "./tests/fixtures/double"
  }

  assert {
    condition     = output.alpha_export_queue != output.beta_export_queue
    error_message = "Le code delle due istanze devono avere nomi distinti."
  }
  assert {
    condition     = output.alpha_schema == "app_alpha" && output.beta_schema == "app_beta"
    error_message = "Ogni istanza deve avere il proprio schema app_<app_id>."
  }
}
