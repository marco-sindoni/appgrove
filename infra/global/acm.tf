# ─────────────────────────────────────────────────────────────────────────────
# Certificati ACM (gratuiti), per livello e per regione (#06 17, #12 12):
#
#   • "prod": apex `appgrove.app` + wildcard `*.appgrove.app`
#     (copre anche `test.appgrove.app`, ma NON i suoi sottodomini)
#   • "test": wildcard `*.test.appgrove.app` (app.test, admin.test, api.test, …)
#
# Ogni livello serve in DUE regioni: us-east-1 per CloudFront (vincolo AWS) e
# eu-west-1 per gli endpoint regionali (API Gateway). Totale: 4 certificati.
#
# Validazione DNS: i record CNAME di validazione sono IDENTICI per lo stesso
# dominio nello stesso account, indipendentemente dalla regione — si creano una
# volta sola (allow_overwrite) e validano entrambe le regioni.
#
# ⚠️ La validazione si completa solo quando i name server della zona sono
# effettivamente delegati al dominio (registrar → NS della zona Route53):
# `first-run` lo segnala prima dell'apply.
# ─────────────────────────────────────────────────────────────────────────────

locals {
  cert_domains = {
    prod = {
      domain = var.domain
      sans   = ["*.${var.domain}"]
    }
    test = {
      domain = "*.test.${var.domain}"
      sans   = []
    }
  }
}

# ── Certificati edge (us-east-1, per CloudFront) ─────────────────────────────
resource "aws_acm_certificate" "edge" {
  for_each = local.cert_domains
  provider = aws.us_east_1

  domain_name               = each.value.domain
  subject_alternative_names = each.value.sans
  validation_method         = "DNS"

  lifecycle {
    create_before_destroy = true # rinnovo/sostituzione senza downtime
  }
}

# ── Certificati regionali (eu-west-1, per API Gateway) ───────────────────────
resource "aws_acm_certificate" "regional" {
  for_each = local.cert_domains

  domain_name               = each.value.domain
  subject_alternative_names = each.value.sans
  validation_method         = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}

# ── Record DNS di validazione (una volta sola, riusati da entrambe le regioni) ──
locals {
  # domain_validation_options è un set di oggetti {domain_name, resource_record_*}:
  # lo si appiattisce su tutti i certificati edge e si deduplica per record name.
  validation_records = {
    for dvo in flatten([
      for cert in aws_acm_certificate.edge : tolist(cert.domain_validation_options)
      ]) : dvo.resource_record_name => {
      type  = dvo.resource_record_type
      value = dvo.resource_record_value
    }
  }
}

resource "aws_route53_record" "acm_validation" {
  for_each = local.validation_records

  zone_id         = aws_route53_zone.main.zone_id
  name            = each.key
  type            = each.value.type
  records         = [each.value.value]
  ttl             = 300
  allow_overwrite = true # lo stesso CNAME vale per il certificato edge e per quello regionale
}

# Attesa esplicita del completamento della validazione (fallisce se gli NS non
# sono delegati: vedere l'avviso in testa al file).
resource "aws_acm_certificate_validation" "edge" {
  for_each = aws_acm_certificate.edge
  provider = aws.us_east_1

  certificate_arn         = each.value.arn
  validation_record_fqdns = [for r in aws_route53_record.acm_validation : r.fqdn]
}

resource "aws_acm_certificate_validation" "regional" {
  for_each = aws_acm_certificate.regional

  certificate_arn         = each.value.arn
  validation_record_fqdns = [for r in aws_route53_record.acm_validation : r.fqdn]
}
