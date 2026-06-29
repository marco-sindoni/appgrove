# UC 0024 — Checkout (token server-initiated, overlay, polling post-checkout)

**Area**: 07-payments · **Fase**: 5 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0023](0023-stub-paddle-locale.md) (stub/provider), UC [0020](../06-frontend/0020-shell-spa-backoffice.md) (shell)
**Fonte decisioni**: #09 C (checkout), #02 (invariante tenant_id), #03 (frontend)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [09-pagamenti](../../09-pagamenti.md), [03-frontend](../../03-frontend.md), [02-auth-sicurezza](../../02-auth-sicurezza.md)

## 1. Obiettivo / Scope
Implementare il **checkout** di acquisto di un'app: overlay Paddle.js avviato **lato server**, con UX post-checkout a polling.
**Incluso**: schermata scelta tier (default **annuale** + sconto, trial); **checkout iniziato lato server** (il backend legge
`tenant_id` dal JWT, risolve `paddle_price_id`, crea la transazione Paddle con `custom_data={tenant_id,app_id}` + customer,
ritorna un **checkout token**); **overlay** Paddle.js col solo token; **attivazione SOLO via webhook**; **UX a stati con polling**
("attivazione in corso" → "attivato", messaggio rassicurante oltre 30-60s).
**Escluso**: la pipeline webhook (UC 0025), il lifecycle (UC 0026), l'enforcement entitlement (UC 0027), il portal (UC 0028);
in locale Paddle.js è lo stub (UC 0023).

## 2. Attori & ruoli
- **Utente** (owner): sceglie il tier, paga nell'overlay.
- **Backend (billing core)**: inizia il checkout server-side, espone il token.
- **Paddle** (MoR): gestisce carte/3DS/tasse/valute/antifrode nell'overlay.

## 3. Precondizioni
- Catalogo+pricing sincronizzati (UC 0022) con `paddle_price_id`; provider (stub locale UC 0023 o Paddle reale); utente autenticato.

## 4. Flusso principale
1. **Scelta tier**: UI con **default annuale** + sconto esplicito ("2 mesi gratis") + trial (#09 A2/K49).
2. **Server-initiated** (invariante #1): il FE chiede al backend di iniziare l'acquisto; il backend legge **`tenant_id` dal JWT**, risolve `paddle_price_id`, crea la transazione Paddle con `custom_data={tenant_id,app_id}` + customer (lazy al primo acquisto) e ritorna un **checkout token** (#09 C14/C15).
3. **Overlay**: il FE apre Paddle.js passando **solo** il token; i `custom_data` sono impostati server-side → non manomettibili (#09 C13/C14).
4. **Attivazione SOLO via webhook**: l'entitlement si attiva quando il webhook aggiorna `subscription` (UC 0025), **mai** sull'evento client (`checkout.completed` = solo UX) (#09 C16).
5. **UX post-checkout (polling)**: spinner "attivazione in corso", poll del backend ogni 1-2s finché `status=active` → "attivato"; oltre ~30-60s messaggio **rassicurante** (pagamento riuscito, attivazione in arrivo), **mai errore** (#09 C17).

## 5. Flussi alternativi / edge / errori
- **Webhook in ritardo**: la UX resta rassicurante; nessun errore mostrato; niente push (polling, cost-min) (#09 C17).
- **Customer esistente**: dai checkout successivi si passa il `paddle_customer_id` salvato (#09 C15).
- **Manomissione client**: impossibile su `tenant_id` (impostato server-side nei custom_data) (#09 C14).
- **Locale**: stub Paddle.js + webhook sintetico firmato (UC 0023) → stessa UX.

## 6. Schermate & stati
Scelta tier (mensile/annuale default annuale + sconto + trial), overlay Paddle, **stati post-checkout**: attivazione in
corso (spinner+polling) → attivato; rassicurante oltre soglia. Errori di rete del nostro backend gestiti (problem+json), mai
"errore di pagamento" inventato.

## 7. Dati toccati
Crea/usa `paddle_customer_id` (account) e avvia `subscription` via webhook (UC 0025). **Dati pagamento mai toccati** (MoR). Email
passata al primo checkout (per il customer). Manifest: trattamento "billing/abbonamento (lato appgrove)" base contratto; dati fiscali in capo a Paddle.

## 8. Permessi & gate
- **Invariante #1**: `tenant_id` dal JWT verificato lato server; il client non lo fornisce. Checkout possibile solo da utente autenticato (owner).
- L'entitlement si attiva **solo** via webhook (source of truth), non lato client.

## 9. Requisiti di test
- **L2 E2E** (Playwright, Paddle.js **mockato**, #09 D20): scelta tier → server-initiated → UX polling simulando l'arrivo webhook ("attivazione"→"attivato").
- **Integration**: il backend imposta `custom_data` server-side; token corretto; nessuna attivazione su evento client.
- **L3** smoke reale (UC 0029) pre-release.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #09 A2, C13/C14/C15/C16/C17, K49, D20.
- **DoD**:
  1. Scelta tier (default annuale+sconto+trial); checkout **server-initiated** con `custom_data` server-side.
  2. Overlay col solo token; attivazione SOLO via webhook; UX polling rassicurante.
  3. Invariante tenant_id-da-JWT rispettata; dati pagamento mai toccati.
  4. L2 E2E + integration verdi; L3 pre-release.

## Punti aperti / decisioni differite

### Stato dopo la change `0022-use-case-0024-…` (slice verticale locale)

**Fatto**: catalogo lato cliente + checkout server-initiated + polling, end-to-end in locale.
- **Backend** (`services/core`): `CheckoutResource` — `GET /checkout/apps/{appSlug}/tiers` (tier+prezzi),
  `POST /checkout/apps/{appSlug}` (OWNER, token, `custom_data` server-side dal JWT, customer lazy),
  `GET /checkout/apps/{appSlug}/subscription` (stato minimale per il polling, `active` dalla mappa #09 E29).
  Gli endpoint sono **slug-based** (chiave stabile = `app.slug`, allineata al registry FE).
- **Frontend** (`backoffice`, pagina `/billing`): scelta app→tier (default annuale + sconto + trial), overlay
  via `@appgrove/paddle-stub`, UX a polling rassicurante (mai errore di pagamento). Test: integration L1 +
  vitest (state machine) + **L2 Playwright** (`e2e/checkout.spec.ts`, Paddle.js mockato).
- **Attivazione fedele**: `CheckoutResource` non attiva nulla; emette un evento CDI `CheckoutStarted`. In
  prod nessun osservatore (attivazione solo via webhook reale). In dev/test un bean **stub-only**
  (`StubCheckoutActivation`, gate `provider=stub`) emette il webhook `happy_path` via la pipeline firmata
  (UC 0023/0025) → il polling locale arriva ad "attivato" da solo.

**Differito** (con proprietario):
- **Overlay Paddle.js reale** (loader script remoto + client-token) e **`PaddlePaymentProvider.startCheckout`
  reale**: gated **#14** (nessun account Paddle); smoke **L3** → **UC 0029**. La factory FE `billing/paddle.ts`
  e il provider backend sono i due seam pronti da sostituire.
- **Marketplace / discovery delle app non possedute**: oggi `/billing` lista le app dal **registry build-time**
  (`MODULES`); manca una vetrina che elenchi dal **catalogo reale** le app acquistabili (incl. quelle non nel
  bundle FE). *Follow-up* — proprietà di questo UC 0024 (o nuovo UC catalogo pubblico se cresce).
- **Formattazione prezzo per-locale**: `formatPrice` usa `it-IT` fisso (la UI però è multilingua). Polish
  minore → UC 0024 (passare il locale corrente).
- **Cablaggio entitlement reale del registry** (l'app appena comprata non appare in sidebar senza refresh) +
  endpoint `/me/entitlements` + riconciliazione chiave **slug↔UUID** dell'entitlement → **UC 0027** (vedi nota lì).
