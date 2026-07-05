# ─────────────────────────────────────────────────────────────────────────────
# Service/task ECS Fargate (#06 9/10/22): 0.25 vCPU / 0.5 GB, 1 task (HA = E3),
# Spot in test / on-demand in prod, registrato su Cloud Map (record SRV: l'API
# Gateway scopre ip+porta via DiscoverInstances). Topologia no-NAT (#06 18):
# task in subnet pubblica con IP pubblico, ingress chiuso al perimetro VPC.
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_security_group" "service" {
  name        = local.name
  description = "Task ${var.app_id}: ingress solo dalla VPC (VPC Link e servizi interni)"
  vpc_id      = var.shared.vpc_id

  ingress {
    description = "Porta del servizio dal perimetro VPC (VPC Link, inter-servizio)"
    from_port   = var.container_port
    to_port     = var.container_port
    protocol    = "tcp"
    cidr_blocks = [var.shared.vpc_cidr]
  }

  egress {
    description = "Verso gli endpoint AWS pubblici (ECR, SQS, S3, Secrets; topologia no-NAT #06 18)"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  #checkov:skip=CKV_AWS_382:Egress aperto by-design: topologia no-NAT (#06 18), i task raggiungono gli endpoint AWS pubblici; ingress chiuso al perimetro VPC

  tags = {
    Name = local.name
  }
}

# Service discovery su Cloud Map: record SRV (ip+porta), è ciò che l'API GW
# usa per l'integrazione (ingress.tf di platform_shared).
resource "aws_service_discovery_service" "this" {
  name = var.app_id

  dns_config {
    namespace_id   = var.shared.cloud_map_namespace_id
    routing_policy = "MULTIVALUE"

    dns_records {
      ttl  = 10
      type = "SRV"
    }
  }

  health_check_custom_config {}

  tags = {
    Name = "${var.app_id}.appgrove-${var.env}.internal"
  }
}

resource "aws_ecs_task_definition" "this" {
  family                   = local.name
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.cpu
  memory                   = var.memory
  execution_role_arn       = aws_iam_role.execution.arn
  task_role_arn            = aws_iam_role.task.arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "ARM64" # Graviton: ~-20% (immagini native GraalVM multi-arch, UC 0005)
  }

  container_definitions = jsonencode([{
    name      = var.app_id
    image     = "${aws_ecr_repository.this.repository_url}:${var.image_tag}"
    essential = true

    portMappings = [{
      containerPort = var.container_port
      protocol      = "tcp"
    }]

    # Contratto di configurazione dei servizi Quarkus (chiavi → env var):
    # il cablaggio lato servizi (profilo cloud) arriva con il deploy (UC 0005).
    environment = [
      { name = "QUARKUS_DATASOURCE_JDBC_URL", value = "jdbc:postgresql://${var.shared.aurora_endpoint}:${var.shared.aurora_port}/${var.shared.aurora_database_name}?currentSchema=${local.db_schema}" },
      { name = "QUARKUS_DATASOURCE_USERNAME", value = local.db_schema },
      { name = "APPGROVE_SQS_QUEUE_PREFIX", value = var.shared.sqs_queue_prefix },
      { name = "APPGROVE_SQS_REGION", value = data.aws_region.current.region },
      { name = "APPGROVE_GDPR_EXPORT_BUCKET", value = var.shared.gdpr_export_bucket },
    ]

    secrets = [
      { name = "QUARKUS_DATASOURCE_PASSWORD", valueFrom = "${aws_secretsmanager_secret.db.arn}:password::" },
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = aws_cloudwatch_log_group.this.name
        awslogs-region        = data.aws_region.current.region
        awslogs-stream-prefix = var.app_id
      }
    }
  }])

  tags = {
    Name = local.name
  }
}

resource "aws_ecs_service" "this" {
  name          = var.app_id
  cluster       = var.shared.ecs_cluster_arn
  desired_count = 1 # 1 task, no HA (cost-min #06 9; HA = E3)

  task_definition = aws_ecs_task_definition.this.arn

  capacity_provider_strategy {
    capacity_provider = var.use_fargate_spot ? "FARGATE_SPOT" : "FARGATE"
    weight            = 1
  }

  network_configuration {
    subnets         = var.shared.subnet_ids
    security_groups = [aws_security_group.service.id]
    # No-NAT (#06 18): l'IP pubblico serve per raggiungere ECR/SQS/S3;
    # l'ingress resta chiuso al perimetro VPC (security group).
    assign_public_ip = true
  }

  service_registries {
    registry_arn = aws_service_discovery_service.this.arn
    port         = var.container_port # nel record SRV: l'API GW scopre ip+porta
  }

  # `test-start`/`test-stop` (#07 28) pilotano il desired count fuori da
  # Terraform: un apply non deve riaccendere ciò che il cron ha spento.
  lifecycle {
    ignore_changes = [desired_count]
  }

  #checkov:skip=CKV_AWS_333:IP pubblico by-design: topologia no-NAT (#06 18), serve per ECR/SQS/S3; ingress chiuso dal security group

  # Il ruolo/schema DB deve esistere prima che il primo task provi a connettersi.
  depends_on = [aws_lambda_invocation.db_bootstrap]

  tags = {
    Name = local.name
  }
}
