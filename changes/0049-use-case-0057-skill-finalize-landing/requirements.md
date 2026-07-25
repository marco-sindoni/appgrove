# Change 0049: skill `finalize-landing` — bozza landing → landing pubblicata

**Branch**: `change/0049-use-case-0057-skill-finalize-landing`
**Aree**: `.claude/skills/finalize-landing/` (skill nuova), `tools/finalize-landing/` (helper deterministico + test), `tools/new-application/` (generazione della bozza landing, prima mancante), `.claude/skills/new-change/` (promemoria "landing stale" in chiusura), `site/` (convenzione asset + eventuale preflight), `run-tests.sh` (registrazione test area tooling), `docs/` (indice esecuzione + use case 0044/0046)
**Data**: 2026-07-25
**Autore**: Platform Engineering (modalità autopilot)
**Use case sorgente**: [docs/usecases/10-skills-tooling/0057-skill-finalize-landing.md](../../docs/usecases/10-skills-tooling/0057-skill-finalize-landing.md)
**Tocca dati personali?**: No — è tooling di contenuti (genera md/asset della landing, nessun dato runtime né personale; manifesto GDPR non applicabile, UC 0057 §7).

## Problema / Obiettivo

Oggi il sito vetrina (UC 0036/0038) sa **rendere** le landing per-app e ha il gate di pubblicazione
(`status: published` → il build le renderizza; le bozze restano invisibili), ma **manca lo strumento che porta
una landing dalla bozza alla pubblicazione**. La bozza è generata (o va generata) da `new-application` con
placeholder: screenshot assenti (`screenshot.src = null`), immagine per l'anteprima social assente
(`meta.ogImage = null`), copy iniziale da rifinire. Nessuno strumento cattura gli screenshot reali, genera
l'immagine social, rifinisce e rivede il copy nelle 5 lingue, e imposta `published`.

Obiettivo: creare la skill **`finalize-landing`** che, invocata su un'app arrivata a MVP, porta la sua landing
allo stato **pubblicato** — è il **secondo momento** della landing (#14 dec.51). Al termine deve esistere una PR
con: copy rifinito nelle 5 lingue, screenshot reali (uno per lingua), immagine social, meta/SEO completi e
`status: published`; al merge la CI valida e pubblica (UC 0036). Risultato osservabile: `/finalize-landing <app_id>`
trasforma una landing `draft` con placeholder in una landing `published` che supera il controllo post-build della
vetrina (parità 5 lingue + Open Graph), **senza** deploy manuale e **senza** che la skill pubblichi da sé.

## Scope

**Architettura a due metà** (come `new-application`, decisione autopilot 3):

- **La skill** `.claude/skills/finalize-landing/` — orchestrazione in prosa, rispecchia formato e tono di
  `new-application` (frontmatter con `triggers: /finalize-landing`; sezioni intro/istruzioni/gate/modalità/
  questioning-style/cosa-NON-fa). Co-pilota i passi non meccanizzabili: rifinitura copy definitivo nelle 5 lingue
  (EN sorgente marketing → IT/FR/ES/DE), scelta dei visual on-brand, **review interattiva** delle 5 lingue con
  approvazione dell'utente (#14 dec.35). Chiude con **flusso leggero branch + PR**, non col gate test/snapshot pieno
  di `new-change` (decisione 5).
- **L'helper deterministico** `tools/finalize-landing/` — esegue e rende testabili i passi meccanici:
  1. **Cattura screenshot** reali via Playwright contro il modulo frontend dell'app, **uno per lingua**, con seed
     via intercettazione delle rotte (stesso schema delle prove e2e generate; decisione 8); salva i file secondo una
     **convenzione asset** sotto `site/public/` (es. `site/public/landings/<app_id>/…`) e valorizza `screenshot.src`.
  2. **Genera l'immagine social (Open Graph)** — PNG 1200×630 on-brand da template che usa i token del design
     system (UC 0019) e lo stile illustrazioni del sito; salva sotto la stessa convenzione e valorizza `meta.ogImage`
     (decisione 7).
  3. **Completa SEO/GEO** — meta/OG/hreflang già cablati dal template (UC 0038/0040); l'helper riempie i campi
     mancanti richiesti dai gate (title/description/OG) e il materiale GEO (UC 0041) dove previsto dal modello dati.
  4. **Preflight + transizione di stato** — valida che i placeholder siano risolti (screenshot e immagine social
     presenti, nessun testo-placeholder residuo, parità 5 lingue, slug validi via `validateLandings()`); solo se il
     preflight è verde imposta `status: 'published'` a livello app in `site/src/content/landings/index.ts`.

**Generazione della bozza in `new-application`** (decisione 4, corretta con lo sviluppatore): il generatore
`tools/new-application/` oggi **non** scrive la voce landing di bozza (la prosa della skill lo dà per fatto, il codice
no). Poiché UC 0046 è già ✅ in main, differire lì sarebbe un **rimando orfano** e senza bozza `finalize-landing` è
inservibile: quindi la generazione della bozza si fa **in questa change**. Si aggiunge un template `landings/`
(5 file lingua con placeholder: `screenshot.src: null`, `ogImage: null`, copy on-brand di partenza) e un passo del
generatore che scrive quei file e registra la voce `status: 'draft'` in `site/src/content/landings/index.ts`, con slug
localizzato per lingua (evitando gli `RESERVED_SLUGS`). `finalize-landing` consuma quella bozza.

**Precondizione della skill**: `finalize-landing` consuma una **bozza esistente** per `app_id`; se manca, si ferma con
un messaggio che rimanda a `/new-application`. Non crea la bozza da sé (ora la crea il generatore).

**Promemoria "landing stale"** (decisione 9, corretta con lo sviluppatore): il segnale che una change ha reso stale
una landing era differito a UC 0044, anch'esso già ✅ → altro rimando orfano. Si aggiunge quindi **in questa change**
un promemoria minimale alla chiusura di `new-change` (`step-04-close.md`): se la change ha toccato feature/pricing di
un'app che ha una landing `published`, ricorda di ri-eseguire `/finalize-landing <app_id>` (#14 dec.55). Nessuna nuova
infrastruttura. La skill `finalize-landing` è comunque **ri-eseguibile** su una landing pubblicata (gestione lato consumo).

**Registrazione test**: i test dell'helper entrano in `run-tests.sh` (area **tooling**, dove sono elencati gli altri
strumenti); l'area **site** continua a validare le landing pubblicate (vitest + `astro build` + `postbuild-check`).

**Indice esecuzione**: `docs/usecases/_INDEX.md` — UC 0057 → 🟡 all'apertura (fatto), → ✅ alla chiusura.

## Fuori scope

- **La pubblicazione di una landing reale** (es. app #1) → è **UC 0053**, downstream. Questa change costruisce lo
  **strumento**; nessuna app reale viene pubblicata qui. La fixture `example` resta `draft` (decisione 6).
- **Il deploy / la pubblicazione effettiva** → la fa la CI al merge (UC 0036/0005, #14 dec.53). La skill scrive
  contenuti e apre la PR, non pubblica.
- **Il template/struttura della landing** (UC 0038), **la homepage e le pagine non-app** (UC 0037), **i testi legali**
  (UC 0002) — la skill copre solo le landing per-app.
- **Seed completo dello stack reale** (Postgres + `seed.sql`) per gli screenshot → raffinamento tracciato; il default
  è il modello mock-route.

## Criteri di accettazione

- [ ] Esiste la skill `.claude/skills/finalize-landing/` (SKILL.md + step) con `triggers: /finalize-landing`, che
      rispecchia formato/tono di `new-application` e chiude con flusso **branch + PR** (no gate test/snapshot pieno).
- [ ] Esiste l'helper deterministico `tools/finalize-landing/` che: cattura screenshot per-lingua via Playwright
      (seed mock-route), genera un PNG Open Graph 1200×630 on-brand, esegue il preflight e imposta `status: published`
      solo a preflight verde.
- [ ] Il **preflight** rifiuta la pubblicazione se: manca una lingua, uno slug è invalido/riservato/duplicato,
      `screenshot.src` o `meta.ogImage` è `null`, o resta un testo-placeholder — con messaggio chiaro. Coperto da test.
- [ ] I test dell'helper sono verdi e registrati in `run-tests.sh` (area tooling): generazione PNG valida, preflight
      (verde e rosso) su fixture, transizione di stato su fixture temporanea. `./run-tests.sh tooling` verde.
- [ ] La fixture `example` resta `status: 'draft'`; `./run-tests.sh site` resta verde (nessuna bozza pubblicata,
      nessuna regressione del controllo post-build).
- [ ] La skill è **idempotente**: ri-eseguirla su una landing già `published` la ri-finalizza (ri-cattura + ri-copy)
      mantenendo `published`, senza errori.
- [ ] La precondizione è gestita: su `app_id` senza bozza, la skill si ferma con un messaggio che rimanda a
      `/new-application` (la bozza la crea il generatore, non `finalize-landing`).
- [ ] **`new-application` genera la bozza**: il generatore scrive i 5 file lingua con placeholder e registra la voce
      `status: 'draft'` in `site/src/content/landings/index.ts`; il collaudo di parità/smoke del tooling resta verde.
- [ ] **Promemoria "landing stale"** presente nella chiusura di `new-change` (step-04): documentato l'euristica che, su
      change che toccano feature/pricing di un'app con landing pubblicata, suggerisce `/finalize-landing`.
- [ ] I due use case toccati (UC 0046 bozza, UC 0044 segnale stale) sono aggiornati come **chiusi da questa change**,
      non come rimandi orfani a use case già completati.

## Invarianti appgrove toccati

Nessuno dei quattro invarianti runtime si applica direttamente (tooling di contenuti del sito statico: nessun
`tenant_id`/JWT, nessuna query tenant-scoped, nessun modulo Terraform, nessun log applicativo). Vincoli propri di
quest'area comunque da mantenere:

- **Gate di pubblicazione** (#14 dec.52): solo `status: published` diventa pagina; il preflight non deve poter
  pubblicare una landing incompleta. La skill **non** pubblica né fa deploy (#14 dec.53).
- **Parità 5 lingue bloccante** (UC 0038/0030): la finalizzazione deve lasciare la vetrina verde al controllo
  post-build (parità 5 lingue + Open Graph presenti).
- **Parità dei modelli di scaffolding**: la change **estende** il generatore `new-application` con un nuovo template
  `landings/`. È contenuto generato **nuovo**, non uno specchio di un file di `fatture` (app #1 non ha landing → UC 0053),
  quindi non introduce divergenza rispetto a `fatture`. Vincolo: il collaudo di parità e il collaudo "livello 3" del
  tooling (`tools/scaffold-parity` + `generate-smoke.sh`) devono restare **verdi** — l'app generata deve nascere con una
  bozza landing valida (5 lingue, slug validi, `status: 'draft'`). Se il collaudo tocca un percorso-sorgente, si applica
  il gate di parità di `new-change` (step-04): aggiornare i template o registrare la deviazione in `docs/_PARITA-SCAFFOLD.md`.

## Requisiti di test

- **Helper (area tooling)**: unit test deterministici — (a) il generatore Open Graph produce un PNG 1200×630 valido
  dato un contenuto landing; (b) il preflight è **verde** su una fixture completa e **rosso** su ognuna delle
  violazioni (lingua mancante, slug invalido/riservato/duplicato, `screenshot.src` null, `ogImage` null, placeholder
  residuo); (c) la transizione di stato porta `draft → published` su una fixture temporanea senza toccare le altre voci.
- **Cattura screenshot**: una prova che, contro una pagina servita col seed mock-route, produce un PNG per lingua nel
  percorso atteso (può essere segnata come prova più pesante/ambiente-dipendente se serve, ma il meccanismo deve
  essere dimostrato, non solo descritto).
- **Generazione bozza (area tooling)**: il collaudo di parità/livello-3 (`generate-smoke.sh`) deve mostrare che l'app
  generata nasce con una bozza landing valida (5 lingue, slug validi, `status: 'draft'`); `./run-tests.sh tooling` verde.
- **Regressione vetrina (area site)**: `./run-tests.sh site` resta verde; `example` resta `draft`.
- **Nessun test applicativo** oltre a questi: una landing non ha logica applicativa (UC 0057 §9).

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | N/A a runtime. Consuma il contratto dati landing di UC 0038 (`site/src/content/landings/types.ts`, `lib/landings.ts`) — lettura/append, non lo modifica. Estende il generatore `new-application` (nuovo template + passo) e la chiusura di `new-change` (promemoria). |
| Version bump | minor (nuova skill + nuovo strumento + generazione bozza in `new-application`; nessun cambiamento incompatibile) |
