# Implementation Log — Change 0046: Scheletro del sito vetrina (Astro SSG)

**Branch**: `change/0046-use-case-0036-vetrina-astro-scheletro`
**Aree**: nuovo artefatto `site/` (Astro SSG); `infra/` (distribuzione statica, come codice non applicato); `run-tests.sh` (nuova area `site`); `docs/`.
**Completata**: 2026-07-25
**Modalità**: autopilot — le risposte alle domande di approfondimento sono dell'agente e sono tracciate in [decisions.json](decisions.json) (17 decisioni, 15 in autopilot). I tre gate (approvazione requisiti, consenso al commit, consenso al merge) restano dello sviluppatore.

## File modificati

| File | Azione |
|---|---|
| `site/package.json`, `site/package-lock.json` | Creato — progetto Astro, dipendenza `file:` sul design system |
| `site/astro.config.mjs` | Creato — SSG, i18n subpath 5 lingue, integrazioni react+tailwind |
| `site/tailwind.config.mjs`, `site/tsconfig.json`, `site/vitest.config.ts`, `site/.gitignore` | Creato — config |
| `site/src/lib/i18n.ts` | Creato — 5 lingue, hreflang/x-default, normalizzazione percorsi |
| `site/src/lib/legal.ts` | Creato — renderer legali: entity.yaml → sostituzione token → HTML; gate `published` |
| `site/src/lib/legal.test.ts` | Creato — test unitari (token, gate, hreflang, parità 5 lingue, no-residui) |
| `site/src/layouts/BaseLayout.astro` | Creato — head con hreflang+noindex, header, footer legale |
| `site/src/components/LanguageSwitcher.tsx` | Creato — island React selettore lingua |
| `site/src/pages/index.astro` | Creato — root `/` con redirect per lingua (client-side) |
| `site/src/pages/[lang]/index.astro` | Creato — home placeholder per lingua |
| `site/src/pages/[lang]/legal/[component].astro` | Creato — pagine legali (5×5) |
| `site/src/pages/coming-soon.astro` | Creato — backoffice "coming soon" |
| `site/src/styles/global.css` | Creato — import token/font del design system + stili prosa legale |
| `site/scripts/postbuild-check.mjs` | Creato — controllo post-build (parità/token/hreflang/noindex/link) |
| `infra/modules/platform_shared/site.tf` | Creato — S3 privato+OAC+CloudFront+Route53+response headers |
| `infra/modules/platform_shared/functions/site-viewer-request.js.tftpl` | Creato — CloudFront Function (rewrite URL + basic-auth test) |
| `infra/modules/platform_shared/variables.tf` | Modificato — `site_indexable`, `site_basic_auth_userpass` |
| `infra/modules/platform_shared/outputs.tf` | Modificato — `site_bucket_name`, `site_distribution_id`, `site_url` |
| `run-tests.sh` | Modificato — nuova area `site` (vitest + astro build + controllo post-build) |
| `docs/usecases/09-marketing-site/0036-*.md` | Modificato — chiuso il punto aperto token; registrati i rimandi |
| `docs/usecases/_INDEX.md` | Modificato — UC 0036 → ✅ implementato |

## Cosa è stato fatto

Realizzato lo scheletro del sito vetrina come progetto **Astro SSG** separato in `site/`. Il sito genera 32 pagine statiche:
la home placeholder e i 5 documenti legali (privacy/terms/refund/cookie/subprocessors) nelle **5 lingue**, letti dai
markdown già presenti in `content/legal/` con i **token `{{titolare.*}}` risolti** da `entity.yaml`, più il redirect di
root per lingua e la pagina backoffice "coming soon". Riusa il **brand del design system** (UC 0019) via preset Tailwind e
token/font. L'**infra di hosting** (distribuzione statica dedicata) è scritta come codice Terraform e passa fmt+validate;
l'apply resta operativo (phased-env). Il tutto è coperto da test unitari e da un controllo post-build, integrati come nuova
area `site` in `run-tests.sh`.

## Decisioni prese

Condotta in **autopilot**: tutte le scelte sono in [decisions.json](decisions.json). Le principali:
- **Cosa renderizza lo scheletro** (dec. 5): legali reali + home placeholder + coming-soon; contenuti marketing veri esclusi (UC 0037/0038).
- **Renderer legale in proprio** (dec. 14): gray-matter + sostituzione token + marked, per rendere sostituzione e controlli testabili — chiude il contratto token ereditato da UC 0002 (dec. 6).
- **Riuso brand via export sorgente** del design system, senza dipendere dalla sua build (dec. 4/15).
- **Infra come nuovo file di `platform_shared`** riusando zona/certificato/policy esistenti, apply differito (dec. 9/16).
- **Nessun dato personale** (dec. 11) e **gate privacy** risolto: i 14 segnali erano solo dipendenze di build di un sito statico, nessun responsabile esterno a runtime (dec. 17).

## Invarianti appgrove

- **tenant_id dal JWT / filtro row-level**: non applicabili — sito pubblico statico, nessun tenant, nessuna query.
- **Modulo `microsaas_app`**: non applicabile — il sito non è un'app; la distribuzione statica riusa il pattern S3-privato+OAC+CloudFront esistente delle SPA (nessuna infra bespoke parallela).
- **Logging strutturato**: non applicabile — nessun servizio backend introdotto.

## Note per il revisore

- **Infra non applicata** (phased-env): `terraform validate` verde su `envs/test` e `envs/prod`, `terraform test` di `platform_shared` verde (4 passati) col piano che include le nuove risorse del sito. L'apply, la delega DNS, la copertura del certificato per l'apex di test, le credenziali basic-auth, la rimozione di `noindex` al go-live e il job CI di deploy sono **operativi/differiti**, tutti tracciati in `docs/usecases/09-marketing-site/0036-*.md` ("Rimandi aperti dallo scheletro").
- **Contratto cross-area**: il sito consuma `content/legal/*.md` + `entity.yaml` in **sola lettura** (contratto già definito in `content/legal/README.md`). Output Terraform `site_bucket_name`/`site_distribution_id` predisposti per la futura pipeline di deploy.
- **Decisioni differite tracciate**: chiuso il punto aperto sui token; aggiunti in UC 0036 i rimandi per contenuti (UC 0037), landing (UC 0038), redirect edge Accept-Language, attivazione operativa hosting, job CI deploy, riuso profondo componenti design system, host della "coming soon". Nessuna decisione differita lasciata solo in chat.
- **Gate parità scaffold**: nessun percorso-sorgente dei modelli toccato.

## Test

- **site** (`./run-tests.sh site`): `vitest` (10 test: sostituzione token, gate `published`, hreflang, parità 5 lingue, nessun token residuo) + `astro build` (32 pagine) + controllo post-build (parità/token/hreflang/noindex/link interni) — **verde**.
- **infra** (`./run-tests.sh infra`): fmt + validate (`envs/test`, `envs/prod`) + `terraform test` di `platform_shared` + test Lambda Python — **verde**.
- **Verifica in locale** (impegno del DoD): `astro build` + `astro preview` + `astro dev` avviati davvero; rotte `/`, `/en/`, `/it/legal/privacy/`, `/coming-soon/` rispondono 200; schermate di home e privacy verificate (token risolti, brand corretto). In dev emergevano errori Vite sul serving dei font `@fontsource` (risolti da `frontend/node_modules`, fuori da `site/`): corretti allargando `vite.server.fs.allow` alla radice del monorepo (dec. 18) — font ora serviti con 200, zero richieste fallite. Riguarda solo il dev server; la build era già corretta.
- **Gate privacy** (`npm run privacy-scan`): 14 segnali, tutti dipendenze di build classificate come non-personali (nessun manifesto/RoPA). Vedi dec. 17.

## Stato criteri di accettazione

- [x] `site/` si avvia in locale e produce build statica senza errori.
- [x] 5 legali × 5 lingue a `/{lang}/legal/{componente}`; token risolti; nessun `{{` residuo nell'HTML dei legali.
- [x] hreflang (5 lingue + x-default) e meta `noindex` su ogni pagina; root `/` reindirizza per lingua con fallback EN.
- [x] La build emette solo contenuti `published` (verificato dal gate e dai test unitari).
- [x] Home placeholder per lingua e pagina backoffice "coming soon" presenti.
- [x] Brand dal design system via preset/token, senza dipendere dalla sua build.
- [x] Infra del sito scritta e verde a `fmt`/`validate` (non applicata).
- [x] `run-tests.sh` ha l'area `site` ed è verde; aree toccate verdi prima del commit.
- [x] Punto aperto token chiuso in UC 0036; `_INDEX.md` porta 0036 a ✅.
