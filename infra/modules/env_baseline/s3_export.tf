# ─────────────────────────────────────────────────────────────────────────────
# Bucket S3 degli export GDPR (UC 0032; punto differito di UC 0003, #13 D22/E23):
#   • privato (nessun accesso pubblico) e cifrato at rest (SSE-S3);
#   • auto-cancellazione degli oggetti a 7 GIORNI (lifecycle): un export non
#     scaricato scade da solo — niente dati personali che ristagnano;
#   • vi accede SOLO il ruolo dei servizi (UC 0004); l'utente scarica tramite
#     presigned URL a 7 giorni generati dal core — mai accesso diretto.
# In locale il ruolo è svolto da MinIO (stack dev). Il nome è pubblicato su SSM
# (ssm.tf) con la chiave letta dai servizi (`appgrove.gdpr.export-bucket`).
# ─────────────────────────────────────────────────────────────────────────────

data "aws_caller_identity" "current" {}

resource "aws_s3_bucket" "gdpr_export" {
  # Suffisso account ID: i nomi bucket sono globali.
  bucket = "appgrove-gdpr-export-${var.env}-${data.aws_caller_identity.current.account_id}"

  # test: destroy libero (svuota da solo); prod: svuotamento esplicito (#06 24).
  force_destroy = var.force_destroy_buckets

  #checkov:skip=CKV_AWS_21:Versioning inutile: oggetti effimeri auto-cancellati a 7 giorni
  #checkov:skip=CKV_AWS_144:Replica cross-region non necessaria: export effimeri (cost-min)
  #checkov:skip=CKV_AWS_18:Access logging non necessario: accessi solo via presigned URL tracciati dal core (cost-min)
  #checkov:skip=CKV2_AWS_62:Nessuna event notification necessaria (il core fa polling dello stato export)
  #checkov:skip=CKV_AWS_145:SSE-S3 (AES256) sufficiente: chiavi gestite AWS di default (#06 §20bis)
}

resource "aws_s3_bucket_server_side_encryption_configuration" "gdpr_export" {
  bucket = aws_s3_bucket.gdpr_export.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
    bucket_key_enabled = true
  }
}

resource "aws_s3_bucket_public_access_block" "gdpr_export" {
  bucket                  = aws_s3_bucket.gdpr_export.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Auto-cancellazione a 7 giorni (#13 D22): la scadenza dell'export coincide con
# quella del presigned URL. Gli upload multipart interrotti si puliscono anch'essi.
resource "aws_s3_bucket_lifecycle_configuration" "gdpr_export" {
  bucket = aws_s3_bucket.gdpr_export.id

  rule {
    id     = "expire-exports-7d"
    status = "Enabled"

    filter {} # tutto il bucket

    expiration {
      days = 7
    }

    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }
  }
}

# Cifratura in transito obbligatoria (#06 §20bis).
resource "aws_s3_bucket_policy" "gdpr_export_tls_only" {
  bucket = aws_s3_bucket.gdpr_export.id
  policy = data.aws_iam_policy_document.gdpr_export_tls_only.json

  depends_on = [aws_s3_bucket_public_access_block.gdpr_export]
}

data "aws_iam_policy_document" "gdpr_export_tls_only" {
  statement {
    sid     = "DenyInsecureTransport"
    effect  = "Deny"
    actions = ["s3:*"]
    resources = [
      aws_s3_bucket.gdpr_export.arn,
      "${aws_s3_bucket.gdpr_export.arn}/*",
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
