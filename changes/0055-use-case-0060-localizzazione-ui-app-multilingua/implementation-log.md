# Implementation Log — Change 0055: Localizzazione UI delle app alle 5 lingue (shell i18n + modulo fatture)

**Branch**: `change/0055-use-case-0060-localizzazione-ui-app-multilingua`
**Aree**: `frontend/` (pacchetto `@appgrove/i18n`, shell backoffice+admin, modulo `fatture`) · `tools/new-application` (template scaffolding) · `site/` (screenshot landing) · `docs/`
**Completata**: 2026-07-26
**Modalità**: autopilot — le risposte alle domande di approfondimento sono dell'agente e sono tracciate in [decisions.json](decisions.json) (17 decisioni, 14 marcate `(autopilot)`).

## File modificati

| File | Azione |
|---|---|
| `frontend/packages/i18n/src/index.ts` | Modificato — `LANGUAGES` a 5, `detectLanguage`/`persistLanguage`/`LANGUAGE_LABELS` |
| `frontend/packages/i18n/src/resources/{fr,es,de}.ts` | Creati — cataloghi shell nelle 3 nuove lingue (parità imposta dal tipo) |
| `frontend/packages/i18n/src/i18n.test.ts` | Modificato — parità generalizzata a 5 lingue + test rilevamento/persistenza |
| `frontend/apps/backoffice/src/registry/types.ts` | Modificato — `ModuleManifest.resources`; `name`/`label` = chiavi i18n |
| `frontend/apps/backoffice/src/registry/registry.ts` | Modificato — `registerModuleResources()` |
| `frontend/apps/backoffice/src/registry/ShellContext.tsx` | Modificato — `setLanguage` persiste la scelta |
| `frontend/apps/backoffice/src/main.tsx`, `frontend/apps/admin/src/main.tsx` | Modificati — avvio con lingua rilevata (+ registrazione moduli, backoffice) |
| `frontend/apps/backoffice/src/shell/{Topbar,Sidebar,Breadcrumb}.tsx` | Modificati — menu a tendina lingua; etichette moduli via `t()` |
| `frontend/apps/admin/src/shell/Topbar.tsx` | Modificato — menu a tendina lingua |
| `frontend/apps/backoffice/src/modules/fatture/i18n/{en,it,fr,es,de,index,format}.ts` | Creati (`it` è la ridenominazione di `strings.ts`) — bundle per-modulo + hook + formattazione |
| `frontend/apps/backoffice/src/modules/fatture/{manifest,screens/*,components/StatusBadge,components/QuotaBanner}` | Modificati — consumo via `useFattureMessages`, formattazione per-lingua |
| `frontend/apps/backoffice/src/modules/fatture/strings.ts` | Eliminato (rinominato in `i18n/it.ts`) |
| `frontend/apps/backoffice/src/{test/utils.tsx, modules/fatture/fatture.test.tsx, shell/Sidebar.test.tsx, e2e/fatture.spec.ts}` | Modificati — opzione lingua, test localizzazione, e2e allineati a EN |
| `tools/new-application/templates/frontend-module/**` | Modificati/Creati — template convertito all'i18n (parità) |
| `site/public/landings/fatture/hero.{en,it,fr,es,de}.png` | Modificati — screenshot per-lingua rigenerati |
| `docs/usecases/06-frontend/0060-*.md`, `docs/usecases/_INDEX.md` | Modificati — punti aperti + stato ✅ |

## Cosa è stato fatto

Il pacchetto `@appgrove/i18n` passa da 2 a 5 lingue (`en/it/fr/es/de`) con parità delle chiavi verificata (tipo + test) e un rilevamento della lingua all'avvio (`localStorage → navigator.language → en`, con persistenza della scelta manuale). Il modulo `fatture` è stato convertito dallo `strings.ts` cablato a **bundle di traduzione per-modulo** nelle 5 lingue, letti da un hook tipizzato reattivo (`useFattureMessages`) e registrati nell'istanza i18n dalla shell per le etichette di navigazione; importi e date sono ora formattati secondo la lingua attiva. La shell (backoffice + admin) ha un selettore lingua a tendina e risolve le etichette dei moduli (sidebar + breadcrumb) con `t()`. Il modello di scaffolding è stato convertito in parallelo (parità verde). Infine i 5 screenshot della landing `fatture` sono stati rigenerati con il tool `finalize-landing` e verificati visivamente: ora ciascuno mostra la UI nella lingua corrispondente (testi, navigazione e formato numeri/date), chiudendo il divario che ha originato lo use case.

## Decisioni prese

Sintesi (registro completo e strutturato in [decisions.json](decisions.json), stessa storia):
- **Ambito**: convertito il solo `fatture`; `crm`/`demo` restano in italiano e sono tracciati come rimando in UC 0060 (migrazione incrementale prevista dallo use case).
- **Architettura stringhe modulo**: bundle per-modulo (disaccoppiati dalla shell) + hook tipizzato reattivo; i bundle sono registrati in i18next solo per le etichette di navigazione dinamiche (risolte con una vista `t as (key:string)=>string` perché il tipo di `t` è ristretto alle chiavi statiche). La `Breadcrumb` è stata corretta insieme alla `Sidebar` (senza mostrava la chiave grezza — emerso alla verifica visiva del primo screenshot).
- **Lingua di default**: derivata dal browser; nei test unitari si passa la lingua esplicita, negli e2e il default `en-US` porta l'app in inglese → asserzioni del modulo in `fatture.spec.ts` allineate all'inglese.
- **Selettore**: menu a tendina (pattern `<details>`), nessuna nuova dipendenza (nel design-system non c'è una primitiva Select).
- **Screenshot**: ri-cattura inclusa in questa change (su tua richiesta), non rimandata.

## Invarianti appgrove

Nessuno toccato: la change è solo frontend (risorse di traduzione e configurazione lingua). Nessuna query tenant-scoped, nessun `tenant_id`, nessuna infrastruttura, nessun logging backend introdotti. Mantenute invece due regole di prodotto: **parità dei modelli di scaffolding** (template convertito in parallelo, `parity-check` verde) e **"una nuova app gira subito localizzata"** (i bundle i18n sono file copiati con sostituzione dei segnaposto, senza logica di generatore aggiuntiva).

## Note per il revisore

- **Contratto interno `ModuleManifest`**: `name`/`label` sono ora chiavi i18n e il manifest espone `resources`. Impatta solo i moduli del monorepo — `fatture` aggiornato, `crm`/`demo` retro-compatibili (i18next restituisce la chiave se non trovata).
- **Decisioni differite tracciate** (in [UC 0060](../../docs/usecases/06-frontend/0060-localizzazione-ui-app-multilingua.md), "Punti aperti"): conversione i18n di `crm`/`demo`; quota label del template a 5 lingue (owner UC 0046).
- **Gate parità scaffolding**: `source-paths-scan` ha segnalato modulo `fatture` + `registry.ts`. Scelta **via 1** (template aggiornati in parallelo, `parity-check` verde); la modifica a `registry.ts` è infrastruttura condivisa (registrazione automatica dei moduli), non drift per-app → nessuna deviazione in `docs/_PARITA-SCAFFOLD.md`.
- **Gate privacy/RoPA (UC 0031)**: eseguito sul diff finale, **nessun segnale**.
- **Errori di tipo preesistenti** (NON introdotti da questa change, in file non toccati): `packages/design-system/src/components/PageHeader.tsx` (title ReactNode) e `apps/backoffice/e2e/privacy.spec.ts` (`.at`). Non falliscono il gate (`npm test` verde; `npm run build --workspaces` verde, exit 0).
- **Nessuno snapshot visivo end-to-end** esiste: la regola sul baseline visivo non si applica.
- **Landing `fatture`**: pubblicata; questa change ha localizzato la UI e già rigenerato gli screenshot per-lingua (nessuna ri-esecuzione ulteriore di `finalize-landing` necessaria).

## Test

Tutte le aree toccate verdi (comandi canonici / `run-tests.sh`):
- **frontend** — `npm test` (tutti i workspace: pacchetto i18n 9 test incl. parità 5 lingue + rilevamento; backoffice 102 test incl. localizzazione modulo `fatture` in `fr` e etichette sidebar per-lingua) + **e2e Playwright** (backoffice 20, admin 2) verdi in browser reale. `npm run build --workspaces` verde.
- **tooling** — `./run-tests.sh tooling` verde (24 test: `scaffold-parity` parità verde + `finalize-landing`).
- **site** — `./run-tests.sh site` verde (47 pagine, 5 lingue; screenshot rigenerati inclusi nel `public/`).
- **compliance** — `./run-tests.sh compliance` verde.
- Backend/infra non toccati; **smoke** non impattato (nessuna modifica a `services/` o agli artefatti).

Test aggiunti/aggiornati: parità i18n a 5 lingue + rilevamento/persistenza lingua (`i18n.test.ts`); render localizzato del modulo (`fatture.test.tsx`, test `fr` + formato importo); etichette di navigazione per-lingua (`Sidebar.test.tsx`); opzione `language` nelle utility di test; e2e `fatture.spec.ts` allineato a EN.

## Stato criteri di accettazione

- [x] `@appgrove/i18n` espone le 5 lingue con cataloghi `fr/es/de` completi; parità verificata da test **e** dal tipo.
- [x] Shell + modulo `fatture` resi in `fr/es/de` (oltre `it/en`) senza chiavi grezze — verificato via test e visivamente sugli screenshot.
- [x] Lingua all'avvio da `localStorage → navigator.language → en`; selettore a tendina a 5 lingue con scelta persistita; cambio lingua aggiorna anche la sidebar.
- [x] `fatture` senza `strings.ts`: bundle `i18n/{en,it,fr,es,de}.ts`; importi/date formattati per lingua.
- [x] Template `frontend-module` convertito; `parity-check` verde (nessuna deviazione necessaria).
- [x] 5 screenshot `hero.{en,it,fr,es,de}.png` rigenerati, ciascuno con UI nella lingua corrispondente.
- [x] Suite delle aree toccate verdi via `run-tests.sh` (frontend, tooling, site, compliance).
