# ─────────────────────────────────────────────────────────────────────────────
# Code SQS GDPR per-servizio (UC 0032, #06 H-19): una coda export e una coda
# purge per app, ciascuna con la sua DLQ (5 tentativi, come dev/elasticmq.conf).
# I nomi LOGICI (= GdprQueues in services/commons) portano il prefisso
# `appgrove-<env>-` (nomi SQS unici per account/regione, test+prod convivono);
# i servizi ricevono il prefisso a runtime (APPGROVE_SQS_QUEUE_PREFIX).
# La coda condivisa `gdpr-export-results` è di platform_shared.
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_sqs_queue" "gdpr_export_dlq" {
  name                    = "${local.export_queue_name}-dlq"
  sqs_managed_sse_enabled = true # cifratura at rest (#06 §20bis), chiavi gestite SQS

  tags = {
    Name = "${local.export_queue_name}-dlq"
  }
}

resource "aws_sqs_queue" "gdpr_export" {
  name                    = local.export_queue_name
  sqs_managed_sse_enabled = true

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.gdpr_export_dlq.arn
    maxReceiveCount     = 5
  })

  tags = {
    Name = local.export_queue_name
  }
}

resource "aws_sqs_queue" "tenant_purge_dlq" {
  name                    = "${local.purge_queue_name}-dlq"
  sqs_managed_sse_enabled = true

  tags = {
    Name = "${local.purge_queue_name}-dlq"
  }
}

resource "aws_sqs_queue" "tenant_purge" {
  name                    = local.purge_queue_name
  sqs_managed_sse_enabled = true

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.tenant_purge_dlq.arn
    maxReceiveCount     = 5
  })

  tags = {
    Name = local.purge_queue_name
  }
}

# EventBridge deve poter scrivere sulla coda purge (target della regola
# `tenant.offboarded`, vedi eventbridge.tf): consenso limitato a QUELLA regola.
resource "aws_sqs_queue_policy" "tenant_purge_from_eventbridge" {
  queue_url = aws_sqs_queue.tenant_purge.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid       = "AllowEventBridgeRule"
      Effect    = "Allow"
      Principal = { Service = "events.amazonaws.com" }
      Action    = "sqs:SendMessage"
      Resource  = aws_sqs_queue.tenant_purge.arn
      Condition = {
        ArnEquals = { "aws:SourceArn" = aws_cloudwatch_event_rule.tenant_offboarded.arn }
      }
    }]
  })
}

# ─────────────────────────────────────────────────────────────────────────────
# Coda di invalidazione entitlement (UC 0046): una per APP di marketplace.
# Il core vi pubblica un evento sottile ("i diritti del tenant T sono cambiati")
# quando lo stato di billing o dell'account cambia; l'app marca la propria
# proiezione locale da rinfrescare. Sostituisce, sul percorso caldo, la chiamata
# sincrona app→core di UC 0027.
# Il core non ha una propria coda: è il produttore, non un consumatore.
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_sqs_queue" "entitlement_dlq" {
  count = local.has_entitlement_queue ? 1 : 0

  name                    = "${local.entitlement_queue_name}-dlq"
  sqs_managed_sse_enabled = true

  tags = {
    Name = "${local.entitlement_queue_name}-dlq"
  }
}

resource "aws_sqs_queue" "entitlement" {
  count = local.has_entitlement_queue ? 1 : 0

  name                    = local.entitlement_queue_name
  sqs_managed_sse_enabled = true

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.entitlement_dlq[0].arn
    maxReceiveCount     = 5
  })

  tags = {
    Name = local.entitlement_queue_name
  }
}
