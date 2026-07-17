# ─────────────────────────────────────────────────────────────────────────────
# Ingress condiviso (#06 8, cost-min ~$0): API Gateway HTTP API v2 → VPC Link →
# Cloud Map (service discovery). NIENTE ALB (evoluzione E2).
#   • le ROUTE `/api/<app_id>/v1/*` e le integrazioni verso i servizi le crea
#     il modulo `microsaas_app` (UC 0004);
#   • l'authorizer custom (Lambda) arriva con UC 0014;
#   • il throttling su `/api/auth/*` (#06 26) arriva con le route auth (UC 0014).
# Custom domain: api.<env-prefix><domain> (#12 9/10) con certificato regionale
# eu-west-1 emesso dallo stack `global`.
# ─────────────────────────────────────────────────────────────────────────────

# Namespace Cloud Map privato: i service ECS vi si registrano (UC 0004) e le
# integrazioni dell'API GW li scoprono per nome (<app>.appgrove-<env>.internal).
resource "aws_service_discovery_private_dns_namespace" "this" {
  name        = "appgrove-${var.env}.internal"
  description = "Service discovery dei microservizi appgrove (${var.env})"
  vpc         = var.vpc_id
}

resource "aws_security_group" "vpc_link" {
  name        = "appgrove-${var.env}-vpc-link"
  description = "ENI del VPC Link: solo uscita verso i servizi nella VPC"
  vpc_id      = var.vpc_id

  # Il VPC Link origina connessioni verso i task ECS (porte dei servizi,
  # UC 0004): uscita limitata al perimetro della VPC.
  egress {
    description = "Verso i servizi nella VPC"
    from_port   = 0
    to_port     = 65535
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  # Nessuna regola ingress: nessuno si connette alle ENI del link.

  tags = {
    Name = "appgrove-${var.env}-vpc-link"
  }
}

resource "aws_apigatewayv2_api" "this" {
  name          = "appgrove-${var.env}"
  protocol_type = "HTTP"
  description   = "Ingress condiviso dei microservizi appgrove (${var.env}); route per-app da UC 0004"

  # CORS con credenziali (UC 0015, #02 16): origin ESPLICITI (le 2 SPA
  # dell'ambiente), mai wildcard — incompatibile con Allow-Credentials, e il
  # cookie refresh viaggia proprio nelle fetch cross-sottodominio (app.* → api.*).
  cors_configuration {
    allow_origins     = [for host in values(local.spa_hosts) : "https://${host}"]
    allow_methods     = ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"]
    allow_headers     = ["authorization", "content-type", "x-correlation-id"]
    allow_credentials = true
    max_age           = 3600
  }
}

resource "aws_apigatewayv2_vpc_link" "this" {
  name               = "appgrove-${var.env}"
  subnet_ids         = var.subnet_ids
  security_group_ids = [aws_security_group.vpc_link.id]

  tags = {
    Name = "appgrove-${var.env}"
  }
}

# Access log dell'edge (UC 0006, #08 4): JSON minimale — requestId (= il
# correlation id propagato ai servizi via X-Correlation-Id, microsaas_app/api.tf),
# rotta, esito, latenze. L'IP sorgente resta FUORI: minimizzazione (#13), il
# triage passa da requestId/correlation_id nei log dei servizi.
resource "aws_cloudwatch_log_group" "apigw_access" {
  name              = "/appgrove/${var.env}/apigw-access"
  retention_in_days = local.obs_log_retention_days

  #checkov:skip=CKV_AWS_158:Cifratura at rest di default (chiavi gestite CloudWatch); CMK solo se servirà (#06 §20bis)
  #checkov:skip=CKV_AWS_338:Retention 7gg test / 30gg prod by-design (#08 26, cost-min): non sono log di audit

  tags = {
    Name = "appgrove-${var.env}-apigw-access"
  }
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.this.id
  name        = "$default"
  auto_deploy = true

  access_log_settings {
    destination_arn = aws_cloudwatch_log_group.apigw_access.arn
    format = jsonencode({
      requestId               = "$context.requestId"
      requestTime             = "$context.requestTime"
      httpMethod              = "$context.httpMethod"
      routeKey                = "$context.routeKey"
      status                  = "$context.status"
      responseLength          = "$context.responseLength"
      integrationLatency      = "$context.integrationLatency"
      responseLatency         = "$context.responseLatency"
      integrationErrorMessage = "$context.integrationErrorMessage"
    })
  }

  # Metriche per-rotta (dimensione Route): servono agli allarmi 5xx/latenza
  # per-servizio di microsaas_app (#08 16).
  default_route_settings {
    detailed_metrics_enabled = true
  }

  # Throttling dedicato su /api/auth/* (UC 0015, #06 26): 10 req/s burst 20;
  # la protezione brute-force sul login resta il lockout integrato Cognito.
  # Dinamico: la rotta esiste solo quando la Lambda auth è deployata.
  dynamic "route_settings" {
    for_each = local.auth_lambda_enabled ? ["POST /api/auth/{proxy+}"] : []

    content {
      route_key                = route_settings.value
      throttling_rate_limit    = 10
      throttling_burst_limit   = 20
      detailed_metrics_enabled = true
    }
  }

  #checkov:skip=CKV2_AWS_29:WAF rimandato by-design (evoluzione E6, #06 21)

  tags = {
    Name = "appgrove-${var.env}"
  }
}

resource "aws_apigatewayv2_domain_name" "api" {
  domain_name = local.api_host

  domain_name_configuration {
    certificate_arn = data.aws_acm_certificate.regional.arn
    endpoint_type   = "REGIONAL"
    security_policy = "TLS_1_2" # TLS ovunque (#06 §20bis)
  }

  tags = {
    Name = local.api_host
  }
}

resource "aws_apigatewayv2_api_mapping" "api" {
  api_id      = aws_apigatewayv2_api.this.id
  domain_name = aws_apigatewayv2_domain_name.api.id
  stage       = aws_apigatewayv2_stage.default.id
}

resource "aws_route53_record" "api" {
  for_each = toset(["A", "AAAA"])

  zone_id = data.aws_route53_zone.main.zone_id
  name    = local.api_host
  type    = each.key

  alias {
    name                   = aws_apigatewayv2_domain_name.api.domain_name_configuration[0].target_domain_name
    zone_id                = aws_apigatewayv2_domain_name.api.domain_name_configuration[0].hosted_zone_id
    evaluate_target_health = false
  }
}
