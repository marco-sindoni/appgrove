# Implementation Log — Change 0057: Freschezza del `dist` dei pacchetti-libreria frontend

**Branch**: `change/0057-fix-frontend-packages-dist-build`
**Aree**: frontend, tooling (`run-tests.sh`)
**Completata**: 2026-07-26
**Modalità**: autopilot — le risposte alle domande di approfondimento sono dell'agente e sono tracciate in [decisions.json](decisions.json)

## File modificati

| File | Azione |
|---|---|
| frontend/packages/api-client/package.json | Modificato (aggiunto script `prepare`) |
| frontend/packages/design-system/package.json | Modificato (aggiunto script `prepare`) |
| frontend/packages/error-reporter/package.json | Modificato (aggiunto script `prepare`) |
| frontend/packages/i18n/package.json | Modificato (aggiunto script `prepare`) |
| frontend/packages/paddle-stub/package.json | Modificato (aggiunto script `prepare`) |
| run-tests.sh | Modificato (helper `build_frontend_packages` + build dei pacchetti in `run_frontend`) |

## Cosa è stato fatto

Correzione durevole in due parti complementari, senza toccare alcun sorgente applicativo, traduzione o test:

1. **`prepare` su ogni pacchetto-libreria** (`"prepare": "npm run build"`): npm ricostruisce il `dist` di
   ciascun pacchetto automaticamente a ogni `npm ci`/`npm install`. È il punto idiomatico dove un pacchetto
   si costruisce prima di essere consumato e copre l'integrazione continua (che fa `npm ci`) **senza**
   modificare `.github/workflows/verify-pr.yml`, oltre allo scenario locale di `git pull` + install che aveva
   prodotto il rosso.
2. **Build dei pacchetti in `run-tests.sh`**: la funzione dell'area frontend costruisce i pacchetti-libreria
   del workspace **prima** di vitest e degli end-to-end Playwright, scoprendoli dinamicamente (itera
   `frontend/packages/*/` e include quelli con uno script `build`). Ripristina l'invariante per cui
   `run-tests.sh` è l'entrypoint canonico **autoconsistente**, coprendo anche il caso in cui si modifica un
   sorgente-pacchetto senza reinstallare.

## Decisioni prese

Change condotta in **autopilot**; il registro strutturato è in [decisions.json](decisions.json) (5 decisioni,
4 marcate `(autopilot)`). In sintesi:

- **Causa radice**: i pacchetti-libreria sono consumati solo dal loro `dist/` costruito (gitignorato) e nulla
  lo ricostruiva prima dei consumatori; le change 0055/0060 (5 lingue) e 0056 (chiavi `legal.*`) hanno reso il
  `dist` stantìo → il gate legale rendeva le chiavi grezze → e2e/unit rossi.
- **Il sintomo iniziale su `react-markdown`** era solo `node_modules` locale disallineato (dipendenza già nel
  lockfile), risolto da `npm install`: **non** un difetto del repo, fuori scope.
- **Meccanismo**: scelta A+B (prepare + build in run-tests.sh); scartata l'opzione di consumare i pacchetti
  dai sorgenti come sproporzionata per un bugfix.
- **Scoperta dinamica** dei pacchetti in `run-tests.sh`, coerente con l'auto-scoperta dei servizi.

## Invarianti appgrove

Nessun invariante architetturale toccato (tenant_id dal JWT, filtro row-level, modulo `microsaas_app`, logging
strutturato non sono in gioco). La change **rafforza** l'invariante di CLAUDE.md "`run-tests.sh` entrypoint
canonico" rendendo l'area frontend autoconsistente.

## Note per il revisore

- La parte `prepare` copre l'integrazione continua tramite `npm ci` senza modificare la pipeline; non è stato
  necessario toccare `verify-pr.yml`. Un possibile consolidamento futuro (far passare anche il job frontend da
  `run-tests.sh`, come già fa il job backend) è stato lasciato **fuori scope** e annotato nei requisiti.
- La doppia costruzione in alcuni flussi (`npm ci` via `prepare`, poi `run-tests.sh`) è ridondanza voluta a
  favore della correttezza; costo trascurabile (pochi secondi).
- **Nessuna decisione differita**: non sono emersi punti aperti appartenenti ad altri use case.
- **Gate privacy (UC 0031)**: nessun segnale (`privacy-scan` exit 0).
- **Gate parità scaffold (UC 0046)**: nessun percorso-sorgente dei modelli toccato (exit 0).
- **Promemoria landing**: non applicabile — nessuna superficie feature/pricing di un'app toccata.
- Nessun aggiornamento di baseline visive: gli e2e sono passati senza modificare snapshot.

## Test

Codice eseguibile toccato → suite dell'area eseguita.

- **frontend** (`./run-tests.sh frontend`): **verde** — vitest unit/component + 22/22 end-to-end Playwright.
  Prova di riproducibilità: rimossi tutti i `dist/` dei pacchetti, `./run-tests.sh frontend` li ha ricostruiti
  e chiuso in verde senza passi manuali.
- **Parte A validata**: rimossi i `dist/`, `npm ci` li ha ricostruiti via `prepare` con contenuto corrente
  (chiave `legal.gateTitle` presente nel `dist` di `@appgrove/i18n`) — esattamente ciò che fa la CI.
- Non sono stati aggiunti test nuovi: la correzione è provata dalle suite esistenti (prima rosse, ora verdi),
  come da requisiti.

## Stato criteri di accettazione

- [x] `./run-tests.sh frontend` verde partendo anche da `dist` assente/stantìo.
- [x] Ogni pacchetto con script `build` ha `prepare`; dopo `npm ci` i `dist/` esistono e sono correnti.
- [x] `run-tests.sh` costruisce i pacchetti prima dei test, con scoperta dinamica.
- [x] Nessuna modifica ai sorgenti applicativi, alle traduzioni, ai componenti o ai test esistenti.
