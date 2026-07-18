# ─────────────────────────────────────────────────────────────────────────────
# Lambda Pre-Token-Generation (UC 0016, #02 9/10/11): Cognito la invoca a ogni
# emissione token → legge la membership da `platform.users` via RDS Proxy e
# inietta `tenant_id`+`roles` nell'ACCESS token (fail-closed). In VPC (endpoint
# secretsmanager + proxy RDS), runtime Python con driver pg8000 vendorizzato
# (nessun binario nativo, archive_file autocontenuto — come db_bootstrap).
#
# Usa il ruolo DB dedicato least-privilege `auth_lambdas` (auth_db_role.tf), non
# più le credenziali master. Sempre creata (l'archive non dipende da un artefatto
# CI): il trigger sul pool (auth.tf) la richiede sempre.
# ─────────────────────────────────────────────────────────────────────────────

locals {
  pre_token_gen_name = "appgrove-${var.env}-pre-token-gen"
}

data "archive_file" "pre_token_gen" {
  type        = "zip"
  source_dir  = "${path.module}/lambda/pre_token_gen"
  output_path = "${path.module}/lambda/pre_token_gen.zip"
  excludes    = ["test_handler.py"]
}

# Security group: nessun ingresso; uscita 443 (endpoint Secrets Manager) e 5432
# (proxy RDS). Dedicato (non condiviso col BFF) così UC 0014 potrà stringere il
# proxy alle sole SG delle Lambda auth.
resource "aws_security_group" "pre_token_gen" {
  name        = "${local.pre_token_gen_name}-lambda"
  description = "Lambda pre-token-gen: egress verso VPC endpoint (443) e RDS Proxy (5432)"
  vpc_id      = var.vpc_id

  egress {
    description = "HTTPS verso i VPC endpoint (Secrets Manager)"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    description = "Postgres verso il proxy RDS"
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  tags = {
    Name = "${local.pre_token_gen_name}-lambda"
  }
}

resource "aws_iam_role" "pre_token_gen" {
  name = local.pre_token_gen_name

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "lambda.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })

  tags = {
    Name = local.pre_token_gen_name
  }
}

resource "aws_iam_role_policy_attachment" "pre_token_gen_logs" {
  role       = aws_iam_role.pre_token_gen.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy_attachment" "pre_token_gen_vpc" {
  role       = aws_iam_role.pre_token_gen.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"
}

data "aws_iam_policy_document" "pre_token_gen" {
  # Credenziali del ruolo DB dedicato (auth_db_role.tf) per il proxy RDS.
  statement {
    sid       = "ReadDbSecret"
    effect    = "Allow"
    actions   = ["secretsmanager:GetSecretValue"]
    resources = [aws_secretsmanager_secret.auth_lambdas_db.arn]
  }

  statement {
    sid       = "DecryptDbSecret"
    effect    = "Allow"
    actions   = ["kms:Decrypt"]
    resources = ["*"] # chiave gestita aws/secretsmanager, ristretta da ViaService

    condition {
      test     = "StringEquals"
      variable = "kms:ViaService"
      values   = ["secretsmanager.${data.aws_region.current.region}.amazonaws.com"]
    }
  }
}

resource "aws_iam_role_policy" "pre_token_gen" {
  name   = "pre-token-gen"
  role   = aws_iam_role.pre_token_gen.id
  policy = data.aws_iam_policy_document.pre_token_gen.json
}

resource "aws_cloudwatch_log_group" "pre_token_gen" {
  name              = "/aws/lambda/${local.pre_token_gen_name}"
  retention_in_days = local.obs_log_retention_days

  #checkov:skip=CKV_AWS_158:Cifratura at rest di default (chiavi gestite CloudWatch); CMK solo se servirà (#06 §20bis)
  #checkov:skip=CKV_AWS_338:Retention 7gg test / 30gg prod by-design (#08 26, cost-min): non sono log di audit

  tags = {
    Name = local.pre_token_gen_name
  }
}

resource "aws_lambda_function" "pre_token_gen" {
  function_name = local.pre_token_gen_name
  description   = "Pre-Token-Gen (UC 0016): inietta tenant_id/roles nell'access token leggendo platform.users via RDS Proxy"
  role          = aws_iam_role.pre_token_gen.arn

  filename         = data.archive_file.pre_token_gen.output_path
  source_code_hash = data.archive_file.pre_token_gen.output_base64sha256
  handler          = "handler.handler"
  runtime          = "python3.13"
  architectures    = ["arm64"]

  # Cognito taglia i trigger sincroni a 5s: oltre non ha senso attendere. Il
  # rischio cold-start Aurora in pausa (>5s) è tracciato (punto aperto UC 0016).
  timeout     = 5
  memory_size = 256

  # Login/refresh: coerente col throttling dell'API auth (10 rps).
  reserved_concurrent_executions = 10

  environment {
    variables = {
      DB_PROXY_HOST = aws_db_proxy.this.endpoint
      DB_PORT       = "5432"
      DB_NAME       = aws_rds_cluster.this.database_name
      DB_SECRET_ARN = aws_secretsmanager_secret.auth_lambdas_db.arn
      # Allow-list dei `sub` platform-admin (parità con auth.local.platform-admin-subjects).
      # Vuota finché non esiste il platform-admin reale su Cognito → punto aperto UC 0016.
      PLATFORM_ADMIN_SUBS = ""
    }
  }

  vpc_config {
    subnet_ids         = var.subnet_ids
    security_group_ids = [aws_security_group.pre_token_gen.id]
  }

  #checkov:skip=CKV_AWS_50:X-Ray spento by-design: strumentazione pronta, export tracce = evoluzione E10 (#08 11)
  #checkov:skip=CKV_AWS_116:Niente DLQ: invocazione sincrona da Cognito (il login rivede l'esito)
  #checkov:skip=CKV_AWS_173:Env senza segreti by-design: solo ARN/nomi; le credenziali DB stanno in Secrets Manager
  #checkov:skip=CKV_AWS_272:Code signing oltre lo scopo: sorgente versionato nel repo, deploy solo via Terraform

  depends_on = [aws_cloudwatch_log_group.pre_token_gen]

  tags = {
    Name = local.pre_token_gen_name
  }
}

# Cognito è autorizzato a invocare la Lambda (trigger pre-token-generation).
resource "aws_lambda_permission" "pre_token_gen_cognito" {
  statement_id  = "AllowCognitoInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.pre_token_gen.function_name
  principal     = "cognito-idp.amazonaws.com"
  source_arn    = aws_cognito_user_pool.this.arn
}
