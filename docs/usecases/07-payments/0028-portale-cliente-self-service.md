# UC 0028 — Customer portal & gestione abbonamento self-service

**Area**: 07-payments · **Fase**: 5 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0026](0026-ciclo-vita-abbonamento.md) (lifecycle), UC [0020](../06-frontend/0020-shell-spa-backoffice.md) (shell)
**Fonte decisioni**: #09 G (customer portal & self-service)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [09-pagamenti](../../09-pagamenti.md), [03-frontend](../../03-frontend.md)

## 1. Obiettivo / Scope
Implementare la **gestione abbonamento self-service** con approccio **ibrido**: nel nostro backoffice ciò che controlliamo e si
lega a tier/quota; delega a **Paddle** ciò che è PCI/finanziario.
**Incluso (nostra UI)**: vedi abbonamenti (status/tier/fine periodo/cambi programmati, legge `subscription`); **upgrade/downgrade**
(rispetta gating flow/stock + semantica tier); **disdici/riattiva**; **uso quota** (consumo/banner). **Delega Paddle**: pulsante
**"Gestisci pagamento e fatture"** → **Customer Portal** Paddle generato **server-side** (metodo di pagamento PCI + fatture/ricevute MoR).
**Escluso**: la semantica lifecycle (UC 0026), l'enforcement (UC 0027), il checkout iniziale (UC 0024), l'admin (UC 0021).

## 2. Attori & ruoli
- **Utente** (owner): gestisce l'abbonamento.
- **Backend**: legge `subscription`, applica cambi (rispettando gating), genera la sessione portal Paddle.
- **Paddle**: metodo di pagamento + fatture (MoR).

## 3. Precondizioni
- `subscription` aggiornata (UC 0025/0026); shell (UC 0020); provider (stub locale/Paddle reale); customer Paddle esistente.

## 4. Flusso principale
1. **Vedi abbonamenti**: status/tier/fine periodo/cambi programmati dalla `subscription` (#09 G33).
2. **Upgrade/downgrade**: rispetta gating **flow/stock** (E23) + semantica tier che solo noi conosciamo (#09 G33).
3. **Disdici/riattiva**: cancel_at a fine periodo / annulla disdetta (E25) (#09 G33).
4. **Uso quota**: mostra consumo/banner (la metrica è applicativa).
5. **"Gestisci pagamento e fatture"** → apre la **sessione Customer Portal Paddle** generata **server-side** (API) per metodo di pagamento (PCI) e fatture/ricevute (MoR) (#09 G33).

## 5. Flussi alternativi / edge / errori
- **Downgrade stock con stato sopra capacità** → bloccato + remediation (#09 E23).
- **Dati carta**: mai toccati da noi; solo via portal Paddle (#09 G33).
- **EU-purista**: il portal è hosted da Paddle (UK, MoR) → coerente con sub-processor accettato; deleghiamo solo ciò che è intrinsecamente suo (#09 G33).
- **Subscription scaduta**: la schermata offre riattiva **+** esporta/elimina dati (diritti GDPR esenti, #09 F31).

## 6. Schermate & stati
Pannello abbonamenti (lista app con status/tier/period/cambi); azioni upgrade/downgrade/disdici/riattiva; banner uso quota;
pulsante "Gestisci pagamento e fatture" (apre portal Paddle). Stati loading/empty/error; conferme per azioni a impatto.

## 7. Dati toccati
Legge/aggiorna `subscription` (cambi programmati); apre il portal Paddle (server-side). **Dati pagamento/fatture in capo a
Paddle** (MoR). Manifest: billing/abbonamento (base contratto); fatture/fiscale = Paddle (titolare autonomo, #09 J45).

## 8. Permessi & gate
- **Invarianti**: `tenant_id` dal JWT; operazioni sull'abbonamento del **proprio** tenant; ruolo owner per cambi billing.
- I cambi rispettano i gate lifecycle (gating stock); diritti GDPR sempre disponibili (#09 F31).

## 9. Requisiti di test
- **L2 E2E**: vedi/upgrade/downgrade/disdici/riattiva; apertura portal Paddle (mockata in dev).
- **Integration**: cambi rispettano gating flow/stock; sessione portal generata server-side.
- **Security**: si gestisce solo il proprio tenant; nessun cross-tenant.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #09 G33, E23/E25, F31, J45.
- **DoD**:
  1. UI self-service (vedi/upgrade/downgrade/disdici/riattiva/uso quota) sulla nostra `subscription`.
  2. Pulsante portal Paddle (pagamento+fatture) generato server-side.
  3. Gating flow/stock rispettato; diritti GDPR disponibili anche a subscription scaduta.
  4. L2 E2E + integration + security verdi.

## Punti aperti / decisioni differite

> **Stato implementazione (change `0024-use-case-0028-…`)**: implementati nel backoffice il read-model dedicato
> `GET /me/subscriptions` (tutte le subscription, anche non-attive), le mutazioni self-service
> (upgrade immediato / downgrade schedulato / disdici / riattiva) col modello **command → provider → webhook →
> read-model**, la sessione **Customer Portal** Paddle server-side (`POST /me/portal-session`), il pannello
> Billing + i **banner azionabili 402/429**. **Risolti** i due punti aperti sotto (persistenza cambio tier
> schedulato ✅; UX enforcement 402/429 ✅). Restano differiti i residui elencati in coda.

### Residui differiti (aperti dopo change 0024)

- **Consumo quota in tempo reale nel pannello** *(owner: questo UC 0028 / condiviso con UC 0027)*. Oggi il
  banner mostra i **limiti del piano** (dal read-model entitlement/subscription); il **consumo corrente** (es.
  "73/100") è **applicativo** (SPI `QuotaService.currentUsage`, lato servizio app) e non leggibile da `core`.
  *Cosa manca:* un contratto di lettura usage per-app (o un read-model usage trasversale) che alimenti il
  banner. *Perché differito:* richiede un'API per-app trasversale, fuori dallo scope del portale.
- **Gate stock del downgrade contro l'uso reale** *(stesso owner del punto sopra)*. La logica di blocco
  (`TierChangePolicy.evaluateDowngrade`) è reale e testata, ma il resource passa un uso corrente **vuoto**
  (stessa lacuna: `core` non conosce lo stock per-app), quindi a runtime il downgrade non viene bloccato.
  *Cosa manca:* cablare l'uso stock reale per-app nel gate (dipende dal contratto usage sopra).
- **Motore reale di esporta/elimina dati (GDPR)** *(owner: UC 0033)*. Il pannello (schermata scaduta) offre il
  **CTA** verso i diritti GDPR (oggi rimanda a `/account`); il motore reale di export/cancellazione è di
  **UC 0033** (`self-service-gdpr`).
- **Implementazione reale Paddle** *(gated #14)*. `PaddlePaymentProvider.changeSubscriptionTier/cancelSubscription/
  resumeSubscription/createCustomerPortalSession` lanciano `UnsupportedOperationException`: il client Paddle reale
  è bloccato da #14 (nessun account senza sito+ToU/PP). Lo stub locale copre dev/test.

### Storico (risolti da change 0024)

- **Persistenza del cambio tier schedulato (downgrade a fine periodo)** ✅ *risolto in change 0024* (colonne
  `subscription.scheduled_tier_id` + `scheduled_change_at`, mappate dal consumer webhook; esposte da
  `/me/subscriptions`). *(owner: questo UC 0028)*. La
  change `0021-use-case-0026-…` ha implementato la semantica di accesso del ciclo di vita (mappa
  status→accesso + `SubscriptionLifecycle`), ma **non** la *surfacing* del downgrade schedulato "attivo
  dal giorno X verso il tier Y": a livello di accesso un downgrade schedulato è identico a `ACTIVE`
  (tier corrente fino a fine periodo), quindi la sola derivazione non basta. *Cosa manca:* persistere il
  cambio schedulato (es. colonna `subscription.scheduled_tier_id` + `scheduled_change_at`) e mapparlo
  dall'evento webhook `subscription.updated` (oggi il consumer di UC 0025 mappa solo status/tier
  corrente/period/cancel_at/trial_end), così la UX self-service può mostrarlo. *Perché differito:* serve
  un consumatore (il portale self-service di questo UC) e tocca schema+pipeline → matura qui, non in 0026.

- **UX azionabile per i gate di enforcement (402/403/429)** ✅ *risolto in change 0024* (interceptor 402/429 nel
  `QueryClient` → banner globale con CTA upgrade/riattiva; 403 resta gestito dai guard di ruolo). _(tracciato dalla change
  `0023-use-case-0027-…`)_. UC 0027 ha
  reso reale il **confine di enforcement lato backend** (gate 402 entitlement/stato, 429 quota, 403 ruolo) + l'endpoint
  `/me/entitlements`, ma il frontend è **solo UX** (#09 F30): mancano i **banner azionabili** che traducono questi esiti —
  "limite raggiunto, fai upgrade" (429), "abbonamento scaduto: **riattiva oppure esporta/elimina** i tuoi dati" (402,
  #09 F31, ribadisce che i dati restano esercitabili) — e l'aggancio ai flussi di **riattivazione/upgrade**. *Perché qui:*
  è UX di self-service billing, naturale nel portale cliente; UC 0027 ha lasciato solo il **wiring del registry** (app
  acquistata compare in sidebar) e le risposte problem+json con i messaggi. *Cosa manca:* intercettare 402/429 nel client
  API e mostrare i banner/CTA con i link a checkout/portal.
