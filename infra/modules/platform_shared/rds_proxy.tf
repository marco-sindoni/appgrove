# ─────────────────────────────────────────────────────────────────────────────
# RDS Proxy — SOLO per le Lambda auth/pre-token-gen/authorizer (#05 dec.3,
# #06 15): pooling delle connessioni per componenti effimeri. I task Fargate
# — incluso il Flyway one-shot — si connettono DIRETTI al cluster (Agroal).
# Voce di costo da monitorare (~$12/mese/env, floor always-on → _COSTI-AWS).
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_security_group" "rds_proxy" {
  name        = "appgrove-${var.env}-rds-proxy"
  description = "RDS Proxy: riceve dalle Lambda auth (UC 0014) e parla solo con Aurora"
  vpc_id      = var.vpc_id

  ingress {
    # Le Lambda auth non esistono ancora (UC 0014): perimetro = VPC intera;
    # UC 0014 potrà stringere al solo security group delle Lambda.
    description = "Postgres dalla VPC (Lambda auth, UC 0014)"
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    description     = "Postgres verso il cluster Aurora"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.aurora.id]
  }

  tags = {
    Name = "appgrove-${var.env}-rds-proxy"
  }
}

# Ruolo con cui il proxy legge le credenziali master dal segreto gestito da RDS.
data "aws_iam_policy_document" "rds_proxy_assume" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["rds.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "rds_proxy" {
  name               = "appgrove-${var.env}-rds-proxy"
  description        = "Permette a RDS Proxy di leggere il segreto delle credenziali Aurora"
  assume_role_policy = data.aws_iam_policy_document.rds_proxy_assume.json
}

data "aws_iam_policy_document" "rds_proxy_secret" {
  statement {
    sid     = "GetDbSecret"
    effect  = "Allow"
    actions = ["secretsmanager:GetSecretValue"]
    resources = [
      aws_rds_cluster.this.master_user_secret[0].secret_arn,
      # Ruolo DB dedicato least-privilege delle Lambda auth (UC 0016, E23-B).
      aws_secretsmanager_secret.auth_lambdas_db.arn,
    ]
  }

  statement {
    sid       = "DecryptDbSecret"
    effect    = "Allow"
    actions   = ["kms:Decrypt"]
    resources = ["*"] # chiave gestita AWS aws/secretsmanager, ristretta dalla condizione ViaService

    condition {
      test     = "StringEquals"
      variable = "kms:ViaService"
      values   = ["secretsmanager.${data.aws_region.current.region}.amazonaws.com"]
    }
  }
}

resource "aws_iam_role_policy" "rds_proxy_secret" {
  name   = "read-db-secret"
  role   = aws_iam_role.rds_proxy.id
  policy = data.aws_iam_policy_document.rds_proxy_secret.json
}

resource "aws_db_proxy" "this" {
  name           = "appgrove-${var.env}"
  engine_family  = "POSTGRESQL"
  role_arn       = aws_iam_role.rds_proxy.arn
  vpc_subnet_ids = var.subnet_ids
  vpc_security_group_ids = [
    aws_security_group.rds_proxy.id,
  ]

  require_tls         = true # TLS obbligatorio (#06 §20bis)
  idle_client_timeout = 1800
  debug_logging       = false

  auth {
    auth_scheme = "SECRETS"
    secret_arn  = aws_rds_cluster.this.master_user_secret[0].secret_arn
    # IAM auth delle Lambda verso il proxy: decisione di UC 0014 (per ora
    # autenticazione con credenziali dal segreto).
    iam_auth = "DISABLED"
  }

  # Ruolo DB dedicato least-privilege delle Lambda auth (UC 0016, E23-B): le
  # Lambda auth (pre-token-gen + BFF) si autenticano con QUESTO segreto, non più
  # col master. Il ruolo Postgres lo crea db-bootstrap (modalità grant).
  auth {
    auth_scheme = "SECRETS"
    secret_arn  = aws_secretsmanager_secret.auth_lambdas_db.arn
    iam_auth    = "DISABLED"
  }

  tags = {
    Name = "appgrove-${var.env}"
  }
}

resource "aws_db_proxy_default_target_group" "this" {
  db_proxy_name = aws_db_proxy.this.name

  connection_pool_config {
    max_connections_percent = 100
  }
}

resource "aws_db_proxy_target" "aurora" {
  db_proxy_name         = aws_db_proxy.this.name
  target_group_name     = aws_db_proxy_default_target_group.this.name
  db_cluster_identifier = aws_rds_cluster.this.cluster_identifier
}
