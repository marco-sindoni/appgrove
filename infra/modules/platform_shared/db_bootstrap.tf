# ─────────────────────────────────────────────────────────────────────────────
# Lambda `db-bootstrap` (UC 0004, #06 23): crea ruolo Postgres + schema vuoto
# per-servizio in modo idempotente. La invoca il modulo `microsaas_app`
# (`aws_lambda_invocation`) a ogni apply; le tabelle le crea Flyway (UC 0005).
#
# Fuori VPC by-design: parla con Aurora tramite la Data API (rds-data, API
# firmata) e con Secrets Manager pubblico — niente ENI, niente driver Postgres,
# costo solo a invocazione (cost-min).
# ─────────────────────────────────────────────────────────────────────────────

data "archive_file" "db_bootstrap" {
  type        = "zip"
  source_file = "${path.module}/lambda/db_bootstrap.py"
  output_path = "${path.module}/lambda/db_bootstrap.zip"
}

resource "aws_iam_role" "db_bootstrap" {
  name = "appgrove-${var.env}-db-bootstrap"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "lambda.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })

  tags = {
    Name = "appgrove-${var.env}-db-bootstrap"
  }
}

resource "aws_iam_role_policy_attachment" "db_bootstrap_logs" {
  role       = aws_iam_role.db_bootstrap.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy" "db_bootstrap" {
  name = "db-bootstrap"
  role = aws_iam_role.db_bootstrap.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid      = "DataApi"
        Effect   = "Allow"
        Action   = ["rds-data:ExecuteStatement", "rds-data:BatchExecuteStatement"]
        Resource = aws_rds_cluster.this.arn
      },
      {
        Sid    = "ReadSecrets"
        Effect = "Allow"
        Action = ["secretsmanager:GetSecretValue"]
        Resource = [
          # Master (per la Data API) + segreti per-app creati da microsaas_app
          # (convenzione nome: appgrove/<env>/<app_id>/db).
          aws_rds_cluster.this.master_user_secret[0].secret_arn,
          "arn:aws:secretsmanager:${data.aws_region.current.region}:${data.aws_caller_identity.current.account_id}:secret:appgrove/${var.env}/*",
        ]
      },
    ]
  })
}

resource "aws_lambda_function" "db_bootstrap" {
  function_name = "appgrove-${var.env}-db-bootstrap"
  description   = "Bootstrap idempotente ruolo+schema per-servizio su Aurora (UC 0004, #06 23)"
  role          = aws_iam_role.db_bootstrap.arn

  filename         = data.archive_file.db_bootstrap.output_path
  source_code_hash = data.archive_file.db_bootstrap.output_base64sha256
  handler          = "db_bootstrap.handler"
  runtime          = "python3.13"
  timeout          = 150 # copre il risveglio del cluster in pausa (~10-15s) con margine
  memory_size      = 128

  environment {
    variables = {
      CLUSTER_ARN       = aws_rds_cluster.this.arn
      MASTER_SECRET_ARN = aws_rds_cluster.this.master_user_secret[0].secret_arn
      DB_NAME           = aws_rds_cluster.this.database_name
    }
  }

  #checkov:skip=CKV_AWS_117:Fuori VPC by-design: usa la Data API (rds-data), non una connessione di rete al DB
  #checkov:skip=CKV_AWS_50:X-Ray spento (cost-min); tracing con l'observability (UC 0006)
  #checkov:skip=CKV_AWS_115:Nessun limite di concorrenza riservata: invocata solo da Terraform durante gli apply
  #checkov:skip=CKV_AWS_116:Niente DLQ: invocazione SINCRONA da Terraform, l'errore fallisce l'apply
  #checkov:skip=CKV_AWS_173:Le env var non contengono segreti (solo ARN/nomi): chiave gestita AWS sufficiente
  #checkov:skip=CKV_AWS_272:Code signing oltre lo scopo: sorgente versionato nel repo, deploy solo via Terraform

  tags = {
    Name = "appgrove-${var.env}-db-bootstrap"
  }
}
