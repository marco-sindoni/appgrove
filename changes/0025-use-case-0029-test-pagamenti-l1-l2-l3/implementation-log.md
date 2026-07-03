# Implementation Log — Change 0025: Test pagamenti L1/L2/L3 (UC 0029)

**Branch**: `change/0025-use-case-0029-test-pagamenti-l1-l2-l3`
**Aree**: `services/core` (test), `frontend`, `run-tests.sh`, `docs`
**Completata**: 2026-07-03

## File modificati

| File | Azione |
|---|---|
| `services/core/src/test/java/app/appgrove/core/billing/WebhookEntitlementChainTest.java` | Creato |
| `services/core/src/test/java/app/appgrove/core/TestData.java` | Modificato (overload `appTier` con `limits` jsonb) |
| `run-tests.sh` | Modificato (area `frontend` = vitest + Playwright e2e; `ensure_playwright`) |
| `frontend/package.json` | Modificato (script root `e2e` sui workspaces) |
| `frontend/apps/backoffice/playwright.l3.config.ts` | Creato (config L3 sandbox, skip-by-default) |
| `frontend/apps/backoffice/e2e-l3/checkout-smoke.spec.ts` | Creato (smoke L3, auto-skip senza env) |
| `frontend/apps/backoffice/e2e-l3/README.md` | Creato (runbook L3 / release) |
| `frontend/apps/backoffice/e2e/fatture.spec.ts` | Modificato (mock `/me/entitlements`, vedi Decisioni) |
| `frontend/apps/backoffice/tsconfig.json` | Modificato (include `e2e-l3` + config L3 nel typecheck) |
| `CLAUDE.md` | Modificato (descrizione area frontend di `run-tests.sh`) |
| `docs/usecases/02-devops-infra/0005-pipeline-cicd.md` | Modificato (punti differiti: per-PR, job L3+gate/override) |
| `docs/usecases/01-business-legal/0001-setup-business-legale.md` | Modificato (punto differito: sandbox sblocca L3) |
| `docs/usecases/07-payments/0029-test-pagamenti-l1-l2-l3.md` | Modificato (punti aperti post-implementazione) |
| `docs/usecases/_INDEX.md` | Modificato (0029 → ✅) |

## Cosa è stato fatto

**L1**: `WebhookEntitlementChainTest` chiude il gap "catena unica": payload webhook sintetici
**firmati** → pipeline reale (ingest → coda → consumer) → `subscription` → `GET /me/entitlements`
**derivato**, senza alcun seed SQL dello stato subscription (5 test: baseline free, attivazione pro
con phase/limiti, cambio tier con nuovo cap quota, cancellazione, isolamento tenant). **L2**:
`run-tests.sh` area `frontend` ora esegue vitest **e** gli E2E Playwright di backoffice+admin
(opzione A), con auto-install idempotente di chromium (`ensure_playwright`, stesso spirito di
`ensure_colima`); "frontend verde" = tutto verde, e il per-PR bloccante diventa vero per costruzione
quando UC 0005 eseguirà lo script in CI. **L3**: struttura skip-by-default (`playwright.l3.config.ts`
+ `e2e-l3/`), si attiva solo con env `APPGROVE_L3_*`, runbook di release nel README.

## Decisioni prese

1. **Cancellazione = accesso revocato, non "ritorno a free"**: i requirements dicevano "ritorno a
   baseline free", ma il read-model (gate 3, #09 dec.30, `EntitlementReadModel`) nega l'accesso
   quando esiste una subscription canceled/paused, **anche** se l'app ha un tier free. Il test
   asserisce il comportamento reale e documentato (app assente da `/me/entitlements`).
2. **Fix consapevole di `fatture.spec.ts`** (non un re-record cieco, #10 F): il primo run del gate
   ha scovato l'e2e rosso perché il mock era fermo a prima di UC 0027 — il registry ora deriva
   l'accesso da `GET /me/entitlements`, che il mock non stubbava ("You don't have access"). Aggiunto
   il mock coerente con gli altri spec, condizionato sull'env (`local` → entitled, `test` → no, per
   il test della route guard). È la prova concreta del valore del cablaggio: fuori dal gate gli e2e
   erano già marciti.
3. **Enforcement 402/429 fuori dalla catena L1 single-JVM**: `fatture` risolve gli entitlement via
   REST verso core (`RestEntitlementService`); la catena si ferma al confine `/me/entitlements` e i
   test esistenti di `fatture` coprono il gate dal contratto in poi (documentato nei requirements).

## Invarianti appgrove

- **Tenant solo da fonte fidata**: la catena L1 verifica che l'entitlement derivi dal `custom_data`
  del payload **firmato** e sia letto via JWT (`TestTokens.withTenant`) — mai da parametri richiesta.
- **Row-level filter**: `derivedEntitlementIsTenantScoped` prova che il tenant B non vede il tier
  a pagamento del tenant A (B resta su free). Modulo Terraform e logging: non toccati (solo test/script/docs).

## Note per il revisore

- **Decisioni differite tracciate** (gate costituzionale): cablaggio per-PR di `run-tests.sh` e job
  L3 release + gate/override → **UC 0005**; account sandbox → **UC 0001**; loader vero Paddle.js /
  `PaddlePaymentProvider` reale (gated #14) e selettori iframe dello smoke → **UC 0029, punti aperti**.
- `./run-tests.sh frontend` è più lento di prima (build + browser): è il costo dell'opzione A,
  scelto esplicitamente; il feedback rapido resta con `npm test` / `npm run e2e` per-workspace.
- Nessun contratto cross-area modificato; nessun codice di produzione toccato.

## Test

- **Backend** (`./run-tests.sh backend`): verde — include i 5 nuovi test di
  `WebhookEntitlementChainTest` (Testcontainers Postgres 17, coda in-memory, drain esplicito).
- **Frontend** (`./run-tests.sh frontend`): verde — vitest (tutti i workspaces) + 10 e2e backoffice
  + e2e admin, con chromium auto-installato.
- **Suite L3**: `npx playwright test -c playwright.l3.config.ts` senza env → `1 skipped`, exit 0.
- **Infra**: non toccata.

## Stato criteri di accettazione

- [x] Test L1 "catena" verde (attivazione, cambio tier, cancellazione*, isolamento) senza seed SQL
      — *asserzione allineata al comportamento reale: accesso revocato (Decisione 1).
- [x] `./run-tests.sh frontend` = vitest + Playwright e2e con auto-install chromium, rosso se un
      e2e fallisce (dimostrato sul campo: ha intercettato `fatture.spec.ts` rosso).
- [x] Suite L3 presente, auto-skip senza env sandbox, runbook in `e2e-l3/README.md`.
- [x] Differiti registrati in UC 0005 / UC 0001 / UC 0029; `_INDEX.md` 0029 → ✅.
