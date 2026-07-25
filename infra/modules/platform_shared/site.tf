# ─────────────────────────────────────────────────────────────────────────────
# Vetrina Astro (UC 0036, #14 B4/15) — TERZO artefatto oltre alle 2 SPA.
#
# Distribuzione statica dedicata: bucket S3 PRIVATO letto da CloudFront via OAC
# (stesso pattern delle SPA in cloudfront.tf, riusato invece che duplicato), con
# una CloudFront Function che riscrive gli URL "puliti" verso i file index.html
# di Astro e, in test, applica basic auth (#14 54). Host: <env-prefix><domain>
# (prod: appgrove.app; test: test.appgrove.app). Il backoffice "coming soon" è una
# PAGINA dell'artefatto: l'host app.<env> resta della SPA (cloudfront.tf) — quale
# host serva la "coming soon" durante un rollout statico-first è scelta operativa.
#
# APPLY differito (phased-env: locale→test→prod). Qui l'infra deve solo passare
# fmt+validate; il deploy dei file e l'invalidazione sono della pipeline (UC 0005).
# ─────────────────────────────────────────────────────────────────────────────

locals {
  site_host = "${local.dns_prefix}${var.domain}"

  # Basic auth attiva SOLO in test e SOLO se sono state fornite le credenziali
  # (nessun segreto committato: default vuoto → disattiva in validate/prod).
  site_basic_auth_enabled = var.env == "test" && var.site_basic_auth_userpass != ""
}

resource "aws_s3_bucket" "site" {
  bucket        = "appgrove-site-${var.env}-${data.aws_caller_identity.current.account_id}"
  force_destroy = var.force_destroy_buckets

  #checkov:skip=CKV_AWS_21:Versioning inutile: sito statico rigenerabile, ripubblicato dalla CI (UC 0005)
  #checkov:skip=CKV_AWS_144:Replica cross-region non necessaria: asset rigenerabili (cost-min)
  #checkov:skip=CKV_AWS_18:Access logging non necessario: accessi solo da CloudFront via OAC (cost-min)
  #checkov:skip=CKV2_AWS_62:Nessuna event notification necessaria (pubblicazione push dalla CI)
  #checkov:skip=CKV_AWS_145:SSE-S3 (AES256) sufficiente: chiavi gestite AWS di default (#06 §20bis)
  #checkov:skip=CKV2_AWS_61:Nessun lifecycle: la CI sovrascrive i file a ogni release (niente oggetti orfani)

  tags = { Name = "appgrove-site-${var.env}" }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "site" {
  bucket = aws_s3_bucket.site.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
    bucket_key_enabled = true
  }
}

resource "aws_s3_bucket_public_access_block" "site" {
  bucket                  = aws_s3_bucket.site.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_cloudfront_origin_access_control" "site" {
  name                              = "appgrove-site-${var.env}"
  description                       = "Accesso CloudFront → bucket vetrina (${var.env})"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

# Riscrittura URL puliti → index.html (+ basic auth in test). Il codice è generato
# da template così il blocco basic-auth esiste solo in test.
resource "aws_cloudfront_function" "site_viewer_request" {
  name    = "appgrove-site-${var.env}-viewer-request"
  runtime = "cloudfront-js-2.0"
  comment = "Vetrina ${var.env}: rewrite URL directory→index.html; basic auth in test"
  publish = true
  code = templatefile("${path.module}/functions/site-viewer-request.js.tftpl", {
    basic_auth_enabled = local.site_basic_auth_enabled
    basic_auth_b64     = local.site_basic_auth_enabled ? base64encode(var.site_basic_auth_userpass) : ""
  })
}

# Header di risposta: sicurezza (HSTS ecc.) + X-Robots-Tag noindex fino al go-live
# (#14 54: rimozione = passare var.site_indexable = true). Ridondante col meta
# noindex delle pagine, ma copre anche risposte non-HTML.
resource "aws_cloudfront_response_headers_policy" "site" {
  name = "appgrove-site-${var.env}"

  security_headers_config {
    strict_transport_security {
      access_control_max_age_sec = 63072000
      include_subdomains         = true
      preload                    = true
      override                   = true
    }
    content_type_options { override = true }
    frame_options {
      frame_option = "DENY"
      override     = true
    }
    referrer_policy {
      referrer_policy = "strict-origin-when-cross-origin"
      override        = true
    }
  }

  dynamic "custom_headers_config" {
    for_each = var.site_indexable ? [] : [1]
    content {
      items {
        header   = "X-Robots-Tag"
        value    = "noindex, nofollow"
        override = true
      }
    }
  }
}

resource "aws_cloudfront_distribution" "site" {
  enabled             = true
  is_ipv6_enabled     = true
  comment             = "appgrove vetrina (${var.env}) — ${local.site_host}"
  default_root_object = "index.html"
  aliases             = [local.site_host]
  price_class         = "PriceClass_100" # Europa + Nord America: cost-min, utenti UE (#06 6)

  origin {
    origin_id                = "s3-site"
    domain_name              = aws_s3_bucket.site.bucket_regional_domain_name
    origin_access_control_id = aws_cloudfront_origin_access_control.site.id
  }

  default_cache_behavior {
    target_origin_id           = "s3-site"
    allowed_methods            = ["GET", "HEAD"]
    cached_methods             = ["GET", "HEAD"]
    viewer_protocol_policy     = "redirect-to-https" # TLS ovunque (#06 §20bis)
    compress                   = true
    cache_policy_id            = data.aws_cloudfront_cache_policy.caching_optimized.id
    response_headers_policy_id = aws_cloudfront_response_headers_policy.site.id

    function_association {
      event_type   = "viewer-request"
      function_arn = aws_cloudfront_function.site_viewer_request.arn
    }
  }

  # Sito multi-pagina (non SPA): un percorso inesistente è un vero 404.
  custom_error_response {
    error_code            = 404
    response_code         = 404
    response_page_path    = "/404.html"
    error_caching_min_ttl = 10
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    acm_certificate_arn      = data.aws_acm_certificate.edge.arn # us-east-1 (vincolo AWS)
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }

  #checkov:skip=CKV_AWS_68:WAF rimandato by-design (evoluzione E6, #06 21)
  #checkov:skip=CKV2_AWS_47:WAF (e regole Log4j) rimandati by-design (evoluzione E6, #06 21)
  #checkov:skip=CKV_AWS_86:Access logging CloudFront spento (cost-min); observability = UC 0006
  #checkov:skip=CKV_AWS_310:Nessun origin failover: origine unica S3 (HA = evoluzione E3)
  #checkov:skip=CKV2_AWS_42:Il certificato ACM custom c'è (viewer_certificate); falso positivo con data source
  #checkov:skip=CKV_AWS_374:Nessuna restrizione geografica: SaaS pubblico, mercato UE ma accesso globale

  tags = { Name = "appgrove-site-${var.env}" }
}

# Il bucket accetta SOLO letture da CloudFront (OAC) e SOLO su TLS (#06 §20bis).
data "aws_iam_policy_document" "site_bucket" {
  statement {
    sid       = "AllowCloudFrontOAC"
    effect    = "Allow"
    actions   = ["s3:GetObject"]
    resources = ["${aws_s3_bucket.site.arn}/*"]

    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.site.arn]
    }
  }

  statement {
    sid     = "DenyInsecureTransport"
    effect  = "Deny"
    actions = ["s3:*"]
    resources = [
      aws_s3_bucket.site.arn,
      "${aws_s3_bucket.site.arn}/*",
    ]

    principals {
      type        = "AWS"
      identifiers = ["*"]
    }

    condition {
      test     = "Bool"
      variable = "aws:SecureTransport"
      values   = ["false"]
    }
  }
}

resource "aws_s3_bucket_policy" "site" {
  bucket = aws_s3_bucket.site.id
  policy = data.aws_iam_policy_document.site_bucket.json

  depends_on = [aws_s3_bucket_public_access_block.site]
}

# Alias DNS della vetrina (A + AAAA) verso la distribuzione.
resource "aws_route53_record" "site" {
  for_each = toset(["A", "AAAA"])

  zone_id = data.aws_route53_zone.main.zone_id
  name    = local.site_host
  type    = each.value

  alias {
    name                   = aws_cloudfront_distribution.site.domain_name
    zone_id                = aws_cloudfront_distribution.site.hosted_zone_id
    evaluate_target_health = false
  }
}
