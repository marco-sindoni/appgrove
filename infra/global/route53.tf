# ─────────────────────────────────────────────────────────────────────────────
# Zona DNS pubblica `appgrove.app` (#12 12: un solo dominio, una hosted zone).
#
# ⚠️ Se il dominio è stato REGISTRATO tramite Route53, una hosted zone esiste
# già nell'account: NON crearne una seconda — importare quella esistente nello
# state prima del primo `up global` (lo ricorda anche `scripts/first-run`):
#
#   ZONE_ID=$(aws route53 list-hosted-zones-by-name --dns-name appgrove.app \
#             --query 'HostedZones[0].Id' --output text | cut -d/ -f3)
#   ./infra/scripts/up global --import-zone "$ZONE_ID"
#
# I record applicativi (app., admin., api., …) NON nascono qui: li creano gli
# use case di edge/API (CloudFront, API Gateway — UC 0004+). Qui vivono solo la
# zona e i record di validazione dei certificati ACM (acm.tf).
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_route53_zone" "main" {
  name    = var.domain
  comment = "appgrove — zona unica (prod + test come sottodominio, #12 9/10)"
}
