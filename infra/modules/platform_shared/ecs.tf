# ─────────────────────────────────────────────────────────────────────────────
# Cluster ECS dell'ambiente (#06 9): contenitore logico su cui il modulo
# `microsaas_app` (UC 0004) registra i service/task Fargate (0.25 vCPU/0.5 GB,
# immagini native GraalVM, 1 task per servizio — HA = evoluzione E3).
# Capacità (#06 10): Fargate SPOT in test (~-70%, servizi stateless dietro
# retry), on-demand in prod (stabilità).
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_ecs_cluster" "this" {
  name = "appgrove-${var.env}"

  setting {
    # Container Insights spento (costa per metrica/log): l'observability di
    # base arriva con UC 0006 (log JSON + metriche EMF, cost-min #08).
    name  = "containerInsights"
    value = "disabled"
  }

  #checkov:skip=CKV_AWS_65:Container Insights spento by-design (cost-min); observability = UC 0006

  tags = {
    Name = "appgrove-${var.env}"
  }
}

resource "aws_ecs_cluster_capacity_providers" "this" {
  cluster_name       = aws_ecs_cluster.this.name
  capacity_providers = ["FARGATE", "FARGATE_SPOT"]

  default_capacity_provider_strategy {
    capacity_provider = var.use_fargate_spot ? "FARGATE_SPOT" : "FARGATE"
    weight            = 1
  }
}
