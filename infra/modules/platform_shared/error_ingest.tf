# ─────────────────────────────────────────────────────────────────────────────
# Ingest errori frontend (UC 0006, #08 23): POST /ingest/errors sull'API
# condivisa → Lambda → CloudWatch Logs. Solo errori (niente tracking
# comportamentale); il reporter vive in frontend/packages/error-reporter.
# Rotta SENZA authorizer by-design: gli errori vanno raccolti anche prima del
# login; la Lambda tratta il payload come non fidato (allowlist + limiti) e la
# concorrenza riservata fa da tetto anti-abuso (cost-min).
# ─────────────────────────────────────────────────────────────────────────────

data "archive_file" "error_ingest" {
  type        = "zip"
  source_file = "${path.module}/lambda/error_ingest.py"
  output_path = "${path.module}/lambda/error_ingest.zip"
}

resource "aws_iam_role" "error_ingest" {
  name = "appgrove-${var.env}-error-ingest"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "lambda.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })

  tags = {
    Name = "appgrove-${var.env}-error-ingest"
  }
}

resource "aws_iam_role_policy_attachment" "error_ingest_logs" {
  role       = aws_iam_role.error_ingest.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

# Log group esplicito (retention #08 26): è QUI che finiscono gli errori
# frontend normalizzati (una riga JSON per evento, log_type=frontend_error).
resource "aws_cloudwatch_log_group" "error_ingest" {
  name              = "/aws/lambda/appgrove-${var.env}-error-ingest"
  retention_in_days = local.obs_log_retention_days

  #checkov:skip=CKV_AWS_158:Cifratura at rest di default (chiavi gestite CloudWatch); CMK solo se servirà (#06 §20bis)
  #checkov:skip=CKV_AWS_338:Retention 7gg test / 30gg prod by-design (#08 26, cost-min): non sono log di audit

  tags = {
    Name = "appgrove-${var.env}-error-ingest"
  }
}

resource "aws_lambda_function" "error_ingest" {
  function_name = "appgrove-${var.env}-error-ingest"
  description   = "Ingest errori JS del frontend → CloudWatch Logs (UC 0006, #08 23)"
  role          = aws_iam_role.error_ingest.arn

  filename         = data.archive_file.error_ingest.output_path
  source_code_hash = data.archive_file.error_ingest.output_base64sha256
  handler          = "error_ingest.handler"
  runtime          = "python3.13"
  timeout          = 3
  memory_size      = 128

  # Tetto anti-abuso (endpoint pubblico, cost-min): 2 esecuzioni concorrenti
  # bastano per volumi legittimi; oltre, API GW risponde 5xx e il reporter
  # (fire-and-forget, max 10 invii/sessione) non ritenta.
  reserved_concurrent_executions = 2

  #checkov:skip=CKV_AWS_117:Fuori VPC by-design: scrive solo sui propri log CloudWatch, nessuna risorsa in VPC
  #checkov:skip=CKV_AWS_50:X-Ray spento by-design: strumentazione pronta, export tracce = evoluzione E10 (#08 11)
  #checkov:skip=CKV_AWS_116:Niente DLQ: invocazione sincrona da API GW, un evento perso è tollerabile (best-effort)
  #checkov:skip=CKV_AWS_173:Nessuna env var configurata: niente da cifrare
  #checkov:skip=CKV_AWS_272:Code signing oltre lo scopo: sorgente versionato nel repo, deploy solo via Terraform

  depends_on = [aws_cloudwatch_log_group.error_ingest]

  tags = {
    Name = "appgrove-${var.env}-error-ingest"
  }
}

resource "aws_apigatewayv2_integration" "error_ingest" {
  api_id                 = aws_apigatewayv2_api.this.id
  integration_type       = "AWS_PROXY"
  integration_uri        = aws_lambda_function.error_ingest.invoke_arn
  payload_format_version = "2.0"
}

resource "aws_apigatewayv2_route" "error_ingest" {
  api_id    = aws_apigatewayv2_api.this.id
  route_key = "POST /ingest/errors"
  target    = "integrations/${aws_apigatewayv2_integration.error_ingest.id}"

  #checkov:skip=CKV_AWS_309:Rotta pubblica by-design: gli errori JS vanno raccolti anche senza sessione; payload non fidato, validato dalla Lambda
}

resource "aws_lambda_permission" "error_ingest" {
  statement_id  = "AllowApiGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.error_ingest.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.this.execution_arn}/*/*/ingest/errors"
}
