# Pagamenti (Paddle) — Decisioni

**Stato**: 🟡 in corso
**Ultimo aggiornamento**: 2026-06-21

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

## Agenda topic (A–K)
- **A. Modello di pricing per-app** ✅ — tipi supportati e come si modellano
- **B. Catalogo & mapping dati** ✅ — catalog interno ↔ `product`/`price` Paddle; campi su entitlements (#05)
- **C. Checkout** ✅ — overlay/inline/hosted (Paddle.js); passthrough `tenant_id`/`app_id`
- **D. Webhook & source of truth** ✅ — firma HMAC, idempotenza, eventi → entitlement
- **E. Ciclo di vita subscription** — stati, proration, upgrade/downgrade tier, dunning, grace
- **F. Entitlement & attivazione/disattivazione** — entitlement per-tenant abilita l'app a runtime
- **G. Customer portal & self-service** — portale Paddle vs UI nostra; cosa esponiamo nel backoffice
- **H. Configurazione admin del pricing** — il platform-admin definisce i tier dal pannello admin
- **I. Sandbox & ambienti** — sandbox Paddle in locale/test, secret per ambiente, test webhook
- **J. Compliance & fatturazione (MoR)** — cosa copre Paddle, cosa resta a noi (border #13)
- **K. Fee & impatto sul business** — modello fee, prezzo minimo sostenibile, leve annuale/bundling

## Decisioni prese

### A. Modello di pricing per-app
1. **Solo subscription** (ricorrente) al lancio. **One-time escluso** (nessun caso lo richiede; evoluzione futura).
   **Niente metered/usage-based con sforamento addebitato** (evoluzione futura).
2. **Doppio billing mensile/annuale**, **default ANNUALE** in checkout, con **sconto annuale esplicito** (es. "2 mesi
   gratis"). Razionale: la quota fissa Paddle (~$0.50/transazione) penalizza le app cheap a fatturazione mensile;
   l'annuale riduce le transazioni a 1/anno → margine migliore + cassa anticipata (vedi K).
3. **Modello unificato "lista di tier" per-app**: ogni app ha **1..N tier**; ogni tier =
   `{ prezzo: 0 | {mensile, annuale}, limiti: <vettore quote>, feature: <flag> }`. Un unico schema copre tutti i casi:
   - **App gratis** = 1 tier a prezzo `0`;
   - **Subscription semplice** = 1 tier a pagamento;
   - **Tiered** (es. fatture 10/100/1000, CRM seat 5/20/100) = N tier a pagamento con limiti crescenti;
   - **Freemium** = tier 0 gratis + tier superiori a pagamento (✅ supportato — è solo "tier con prezzo 0");
   - **Trial** = flag "trial N giorni" sul tier a pagamento.
4. **"Usage" e "seat/quantity" collassano nel modello a tier+quota**: l'utente sceglie un **tier a prezzo fisso** che
   porta un **limite**. Non è metering: Paddle vede solo subscription, la quota è applicativa.
5. **Quota disaccoppiata dal billing**, **metrica E finestra definite per-app** (opzione c). Ogni app dichiara la propria
   metrica (n. fatture, n. documenti, n. seat, …) e la finestra (mensile/annuale/…). Serve un **contratto/interfaccia
   generico di quota** che ogni servizio implementa (stesso spirito del contratto GDPR `exportData`/`purgeData`, #13).
6. **Hard limit** al raggiungimento della quota: l'azione è **bloccata**; il tenant può **aspettare il reset della
   finestra** (riazzera il conteggio) **oppure fare upgrade** a un tier superiore. Nessuna bolletta a sorpresa.
7. **`new-application` = co-pilota pricing/quota** (richiesto 2026-06-21): la skill **guida passo-passo** la definizione
   del modello (tier, prezzi mensile/annuale+sconto, freemium, trial, metrica+finestra+limiti per tier) e genera lo stub
   del **contratto di quota** + wiring catalog/entitlements/Paddle. Tracciato in memoria `skills-backlog` e in [_BACKLOG](_BACKLOG.md).

### B. Catalogo & mapping dati
8. **Mapping minimale Paddle**: **1 app = 1 Paddle Product**; **ogni (tier × ciclo) = 1 Paddle Price** sotto quel product
   (es. app con 3 tier → 1 product + 6 price: 3 tier × mensile/annuale). **Limiti e feature NON stanno in Paddle**:
   vivono nel nostro DB, legati al `paddle_price_id`. Coerente con A (quote applicative): a Paddle interessa solo
   importo + ciclo. **Upgrade/downgrade** = la subscription cambia Price **dentro lo stesso Product**.
9. **Split fonte di verità**:
   - **Nostro DB = verità del "cosa si vende"** (definizione app/tier/prezzi + mapping ID Paddle);
   - **Paddle = verità dello "stato di billing"** (status subscription, prezzo corrente, fine periodo, stato pagamenti),
     sincronizzato via **webhook** (topic D).
10. **Modello dati** — lato **catalogo** (platform-level, non tenant-scoped):
    - `app`: `app_id`, nome, stato (enabled/disabled #03), **`paddle_product_id`**;
    - `app_tier`: `tier_id`, `app_id`, nome, **`limits`** (JSON metrica→tetto+finestra), **`features`** (JSON flag), `trial_days`;
    - `app_price`: `price_id`, `tier_id`, `billing_cycle` (monthly/annual), **`paddle_price_id`**, importo, valuta.
11. **Modello dati** — lato **tenant** (tenant-scoped, `WHERE tenant_id`):
    - `account` (esistente) **+ `paddle_customer_id`** → **un customer Paddle per account**;
    - `subscription`: `tenant_id`, `app_id`, **`paddle_subscription_id`**, `paddle_price_id` corrente (→ risolve il tier),
      **`status`** (trialing/active/past_due/paused/canceled), `current_period_start/end`, `cancel_at`, `trial_end`.
    - Le **subscription sono per (tenant, app)** → un tenant può avere N subscription (una per app acquistata).
12. **Entitlement DERIVATO** (opzione 1, non materializzato): non esiste tabella `entitlement`; "app X attiva per tenant T
    al tier Y con limiti Z" si **calcola al volo** da `subscription` (status) + `app_price`/`app_tier` (definizioni).
    **Unica fonte di verità = `subscription`**; i webhook toccano **solo** `subscription`, zero rischio di disallineamento.
    Materializzazione/cache resta **ottimizzazione futura** se le letture diventassero un collo di bottiglia.

### C. Checkout
13. **Modalità overlay** (Paddle.js): finestra modale Paddle sopra la SPA. **Inline** = evoluzione futura; **hosted/redirect**
    scartato (porta l'utente fuori dall'app). Paddle gestisce carte/3DS/tasse/valute/antifrode (MoR): noi non tocchiamo
    mai dati di pagamento.
14. **Checkout iniziato lato server** (coerente con **invariante #1**): il frontend chiede al **nostro backend** (capability
    "billing" core #04) di iniziare l'acquisto; il backend legge **`tenant_id` dal JWT verificato**, risolve il
    `paddle_price_id` dal catalogo, **crea la transazione su Paddle (API)** con `custom_data = {tenant_id, app_id}` +
    `customer`, e restituisce un **checkout token**; il frontend apre l'overlay passando **solo** il token. I `custom_data`
    (incluso `tenant_id`) sono impostati **lato server** → fidati; il client non può manometterli.
15. **Customer Paddle creato lazy al primo acquisto**: si passa l'email al primo checkout, Paddle crea il customer, si
    salva `paddle_customer_id` sull'account; dai checkout successivi si passa l'ID salvato. (Niente customer per chi non
    compra → cost-min + minimizzazione dati, postura EU-purista.)
16. **Attivazione SOLO via webhook** (fonte di verità): l'entitlement si attiva quando il **webhook** Paddle aggiorna
    `subscription` (topic D), **mai** sull'evento client. L'evento client `checkout.completed` serve **solo per la UX**.
17. **UX post-checkout a stati con polling**: dopo l'overlay, la SPA mostra "attivazione in corso" (spinner) e fa **poll**
    del backend ogni 1–2 s finché `status=active` → "attivato". Oltre ~30–60 s: messaggio **rassicurante** (pagamento già
    riuscito, attivazione in arrivo / avviso email), **mai un errore**. **Polling, non push** (no infra realtime → cost-min).
    Dettaglio schermate/copy/edge → **use case "Acquisto / checkout"** ([_BACKLOG](_BACKLOG.md)).

### D. Webhook & source of truth
18. **Non negoziabili** (best practice Paddle): (a) **verifica firma HMAC** dell'header `Paddle-Signature` con secret in
    Secrets Manager — firma non valida → **401**, niente processing; (b) **idempotenza su `event_id`** (Paddle ri-invia →
    dedup, evento applicato una volta sola); (c) **gestione out-of-order** via `occurred_at` (un evento più vecchio non
    sovrascrive uno stato più recente).
19. **Architettura di ricezione (opzione 1)**: API GW → **Lambda di ingest** (verifica firma + dedup + accoda su **SQS**)
    → risponde `200` subito; poi un **consumer** legge da SQS e aggiorna `subscription` in modo **idempotente**, con
    **retry + DLQ + allarme** (#08). Disaccoppia ricezione da elaborazione; la Lambda è sempre disponibile e leggera
    (utile col servizio core **scale-to-0 in test**). Usa SQS già nello stack (#06). Costo trascurabile ai volumi webhook.
20. **Strategia di test del flusso di pagamento (a 3 livelli)** — richiesto 2026-06-21; coerente con #10 e topic I.
    **Principio: NON si guida l'iframe Paddle con Playwright** (dominio del terzo, fragile) → si mocka il confine e si
    testa a fondo il nostro codice.
    - **L1 — Integration test del processing webhook (esaustivo, per-PR, BLOCCANTE)** 🔑: payload webhook **sintetici
      firmati** (HMAC con secret di test) → si verifica l'evoluzione di `subscription`. Copre firma valida/errata,
      idempotenza, out-of-order, ogni tipo di evento, linkage tenant (custom_data), derivazione entitlement, enforcement
      quota (#09 A). Testcontainers Postgres reale (#10 C). Deterministico, no rete esterna.
    - **L2 — E2E Playwright dei nostri pezzi (per-PR, BLOCCANTE), Paddle.js MOCKATO**: schermata scelta tier (default
      annuale+sconto), click acquisto → chiamata backend server-initiated, **UX post-checkout con polling** simulando
      l'arrivo del webhook ("attivazione" → "attivato"). Testa la nostra UX/wiring, non l'iframe Paddle.
    - **L3 — Smoke E2E reale contro Paddle Sandbox (PRE-RELEASE)**: vero Paddle.js + carte test + vero webhook → valida
      il contratto d'integrazione reale. **Eseguito nel flusso di promozione a prod** (tag → prod, #07), **prima** del
      go-live di una release, **non** nel gate per-PR (esterno/lento/flaky). L'esito **confluisce nel gate di approvazione
      manuale** (#07 b1): un fallimento **ferma la release** in attesa di revisione umana. **Override manuale obbligatorio**:
      poiché il sandbox è infrastruttura di terzi (può essere down per cause non nostre), chi approva può **forzare la
      release** nonostante il fallimento L3 — con **motivazione esplicita registrata** (audit, es. "sandbox Paddle down").
    - **Locale (#11)**: Paddle è esterno → **sandbox** o **stub Paddle.js**, mai pagamenti reali.

21. **Set di eventi sottoscritti** (mapping su `subscription`): `subscription.created` (crea riga + linkage tenant/app via
    custom_data), `subscription.activated` (trial→active/prima attivazione), **`subscription.updated`** (catch-all: status,
    `paddle_price_id`/cambio tier, `current_period_end`, `cancel_at`, anche `past_due`), `subscription.canceled`
    (status=canceled), `subscription.paused`/`.resumed`, `transaction.completed` (pagamento riuscito: attivazione **e
    rinnovi**), `transaction.payment_failed` (→ dunning), `customer.created`/`.updated` (cattura `paddle_customer_id`,
    creazione lazy C). **Non** sottoscriviamo eventi inutilizzati (meno rumore/superficie). Fatture/ricevute = gestite da
    **Paddle MoR** (topic J), non come evento. Il *cosa fare* su cancel/pause/past_due (accesso fino a quando, grace) → **E**.

## Questioni aperte
- **Upgrade/downgrade tra tier** (proration, effetto immediato vs a fine ciclo, comportamento della quota al cambio) →
  da dettagliare nel **topic E (ciclo di vita)**.
- **Creazione/sync di Product e Price su Paddle** (manuale da dashboard Paddle vs via API da `new-application`/admin) →
  topic **H (config admin del pricing)**.
- Topic **E–K** ancora da affrontare.

## Alternative valutate / scartate
_—_

## Impatti su altre aree
_—_
