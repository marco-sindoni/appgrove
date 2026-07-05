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
}

resource "aws_apigatewayv2_vpc_link" "this" {
  name               = "appgrove-${var.env}"
  subnet_ids         = var.subnet_ids
  security_group_ids = [aws_security_group.vpc_link.id]

  tags = {
    Name = "appgrove-${var.env}"
  }
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.this.id
  name        = "$default"
  auto_deploy = true

  #checkov:skip=CKV2_AWS_29:WAF rimandato by-design (evoluzione E6, #06 21)
  #checkov:skip=CKV_AWS_76:Access logging API GW rimandato all'observability (UC 0006, cost-min)

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
