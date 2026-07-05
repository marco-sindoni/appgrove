# ─────────────────────────────────────────────────────────────────────────────
# Log group del servizio (#08 26): retention ESPLICITA (test 7gg, prod 30gg),
# mai "never expire". I log sono JSON strutturato con tenant_id/app_id/user_id
# (convenzioni MDC in services/commons); widget e allarmi arrivano con UC 0006.
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_cloudwatch_log_group" "this" {
  name              = "/appgrove/${var.env}/${var.app_id}"
  retention_in_days = local.log_retention_days

  #checkov:skip=CKV_AWS_158:Cifratura at rest di default (chiavi gestite CloudWatch); CMK solo se servirà (#06 §20bis)
  #checkov:skip=CKV_AWS_338:Retention 7gg test / 30gg prod by-design (#08 26, cost-min): l'archivio audit 12 mesi è di UC 0006

  tags = {
    Name = local.name
  }
}
