# ─────────────────────────────────────────────────────────────────────────────
# Aurora Serverless v2 (PostgreSQL) — 1 cluster writer per ambiente (#06 13):
#   • scale-to-0 su test E prod (#06 14, #12 4): min 0 ACU + auto-pause; il
#     cold-start ~10-15s da idle è accettato (readiness tarata in UC 0006);
#   • max ACU basso ≈2 (#06 13, cost-min);
#   • PITR ~7 giorni (#06 16); cifratura at rest (#06 §20bis);
#   • credenziali master gestite da Secrets Manager (rotation integrata RDS);
#   • prod: deletion protection + snapshot finale; test: destroy libero (#06 K).
# Lo schema-per-app (ruoli, grant) è del modulo `microsaas_app` (UC 0004);
# le tabelle le crea Flyway in CI (UC 0005/0012).
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_db_subnet_group" "aurora" {
  name        = "appgrove-${var.env}"
  description = "Subnet group Aurora dell'ambiente ${var.env} (2 AZ, richiesto da RDS)"
  subnet_ids  = var.subnet_ids

  tags = {
    Name = "appgrove-${var.env}"
  }
}

resource "aws_security_group" "aurora" {
  name        = "appgrove-${var.env}-aurora"
  description = "Accesso Postgres al cluster Aurora dai soli indirizzi della VPC"
  vpc_id      = var.vpc_id

  ingress {
    description = "Postgres dalla VPC: task Fargate diretti (#05 dec.3) e RDS Proxy"
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  # Nessuna regola egress: il cluster riceve soltanto.

  tags = {
    Name = "appgrove-${var.env}-aurora"
  }
}

resource "aws_rds_cluster" "this" {
  cluster_identifier = "appgrove-${var.env}"

  engine      = "aurora-postgresql"
  engine_mode = "provisioned" # Serverless v2 usa il mode "provisioned" + istanze db.serverless
  # 16.3+ richiesto per min 0 ACU (auto-pause); pin esplicito, upgrade deliberati.
  engine_version = "16.6"

  database_name   = "appgrove"
  master_username = "appgrove_admin"
  # Credenziali master in Secrets Manager, gestite da RDS (#06 20): niente
  # password nel codice né nello state.
  manage_master_user_password = true

  db_subnet_group_name   = aws_db_subnet_group.aurora.name
  vpc_security_group_ids = [aws_security_group.aurora.id]
  port                   = 5432

  serverlessv2_scaling_configuration {
    min_capacity = 0 # scale-to-0 (#06 14, #12 4)
    max_capacity = 2 # tetto basso, cost-min (#06 13)
    # Auto-pause dopo 5' di inattività (minimo consentito): massimo risparmio,
    # cold-start ~10-15s accettato nel PoC (#06 14).
    seconds_until_auto_pause = 300
  }

  backup_retention_period = 7 # PITR ~7 giorni (#06 16)
  storage_encrypted       = true
  copy_tags_to_snapshot   = true

  # Sicurezze di destroy (#06 16/24): prod protetto + snapshot finale,
  # test libero.
  deletion_protection       = var.deletion_protection
  skip_final_snapshot       = !var.deletion_protection
  final_snapshot_identifier = "appgrove-${var.env}-final"

  #checkov:skip=CKV_AWS_162:Autenticazione via Secrets Manager (#06 20); IAM auth valutabile in UC 0014 con le Lambda
  #checkov:skip=CKV_AWS_327:Cifratura con chiave gestita AWS by-design (#06 §20bis: CMK solo se servirà)
  #checkov:skip=CKV_AWS_139:Deletion protection pilotata per ambiente (#06 16/24): true su prod, test libera di fare destroy
  #checkov:skip=CKV_AWS_324:Export log Postgres su CloudWatch rimandato all'observability (UC 0006, cost-min)
  #checkov:skip=CKV_AWS_96:storage_encrypted è true; il check chiede una CMK — chiavi gestite AWS di default (#06 §20bis)
  #checkov:skip=CKV2_AWS_8:Backup gestito da PITR nativo (retention 7gg); niente AWS Backup plan (cost-min)
  #checkov:skip=CKV2_AWS_27:Query logging rimandato all'observability (UC 0006, cost-min)
}

resource "aws_rds_cluster_instance" "writer" {
  identifier         = "appgrove-${var.env}-writer"
  cluster_identifier = aws_rds_cluster.this.id

  instance_class = "db.serverless" # Serverless v2: la capacità la governa lo scaling del cluster
  engine         = aws_rds_cluster.this.engine
  engine_version = aws_rds_cluster.this.engine_version

  publicly_accessible        = false
  auto_minor_version_upgrade = true

  #checkov:skip=CKV_AWS_353:Performance Insights spento (cost-min); si accende con l'observability (UC 0006)
  #checkov:skip=CKV_AWS_354:Performance Insights spento: nessuna chiave da cifrare
  #checkov:skip=CKV_AWS_118:Enhanced monitoring spento (cost-min); si accende con l'observability (UC 0006)

  tags = {
    Name = "appgrove-${var.env}-writer"
  }
}
