# ─────────────────────────────────────────────────────────────────────────────
# Baseline SSM Parameter Store (#06 20, #12 7).
#
# Convenzione dei path: /appgrove/<env>/<area>/<chiave>
# I servizi leggono i parametri A RUNTIME (mai la CI, #07 26). I valori segreti
# usano SecureString; i nomi di risorsa (non segreti) usano String.
#
# Le credenziali DB stanno in SECRETS MANAGER (rotation/integrazione RDS) e
# nascono con il cluster Aurora (use case dedicato) — qui nessun segreto.
# ─────────────────────────────────────────────────────────────────────────────

# Nome del bucket export GDPR → letto dal core come `appgrove.gdpr.export-bucket`
# (chiave definita nella change 0028; qui si pubblica il valore per-ambiente).
resource "aws_ssm_parameter" "gdpr_export_bucket" {
  name        = "/appgrove/${var.env}/gdpr/export-bucket"
  description = "Bucket S3 degli export GDPR dell'ambiente (UC 0032)."
  type        = "String" # è un nome di risorsa, non un segreto
  value       = aws_s3_bucket.gdpr_export.bucket

  #checkov:skip=CKV_AWS_337:Il nome del bucket non è un segreto: String è corretto, SecureString andrebbe oltre lo scopo
}
