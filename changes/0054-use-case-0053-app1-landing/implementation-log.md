# Log di implementazione — App #1 landing: bozza → published (UC 0053)

**Change**: `0054-use-case-0053-app1-landing` · **Modalità**: autopilot
**Use case**: [docs/usecases/11-apps/0053-app1-landing.md](../../docs/usecases/11-apps/0053-app1-landing.md) → ✅
**Aree toccate**: `site/` (contenuto landing, homepage, test) · `tools/finalize-landing/` (fix seed screenshot + test)

## Cosa è stato fatto

Portata la landing pubblica dell'app #1 **`Fatture`** da inesistente a **`published`**, completando l'intera Definition of
Done di UC 0053. Lo scope è stato **allargato dallo sviluppatore** (da sola bozza a published) su domanda dell'autopilot,
riconoscendo come difetto da chiudere qui un buco in codice già consegnato (seed screenshot di UC 0057).

### 1. Contenuto landing (`site/src/content/landings/fatture/`)
- 6 file (`en/it/fr/es/de/index`), 8 sezioni on-brand nelle 5 lingue, copy **fondata sulle funzionalità reali** (CRUD
  fattura con cliente e righe, numero progressivo, stati Bozza/Emessa/Pagata/Annullata, quota mensile, dati in UE con
  diritti GDPR self-service).
- **Pricing veritiero**: un solo piano gratuito (10 fatture/mese), senza trial/tier a pagamento inesistenti (il listino
  reale `pricing/fatture.yaml` ha solo `free`). FAQ allineate al tetto mensile reale.
- Registrata in [index.ts](../../site/src/content/landings/index.ts) (`LANDINGS`).
- Gate umano: la copy 5 lingue è stata **approvata esplicitamente dallo sviluppatore** prima della pubblicazione (#14 dec.35).

### 2. Fix del seed screenshot (`tools/finalize-landing/`)
- Il comando `screenshots` cablava il mock-route sulla sola forma generica dello scaffold (risorsa `items`): per `Fatture`
  (risorsa `invoices`) la lista risultava **vuota**. Reso **guidato dai dati per-app**: nuovo [lib/seeds.mjs](../../tools/finalize-landing/lib/seeds.mjs)
  (`DEFAULT_SEED` generico + `resolveSeed`/`normalizeSeed`); [lib/screenshot.mjs](../../tools/finalize-landing/lib/screenshot.mjs)
  deriva le rotte dal seed risolto; nuovo [seeds/fatture.mjs](../../tools/finalize-landing/seeds/fatture.mjs) allineato alla
  prova e2e reale. Nessuna divergenza di parità scaffold (il fallback generico copre le app scaffoldate).
- Nuovo test [test/seeds.test.mjs](../../tools/finalize-landing/test/seeds.test.mjs): risoluzione del seed (fatture→invoices,
  ignoto→default, normalize).

### 3. Finalizzazione (tool deterministico)
- **OG image**: `finalize.mjs og` → [site/public/landings/fatture/og.png](../../site/public/landings/fatture/og.png) (1200×630, cat-blue, verificata a vista).
- **Screenshot reali** (5 lingue): buildato il frontend dalla radice, servito il backoffice in anteprima (`:4173`),
  `finalize.mjs screenshots --metric fatture --free-cap 10` → `hero.<lang>.png`. Verificati a vista: lista popolata (4 fatture,
  banner quota 4/10). La UI del modulo è italiana → le 5 catture mostrano la stessa interfaccia italiana (stato reale dell'app).
- **wire-assets** → **preflight** verde → **publish** → `status: published`.

### 4. Homepage e test
- Card faro `#apps` collegata alla landing pubblicata (`/<lang>/<slug>/`), [index.astro](../../site/src/pages/[lang]/index.astro).
- Adeguati i test che assumevano "nessuna landing pubblicata": [landings.test.ts](../../site/src/content/landings/landings.test.ts)
  (gate verificato sui dati veri) e [llms.test.ts](../../site/src/lib/llms.test.ts) (caso "nessuna app" su registro esplicitamente vuoto + nuova copertura che il registro reale espone `fatture`).

## Test

- `./run-tests.sh site tooling` → **verde** (exit 0). Site: 84 test vitest + `astro build` (47 pagine, +5 = fatture 5 lingue) +
  controllo post-build verde (5 lingue, Open Graph, `SoftwareApplication`+`FAQPage`, hreflang). Tooling: 24 test finalize-landing
  (incluso `resolveSeed`) + gli altri strumenti, tutti verdi.
- Nota ambiente: build frontend dalla radice (ricompila `@appgrove/design-system` prima del backoffice); installato chromium
  per playwright 1.62 del tool (`npx playwright install chromium`). Dettagli in `decisions.json` (dec. 17).

## Rimandi tracciati
- **Seed contro stack backend reale + `seed.sql`** → UC 0057 §Punti aperti (non necessario ora).
- **Riconciliazione messaggio homepage ↔ landing** ("coming soon"/nome "Invoicing" vs landing pubblicata) e **collegamento
  card app future** → UC 0053 §Punti aperti (decisione di go-live).
- **Localizzazione UI dei moduli app** (per screenshot per-lingua reali) → già in `docs/_BACKLOG.md`.

## Rifiniture post-revisione dello sviluppatore
- **Slug localizzati** (dec. #14 31): `en/invoices`, `it/fatture`, `fr/factures`, `es/facturas`, `de/rechnungen` (prima
  `fatture` in tutte le lingue). La card #apps della homepage segue lo slug per-lingua senza modifiche. Site verde.
- **Screenshot per-lingua** rimandati: l'app non è localizzata (i18n shell solo it/en; modulo `fatture` in italiano), quindi
  gli screenshot escono in italiano in tutte le lingue. Scorporato nel **nuovo use case 0060** (06-frontend), inserito in
  `_INDEX.md` alla **posizione 52** (subito dopo UC 0053) su richiesta dello sviluppatore; registrato in README + CLAUDE.md.

## Definition of Done — stato
Tutti i punti soddisfatti; UC 0053 → ✅ in `_INDEX.md`. Nessun deploy (la CI pubblica al merge, UC 0036).
