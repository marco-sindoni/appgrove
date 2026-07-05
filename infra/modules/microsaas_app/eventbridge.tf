# ─────────────────────────────────────────────────────────────────────────────
# Fan-in dell'offboarding (#06 H-19, UC 0032): sul bus dell'ambiente, l'evento
# `tenant.offboarded` (pubblicato dal core) viene instradato alla coda purge di
# QUESTO servizio; ogni istanza del modulo aggiunge la propria regola, così
# tutti i servizi ricevono il purge senza che il core li conosca.
# In locale il bus non esiste (il core invia dritto alle code): il transformer
# consegna il solo `detail`, così il corpo del messaggio è identico al locale.
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_cloudwatch_event_rule" "tenant_offboarded" {
  name           = "${local.name}-tenant-offboarded"
  description    = "tenant.offboarded → coda purge di ${var.app_id} (UC 0032)"
  event_bus_name = var.shared.event_bus_name

  # Contratto minimo: si aggancia al solo detail-type; il publisher cloud del
  # core nascerà con UC 0032/0035 (vedi "Punti aperti" di UC 0004).
  event_pattern = jsonencode({
    detail-type = ["tenant.offboarded"]
  })

  tags = {
    Name = "${local.name}-tenant-offboarded"
  }
}

resource "aws_cloudwatch_event_target" "tenant_purge" {
  rule           = aws_cloudwatch_event_rule.tenant_offboarded.name
  event_bus_name = var.shared.event_bus_name
  target_id      = "tenant-purge-${var.app_id}"
  arn            = aws_sqs_queue.tenant_purge.arn

  # Solo il payload dell'evento: stesso corpo che il consumer legge in locale.
  input_transformer {
    input_paths = {
      detail = "$.detail"
    }
    input_template = "<detail>"
  }
}
