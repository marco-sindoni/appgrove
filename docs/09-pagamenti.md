# Pagamenti (Paddle) — Decisioni

**Stato**: 🔴 da definire
**Ultimo aggiornamento**: 2026-06-14

## Scope
Integrazione Paddle (Merchant of Record), webhook, entitlement/attivazione app per tenant, e **modello di
costo per singola applicazione**. Border con [02-auth-sicurezza](02-auth-sicurezza.md)/[05-persistenza-dati](05-persistenza-dati.md)
(entitlements, `paddle_product_id`/`paddle_subscription_id`) e con il **pannello admin** (→ #03).

## Da discutere (richiesto dall'utente, 2026-06-14)
- **Modello di costo per-app**: ogni app ha il suo pricing; come si modella (free/one-time/subscription/usage),
  mapping verso prodotti/prezzi Paddle, trial, cambio piano, dunning.
- **Configurazione admin del costo**: come un `platform-admin` definisce/edita il modello di costo di ogni app
  dal pannello admin, e come si lega a catalog/entitlements. Vedi anche nota admin in [03-frontend](03-frontend.md).

## Decisioni prese
_Nessuna ancora._

## Questioni aperte
_Da elencare all'avvio dell'argomento._

## Alternative valutate / scartate
_—_

## Impatti su altre aree
_—_
