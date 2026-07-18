# ─────────────────────────────────────────────────────────────────────────────
# Ruolo DB dedicato least-privilege delle Lambda auth (UC 0016, opzione B di E23,
# #05 dec.3): Terraform genera la password e la custodisce in Secrets Manager;
# il RUOLO Postgres `auth_lambdas` lo crea la Lambda db-bootstrap in modalità
# "grant" (invocazione nel root env, DOPO che lo schema `platform` esiste), con
# privilegi minimi: SELECT su tutto lo schema `platform` (lettura pre-token-gen)
# + INSERT/UPDATE sulle sole tabelle scritte dal BFF (signup/accept invito).
#
# Sostituisce l'uso delle credenziali MASTER da parte delle Lambda auth: il
# segreto è agganciato al proxy accanto al master (rds_proxy.tf). Autenticazione
# IAM del proxy + stretta del security group → UC 0014 (parte residua di E23).
# ─────────────────────────────────────────────────────────────────────────────

resource "random_password" "auth_lambdas_db" {
  length = 32
  # Niente apici/backslash: la password attraversa un letterale SQL (quotato
  # dalla Lambda) e viene usata dal proxy per autenticarsi ad Aurora.
  override_special = "!#$%&*()-_=+[]{}<>:."
}

resource "aws_secretsmanager_secret" "auth_lambdas_db" {
  name        = "appgrove/${var.env}/auth-lambdas/db"
  description = "Credenziali del ruolo Postgres least-privilege delle Lambda auth (pre-token-gen lettura + BFF scritture su platform)"

  recovery_window_in_days = var.force_destroy_buckets ? 0 : 30

  #checkov:skip=CKV2_AWS_57:Rotation automatica rimandata: richiede una Lambda di rotation dedicata (hardening, _EVOLUZIONI-DEVOPS E21)
  #checkov:skip=CKV_AWS_149:Cifratura con chiave gestita AWS by-design (#06 §20bis: CMK solo se servirà)

  tags = {
    Name = "appgrove/${var.env}/auth-lambdas/db"
  }
}

resource "aws_secretsmanager_secret_version" "auth_lambdas_db" {
  secret_id = aws_secretsmanager_secret.auth_lambdas_db.id

  secret_string = jsonencode({
    username = "auth_lambdas"
    password = random_password.auth_lambdas_db.result
    host     = aws_db_proxy.this.endpoint
    port     = 5432
    dbname   = aws_rds_cluster.this.database_name
  })
}
