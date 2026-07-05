# ─────────────────────────────────────────────────────────────────────────────
# ECR repo del servizio (#06 22): un repo per app PER AMBIENTE (test e prod
# vivono nello stesso account: nomi separati, promozione immagini in UC 0005).
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_ecr_repository" "this" {
  name = local.name

  image_scanning_configuration {
    scan_on_push = true
  }

  # La pipeline (UC 0005) deciderà lo schema di tagging; finché il deploy usa
  # tag mobili (es. "latest") l'immutabilità li romperebbe.
  image_tag_mutability = "MUTABLE"
  #checkov:skip=CKV_AWS_51:Tag mobili finché la pipeline (UC 0005) non fissa lo schema di tagging/promozione

  # Test: destroy libero anche col repo pieno (#06 24); prod: svuotare è esplicito.
  force_delete = var.force_destroy

  encryption_configuration {
    encryption_type = "AES256"
  }
  #checkov:skip=CKV_AWS_136:Cifratura con chiave gestita AWS by-design (#06 §20bis: CMK solo se servirà)

  tags = {
    Name = local.name
  }
}

# Cost-min: si conservano solo le ultime 10 immagini (storage ECR a consumo).
resource "aws_ecr_lifecycle_policy" "this" {
  repository = aws_ecr_repository.this.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Conserva solo le ultime 10 immagini"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 10
      }
      action = { type = "expire" }
    }]
  })
}
