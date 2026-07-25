# Implementation Log — Change 0049: skill `finalize-landing` (UC 0057)

**Branch**: `change/0049-use-case-0057-skill-finalize-landing`
**Aree**: `.claude/skills/finalize-landing/` (skill nuova), `tools/finalize-landing/` (helper + test), `tools/new-application/` (generazione bozza), `tools/drop-application/` (inverso), `.claude/skills/new-change/` (promemoria stale), `site/` (test), `run-tests.sh`, `docs/`
**Completata**: 2026-07-25
**Modalità**: autopilot — le risposte alle domande di approfondimento sono dell'agente e sono tracciate in [decisions.json](decisions.json) (14 decisioni, 9 marcate `(autopilot)`). Due decisioni di scope sono state corrette dallo sviluppatore (no rimandi orfani a UC già chiusi).

## File modificati

| File | Azione |
|---|---|
| `.claude/skills/finalize-landing/SKILL.md` + `step-01…04` | Creato — skill nuova (2 metà: co-pilota + tool) |
| `tools/finalize-landing/{finalize.mjs, lib/*, test/*, package.json, package-lock.json}` | Creato — helper deterministico (OG, screenshot, preflight, publish) |
| `tools/new-application/templates/landings/@@APP_ID@@/{en,it,fr,es,de,index}.ts` | Creato — modello bozza landing 5 lingue |
| `tools/new-application/lib/context.mjs` | Modificato — `LANDING_SLUG`/`toLandingSlug`/`RESERVED_LANDING_SLUGS` |
| `tools/new-application/lib/edits.mjs` | Modificato — `editLandingsIndex` + registrazione |
| `tools/new-application/generate.mjs` | Modificato — espansione template landings, guardia slug, rollback |
| `tools/drop-application/lib/{plan.mjs, unedits.mjs}` | Modificato — inverso: rimozione cartella + `uneditLandingsIndex` |
| `tools/drop-application/test/unedits.test.mjs` | Modificato — test dell'un-editor landing |
| `site/src/content/landings/landings.test.ts` | Modificato — gate reso robusto a più landing |
| `.claude/skills/new-change/step-04-close.md` | Modificato — promemoria "landing stale" |
| `run-tests.sh` | Modificato — test `finalize-landing` in area tooling |
| `docs/usecases/_INDEX.md` | Modificato — UC 0057 → ✅ |
| `docs/usecases/10-skills-tooling/0044-…md`, `0046-…md` | Modificato — punti aperti chiusi da questa change |

## Cosa è stato fatto

Creata la skill `finalize-landing` (UC 0057) che porta la bozza di landing di un'app allo stato pubblicato,
con architettura a due metà come `new-application`: la skill co-pilota copy/visual/review 5 lingue; l'helper
deterministico `tools/finalize-landing/` fa i passi meccanici — immagine Open Graph on-brand (SVG→PNG via
`sharp`), cattura screenshot per-lingua (Playwright, seed mock-route), preflight di pubblicazione, transizione
`draft→published`. Poiché il generatore `new-application` non scriveva davvero la bozza (divergenza prosa/codice)
e UC 0046 è già chiuso, la generazione della bozza è stata implementata qui (nuovo template + editor del registro),
con l'inverso in `drop-application`. Aggiunto in `new-change` il promemoria "landing stale" (#14 dec.55).

## Decisioni prese

Sintesi (registro completo e strutturato in [decisions.json](decisions.json), 14 voci):

- **due metà** skill + tool deterministico testato (dec. 3);
- **generazione bozza fatta qui** e non differita a UC 0046 (già ✅) — sarebbe stato un rimando orfano (dec. 4, 10, 11);
- **promemoria stale fatto qui** e non differito a UC 0044 (già ✅) (dec. 9, 10);
- **ripple su drop-application** (inverso obbligatorio) per non rompere il round-trip (dec. 12);
- **immagine OG** 1200×630 da template SVG on-brand con colore-categoria del design system (dec. 7);
- **screenshot** con seed mock-route (portabile, no stack backend) come default; stack reale = raffinamento tracciato (dec. 8);
- **preflight** che rifiuta la pubblicazione con sentinella «DA RIFINIRE», screenshot/OG mancanti o lingua assente (dec. 6);
- **chiusura leggera** branch + PR, validazione via area `site` (dec. 5);
- **fix test fragile** del gate landing (assumeva una sola landing) (dec. 13);
- **classificazione privacy**: `playwright`/`sharp` sono strumenti di build locali, nessun dato personale, nessun responsabile esterno, MINOR (dec. 14).

## Invarianti appgrove

Nessuno dei quattro invarianti runtime è toccato: è tooling di contenuti del sito statico (nessun `tenant_id`/JWT,
nessuna query tenant-scoped, nessun modulo Terraform, nessun log applicativo). Vincoli d'area mantenuti: gate di
pubblicazione (`published` + preflight, #14 52; la skill non pubblica né fa deploy), parità 5 lingue (vetrina verde),
parità scaffolding (nessun percorso-sorgente toccato; gate verde).

## Note per il revisore

- **Contratto cross-area** verso il modello dati landing di UC 0038 (`site/src/content/landings/*`): lettura/append,
  non modificato.
- **Nuove dipendenze** locali dell'helper: `sharp` (già transitivo del sito via Astro) e `playwright` (browser dalla
  cache globale condivisa col frontend — nessun secondo download). Classificate: non responsabili esterni, nessun
  dato personale (dec. 14).
- **Decisioni differite chiuse in questa change** (non più rimandi orfani a UC già ✅): bozza landing in UC 0046,
  segnale stale in UC 0044 — entrambi i file use case aggiornati con ✅ e il perché.
- **Nessuna decisione differita nuova** lasciata aperta salvo i raffinamenti già annotati in UC 0057 (seed completo
  dello stack reale per gli screenshot).

## Test

- **`tools/finalize-landing` (area tooling)** — 21 test, 20 verdi + 1 saltato: OG (SVG/PNG 1200×630 valido), preflight
  (verde + 5 casi rossi), registry (flip stato idempotente, cablaggio asset). Lo screenshot **salta con grazia** se il
  browser non è lanciabile nell'ambiente (prova ambiente-dipendente, come previsto da UC 0057). Smoke CLI end-to-end
  eseguito a mano su una landing di prova: og → wire-assets → preflight (rosso corretto) → publish (verde).
- **`tools/drop-application` (area tooling)** — 11 test verdi, incl. round-trip "genera→de-genera: repo identico" (copre
  la landing) e il test esplicito dell'un-editor.
- **`tools/scaffold-parity` (area tooling)** — 20 test + parità verdi; gate percorsi-sorgente verde.
- **`site`** — vitest 37 verdi, `astro build` (42 pagine) verde, controllo post-build verde (5 lingue + OG). Generazione
  reale di un'app di prova (`note`) validata a mano: bozza landing valida (5 lingue, slug valido), poi ripristinata.
- **Non eseguito in questo ambiente**: `generate-smoke.sh` (livello 3, richiede Docker/Maven) — la metà di *generazione*
  (che è ciò che questa change tocca) è stata validata a mano; la metà di *compilazione* Java non è interessata dal cambio.
- **Privacy/RoPA (UC 0031)**: scanner eseguito → 2 segnali (dipendenze), classificati come non-personali/non-sub-processor (dec. 14).

## Stato criteri di accettazione

- [x] Skill `.claude/skills/finalize-landing/` con `triggers: /finalize-landing`, formato/tono di new-application, chiusura branch+PR
- [x] Helper `tools/finalize-landing/` (screenshot Playwright, OG 1200×630, preflight, flip published solo a preflight verde)
- [x] Preflight rifiuta lingua mancante / slug / screenshot null / ogImage null / placeholder residuo — coperto da test
- [x] Test dell'helper verdi e registrati in `run-tests.sh` (area tooling)
- [x] Fixture `example` resta `draft`; `./run-tests.sh site` verde
- [x] Skill idempotente (ri-eseguibile su landing pubblicata) — descritto nella skill; `publish` idempotente testato
- [x] Precondizione gestita (no bozza → stop → /new-application), a livello skill e tool
- [x] `new-application` genera la bozza (5 lingue + voce draft nel registro); parità/collaudo tooling verdi
- [x] Promemoria "landing stale" presente nella chiusura di `new-change`
- [x] UC 0046 (bozza) e UC 0044 (segnale stale) aggiornati come **chiusi da questa change**, non rimandi orfani
