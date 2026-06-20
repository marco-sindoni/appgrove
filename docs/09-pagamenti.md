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

## Requisiti già fissati (da approfondire/dettagliare negli use case)
- **Doppia proposta billing mensile/annuale** (richiesto 2026-06-21): per ogni app con abbonamento, la schermata di
  acquisto **propone sia il piano mensile sia quello annuale**, con **default impostato su ANNUALE** e uno **sconto
  esplicito** sull'acquisto annuale (es. "2 mesi gratis"). Razionale economico: la quota fissa Paddle (~$0.50/transazione)
  pesa molto sulle app cheap a fatturazione mensile (es. €5/mese → ~15% effettivo); l'annuale riduce le transazioni a 1/anno
  (~5–6% effettivo) → margine migliore + cassa anticipata. Mapping: due `price` Paddle (monthly/yearly) per ogni `product`
  app. Da dettagliare in: topic A (modello pricing), K (fee/business) e nello **use case di acquisto/checkout**.

## Decisioni prese
_Nessuna ancora._

## Questioni aperte
_Da elencare all'avvio dell'argomento._

## Alternative valutate / scartate
_—_

## Impatti su altre aree
_—_
