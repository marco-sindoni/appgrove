# ─────────────────────────────────────────────────────────────────────────────
# Bootstrap dello state Terraform (one-time, manuale — MAI in CI). #06 4, #07 5
#
# Crea le due risorse che ospitano lo "state" di tutti gli altri stack:
#   • bucket S3   appgrove-tfstate-<account-id>  (versionato, cifrato, privato)
#   • tabella DDB appgrove-tfstate-lock          (lock: previene apply concorrenti)
#
# Questo stack usa state LOCALE (terraform.tfstate in questa cartella): è l'uovo
# che crea la gallina. Lo state locale non contiene segreti ma resta fuori da git.
#
# Runbook: ./infra/scripts/bootstrap (con --help). Nel teardown completo
# ("spegnere l'iniziativa") queste risorse si eliminano PER ULTIME (#06 24).
# ─────────────────────────────────────────────────────────────────────────────

terraform {
  required_version = ">= 1.9"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}

provider "aws" {
  region = var.region
  default_tags {
    tags = {
      project    = "appgrove"
      stack      = "bootstrap"
      managed_by = "terraform"
    }
  }
}

data "aws_caller_identity" "current" {}

locals {
  # I nomi dei bucket S3 sono globali: il suffisso con l'account ID garantisce l'unicità.
  state_bucket = "appgrove-tfstate-${data.aws_caller_identity.current.account_id}"
}

# ── Bucket S3 dello state ────────────────────────────────────────────────────
resource "aws_s3_bucket" "tfstate" {
  bucket = local.state_bucket

  #checkov:skip=CKV_AWS_144:Replica cross-region non necessaria (cost-min, PoC)
  #checkov:skip=CKV_AWS_18:Access logging del bucket di state non necessario (cost-min)
  #checkov:skip=CKV2_AWS_62:Nessuna event notification necessaria sul bucket di state
  #checkov:skip=CKV_AWS_145:SSE-S3 (AES256) sufficiente: chiavi gestite AWS di default (#06 §20bis)
  #checkov:skip=CKV2_AWS_61:Nessuna lifecycle policy: lo state è piccolo e versionato, niente da scadere

  # Guardrail teardown: il bucket di state non si distrugge mai per errore.
  # L'eliminazione finale (dopo `down` di envs e global) è un passo manuale
  # documentato in infra/README.md (#06 24).
  lifecycle {
    prevent_destroy = true
  }
}

# Versioning: ogni scrittura dello state è recuperabile (rollback da corruzione).
resource "aws_s3_bucket_versioning" "tfstate" {
  bucket = aws_s3_bucket.tfstate.id
  versioning_configuration {
    status = "Enabled"
  }
}

# Cifratura at rest di default (SSE-S3): "encryption ovunque" (#06 §20bis).
resource "aws_s3_bucket_server_side_encryption_configuration" "tfstate" {
  bucket = aws_s3_bucket.tfstate.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
    bucket_key_enabled = true
  }
}

# Nessun accesso pubblico, in nessuna forma.
resource "aws_s3_bucket_public_access_block" "tfstate" {
  bucket                  = aws_s3_bucket.tfstate.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Cifratura in transito: rifiuta ogni accesso non-TLS (#06 §20bis).
resource "aws_s3_bucket_policy" "tfstate_tls_only" {
  bucket = aws_s3_bucket.tfstate.id
  policy = data.aws_iam_policy_document.tfstate_tls_only.json

  depends_on = [aws_s3_bucket_public_access_block.tfstate]
}

data "aws_iam_policy_document" "tfstate_tls_only" {
  statement {
    sid     = "DenyInsecureTransport"
    effect  = "Deny"
    actions = ["s3:*"]
    resources = [
      aws_s3_bucket.tfstate.arn,
      "${aws_s3_bucket.tfstate.arn}/*",
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

# ── Tabella DynamoDB di lock ─────────────────────────────────────────────────
# Previene due `apply` concorrenti sullo stesso state. PAY_PER_REQUEST: costo ~0
# (poche richieste al giorno, solo durante plan/apply).
resource "aws_dynamodb_table" "tfstate_lock" {
  name         = "appgrove-tfstate-lock"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "LockID"

  attribute {
    name = "LockID"
    type = "S"
  }

  #checkov:skip=CKV_AWS_28:PITR inutile: la tabella contiene solo lock effimeri
  #checkov:skip=CKV_AWS_119:Cifratura con chiave gestita AWS sufficiente: solo lock effimeri, nessun dato (#06 §20bis)
  #checkov:skip=CKV2_AWS_16:Auto-scaling non applicabile: billing PAY_PER_REQUEST

  lifecycle {
    prevent_destroy = true
  }
}
