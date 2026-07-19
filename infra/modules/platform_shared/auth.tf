# ─────────────────────────────────────────────────────────────────────────────
# Cognito per ambiente (UC 0015, #02 A/B, #06 18): user pool "solo
# autenticazione" (membership/ruoli nel core, #01) + app client CONFIDENZIALE
# (con secret) usato dal solo BFF auth server-side — niente SRP nel browser
# (#02 2/4). Il secret vive in SSM SecureString (#02 15, store #12): la Lambda
# lo legge a runtime, mai in chiaro nelle env.
# Le email (verifica/reset) partono da SES come `noreply@<domain>` con firma DKIM,
# testo e lingua EN/IT dal Custom Message Lambda (UC 0018).
# L'iniezione dei claim tenant_id/roles è del Pre-Token-Gen (UC 0016).
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_cognito_user_pool" "this" {
  name = "appgrove-${var.env}-users"

  # Piano funzionalità ESSENTIALS (UC 0016): prerequisito per personalizzare
  # l'ACCESS token dal Pre-Token-Gen (evento trigger V2_0). Fascia gratuita di
  # 10.000 utenti attivi/mese, è già il piano di default dei nuovi pool (#02 11,
  # costi → _COSTI-AWS).
  user_pool_tier = "ESSENTIALS"

  # Login con la propria email (case-insensitive); verifica email automatica.
  username_attributes      = ["email"]
  auto_verified_attributes = ["email"]

  # Pre-Token-Generation (UC 0016): inietta tenant_id/roles nell'access token.
  # V2_0 = personalizzazione dell'access token (richiede il piano ESSENTIALS).
  # Custom Message (UC 0018): compone testo e lingua delle email prima dell'invio.
  lambda_config {
    pre_token_generation_config {
      lambda_arn     = aws_lambda_function.pre_token_gen.arn
      lambda_version = "V2_0"
    }

    custom_message = aws_lambda_function.custom_message.arn
  }

  # Invio via SES con la NOSTRA identità di dominio (UC 0018, #06 26) invece del
  # mittente di default di Cognito, che ha copy inglese non brandizzato e un tetto
  # di circa 50 email al giorno — inutilizzabile oltre la fase di prova.
  #
  # `from_email_address` è ciò che vede il destinatario; l'identità è verificata con
  # firma DKIM nello stack `global` (una sola per dominio, condivisa fra ambienti).
  email_configuration {
    email_sending_account = "DEVELOPER"
    source_arn            = data.aws_sesv2_email_identity.main.arn
    from_email_address    = "noreply@${var.domain}"
  }

  username_configuration {
    case_sensitive = false
  }

  # Policy #02 19: min 10, maiuscola+minuscola+numero (simboli non richiesti).
  password_policy {
    minimum_length    = 10
    require_uppercase = true
    require_lowercase = true
    require_numbers   = true
    require_symbols   = false
  }

  # 2FA TOTP opzionale, opt-in dal profilo (#02 18): mai obbligatoria al signup.
  mfa_configuration = "OPTIONAL"

  software_token_mfa_configuration {
    enabled = true
  }

  account_recovery_setting {
    recovery_mechanism {
      name     = "verified_email"
      priority = 1
    }
  }

  # prod: il pool contiene le utenze reali → protetto da destroy (#06 24).
  deletion_protection = var.deletion_protection ? "ACTIVE" : "INACTIVE"

  tags = {
    Name = "appgrove-${var.env}-users"
  }
}

resource "aws_cognito_user_pool_client" "bff" {
  name         = "appgrove-${var.env}-auth-bff"
  user_pool_id = aws_cognito_user_pool.this.id

  # Client confidenziale (#02 4): l'auth è server-side nella Lambda BFF.
  generate_secret = true

  # USER_PASSWORD_AUTH: le credenziali viaggiano SOLO BFF→Cognito su TLS
  # dentro la VPC (endpoint cognito-idp); il browser non parla mai con Cognito.
  explicit_auth_flows = [
    "ALLOW_USER_PASSWORD_AUTH",
    "ALLOW_REFRESH_TOKEN_AUTH",
  ]

  # TTL #02 5: access/id 15 minuti, refresh 24 ore.
  access_token_validity  = 15
  id_token_validity      = 15
  refresh_token_validity = 24

  token_validity_units {
    access_token  = "minutes"
    id_token      = "minutes"
    refresh_token = "hours"
  }

  # Rotazione del refresh token a ogni refresh (#02 2/5): il BFF ruota il
  # cookie col token nuovo; breve grazia per le richieste concorrenti.
  refresh_token_rotation {
    feature                    = "ENABLED"
    retry_grace_period_seconds = 30
  }

  # Logout con revoca (#02 2) e risposte anti-enumeration.
  enable_token_revocation       = true
  prevent_user_existence_errors = "ENABLED"
}

# Client secret → SSM SecureString (#02 15, convenzione /appgrove/<env>/<area>/<chiave>).
resource "aws_ssm_parameter" "auth_client_secret" {
  name        = "/appgrove/${var.env}/auth/client-secret"
  description = "Client secret dell'app client Cognito del BFF auth (letto a runtime dalla Lambda)"
  type        = "SecureString"
  value       = aws_cognito_user_pool_client.bff.client_secret

  #checkov:skip=CKV_AWS_337:SecureString con chiave gestita AWS (aws/ssm) by-design: CMK solo se servirà (#06 §20bis)

  tags = {
    Name = "/appgrove/${var.env}/auth/client-secret"
  }
}

locals {
  # Issuer del pool: usato dall'authorizer JWT dell'edge (UC 0014), dagli output
  # per i servizi (validazione in profondità, UC 0016) e per l'URL delle JWKS.
  cognito_issuer = "https://cognito-idp.${data.aws_region.current.region}.amazonaws.com/${aws_cognito_user_pool.this.id}"
}
