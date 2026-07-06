# ─────────────────────────────────────────────────────────────────────────────
# Osservabilità condivisa dell'ambiente (UC 0006, #08):
#   • topic SNS `critical`/`warning` (#08 15): recapito email; Slack/Telegram
#     sono un'evoluzione (basta aggiungere una subscription);
#   • archivio audit/sicurezza (#08 28/29): subscription filter per-servizio
#     (li crea microsaas_app) → Firehose → S3 → Glacier → scadenza 12 mesi.
#     Si archivia SOLO l'audit (log_type=audit): i log operativi scadono con
#     la retention breve (minimizzazione GDPR).
# Gli allarmi/dashboard che CONSUMANO questi punti di aggancio vivono nel
# modulo `observability` (a valle delle app) e in `microsaas_app` (per-servizio).
# ─────────────────────────────────────────────────────────────────────────────

locals {
  obs_log_retention_days = var.env == "prod" ? 30 : 7
}

# ── Topic di allarme (#08 15) ────────────────────────────────────────────────

resource "aws_sns_topic" "alarms" {
  for_each = toset(["critical", "warning"])

  name = "appgrove-${var.env}-alarms-${each.key}"

  # Niente CMK: i messaggi di allarme non contengono dati sensibili e la chiave
  # gestita AWS (alias/aws/sns) non è utilizzabile da CloudWatch/EventBridge
  # come publisher; una CMK dedicata (~$1/mese) andrebbe oltre il cost-min.
  #checkov:skip=CKV_AWS_26:Solo metadati di allarme (nessun dato personale); CMK non giustificata (#06 §20bis, cost-min)

  tags = {
    Name = "appgrove-${var.env}-alarms-${each.key}"
  }
}

resource "aws_sns_topic_subscription" "alarms_email" {
  for_each = aws_sns_topic.alarms

  topic_arn = each.value.arn
  protocol  = "email"
  endpoint  = var.alert_email # la subscription email va CONFERMATA dal destinatario (mail AWS)
}

# EventBridge (regola "task ECS non parte", modulo observability) deve poter
# pubblicare sul topic critical.
resource "aws_sns_topic_policy" "alarms_critical_events" {
  arn = aws_sns_topic.alarms["critical"].arn

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "AllowEventBridgePublish"
        Effect    = "Allow"
        Principal = { Service = "events.amazonaws.com" }
        Action    = "sns:Publish"
        Resource  = aws_sns_topic.alarms["critical"].arn
        Condition = {
          StringEquals = { "aws:SourceAccount" = data.aws_caller_identity.current.account_id }
        }
      }
    ]
  })
}

# ── Archivio audit/sicurezza (#08 28/29, #13 E) ──────────────────────────────

resource "aws_s3_bucket" "audit_archive" {
  # Suffisso account ID: i nomi bucket sono globali.
  bucket = "appgrove-audit-archive-${var.env}-${data.aws_caller_identity.current.account_id}"

  # test: destroy libero; prod: l'archivio non si svuota mai da solo (#06 24).
  force_destroy = var.force_destroy_buckets

  #checkov:skip=CKV_AWS_21:Versioning inutile: archivio append-only alimentato solo da Firehose, scadenza gestita dal lifecycle
  #checkov:skip=CKV_AWS_144:Replica cross-region non necessaria (cost-min): copertura forense, non disaster recovery
  #checkov:skip=CKV_AWS_18:Access logging non necessario: nessun accesso applicativo, solo Firehose in scrittura e consultazione forense
  #checkov:skip=CKV2_AWS_62:Nessuna event notification: l'archivio si consulta, non si processa
  #checkov:skip=CKV_AWS_145:SSE-S3 (AES256) sufficiente: chiavi gestite AWS di default (#06 §20bis)

  tags = {
    Name = "appgrove-audit-archive-${var.env}"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "audit_archive" {
  bucket = aws_s3_bucket.audit_archive.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
    bucket_key_enabled = true
  }
}

resource "aws_s3_bucket_public_access_block" "audit_archive" {
  bucket                  = aws_s3_bucket.audit_archive.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Ciclo di vita (#08 28): S3 → Glacier a 30 giorni → scadenza a 12 mesi
# (retention decisa internamente, #13 E: copre la finestra forense tipica).
resource "aws_s3_bucket_lifecycle_configuration" "audit_archive" {
  bucket = aws_s3_bucket.audit_archive.id

  rule {
    id     = "glacier-30d-expire-12m"
    status = "Enabled"

    filter {} # tutto il bucket

    transition {
      days          = 30
      storage_class = "GLACIER"
    }

    expiration {
      days = 365
    }

    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }
  }
}

data "aws_iam_policy_document" "audit_archive_tls_only" {
  statement {
    sid     = "DenyInsecureTransport"
    effect  = "Deny"
    actions = ["s3:*"]
    resources = [
      aws_s3_bucket.audit_archive.arn,
      "${aws_s3_bucket.audit_archive.arn}/*",
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

resource "aws_s3_bucket_policy" "audit_archive" {
  bucket = aws_s3_bucket.audit_archive.id
  policy = data.aws_iam_policy_document.audit_archive_tls_only.json

  depends_on = [aws_s3_bucket_public_access_block.audit_archive]
}

# Firehose: bufferizza gli eventi audit dei subscription filter e li deposita
# su S3 compressi (volumi bassissimi → costo di pochi centesimi/mese).
resource "aws_kinesis_firehose_delivery_stream" "audit_archive" {
  name        = "appgrove-${var.env}-audit-archive"
  destination = "extended_s3"

  #checkov:skip=CKV_AWS_241:Cifratura con chiave posseduta da AWS sufficiente (#06 §20bis): una CMK dedicata (~$1/mese) va oltre il cost-min; a riposo i dati vivono su S3 con SSE

  server_side_encryption {
    enabled  = true
    key_type = "AWS_OWNED_CMK"
  }

  extended_s3_configuration {
    role_arn   = aws_iam_role.firehose_audit.arn
    bucket_arn = aws_s3_bucket.audit_archive.arn
    prefix     = "audit/"

    buffering_size     = 5   # MB
    buffering_interval = 300 # secondi: latenza d'archivio irrilevante
    compression_format = "GZIP"
  }

  tags = {
    Name = "appgrove-${var.env}-audit-archive"
  }
}

resource "aws_iam_role" "firehose_audit" {
  name = "appgrove-${var.env}-firehose-audit"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "firehose.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })

  tags = {
    Name = "appgrove-${var.env}-firehose-audit"
  }
}

resource "aws_iam_role_policy" "firehose_audit_s3" {
  name = "write-audit-archive"
  role = aws_iam_role.firehose_audit.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid    = "WriteArchive"
      Effect = "Allow"
      Action = [
        "s3:AbortMultipartUpload",
        "s3:GetBucketLocation",
        "s3:GetObject",
        "s3:ListBucket",
        "s3:ListBucketMultipartUploads",
        "s3:PutObject",
      ]
      Resource = [
        aws_s3_bucket.audit_archive.arn,
        "${aws_s3_bucket.audit_archive.arn}/*",
      ]
    }]
  })
}

# Ruolo che CloudWatch Logs assume per consegnare a Firehose: lo usano i
# subscription filter per-servizio creati da microsaas_app.
resource "aws_iam_role" "logs_to_firehose" {
  name = "appgrove-${var.env}-logs-to-firehose"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "logs.${data.aws_region.current.region}.amazonaws.com" }
      Action    = "sts:AssumeRole"
      Condition = {
        StringEquals = { "aws:SourceAccount" = data.aws_caller_identity.current.account_id }
      }
    }]
  })

  tags = {
    Name = "appgrove-${var.env}-logs-to-firehose"
  }
}

resource "aws_iam_role_policy" "logs_to_firehose" {
  name = "put-audit-records"
  role = aws_iam_role.logs_to_firehose.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid      = "PutRecords"
      Effect   = "Allow"
      Action   = ["firehose:PutRecord", "firehose:PutRecordBatch"]
      Resource = aws_kinesis_firehose_delivery_stream.audit_archive.arn
    }]
  })
}
