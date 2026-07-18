# ─────────────────────────────────────────────────────────────────────────────
# Authorizer al bordo (UC 0014): chiude le route `/api/<app_id>/v1/*`, che fino
# a qui nascevano SENZA autenticazione. Le route le crea `microsaas_app`, che
# riceve questo id via l'oggetto `shared` — così ogni app futura è protetta
# per costruzione (invariante #3), senza infra su misura.
#
# PERCHÉ NATIVO E NON UNA LAMBDA NOSTRA (deviazione da UC 0014, change 0039).
# Lo use case prevedeva una Lambda che facesse al bordo tre gate: JWT (401),
# app abilitata (403), tenant entitled (402). Non è esprimibile su HTTP API v2:
# il deny di un authorizer custom diventa SEMPRE 403, non personalizzabile (le
# gateway response si riscrivono solo su REST API, più costosa). Effetti:
#   • token SCADUTO → 403 invece di 401 ⇒ il refresh silenzioso della SPA
#     (packages/api-client/src/auth-middleware.ts, contratto #03 dec.5/8) non
#     scatterebbe mai e l'utente cadrebbe fuori a ogni scadenza (pochi minuti);
#   • tenant non entitled → 403 invece di 402 ⇒ salta il banner azionabile
#     "abbonamento richiesto" (apps/backoffice/src/billing/enforcement.ts, UC 0028).
# L'authorizer JWT nativo invece risponde 401 corretto, costa $0, non ha cold
# start e non interroga il DB sul percorso critico di OGNI richiesta (la Lambda
# sì: UC 0014 vieta la cache). I gate app-abilitata/entitled restano NEL
# SERVIZIO (UC 0027, `commons/entitlement`), dove hanno già i codici giusti e
# sono testabili con un DB vero. Vedi UC 0014 §Punti aperti per il residuo
# (blocco all'edge) e changes/0039-*/requirements.md per la decisione completa.
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_apigatewayv2_authorizer" "jwt" {
  api_id          = aws_apigatewayv2_api.this.id
  name            = "appgrove-${var.env}-jwt"
  authorizer_type = "JWT"

  # Solo l'header Authorization (`Bearer <access token>`): se manca, API GW
  # risponde 401 senza nemmeno valutare l'authorizer — esattamente il codice su
  # cui è costruito il refresh silenzioso della SPA.
  identity_sources = ["$request.header.Authorization"]

  jwt_configuration {
    # Access token Cognito: NON ha `aud`, porta `client_id` — API GW valuta
    # `client_id` quando `aud` è assente. È lo stesso app client confidenziale
    # del BFF auth (UC 0015), coerente col check `client_id` che i servizi
    # rifanno in profondità (AccessTokenGuardFilter, UC 0016).
    audience = [aws_cognito_user_pool_client.bff.id]
    issuer   = local.cognito_issuer
  }
}
