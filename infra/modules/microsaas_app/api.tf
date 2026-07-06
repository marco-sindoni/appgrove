# ─────────────────────────────────────────────────────────────────────────────
# Route del servizio sull'API HTTP condivisa (#06 8/22): /api/<app_id>/v1/* →
# VPC Link → Cloud Map (record SRV del service ECS). L'authorizer arriva con
# UC 0014; la configurazione CORS resta differita (UC 0004 "Punti aperti").
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_apigatewayv2_integration" "this" {
  api_id             = var.shared.api_id
  integration_type   = "HTTP_PROXY"
  integration_method = "ANY"
  integration_uri    = aws_service_discovery_service.this.arn

  connection_type = "VPC_LINK"
  connection_id   = var.shared.vpc_link_id

  payload_format_version = "1.0" # richiesto dalle integrazioni HTTP_PROXY private

  # Correlation id generato all'EDGE (#08 4): l'id richiesta di API GW finisce
  # nell'MDC dei servizi (correlation_id) e negli access log dello stage —
  # stessa chiave, correlazione end-to-end. `overwrite` (NON `append`): un
  # eventuale header fornito dal client va SOSTITUITO, mai preservato — il
  # correlation id non deve essere scelto/avvelenato da fuori.
  request_parameters = {
    "overwrite:header.X-Correlation-Id" = "$context.requestId"
  }
}

resource "aws_apigatewayv2_route" "this" {
  api_id    = var.shared.api_id
  route_key = "ANY /api/${var.app_id}/v1/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.this.id}"

  #checkov:skip=CKV_AWS_309:L'authorizer custom (Lambda, verifica JWT) arriva con UC 0014: le route nascono senza auth finché non esiste
}
