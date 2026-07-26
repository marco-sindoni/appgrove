# Change 0057: Freschezza del `dist` dei pacchetti-libreria frontend

**Branch**: `change/0057-fix-frontend-packages-dist-build`
**Aree**: frontend, tooling (`run-tests.sh`)
**Data**: 2026-07-26
**Autore**: Platform Engineering
**Use case sorgente**: Nessuno (change ad-hoc di bugfix)
**Tocca dati personali?**: No

## Problema / Obiettivo

La suite frontend è rossa (`./run-tests.sh frontend`): gli end-to-end `legal.spec.ts` e gli unit
`LegalGate.test.tsx` falliscono perché la schermata del gate legale (UC 0056) rende le **chiavi di
traduzione grezze** (`legal.gateTitle`, `legal.gateIntro`, …) invece del testo tradotto.

La causa radice **non** è nel gate né nelle traduzioni: le chiavi `legal.*` esistono nei sorgenti di
`@appgrove/i18n` (in tutte e 5 le lingue). Il problema è nell'**orchestrazione di build del frontend**.

I pacchetti-libreria del workspace — `@appgrove/i18n`, `@appgrove/design-system`, `@appgrove/api-client`,
`@appgrove/error-reporter`, `@appgrove/paddle-stub` — sono consumati dagli altri moduli **solo tramite il
loro `dist/` costruito** (in ogni `package.json`: `exports` → `./dist/index.js`). Ma:

- `dist/` è **gitignorato** (non committato in git);
- **nessun** pacchetto ha uno script `prepare`/`postinstall` che lo costruisca all'installazione;
- **nessuno** step di build dei pacchetti esiste in `run-tests.sh` né nel job `frontend` di
  `.github/workflows/verify-pr.yml`.

Di conseguenza nulla garantisce che il `dist` sia fresco prima che i consumatori lo usino (gli unit test
di vitest **e** il `vite build` dell'app per gli e2e). Ha funzionato finora solo per coincidenza: il `dist`
esistente combaciava coi sorgenti. Le change **0055/0060** (localizzazione a 5 lingue) e **0056** (chiavi
`legal.*`) hanno modificato i sorgenti di `@appgrove/i18n` rendendo il `dist` **stantìo** → chiavi grezze a
runtime → rosso. Rimosso il `dist` del tutto, il build fallisce proprio perché non lo risolve: il `dist` è
obbligatorio ma non c'è chi lo tenga aggiornato.

Obiettivo: rendere la freschezza del `dist` dei pacchetti-libreria **automatica e riproducibile**, sia in
integrazione continua sia in locale, così che una modifica ai sorgenti di un pacchetto non lasci più i
consumatori con artefatti obsoleti.

## Scope

Correzione durevole in due parti complementari (aree `frontend/` e tooling di root):

1. **`prepare` per ogni pacchetto-libreria** (`frontend/packages/*`): aggiungere uno script `prepare` che
   costruisce il pacchetto. È il punto idiomatico dove npm ricostruisce un pacchetto prima che venga
   consumato: viene eseguito automaticamente a ogni `npm ci` (integrazione continua) e `npm install`
   (locale, es. dopo un `git pull` che cambia un sorgente-pacchetto — lo scenario esatto che ha prodotto il
   rosso). Copre la CI **senza** modificare `verify-pr.yml`.

2. **Build dei pacchetti in `run-tests.sh` (area frontend)**: prima di eseguire vitest e gli e2e Playwright,
   costruire i pacchetti-libreria del workspace. Ripristina l'invariante di CLAUDE.md secondo cui
   `run-tests.sh` è l'entrypoint canonico **autoconsistente** per "eseguire tutti i test", coprendo anche il
   caso in cui si modifica un sorgente-pacchetto senza reinstallare. La scoperta dei pacchetti è **dinamica**
   (itera `frontend/packages/*/` e costruisce quelli che dichiarano uno script `build`), coerente con
   l'ethos di auto-scoperta del repo: un pacchetto nuovo viene incluso da solo.

Nessuna modifica ai sorgenti applicativi, alle traduzioni, al gate legale o ai test: sono già corretti.

## Fuori scope

- **Il sintomo iniziale su `react-markdown`** (`Failed to resolve import "react-markdown"`): era solo il
  `node_modules` locale disallineato dopo aver tirato giù 0056/0060 da remoto. La dipendenza è già
  dichiarata in `package.json` e nel `package-lock.json`; `npm install` la risolve. **Non è un difetto del
  repo**, nessun intervento.
- **Consumare i pacchetti dai sorgenti** (`src` via condizioni `exports`/alias, opzione "C"): eliminerebbe
  del tutto il `dist` interno ma cambierebbe la semantica del bundle di produzione e toccherebbe più
  superfici — sproporzionato per un bugfix. Non in scope.
- **Committare il `dist`** o cambiare il `.gitignore`: gitignorare gli artefatti di build resta corretto.
- **Freschezza dei pacchetti per il server di sviluppo** (`vite dev` dell'app): coperta all'installazione da
  `prepare`; il caso residuo (modifica di un pacchetto mentre `vite dev` gira) è una sfumatura nota dello
  sviluppo in monorepo, non questo bug.
- **Rifattorizzare il job `frontend` di `verify-pr.yml`** per passare da `run-tests.sh` (come fa già il job
  backend): possibile consolidamento, ma non necessario a chiudere questo bug (la parte `prepare` copre già
  la CI). Non in scope.

## Criteri di accettazione

- [ ] `./run-tests.sh frontend` è **verde** (vitest unit/component + e2e Playwright), partendo anche da uno
      stato in cui il `dist` dei pacchetti è assente o stantìo.
- [ ] Ogni pacchetto in `frontend/packages/*` che ha uno script `build` ha anche uno script `prepare` che lo
      costruisce; dopo un `npm ci` (o `npm install`) da zero, i `dist/` dei pacchetti esistono e contengono i
      sorgenti correnti (verifica: la chiave `legal.gateTitle` è presente nel `dist` costruito di
      `@appgrove/i18n`).
- [ ] `run-tests.sh`, nell'area frontend, costruisce i pacchetti-libreria del workspace **prima** di vitest e
      degli e2e, scoprendoli dinamicamente.
- [ ] Nessuna modifica ai sorgenti applicativi, alle traduzioni, ai componenti o ai test esistenti.

## Invarianti appgrove toccati

- **`run-tests.sh` entrypoint canonico** (CLAUDE.md "Esecuzione dei test"): la change **rafforza**
  l'invariante rendendo l'area frontend autoconsistente (non assume più un `dist` pre-costruito).
- Gli altri invarianti architetturali (tenant_id dal JWT, filtro row-level, modulo `microsaas_app`, logging
  strutturato) **non sono toccati**: la change riguarda solo l'orchestrazione di build/test del frontend.

## Requisiti di test

Non si aggiungono test unitari nuovi: la correzione è verificata dalle suite **esistenti** che oggi sono
rosse e devono tornare verdi. Il criterio operativo è la prova di riproducibilità: rimuovere i `dist` dei
pacchetti, quindi `./run-tests.sh frontend` deve ricostruirli e chiudere in verde senza passi manuali.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | N/A |
| Version bump | patch (correzione di tooling/build, nessun cambiamento funzionale) |
