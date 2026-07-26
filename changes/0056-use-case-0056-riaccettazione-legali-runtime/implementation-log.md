# Implementation Log — Change 0056: Ri-accettazione ToU/Privacy a runtime (UC 0056)

**Branch**: `change/0056-use-case-0056-riaccettazione-legali-runtime` (git worktree dedicato: `/Users/msindoni/Projects/appgrove-wt-0056`)
**Aree**: `services/core` · `frontend/apps/backoffice` + `frontend/packages/{api-client,i18n}` · `.github/workflows` · `docs/compliance` · `docs/usecases`, `docs/_REVISIONE-LEGALE.md`
**Completata**: 2026-07-26
**Modalità**: autopilot — le risposte alle domande di approfondimento sono dell'agente e sono tracciate in [decisions.json](decisions.json) (16 decisioni). I punti a rilievo legale sono stati esposti allo sviluppatore al gate di rilettura e registrati in [docs/_REVISIONE-LEGALE.md](../../docs/_REVISIONE-LEGALE.md) (riga L15).

## Cosa è stato fatto

**Backend (`services/core`, schema `platform`)**: migrazione `V11` con `legal_version` (versioni correnti, platform-level) e `legal_acceptance` (log append-only tenant/utente-scoped). Comando command-mode `sync-legal` in `CoreMain` (+ startup-sync in dev/test) che legge i frontmatter di `content/legal/` (bundlati nella classpath via `maven-resources`) e riconcilia `legal_version` (major = prima cifra del semver, IT facente fede). Endpoint: `GET /me/legal/status` (derivazione major→blocco / minor→notifica, tenant/user solo dal JWT), `POST /me/legal/acceptance` (registrazione idempotente), `GET /legal/{component}?lang` (testo coi token `{{titolare.*}}` risolti lato core, porting di `site/src/lib/legal.ts`). Log accettazioni agganciato a export ed erasure GDPR (`PlatformDataContract`) + voce entity-backed nel manifesto.

**Frontend (`backoffice`)**: `LegalGate` avvolge la shell — a sessione autenticata chiama `/me/legal/status`; se ci sono componenti pendenti rende una schermata bloccante full-screen (`LegalGateScreen`) con testo reso via `react-markdown` e azione Accetto/Ho preso atto → `POST` → sblocco; fail-open su errore; route `/privacy` e `/support` esenti (diritti GDPR sempre accessibili). Banner non bloccante per i cambi minor. Hook `useLegalStatus`/`useAcceptLegal`/`useLegalDoc`; tipi rigenerati da OpenAPI; copy i18n `legal.*` nelle 5 lingue.

**CI**: step `oneshot platform sync-legal` dopo il `migrate` in `deploy-test.yml` e `release-prod.yml`.

## Decisioni prese

Sintesi (registro completo in [decisions.json](decisions.json)):
- Endpoint su due classi (`/me/legal/*` e `/legal/{component}`) per evitare la collisione di root con `MeResource` (dava 404).
- Accettazione iniziale catturata dallo **stesso gate al primo login** invece di una checkbox nel wizard di signup (un solo meccanismo, nessun rischio sul flusso auth) — scostamento consapevole dal requisito, tracciato in UC 0056.
- `commit-hash` come `Optional<String>` (config opzionale).
- Classificazione privacy (vedi sotto).

## Invarianti appgrove
- **Tenant/utente solo dal JWT**: gli endpoint leggono `tenant_id`/`sub` da `CallerContext`; il log è `BaseTenantEntity` (`WHERE tenant_id` automatico). Test multi-tenancy dedicato (A non vede/accetta per B).
- **Filtro row-level**: automatico via discriminator sul log.
- **Logging strutturato**: `legal.accept` con `tenant_id`/`user_id`.
- Modulo Terraform `microsaas_app`: non pertinente (solo core).

## Gate privacy/RoPA (UC 0031)
Scanner eseguito: **17 segnali, tutti classificati**. Unico dato personale: `legal_acceptance.user_id` (identificativo) → `@PersonalData` + voce entity-backed in `platform.yaml` + RoPA rigenerata (`npm run assemble`). Gli altri campi di `legal_acceptance` (componente/versione/major/atto/data/commit) e tutta `legal_version` sono metadati dell'atto/prodotto, **non** personali. **react-markdown NON è un sub-processor** (rendering lato browser, nessun dato a terzi). Base giuridica: contratto (art. 6.1.b) + accountability (art. 5.2). Retention: vita account + prescrizione. **Classificazione del cambio: MINOR** (accountability accessoria, non nuovo uso dei dati). Tutto da validare in revisione legale (L15 di `_REVISIONE-LEGALE.md`).

## Note per il revisore
- **Contratto cross-area**: nuovi endpoint core additivi (OpenAPI rigenerato e committato; `oasdiff` in CI non deve segnalare breaking); il frontend consuma i tipi rigenerati.
- **Decisioni differite tracciate** in [UC 0056](../../docs/usecases/04-platform-core/0056-riaccettazione-legali-runtime.md) "Punti aperti": checkbox di accettazione esplicita al signup; preavviso email 30gg sub-processor + finestra di opposizione (canale email, UC 0039); componenti legali per-app (non esistono ancora). Il rendering in-app dei token `{{titolare.*}}` (era un punto aperto) è **chiuso** qui.
- **Punti a rilievo legale** (approvati ai requisiti, da validare): blocco su presa d'atto Privacy·Cookie; base/retention del log; classificazione MINOR — in `_REVISIONE-LEGALE.md` L15.
- **Worktree**: change sviluppata in git worktree; i `node_modules` (frontend, tools/compliance) e i `dist` dei pacchetti sono stati installati/ricostruiti nel worktree (ignorati da git, non committati).

## Test
Tutte le suite delle aree toccate verdi:
- **backend** (`services/core`, Testcontainers): **196 test** verdi (inclusi `LegalTest` — derivazione major→blocco/minor, idempotenza, multi-tenancy, rendering token; `LegalContentLoaderTest`; `PersonalDataManifestTest`; `PlatformGdprContractTest`; `OpenApiContractTest`). Aggiornata l'asserzione di `GdprExportApiTest` (step export 5→6). Altri moduli backend non toccati.
- **frontend** (`backoffice`): vitest **108 test** (inclusi 6 del `LegalGate`) + **e2e Playwright 22** (inclusi 2 nuovi: gate bloccante→accetta→ingresso; esenzione `/privacy`) verdi in browser reale.
- **compliance**: manifesti + RoPA allineati, parità 5 lingue legali — verde.

## Stato criteri di accettazione
- [x] `V11`: `legal_version` (platform) + `legal_acceptance` (tenant/utente-scoped, append-only).
- [x] `sync-legal` (comando + startup) popola `legal_version`; major = prima cifra del semver.
- [x] `/me/legal/status` deriva i pendenti (major→blocco, minor→notifica) leggendo tenant/user solo dal JWT; `/me/legal/acceptance` idempotente e tenant/utente-scoped.
- [x] `/legal/{component}` rende il testo coi token risolti (nessun `{{}}` residuo — test).
- [x] Login con major pendente → schermata bloccante → accetta → ingresso; diritti GDPR (`/privacy`) esenti — verificato via e2e.
- [x] Log accettazioni in export/erasure GDPR; manifesto entity-backed + RoPA allineata.
- [~] Signup: accettazione iniziale catturata dal gate al primo login (checkbox esplicita differita, tracciata).
- [x] Suite verdi: backend (integration + multi-tenancy + contract GDPR + comando) + frontend (unit + e2e) + compliance.
