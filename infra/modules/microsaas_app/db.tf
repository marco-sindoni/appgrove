# ─────────────────────────────────────────────────────────────────────────────
# Ruolo DB + schema vuoto del servizio (#06 23, #05 11): Terraform genera la
# password, la custodisce in Secrets Manager e invoca la Lambda `db-bootstrap`
# (platform_shared) che esegue l'SQL idempotente via Data API — ruolo login,
# schema di sua proprietà, NESSUN privilegio fuori dal proprio schema.
# Le tabelle le crea Flyway in CI (UC 0005/0012), non Terraform.
# `service-remove` NON cancella ruolo/schema: la pulizia è manuale (--help).
# ─────────────────────────────────────────────────────────────────────────────

resource "random_password" "db" {
  length = 32
  # Niente apici/backslash: la password attraversa un letterale SQL (quotato
  # comunque dalla Lambda) e una URL JDBC.
  override_special = "!#$%&*()-_=+[]{}<>:."
}

resource "aws_secretsmanager_secret" "db" {
  name        = "appgrove/${var.env}/${var.app_id}/db"
  description = "Credenziali Postgres del servizio ${var.app_id} (ruolo least-privilege sul solo schema ${local.db_schema})"

  # Test: destroy libero; prod: finestra di recupero standard (30gg).
  recovery_window_in_days = var.force_destroy ? 0 : 30

  #checkov:skip=CKV2_AWS_57:Rotation automatica rimandata: richiede una Lambda di rotation dedicata (hardening, _EVOLUZIONI-DEVOPS)
  #checkov:skip=CKV_AWS_149:Cifratura con chiave gestita AWS by-design (#06 §20bis: CMK solo se servirà)

  tags = {
    Name = "appgrove/${var.env}/${var.app_id}/db"
  }
}

resource "aws_secretsmanager_secret_version" "db" {
  secret_id = aws_secretsmanager_secret.db.id

  secret_string = jsonencode({
    username = local.db_schema # ruolo = schema, per convenzione (#05 11)
    password = random_password.db.result
    host     = var.shared.aurora_endpoint
    port     = var.shared.aurora_port
    dbname   = var.shared.aurora_database_name
    schema   = local.db_schema
  })
}

# Invocazione a ogni apply in cui cambia l'input: la Lambda è idempotente
# (crea ruolo/schema se mancano, riallinea la password se ruotata).
resource "aws_lambda_invocation" "db_bootstrap" {
  function_name = var.shared.db_bootstrap_lambda_name

  input = jsonencode({
    role_name   = local.db_schema
    schema_name = local.db_schema
    secret_arn  = aws_secretsmanager_secret.db.arn
    # La versione del segreto entra nell'input SOLO per ri-innescare
    # l'invocazione quando la password cambia (la Lambda legge da Secrets Manager).
    secret_version_id = aws_secretsmanager_secret_version.db.version_id
  })

  depends_on = [aws_secretsmanager_secret_version.db]
}
