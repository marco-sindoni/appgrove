# ─────────────────────────────────────────────────────────────────────────────
# Lambda Custom Message (UC 0018, #02 6, #06 26): Cognito la invoca prima di
# spedire un'email di autenticazione e ne usa il messaggio restituito — testo e
# grafica nostri, lingua scelta dal parametro `locale` della chiamata.
#
# Differenze deliberate dal Pre-Token-Gen (pre_token_gen.tf), che è per il resto
# il modello seguito:
#
#   • FUORI dalla VPC — non tocca il database. La lingua le arriva già risolta
#     come parametro della chiamata (ClientMetadata dal BFF auth). Metterla in
#     rete privata per leggere una preferenza fra due valori aggiungerebbe avvio
#     a freddo su OGNI email e una connessione a Postgres, senza nulla in cambio.
#   • Nessuna dipendenza esterna da vendorizzare: solo libreria standard.
#
# I testi NON stanno qui: vengono da shared/email-templates, la stessa cartella
# che il servizio Java copia nel proprio artefatto. Due copie divergerebbero in
# silenzio, e la divergenza si vedrebbe solo in cloud (verifica e reimpostazione
# password passano da qui, in locale dal servizio).
# ─────────────────────────────────────────────────────────────────────────────

locals {
  custom_message_name = "appgrove-${var.env}-custom-message"

  # Sorgente unica dei testi, alla radice del repo (vedi shared/email-templates/README.md).
  email_templates_dir = "${path.module}/../../../shared/email-templates"
}

# `source` espliciti invece di `source_dir`: il sorgente sta qui, i template stanno
# nella cartella condivisa. È ciò che rende impossibile la duplicazione del copy.
data "archive_file" "custom_message" {
  type        = "zip"
  output_path = "${path.module}/lambda/custom_message.zip"

  source {
    content  = file("${path.module}/lambda/custom_message/handler.py")
    filename = "handler.py"
  }

  dynamic "source" {
    for_each = fileset(local.email_templates_dir, "*.{json,html,txt}")
    content {
      content  = file("${local.email_templates_dir}/${source.value}")
      filename = "templates/${source.value}"
    }
  }
}

resource "aws_iam_role" "custom_message" {
  name = local.custom_message_name

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "lambda.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })

  tags = {
    Name = local.custom_message_name
  }
}

# Solo i log: non legge segreti, non parla col database, non chiama SES (spedisce
# Cognito). Il permesso più stretto possibile è anche quello sufficiente.
resource "aws_iam_role_policy_attachment" "custom_message_logs" {
  role       = aws_iam_role.custom_message.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_cloudwatch_log_group" "custom_message" {
  name              = "/aws/lambda/${local.custom_message_name}"
  retention_in_days = local.obs_log_retention_days

  #checkov:skip=CKV_AWS_158:Cifratura at rest di default (chiavi gestite CloudWatch); CMK solo se servirà (#06 §20bis)
  #checkov:skip=CKV_AWS_338:Retention 7gg test / 30gg prod by-design (#08 26, cost-min): non sono log di audit

  tags = {
    Name = local.custom_message_name
  }
}

resource "aws_lambda_function" "custom_message" {
  function_name = local.custom_message_name
  description   = "Custom Message (UC 0018): testo e lingua EN/IT delle email di autenticazione Cognito"
  role          = aws_iam_role.custom_message.arn

  filename         = data.archive_file.custom_message.output_path
  source_code_hash = data.archive_file.custom_message.output_base64sha256
  handler          = "handler.handler"
  runtime          = "python3.13"
  architectures    = ["arm64"]

  # Cognito taglia i trigger sincroni a 5s. Qui non c'è rete né database: il lavoro è
  # leggere due file e sostituire delle stringhe, quindi il margine è ampio.
  timeout     = 5
  memory_size = 128

  # Coerente col throttling dell'API auth (10 rps): queste invocazioni nascono da
  # registrazioni e reimpostazioni password, non dal percorso caldo del login.
  reserved_concurrent_executions = 10

  environment {
    variables = {
      APP_BASE_URL = "https://${local.spa_hosts.backoffice}"
    }
  }

  #checkov:skip=CKV_AWS_50:X-Ray spento by-design: strumentazione pronta, export tracce = evoluzione E10 (#08 11)
  #checkov:skip=CKV_AWS_116:Niente DLQ: invocazione sincrona da Cognito (la registrazione rivede l'esito)
  #checkov:skip=CKV_AWS_117:Fuori dalla VPC by-design: non accede al database (vedi testata)
  #checkov:skip=CKV_AWS_173:Nessun segreto nelle env: solo l'URL pubblico della SPA
  #checkov:skip=CKV_AWS_272:Code signing oltre lo scopo: sorgente versionato nel repo, deploy solo via Terraform

  depends_on = [aws_cloudwatch_log_group.custom_message]

  tags = {
    Name = local.custom_message_name
  }
}

resource "aws_lambda_permission" "custom_message_cognito" {
  statement_id  = "AllowCognitoInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.custom_message.function_name
  principal     = "cognito-idp.amazonaws.com"
  source_arn    = aws_cognito_user_pool.this.arn
}
