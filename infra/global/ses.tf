# ─────────────────────────────────────────────────────────────────────────────
# Identità di dominio SES + firma DKIM (UC 0018, #06 26).
#
# Vive in `global` e non per-ambiente perché il dominio è UNO (#12 12): test e prod
# spediscono entrambi da `noreply@appgrove.app`. Due stack che creassero la stessa
# identità andrebbero in conflitto sulla medesima risorsa AWS.
#
# Costo: identità e firma sono GRATUITE; l'invio è ~$0.10/1000 email. Il costo vero
# di questa scelta è l'accesso di rete verso SES dalla rete privata degli ambienti
# (endpoint `email`, ~$7/mese/env) — vedi _COSTI-AWS ed evoluzione E24.
#
# ⚠️ MODALITÀ DI PROVA (sandbox): un account SES nuovo può spedire SOLO a indirizzi
# verificati a mano. L'uscita è una richiesta MANUALE ad AWS che può richiedere
# giorni, e non è automatizzabile da qui: va avviata in anticipo (checklist di prima
# accensione in docs/_BACKLOG.md). Finché non è concessa, la registrazione funziona
# solo verso indirizzi verificati.
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_sesv2_email_identity" "main" {
  email_identity = var.domain

  # EASY_DKIM: AWS genera e ruota le chiavi, noi pubblichiamo i tre CNAME sotto.
  # Senza firma la posta transazionale finisce sistematicamente nello spam.
  dkim_signing_attributes {
    next_signing_key_length = "RSA_2048_BIT"
  }

  tags = {
    Name = var.domain
  }
}

# Record di verifica della firma: tre CNAME generati da AWS. La verifica dell'identità
# si completa solo quando sono risolvibili — quindi solo dopo che i name server della
# zona sono davvero delegati al dominio (stesso vincolo dei certificati, acm.tf).
resource "aws_route53_record" "ses_dkim" {
  count = 3

  zone_id = aws_route53_zone.main.zone_id
  name    = "${aws_sesv2_email_identity.main.dkim_signing_attributes[0].tokens[count.index]}._domainkey.${var.domain}"
  type    = "CNAME"
  ttl     = 300
  records = ["${aws_sesv2_email_identity.main.dkim_signing_attributes[0].tokens[count.index]}.dkim.amazonses.com"]

  allow_overwrite = true
}

# Politica dichiarata nel DNS (DMARC): dice ai destinatari cosa fare con la posta che
# afferma di venire da noi ma non è firmata. `p=none` = sola osservazione, ed è il
# punto di partenza corretto: passare a quarantena/rifiuto prima di aver osservato il
# traffico reale significa rischiare di far sparire posta legittima.
#
# Nessun indirizzo per i rapporti aggregati (`rua`) di proposito: punterebbe a una
# casella che non esiste, e i rapporti rimbalzerebbero. Attivarlo — e poi irrigidire
# la politica — è tracciato fra i punti aperti di UC 0018.
resource "aws_route53_record" "dmarc" {
  zone_id = aws_route53_zone.main.zone_id
  name    = "_dmarc.${var.domain}"
  type    = "TXT"
  ttl     = 300
  records = ["v=DMARC1; p=none;"]
}
