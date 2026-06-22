# UC 0023 — Stub Paddle locale (port PaymentProvider, fake Paddle.js, webhook sintetici firmati, scenari lifecycle)

**Area**: 07-payments · **Fase**: 5 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0008](../03-local-dev/0008-stack-sviluppo-locale.md) (stack locale/ElasticMQ), UC [0013](../04-platform-core/0013-account-utenti-inviti-api.md) (subscription)
**Fonte decisioni**: #09 I39/40 (stub), #11 (locale offline), #10 L (test)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [09-pagamenti](../../09-pagamenti.md), [11-developer-experience](../../11-developer-experience.md), [10-testing](../../10-testing.md)

> **Aggancio da change 0002 (UC 0008).** Le code per i **webhook Paddle** sono già predisposte in `dev/elasticmq.conf`
> (`paddle-webhooks` + DLQ `paddle-webhooks-dlq`). Lo stub qui produce/consuma su quelle code, senza ridefinire ElasticMQ.

## 1. Obiettivo / Scope
Implementare lo **stub Paddle locale** dietro la stessa interfaccia (port) del provider reale, così l'intero flusso pagamenti
gira **offline/deterministico** senza account Paddle (unica via non bloccata da #14).
**Incluso**: **port `PaymentProvider`** con impl **`StubPaymentProvider`** (dev/locale) vs **`PaddlePaymentProvider`** (test/prod);
**Paddle.js finto** (frontend, emette `checkout.completed` sintetico); **API Paddle finta** (ID plausibili) + **emettitore di
webhook sintetici firmati** verso l'endpoint locale, che passano per la **pipeline reale** (Lambda ingest → SQS via **ElasticMQ**
→ consumer → `subscription`); **scenari** del ciclo di vita (happy path, payment_failed/past_due, canceled, upgrade/downgrade).
**Escluso**: l'integrazione Paddle reale (sandbox/prod), il checkout UI (UC 0024), la pipeline webhook prod (UC 0025), L3 smoke reale (UC 0029).

## 2. Attori & ruoli
- **Developer / suite L1-L2**: usano lo stub per testare tutto offline.
- **StubPaymentProvider** (sistema): finge Paddle (JS + API + webhook firmati).

## 3. Precondizioni
- Stack locale (UC 0008) con **ElasticMQ** (SQS); core con `subscription` (UC 0013) + pipeline webhook (UC 0025) eseguibile in locale.

## 4. Flusso principale
1. **Port**: il codice di prodotto parla con `PaymentProvider`; in `%dev` = `StubPaymentProvider` (default), in test/prod = Paddle reale (#09 I39).
2. **Paddle.js finto** sul frontend: stessa interfaccia dell'overlay, emette `checkout.completed` sintetico (UX, #09 C16/I39A).
3. **API Paddle finta**: ritorna ID plausibili (customer/transaction/subscription) (#09 I39B).
4. **Webhook sintetici firmati** (HMAC con secret di test) verso l'endpoint locale → **pipeline reale** (Lambda ingest→ElasticMQ→consumer→`subscription`) (#09 I39B/40).
5. **Scenari** esposti: happy path, payment_failed/past_due, canceled, upgrade/downgrade → simulano tutto il lifecycle E in locale (#09 I39).

## 5. Flussi alternativi / edge / errori
- **Tunnel opt-in** (cloudflared) per ricevere webhook **reali** dal sandbox in locale (debug occasionale) (#09 I40).
- **Mai pagamenti reali in locale**; sandbox solo opt-in quando attivabile (#09 I39).
- **Firma errata / dedup / out-of-order**: lo stub può generare casi limite per L1 (UC 0029).
- **Parità**: stesso confine mockato di L2 (Playwright) — non si guida l'iframe Paddle (#10 L).

## 6. Risorse & runbook
**Componenti**: `StubPaymentProvider` (BE) + fake Paddle.js (FE) + emettitore webhook firmati + libreria di scenari. **Runbook**:
in `dev up` lo stub è attivo; si scelgono scenari per simulare il lifecycle; i webhook passano per la pipeline reale (ElasticMQ).
**Rollback**: nessun effetto esterno (tutto locale).

## 7. Dati toccati
Scrive `subscription` (stati sintetici) via la pipeline reale; dati 100% sintetici (seed UC 0011). Nessun dato di pagamento reale
(mai toccato — MoR). Manifest: N/A (ambiente dev/test). `paddle_*` = ID finti.

## 8. Permessi & gate
- **Invarianti**: i webhook sintetici portano `custom_data={tenant_id, app_id}` → linkage tenant corretto; il consumer aggiorna
  `subscription` per (tenant, app). Entitlement **derivato** da `subscription` (nessuna tabella).
- Nessun gate runtime nello stub; valida la catena gate a valle (entitlement).

## 9. Requisiti di test
È l'abilitatore di **L1/L2 offline** (#10 L / #09 D20): L1 = processing webhook esaustivo (firma/dedup/out-of-order/eventi/linkage/
entitlement/quota) con payload firmati; L2 = E2E UX con Paddle.js mockato. Deterministico, no rete esterna.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #09 I39/40, C16, D20 (L1/L2), #11 15, #10 L.
- **DoD**:
  1. Port `PaymentProvider` con Stub (dev) vs Paddle reale (test/prod).
  2. Paddle.js finto + API finta + webhook sintetici firmati che passano per la pipeline reale (ElasticMQ).
  3. Scenari lifecycle completi (happy/past_due/canceled/upgrade/downgrade) offline.
  4. Abilita L1/L2 deterministici; mai pagamenti reali in locale.
