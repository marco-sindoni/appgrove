# Change 0025: Test pagamenti L1/L2/L3 (UC 0029) — perimetro "test nel repo"

**Branch**: `change/0025-use-case-0029-test-pagamenti-l1-l2-l3`
**Aree**: `services/core` (test), `frontend` (e2e/orchestrazione), `run-tests.sh`, `docs/usecases`
**Data**: 2026-07-03
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/07-payments/0029-test-pagamenti-l1-l2-l3.md](../../docs/usecases/07-payments/0029-test-pagamenti-l1-l2-l3.md)
**Tocca dati personali?**: No — solo dati di test sintetici (L1/L2 offline; nessun pagamento reale).

## Problema / Obiettivo

UC 0029 richiede la strategia di test pagamenti a 3 livelli (#09 D20). La ricognizione mostra che
L1 è quasi completo (firma/replay, idempotenza, out-of-order, tutti gli eventi, linkage tenant,
DLQ — su Testcontainers Postgres) e L2 è funzionalmente completo (Playwright con Paddle.js
mockato via `@appgrove/paddle-stub`), ma:

1. manca la **catena L1 unica** *webhook firmato → subscription → `/me/entitlements` derivato*
   (oggi entitlement testati da seed SQL, non dallo stato prodotto dal webhook);
2. gli **E2E Playwright non sono nel gate**: fuori da `run-tests.sh`, quindi la clausola
   "L2 per-PR bloccante" non è realizzabile nemmeno quando UC 0005 collegherà la CI;
3. **L3 non è iniziato** e i suoi prerequisiti sono esterni e ⬜ (UC 0001 account/sandbox Paddle,
   UC 0005 pipeline release con gate/override, #14 per il vero Paddle.js).

**Perimetro scelto (opzione 1, confermata)**: realizzare ora tutto ciò che vive nel repo (punti 1
e 2), predisporre per L3 solo la **struttura eseguibile-quando-possibile** (suite smoke
skip-by-default attivabile via env), e **tracciare formalmente** ogni parte non implementata nello
use case che la possiede.

## Scope

1. **L1 — catena completa in `services/core`** (solo test, nessun codice di produzione):
   nuovo `@QuarkusTest` che, senza seed SQL dello stato subscription, esercita la catena
   *payload webhook sintetico firmato → pipeline reale (ingest → coda → consumer) → subscription
   → `GET /me/entitlements`* e verifica l'entitlement derivato (accesso, tier, limiti quota nel
   read-model) per: attivazione, upgrade/cambio tier, cancellazione (ritorno a baseline free).
   Il confine è documentato: l'enforcement 402/429 lato app resta coperto dai test esistenti di
   `fatture` sul seam REST `EntitlementClient` (servizi separati, non concatenabili in un JVM).
2. **L2 — promozione a gate canonico**: `run-tests.sh` (area `frontend`, opzione A confermata)
   esegue vitest **e poi** i Playwright e2e dei moduli che li hanno (`apps/backoffice`,
   `apps/admin`), con **auto-install del browser** se mancante (pattern `ensure_colima`:
   `npx playwright install chromium` idempotente). Exit ≠ 0 se una suite e2e è rossa.
3. **L3 — struttura skip-by-default**: suite smoke Playwright separata (progetto/config dedicato,
   fuori da `run-tests.sh` perché pre-release e non per-PR) che **si auto-skippa** senza le
   variabili d'ambiente sandbox (`APPGROVE_L3_BASE_URL`, credenziali Paddle Sandbox) e contiene
   lo scheletro dello smoke (checkout reale con carta test → attivazione via vero webhook).
   Runbook nel README dell'e2e.
4. **Tracciamento decisioni differite** (gate costituzionale, confermato dallo sviluppatore):
   - **UC 0005** (`docs/usecases/02-devops-infra/0005-pipeline-cicd.md`, sez. "Punti aperti /
     decisioni differite"): cablaggio per-PR bloccante di `run-tests.sh`; job L3 nella pipeline
     release tag→prod; gate approvazione manuale con override motivato e audit.
   - **UC 0001** (`.../0001-setup-business-legale.md`, stessa sezione): l'account sandbox Paddle
     è prerequisito per attivare L3.
   - **UC 0029** (sezione "Punti aperti / decisioni differite" nel suo file): cosa resta aperto
     e chi lo possiede (per-PR/pipeline → UC 0005; sandbox → UC 0001; loader vero Paddle.js e
     `PaddlePaymentProvider` reale → gated #14, si attivano con L3).
5. **Indice**: `docs/usecases/_INDEX.md` → 0029 ✅ al commit (già 🟡).

## Fuori scope

- Qualsiasi workflow CI/GitHub Actions (per-PR o release): **UC 0005**.
- Account/sandbox Paddle, esecuzione reale di L3: **UC 0001** + #14.
- Loader del vero Paddle.js e `PaddlePaymentProvider` reale (restano placeholder gated #14).
- Modifiche funzionali a checkout/webhook/lifecycle/enforcement (UC 0024–0028, già consegnati).
- Lo stub Paddle in sé (UC 0023, già consegnato).

## Criteri di accettazione

- [ ] Nuovo test L1 "catena" verde: webhook firmato → subscription → `/me/entitlements` derivato
      (attivazione, cambio tier, cancellazione), senza seed SQL dello stato subscription.
- [ ] `./run-tests.sh frontend` esegue vitest + Playwright e2e (backoffice, admin), auto-installa
      chromium se manca, e fallisce se un e2e è rosso; `./run-tests.sh` completo li include.
- [ ] Suite L3 presente, auto-skip senza env sandbox (0 falliti in locale), runbook scritto.
- [ ] Punti differiti registrati in UC 0005, UC 0001 e UC 0029; `_INDEX.md` 0029 → ✅.

## Invarianti appgrove toccati

- **Tenant ID solo dal JWT / linkage da payload firmato**: il test L1 catena verifica che
  l'entitlement derivi dal `custom_data` del payload **firmato** e sia letto via JWT su
  `/me/entitlements` (nessun tenant da request).
- **Row-level tenant filter**: la catena include un'asserzione di isolamento (l'entitlement di un
  tenant non appare a un altro). Gli altri invarianti (modulo Terraform, logging) non sono toccati
  (change di soli test/script/docs).

## Requisiti di test

La change è essa stessa test. Verifiche: suite backend `services/core` verde (inclusa la nuova
catena L1); `./run-tests.sh frontend` verde end-to-end (dimostra il cablaggio e2e); run locale
senza env sandbox mostra la suite L3 skippata, non fallita.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | N/A (nessun contratto modificato; solo test/script/docs) |
| Version bump | nessuno |
