# ─────────────────────────────────────────────────────────────────────────────
# Lambda BFF auth (UC 0015, #02 2, #06 18): POST /api/auth/* sull'API condivisa
# → Lambda nativa ARM64 (services/auth, profilo Maven `lambda`) in VPC —
# Cognito e Secrets Manager/SSM via VPC endpoint (no-NAT), DB via RDS Proxy
# (scritture platform al signup/accept invito).
#
# Artefatto: function.zip caricato dalla CI (deploy-test/release-prod) nel
# bucket per-env con chiave per-SHA (auth/<sha>-native.zip), promozione stesso
# SHA test→prod come per le immagini ECR. Con `auth_lambda_s3_key` vuota
# (nessuna build ancora pubblicata: attivazione a fasi, _BACKLOG) Lambda e
# route NON vengono create e il plan resta valido.
# ─────────────────────────────────────────────────────────────────────────────

locals {
  auth_lambda_enabled = var.auth_lambda_s3_key != ""
  auth_lambda_name    = "appgrove-${var.env}-auth"
}

# Bucket artefatti Lambda (stessa convenzione dei bucket SPA: privato, SSE-S3).
resource "aws_s3_bucket" "lambda_artifacts" {
  bucket = "appgrove-lambda-artifacts-${var.env}-${data.aws_caller_identity.current.account_id}"

  force_destroy = var.force_destroy_buckets

  #checkov:skip=CKV_AWS_21:Versioning inutile: artefatti per-SHA immutabili, ricostruibili dalla CI
  #checkov:skip=CKV_AWS_144:Replica cross-region non necessaria: artefatti rigenerabili (cost-min)
  #checkov:skip=CKV_AWS_18:Access logging non necessario: scrive solo la CI via OIDC, legge solo Lambda (cost-min)
  #checkov:skip=CKV2_AWS_62:Nessuna event notification necessaria (deploy pilotato da Terraform)
  #checkov:skip=CKV_AWS_145:SSE-S3 (AES256) sufficiente: chiavi gestite AWS di default (#06 §20bis)
  #checkov:skip=CKV2_AWS_61:Lifecycle omesso: pochi KB per SHA, pulizia con l'hardening (_EVOLUZIONI-DEVOPS)

  tags = {
    Name = "appgrove-lambda-artifacts-${var.env}"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "lambda_artifacts" {
  bucket = aws_s3_bucket.lambda_artifacts.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
    bucket_key_enabled = true
  }
}

resource "aws_s3_bucket_public_access_block" "lambda_artifacts" {
  bucket                  = aws_s3_bucket.lambda_artifacts.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Security group della Lambda: nessun ingresso; uscita 443 verso i VPC endpoint
# (cognito-idp, secretsmanager, ssm) e 5432 verso il proxy RDS — tutto in VPC.
resource "aws_security_group" "auth_lambda" {
  name        = "${local.auth_lambda_name}-lambda"
  description = "Lambda BFF auth: egress verso VPC endpoint (443) e RDS Proxy (5432)"
  vpc_id      = var.vpc_id

  egress {
    description = "HTTPS verso i VPC endpoint (Cognito-IDP, Secrets Manager, SSM)"
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
    Name = "${local.auth_lambda_name}-lambda"
  }
}

resource "aws_iam_role" "auth_lambda" {
  name = local.auth_lambda_name

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "lambda.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })

  tags = {
    Name = local.auth_lambda_name
  }
}

resource "aws_iam_role_policy_attachment" "auth_lambda_logs" {
  role       = aws_iam_role.auth_lambda.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

# ENI in VPC (#06 18).
resource "aws_iam_role_policy_attachment" "auth_lambda_vpc" {
  role       = aws_iam_role.auth_lambda.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"
}

data "aws_iam_policy_document" "auth_lambda" {
  # Operazioni admin sul pool (accept invito: AdminCreateUser/AdminSetUserPassword;
  # compensazione signup: AdminDeleteUser). I flussi client (InitiateAuth, SignUp,
  # ConfirmSignUp, ForgotPassword, …) NON richiedono IAM: autenticano col client secret.
  statement {
    sid    = "CognitoAdminOnPool"
    effect = "Allow"
    actions = [
      "cognito-idp:AdminCreateUser",
      "cognito-idp:AdminSetUserPassword",
      "cognito-idp:AdminDeleteUser",
    ]
    resources = [aws_cognito_user_pool.this.arn]
  }

  # Client secret Cognito da SSM (#02 15).
  statement {
    sid       = "ReadClientSecretParam"
    effect    = "Allow"
    actions   = ["ssm:GetParameter"]
    resources = [aws_ssm_parameter.auth_client_secret.arn]
  }

  # Credenziali DB per il proxy RDS: il segreto MASTER gestito da RDS è l'unico
  # attaccato al proxy (rds_proxy.tf). Ruolo DB dedicato least-privilege per le
  # Lambda auth (+ IAM auth del proxy) → decisione UC 0014/0016.
  statement {
    sid       = "ReadDbSecret"
    effect    = "Allow"
    actions   = ["secretsmanager:GetSecretValue"]
    resources = [aws_rds_cluster.this.master_user_secret[0].secret_arn]
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

  # Email d'invito via SES (identità di dominio + template EN/IT → UC 0018).
  statement {
    sid       = "SendInviteEmail"
    effect    = "Allow"
    actions   = ["ses:SendEmail"]
    resources = ["*"]

    condition {
      test     = "StringEquals"
      variable = "ses:FromAddress"
      values   = ["noreply@${var.domain}"]
    }
  }
}

resource "aws_iam_role_policy" "auth_lambda" {
  name   = "auth-bff"
  role   = aws_iam_role.auth_lambda.id
  policy = data.aws_iam_policy_document.auth_lambda.json
}

resource "aws_cloudwatch_log_group" "auth_lambda" {
  name              = "/aws/lambda/${local.auth_lambda_name}"
  retention_in_days = local.obs_log_retention_days

  #checkov:skip=CKV_AWS_158:Cifratura at rest di default (chiavi gestite CloudWatch); CMK solo se servirà (#06 §20bis)
  #checkov:skip=CKV_AWS_338:Retention 7gg test / 30gg prod by-design (#08 26, cost-min): non sono log di audit

  tags = {
    Name = local.auth_lambda_name
  }
}

resource "aws_lambda_function" "auth" {
  count = local.auth_lambda_enabled ? 1 : 0

  function_name = local.auth_lambda_name
  description   = "BFF auth (UC 0015): /api/auth/* → Cognito; nativa ARM64 (services/auth)"
  role          = aws_iam_role.auth_lambda.arn

  s3_bucket = aws_s3_bucket.lambda_artifacts.id
  s3_key    = var.auth_lambda_s3_key

  # Binario nativo GraalVM: custom runtime, l'handler è il bootstrap stesso.
  runtime       = "provided.al2023"
  handler       = "bootstrap"
  architectures = ["arm64"]

  # API Gateway taglia a 29s; il timeout resta sotto. Memoria per il nativo:
  # 512 MB tengono il p95 del login basso senza sovradimensionare (cost-min).
  timeout     = 25
  memory_size = 512

  # Tetto di concorrenza (endpoint pubblico, cost-min): oltre il throttling
  # API GW (10 rps) non servono più esecuzioni simultanee di così.
  reserved_concurrent_executions = 10

  environment {
    variables = {
      QUARKUS_PROFILE             = "cloud"
      QUARKUS_DATASOURCE_JDBC_URL = "jdbc:postgresql://${aws_db_proxy.this.endpoint}:5432/${aws_rds_cluster.this.database_name}?ssl=true&sslmode=require"

      AUTH_DB_SECRET_ARN               = aws_rds_cluster.this.master_user_secret[0].secret_arn
      AUTH_COGNITO_REGION              = data.aws_region.current.region
      AUTH_COGNITO_USER_POOL_ID        = aws_cognito_user_pool.this.id
      AUTH_COGNITO_CLIENT_ID           = aws_cognito_user_pool_client.bff.id
      AUTH_COGNITO_CLIENT_SECRET_PARAM = aws_ssm_parameter.auth_client_secret.name

      AUTH_APP_BASE_URL = "https://${local.spa_hosts.backoffice}"
      AUTH_MAIL_FROM    = "noreply@${var.domain}"
    }
  }

  vpc_config {
    subnet_ids         = var.subnet_ids
    security_group_ids = [aws_security_group.auth_lambda.id]
  }

  #checkov:skip=CKV_AWS_50:X-Ray spento by-design: OTel strumentato con export spento (#08 11)
  #checkov:skip=CKV_AWS_116:Niente DLQ: invocazione sincrona da API GW (il client rivede l'errore)
  #checkov:skip=CKV_AWS_173:Env senza segreti by-design: il client secret sta in SSM, le credenziali DB in Secrets Manager
  #checkov:skip=CKV_AWS_272:Code signing oltre lo scopo: artefatto per-SHA da CI OIDC, deploy solo via Terraform

  depends_on = [aws_cloudwatch_log_group.auth_lambda]

  tags = {
    Name = local.auth_lambda_name
  }
}

resource "aws_apigatewayv2_integration" "auth" {
  count = local.auth_lambda_enabled ? 1 : 0

  api_id                 = aws_apigatewayv2_api.this.id
  integration_type       = "AWS_PROXY"
  integration_uri        = aws_lambda_function.auth[0].invoke_arn
  payload_format_version = "2.0"
}

resource "aws_apigatewayv2_route" "auth" {
  count = local.auth_lambda_enabled ? 1 : 0

  api_id    = aws_apigatewayv2_api.this.id
  route_key = "POST /api/auth/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.auth[0].id}"

  #checkov:skip=CKV_AWS_309:Rotta pubblica by-design: è l'autenticazione stessa; throttling dedicato + lockout Cognito (#06 26)
}

resource "aws_lambda_permission" "auth" {
  count = local.auth_lambda_enabled ? 1 : 0

  statement_id  = "AllowApiGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.auth[0].function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.this.execution_arn}/*/*/api/auth/*"
}
