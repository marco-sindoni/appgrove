# ─────────────────────────────────────────────────────────────────────────────
# Ruoli IAM del task (least-privilege):
#   • execution role — usato da ECS per tirare l'immagine, scrivere i log e
#     iniettare il segreto DB nel container;
#   • task role — usato DAL servizio a runtime: le sue code GDPR, la coda
#     risultati condivisa (send), il bucket export (put del proprio frammento).
#     Il core (`is_platform_core`) aggiunge il ruolo di orchestratore GDPR:
#     dispatch alle code export di tutte le app, consumo della coda risultati,
#     PutEvents sul bus, lettura/aggregazione dei frammenti su S3.
# ─────────────────────────────────────────────────────────────────────────────

locals {
  ecs_tasks_assume = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

# ── Execution role ───────────────────────────────────────────────────────────

resource "aws_iam_role" "execution" {
  name               = "${local.name}-execution"
  assume_role_policy = local.ecs_tasks_assume

  tags = {
    Name = "${local.name}-execution"
  }
}

resource "aws_iam_role_policy_attachment" "execution" {
  role       = aws_iam_role.execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role_policy" "execution_secrets" {
  name = "inject-db-secret"
  role = aws_iam_role.execution.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid      = "InjectDbSecret"
      Effect   = "Allow"
      Action   = ["secretsmanager:GetSecretValue"]
      Resource = aws_secretsmanager_secret.db.arn
    }]
  })
}

# ── Task role ────────────────────────────────────────────────────────────────

resource "aws_iam_role" "task" {
  name               = "${local.name}-task"
  assume_role_policy = local.ecs_tasks_assume

  tags = {
    Name = "${local.name}-task"
  }
}

resource "aws_iam_role_policy" "task_gdpr" {
  name = "gdpr-messaging"
  role = aws_iam_role.task.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = concat(
      [
        {
          Sid    = "ConsumeOwnQueues"
          Effect = "Allow"
          Action = [
            "sqs:ReceiveMessage",
            "sqs:DeleteMessage",
            "sqs:ChangeMessageVisibility",
            "sqs:GetQueueUrl",
            "sqs:GetQueueAttributes",
          ]
          Resource = [
            aws_sqs_queue.gdpr_export.arn,
            aws_sqs_queue.tenant_purge.arn,
          ]
        },
        {
          Sid      = "SendExportResults"
          Effect   = "Allow"
          Action   = ["sqs:SendMessage", "sqs:GetQueueUrl"]
          Resource = var.shared.gdpr_export_results_queue_arn
        },
        {
          Sid      = "PutOwnExportFragment"
          Effect   = "Allow"
          Action   = ["s3:PutObject"]
          Resource = "${var.shared.gdpr_export_bucket_arn}/jobs/*"
        },
      ],
      # Ruolo di orchestratore GDPR del core (UC 0032).
      var.is_platform_core ? [
        {
          Sid      = "DispatchExportJobs"
          Effect   = "Allow"
          Action   = ["sqs:SendMessage", "sqs:GetQueueUrl"]
          Resource = "${local.sqs_arn_prefix}:${var.shared.sqs_queue_prefix}gdpr-export-*"
        },
        {
          Sid    = "ConsumeExportResults"
          Effect = "Allow"
          Action = [
            "sqs:ReceiveMessage",
            "sqs:DeleteMessage",
            "sqs:ChangeMessageVisibility",
            "sqs:GetQueueAttributes",
          ]
          Resource = var.shared.gdpr_export_results_queue_arn
        },
        {
          Sid      = "PublishDomainEvents"
          Effect   = "Allow"
          Action   = ["events:PutEvents"]
          Resource = var.shared.event_bus_arn
        },
        {
          Sid      = "AggregateExportJobs"
          Effect   = "Allow"
          Action   = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"]
          Resource = "${var.shared.gdpr_export_bucket_arn}/jobs/*"
        },
      ] : []
    )
  })
}
