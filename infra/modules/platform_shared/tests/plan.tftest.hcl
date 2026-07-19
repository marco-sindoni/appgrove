# ─────────────────────────────────────────────────────────────────────────────
# terraform test del modulo platform_shared (#10 29). Nato con UC 0014 (change
# 0039): il modulo era l'unico senza suite, e proprio qui vivono le decisioni
# che, se regredissero in silenzio, aprirebbero l'API o il database.
# Provider AWS FINTO (mock_provider): gira offline, senza credenziali.
# ─────────────────────────────────────────────────────────────────────────────

# I valori finti generati di default sono stringhe casuali: alcuni attributi
# (ARN dei certificati, policy JSON) sono validati dal provider già in plan, e
# vanno quindi forniti in forma plausibile.
mock_provider "aws" {
  mock_data "aws_acm_certificate" {
    defaults = {
      arn = "arn:aws:acm:eu-west-1:123456789012:certificate/00000000-0000-0000-0000-000000000000"
    }
  }
  mock_data "aws_iam_policy_document" {
    defaults = {
      json = "{\"Version\":\"2012-10-17\",\"Statement\":[]}"
    }
  }

  # Identità SES creata in `global` (UC 0018): l'ARN è validato dal provider già in
  # plan, quindi va fornito in forma plausibile come per i certificati.
  mock_data "aws_sesv2_email_identity" {
    defaults = {
      arn = "arn:aws:ses:eu-west-1:123456789012:identity/appgrove.app"
    }
  }

  # Il segreto master di Aurora è gestito da RDS (blocco calcolato): senza
  # valore finto il modulo non può referenziarlo.
  mock_resource "aws_rds_cluster" {
    defaults = {
      master_user_secret = [{
        secret_arn = "arn:aws:secretsmanager:eu-west-1:123456789012:secret:appgrove-test-master-000000"
      }]
    }
  }
}

# CloudFront accetta solo certificati ACM di us-east-1 (#06 17): il modulo
# dichiara l'alias, quindi anche il test deve fornirlo (finto).
mock_provider "aws" {
  alias = "us_east_1"

  mock_data "aws_acm_certificate" {
    defaults = {
      arn = "arn:aws:acm:us-east-1:123456789012:certificate/00000000-0000-0000-0000-000000000000"
    }
  }
}

# In `plan` gli id delle risorse non ancora create sono ignoti, e un assert su
# un valore ignoto non è valutabile. Questi override li rendono noti GIÀ in
# plan, così le verifiche possono confrontare identità reali (issuer, audience,
# security group) invece di limitarsi alle costanti.
override_resource {
  target          = aws_cognito_user_pool.this
  override_during = plan
  values = {
    id  = "eu-west-1_TEST0000"
    arn = "arn:aws:cognito-idp:eu-west-1:123456789012:userpool/eu-west-1_TEST0000"
  }
}

override_resource {
  target          = aws_cognito_user_pool_client.bff
  override_during = plan
  values = {
    id = "0000000000000000000000test"
  }
}

override_resource {
  target          = aws_apigatewayv2_api.this
  override_during = plan
  values = {
    id            = "api00000"
    arn           = "arn:aws:apigateway:eu-west-1::/apis/api00000"
    execution_arn = "arn:aws:execute-api:eu-west-1:123456789012:api00000"
  }
}

override_resource {
  target          = aws_security_group.auth_lambda
  override_during = plan
  values = {
    id = "sg-000000000000000a1"
  }
}

override_resource {
  target          = aws_security_group.pre_token_gen
  override_during = plan
  values = {
    id = "sg-000000000000000b2"
  }
}

override_resource {
  target          = aws_lambda_function.custom_message
  override_during = plan
  values = {
    arn = "arn:aws:lambda:eu-west-1:123456789012:function:appgrove-test-custom-message"
  }
}

override_data {
  target          = data.aws_region.current
  override_during = plan
  values = {
    region = "eu-west-1"
  }
}

variables {
  env                   = "test"
  vpc_id                = "vpc-00000000"
  vpc_cidr              = "10.0.0.0/16"
  subnet_ids            = ["subnet-0000000a", "subnet-0000000b"]
  alert_email           = "ops@example.test"
  deletion_protection   = false
  force_destroy_buckets = true
  use_fargate_spot      = true
}

# ── Authorizer dell'edge (UC 0014) ───────────────────────────────────────────

run "authorizer_jwt_dell_edge" {
  command = plan

  # Nativo, NON custom: il deny di un authorizer Lambda su HTTP API v2 sarebbe
  # sempre 403, rompendo il refresh silenzioso (401) e il banner entitlement
  # (402). Vedi authorizer.tf per la decisione completa.
  assert {
    condition     = aws_apigatewayv2_authorizer.jwt.authorizer_type == "JWT"
    error_message = "L'authorizer dell'edge deve essere JWT nativo, non custom (UC 0014, change 0039)."
  }

  # Se l'header manca, API GW risponde 401 senza valutare l'authorizer: è il
  # codice su cui è costruito il refresh silenzioso della SPA (#03 dec.5/8).
  assert {
    condition     = aws_apigatewayv2_authorizer.jwt.identity_sources == toset(["$request.header.Authorization"])
    error_message = "L'identità va letta dal solo header Authorization."
  }

  # Emittente = il pool dell'ambiente; audience = l'app client del BFF auth
  # (l'access token Cognito non ha `aud`, porta `client_id`).
  assert {
    condition     = one(aws_apigatewayv2_authorizer.jwt.jwt_configuration).issuer == local.cognito_issuer
    error_message = "L'issuer dell'authorizer deve essere il pool Cognito dell'ambiente."
  }
  assert {
    condition     = one(aws_apigatewayv2_authorizer.jwt.jwt_configuration).audience == toset([aws_cognito_user_pool_client.bff.id])
    error_message = "L'audience deve essere l'app client confidenziale del BFF auth (UC 0015)."
  }
  assert {
    condition     = aws_apigatewayv2_authorizer.jwt.api_id == aws_apigatewayv2_api.this.id
    error_message = "L'authorizer deve stare sull'API condivisa dell'ambiente."
  }
}

# ── Rotte pubbliche by-design ────────────────────────────────────────────────

run "rotte_pubbliche_restano_scoperte" {
  command = plan

  # L'autenticazione stessa non può richiedere un token (UC 0015).
  assert {
    # Nessun authorizer configurato = attributo assente nel plan (API GW applica
    # "NONE" lato servizio): è esattamente ciò che vogliamo verificare.
    condition     = one(aws_apigatewayv2_route.error_ingest.*.authorization_type) == null
    error_message = "L'ingest degli errori JS deve restare pubblico: gli errori vanno raccolti anche prima del login (#08)."
  }
  assert {
    condition     = aws_apigatewayv2_route.error_ingest.route_key == "POST /ingest/errors"
    error_message = "La rotta di ingest errori non deve finire sotto /api/<app_id>/v1/*, o l'authorizer la coprirebbe."
  }
}

# ── RDS Proxy: perimetro stretto (residuo E23-b, chiuso da UC 0014) ──────────

run "proxy_db_raggiungibile_solo_dalle_lambda_auth" {
  command = plan

  # Regressione da evitare: tornare a `cidr_blocks = [vpc_cidr]` riaprirebbe il
  # proxy a QUALUNQUE cosa giri nella VPC.
  assert {
    condition     = length(coalesce(one(aws_security_group.rds_proxy.ingress).cidr_blocks, [])) == 0
    error_message = "L'ingress del proxy DB non deve più accettare CIDR (E23-b): solo security group."
  }
  assert {
    condition = one(aws_security_group.rds_proxy.ingress).security_groups == toset([
      aws_security_group.auth_lambda.id,
      aws_security_group.pre_token_gen.id,
    ])
    error_message = "Al proxy DB devono arrivare SOLO il BFF auth e il Pre-Token-Gen."
  }
  assert {
    condition     = one(aws_security_group.rds_proxy.ingress).from_port == 5432
    error_message = "L'unica porta in ingresso sul proxy è Postgres (5432)."
  }
  assert {
    condition     = aws_db_proxy.this.require_tls == true
    error_message = "Il proxy deve imporre TLS (#06 §20bis)."
  }
}

# ── Email di autenticazione (UC 0018, change 0040) ───────────────────────────
#
# Queste verifiche presidiano cose che in locale non si rompono mai: in locale le
# email le manda il servizio Java verso Mailpit. Una regressione qui si vedrebbe
# soltanto in cloud, e soltanto a utente reale.

run "email_auth_via_ses_e_custom_message" {
  command = plan

  # Senza il trigger cablato, Cognito manda i suoi testi di default: inglese non
  # brandizzato e nessuna localizzazione. È il cuore dello use case.
  assert {
    condition     = one(aws_cognito_user_pool.this.lambda_config).custom_message == aws_lambda_function.custom_message.arn
    error_message = "Il trigger Custom Message deve essere cablato sul pool (UC 0018)."
  }

  # DEVELOPER = spedisce SES con la nostra identità. Il default (COGNITO_DEFAULT)
  # ha un tetto di circa 50 email al giorno: superato quello, le registrazioni
  # smettono di funzionare senza che nulla nel nostro codice sia cambiato.
  assert {
    condition     = one(aws_cognito_user_pool.this.email_configuration).email_sending_account == "DEVELOPER"
    error_message = "Le email del pool devono partire da SES, non dal mittente di default di Cognito."
  }
  assert {
    condition     = one(aws_cognito_user_pool.this.email_configuration).from_email_address == "noreply@appgrove.app"
    error_message = "Il mittente deve essere noreply@<domain> (identità verificata con DKIM)."
  }

  # Fuori dalla VPC by-design: non tocca il database (la lingua le arriva come
  # parametro della chiamata). Rimetterla in rete privata aggiungerebbe avvio a
  # freddo su ogni email, contro un trigger che Cognito taglia a 5 secondi.
  assert {
    condition     = length(aws_lambda_function.custom_message.vpc_config) == 0
    error_message = "Il Custom Message Lambda non deve stare in VPC: non accede al database (UC 0018)."
  }

  # Non spedisce lei (spedisce Cognito) e non legge segreti: il solo ruolo dei log.
  assert {
    condition     = aws_iam_role_policy_attachment.custom_message_logs.policy_arn == "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
    error_message = "Il Custom Message Lambda deve avere i soli permessi di log."
  }

  # Cognito deve poterla invocare, e solo dal NOSTRO pool.
  assert {
    condition     = aws_lambda_permission.custom_message_cognito.source_arn == aws_cognito_user_pool.this.arn
    error_message = "L'invocazione deve essere autorizzata al solo user pool dell'ambiente."
  }
}
