# ─────────────────────────────────────────────────────────────────────────────
# Route del servizio sull'API HTTP condivisa (#06 8/22): /api/<app_id>/v1/* →
# VPC Link → Cloud Map (record SRV del service ECS), protette dall'authorizer
# JWT dell'edge (UC 0014, `platform_shared/authorizer.tf`). La configurazione
# CORS resta differita (UC 0004 "Punti aperti").
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

  # Gate 1 della catena (#09 dec.30) all'EDGE: firma, issuer, audience e
  # SCADENZA del token verificati da API GW prima di toccare il servizio.
  # Token assente/scaduto/invalido → 401, il codice su cui è costruito il
  # refresh silenzioso della SPA. I gate app-abilitata/entitled restano nel
  # servizio (UC 0027): vedi platform_shared/authorizer.tf per il perché.
  authorization_type = "JWT"
  authorizer_id      = var.shared.authorizer_id
}

# Route PUBBLICHE dell'app: più specifiche del proxy generico, quindi vincono il
# routing di API GW e "bucano" l'authorizer in modo DICHIARATIVO — niente
# allow-list dentro il codice, che si disallinea in silenzio quando qualcuno
# aggiunge un endpoint. Sono chiamate da terzi che non hanno un access token e
# si autenticano in altro modo (oggi solo il webhook Paddle, firma HMAC).
# Il censimento (change 0039) ha verificato che TUTTO il resto sotto
# /api/<app_id>/v1/* è già @Authenticated o @RolesAllowed.
resource "aws_apigatewayv2_route" "public" {
  for_each = toset(var.public_routes)

  api_id    = var.shared.api_id
  route_key = each.value
  target    = "integrations/${aws_apigatewayv2_integration.this.id}"

  #checkov:skip=CKV_AWS_309:Rotta pubblica by-design: il chiamante non ha un access token e si autentica altrimenti (es. firma HMAC del webhook Paddle, PaddleSignature)

  lifecycle {
    precondition {
      # Un'app non può esporre percorsi fuori dal PROPRIO prefisso: la deroga
      # all'authorizer resta confinata al perimetro dell'app che la chiede.
      condition     = can(regex("^(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS|ANY) /api/${var.app_id}/v1/", each.value))
      error_message = "public_routes: '${each.value}' deve essere '<METODO> /api/${var.app_id}/v1/...' — un'app non espone percorsi altrui."
    }
  }
}
