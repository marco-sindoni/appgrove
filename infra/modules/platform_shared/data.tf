# ─────────────────────────────────────────────────────────────────────────────
# Risorse dello stack `global` risolte per NOME (zona Route53, certificati ACM):
# nessun accoppiamento con lo state remoto di `global` — basta che `up global`
# sia già stato applicato (ordine garantito da `first-run`, UC 0003).
# ─────────────────────────────────────────────────────────────────────────────

data "aws_caller_identity" "current" {}

data "aws_region" "current" {}

data "aws_route53_zone" "main" {
  name = var.domain
}

locals {
  # #12 9/10: prod sui sottodomini diretti (app./admin./api.appgrove.app),
  # test sul segmento dedicato (app./admin./api.test.appgrove.app).
  dns_prefix = var.env == "prod" ? "" : "${var.env}."

  # Domini con cui sono stati EMESSI i certificati in `global` (acm.tf):
  # prod = apex + wildcard; test = wildcard *.test.<domain>.
  cert_lookup_domain = var.env == "prod" ? var.domain : "*.${var.env}.${var.domain}"

  # Le 2 SPA (#03 13/17): backoffice clienti e console admin interna.
  spa_hosts = {
    backoffice = "app.${local.dns_prefix}${var.domain}"
    admin      = "admin.${local.dns_prefix}${var.domain}"
  }

  api_host = "api.${local.dns_prefix}${var.domain}"
}

# Certificato edge (us-east-1, per CloudFront — vincolo AWS, #06 17).
data "aws_acm_certificate" "edge" {
  provider = aws.us_east_1

  domain      = local.cert_lookup_domain
  statuses    = ["ISSUED"]
  most_recent = true
}

# Certificato regionale (eu-west-1, per il custom domain API Gateway).
data "aws_acm_certificate" "regional" {
  domain      = local.cert_lookup_domain
  statuses    = ["ISSUED"]
  most_recent = true
}
