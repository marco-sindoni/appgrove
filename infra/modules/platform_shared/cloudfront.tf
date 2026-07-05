# ─────────────────────────────────────────────────────────────────────────────
# Le 2 distribuzioni CloudFront delle SPA (#06 17, #03 13/17):
#   • backoffice clienti → app.<env-prefix><domain>
#   • console admin      → admin.<env-prefix><domain>
# Bucket S3 PRIVATI (nessun sito statico S3): CloudFront legge via OAC; il
# fallback SPA (403/404 → index.html) serve il routing client-side.
# I bundle li pubblica la pipeline frontend (UC 0005).
# La distribuzione della VETRINA Astro è di UC 0036 (esclusa qui).
# ─────────────────────────────────────────────────────────────────────────────

# Cache policy e security-headers policy GESTITE da AWS (niente da mantenere):
# CachingOptimized per asset statici; HSTS e simili sul dominio .app (#12).
data "aws_cloudfront_cache_policy" "caching_optimized" {
  name = "Managed-CachingOptimized"
}

data "aws_cloudfront_response_headers_policy" "security_headers" {
  name = "Managed-SecurityHeadersPolicy"
}

resource "aws_s3_bucket" "spa" {
  for_each = local.spa_hosts

  # Suffisso account ID: i nomi bucket sono globali.
  bucket = "appgrove-spa-${each.key}-${var.env}-${data.aws_caller_identity.current.account_id}"

  # test: destroy libero; prod: svuotamento esplicito (#06 24).
  force_destroy = var.force_destroy_buckets

  #checkov:skip=CKV_AWS_21:Versioning inutile: bundle SPA rigenerabili, ripubblicati dalla CI (UC 0005)
  #checkov:skip=CKV_AWS_144:Replica cross-region non necessaria: asset rigenerabili (cost-min)
  #checkov:skip=CKV_AWS_18:Access logging non necessario: accessi solo da CloudFront via OAC (cost-min)
  #checkov:skip=CKV2_AWS_62:Nessuna event notification necessaria (pubblicazione push dalla CI)
  #checkov:skip=CKV_AWS_145:SSE-S3 (AES256) sufficiente: chiavi gestite AWS di default (#06 §20bis)
  #checkov:skip=CKV2_AWS_61:Nessun lifecycle: la CI sovrascrive i bundle a ogni release (niente oggetti orfani)

  tags = {
    Name = "appgrove-spa-${each.key}-${var.env}"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "spa" {
  for_each = aws_s3_bucket.spa

  bucket = each.value.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
    bucket_key_enabled = true
  }
}

resource "aws_s3_bucket_public_access_block" "spa" {
  for_each = aws_s3_bucket.spa

  bucket                  = each.value.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_cloudfront_origin_access_control" "spa" {
  for_each = local.spa_hosts

  name                              = "appgrove-spa-${each.key}-${var.env}"
  description                       = "Accesso CloudFront → bucket SPA ${each.key} (${var.env})"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

resource "aws_cloudfront_distribution" "spa" {
  for_each = local.spa_hosts

  enabled             = true
  is_ipv6_enabled     = true
  comment             = "appgrove ${each.key} (${var.env}) — ${each.value}"
  default_root_object = "index.html"
  aliases             = [each.value]
  price_class         = "PriceClass_100" # Europa + Nord America: cost-min, utenti UE (#06 6)

  origin {
    origin_id                = "s3-${each.key}"
    domain_name              = aws_s3_bucket.spa[each.key].bucket_regional_domain_name
    origin_access_control_id = aws_cloudfront_origin_access_control.spa[each.key].id
  }

  default_cache_behavior {
    target_origin_id           = "s3-${each.key}"
    allowed_methods            = ["GET", "HEAD"]
    cached_methods             = ["GET", "HEAD"]
    viewer_protocol_policy     = "redirect-to-https" # TLS ovunque (#06 §20bis)
    compress                   = true
    cache_policy_id            = data.aws_cloudfront_cache_policy.caching_optimized.id
    response_headers_policy_id = data.aws_cloudfront_response_headers_policy.security_headers.id
  }

  # Fallback SPA (#06 17): il routing è client-side; con OAC un path inesistente
  # su S3 risponde 403 — entrambi tornano la shell React.
  custom_error_response {
    error_code            = 403
    response_code         = 200
    response_page_path    = "/index.html"
    error_caching_min_ttl = 10
  }

  custom_error_response {
    error_code            = 404
    response_code         = 200
    response_page_path    = "/index.html"
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
  #checkov:skip=CKV2_AWS_42:Il certificato ACM custom c'è (viewer_certificate); falso positivo con for_each/data
  #checkov:skip=CKV_AWS_374:Nessuna restrizione geografica: SaaS pubblico, mercato UE ma accesso globale

  tags = {
    Name = "appgrove-spa-${each.key}-${var.env}"
  }
}

# Il bucket accetta SOLO letture da CloudFront (OAC) e SOLO su TLS (#06 §20bis).
data "aws_iam_policy_document" "spa_bucket" {
  for_each = local.spa_hosts

  statement {
    sid       = "AllowCloudFrontOAC"
    effect    = "Allow"
    actions   = ["s3:GetObject"]
    resources = ["${aws_s3_bucket.spa[each.key].arn}/*"]

    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.spa[each.key].arn]
    }
  }

  statement {
    sid     = "DenyInsecureTransport"
    effect  = "Deny"
    actions = ["s3:*"]
    resources = [
      aws_s3_bucket.spa[each.key].arn,
      "${aws_s3_bucket.spa[each.key].arn}/*",
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

resource "aws_s3_bucket_policy" "spa" {
  for_each = local.spa_hosts

  bucket = aws_s3_bucket.spa[each.key].id
  policy = data.aws_iam_policy_document.spa_bucket[each.key].json

  depends_on = [aws_s3_bucket_public_access_block.spa]
}

# Alias DNS delle SPA (A + AAAA) verso le distribuzioni.
resource "aws_route53_record" "spa" {
  for_each = {
    for pair in setproduct(keys(local.spa_hosts), ["A", "AAAA"]) :
    "${pair[0]}-${pair[1]}" => { app = pair[0], type = pair[1] }
  }

  zone_id = data.aws_route53_zone.main.zone_id
  name    = local.spa_hosts[each.value.app]
  type    = each.value.type

  alias {
    name                   = aws_cloudfront_distribution.spa[each.value.app].domain_name
    zone_id                = aws_cloudfront_distribution.spa[each.value.app].hosted_zone_id
    evaluate_target_health = false
  }
}
