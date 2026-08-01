# UC 0090 — Fondamenta della suite end-to-end di piattaforma (stack reale + primo journey)

**Area**: 20-test-e2e-piattaforma · **Fase**: evo · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0058 (flussi auth locali completi), UC 0018 (email transazionali/template), UC 0020 (backoffice SPA shell), UC 0023 (stub Paddle locale), UC 0029 (test pagamenti L1/L2/L3), change 0037 (smoke stack-headless)
**Fonte decisioni**: #10 (testing), #11 (developer experience)
**Ultimo aggiornamento**: 2026-08-01

## 1. Obiettivo / Scope

Costruire il **quarto livello di test** del monorepo — la **suite end-to-end di piattaforma** — che oggi manca:
browser reale (Playwright) che naviga i **frontend costruiti davvero**, contro lo **stack backend vero** (Postgres,
code, tutti i servizi in profilo `dev`), con le **email transazionali realmente spedite e verificate** via Mailpit e i
pagamenti sul **fake Paddle locale** (UC 0023). Colma il buco tra i livelli esistenti:

| Livello | Backend | Email | Pagamenti | Oggi |
|---|---|---|---|---|
| L2 Playwright (`e2e/`) | simulato (intercettazione rotte) | no | fake Paddle.js | ✅ nel gate |
| Smoke headless (`tools/smoke`) | **vero** | — | — | ✅ ma senza browser |
| **Piattaforma (questo UC)** | **vero** | **vera, catturata (Mailpit)** | fake Paddle locale | ❌ da costruire |
| L3 sandbox (`e2e-l3/`) | vero (cloud test) | vera | **Paddle vero** | ✅ pre-release |

**Incluso**: l'infrastruttura della suite (avvio orchestrato stack+frontend, helper condivisi, comando unico come
nuova area `platform` di `run-tests.sh`) e **un primo journey completo** — registrazione: signup → **email di
verifica ricevuta davvero e cliccata** → onboarding workspace → dashboard — come prova dell'intera catena.

**Escluso**: la batteria completa dei journey utente (UC 0091) e amministratore (UC 0092); il registro di copertura
(UC 0093); l'integrazione nel workflow delle skill (UC 0094); ogni fornitore esterno vero (resta al livello L3
pre-release: Paddle sandbox, email dell'ambiente cloud di test — UC 0081).

**Principio di disegno (vincolante per gli UC figli)**: la certezza "nessuna regressione end-to-end" la dà
**l'esecuzione completa del comando unico**, non un singolo test monolitico. La suite è composta da **journey
(percorsi) indipendenti e parallelizzabili**, ognuno con un tenant creato da zero: un rosso indica subito *quale*
percorso è rotto.

## 2. Attori & ruoli

- **Sviluppatore / CI**: lancia `./run-tests.sh platform` (o l'esecuzione completa) e legge il verdetto.
- **Utenti sintetici**: creati dai journey stessi (owner del tenant, membri invitati) — mai utenze reali.
- **Sistemi orchestrati**: servizi Quarkus (`services/*`, scoperti da `dev/lib/services.sh`), Postgres ed ElasticMQ dal
  compose dev, **Mailpit** (cattura SMTP + API HTTP di lettura), fake Paddle (UC 0023), browser Chromium di Playwright.

## 3. Precondizioni

- Docker disponibile (come per i test backend e lo smoke).
- Artefatti backend impacchettabili e frontend costruibili (`npm run build`); nessun ambiente cloud richiesto.
- Nessuna dipendenza da fornitori esterni: la suite deve girare **offline** (determinismo).

## 4. Flusso principale

Orchestrazione del comando `./run-tests.sh platform` (nuova area, stessa filosofia di `smoke`):

1. **Avvio infrastruttura vera**: Postgres + ElasticMQ + **Mailpit** dal compose dev (idempotente, convive con lo
   stack di sviluppo acceso — stessa tecnica delle porte alternative di `tools/smoke/stack-headless.sh`).
2. **Migrazioni + seed**: gli stessi passi di `app-start` (`dev migrate` + seed idempotente).
3. **Avvio servizi**: tutti i `services/*` **scoperti automaticamente** (una nuova app entra nella suite senza toccare
   l'orchestratore — invariante "Avvio locale" di CLAUDE.md), in profilo `dev`, su porte dedicate alla suite.
4. **Build + pubblicazione frontend**: le SPA (`backoffice`, `admin`) costruite con configurazione che punta ai
   servizi della suite; servite da un server statico locale con **inoltro delle rotte `/api/<app_id>/v1/*`** derivato
   dalla stessa scoperta servizi (equivalente headless del blocco `api-routes` del `dev/Caddyfile`).
5. **Esecuzione journey**: Playwright esegue i journey (qui: J-REG "registrazione"), **in parallelo**, ognuno su un
   tenant nuovo creato dal journey stesso.
6. **Verdetto e teardown**: exit-code ≠ 0 se un journey è rosso; artefatti di diagnosi (trace, screenshot, video
   Playwright) salvati; processi della suite fermati (i container compose restano su, come per lo smoke).

**Primo journey J-REG (incluso in questo UC)**:
1. Apre il backoffice → wizard di registrazione → compila account con email sintetica unica (es.
   `e2e+<runId>@test.appgrove.local`).
2. Interroga l'**API di Mailpit** finché arriva l'email di verifica per quel destinatario; assert su mittente,
   oggetto nella lingua attesa, presenza del link.
3. **Clicca il link estratto dall'email** nel browser → pagina di verifica → conferma.
4. Accede, completa l'onboarding del workspace → dashboard visibile → la sidebar mostra lo stato "senza app attive"
   con invito all'acquisto (nessun entitlement ancora).
5. Verifica a livello DB (leak detector, #10 dec. 13): l'account/utente creato esiste col `tenant_id` atteso e
   nessun dato è finito fuori dal tenant.

## 5. Flussi alternativi / edge / errori

- **Stack già acceso / porte occupate**: porte dedicate della suite (offset diverso da quello dello smoke) → convive
  con `app-start` e con lo smoke; il compose è idempotente.
- **Email non arrivata entro il timeout**: fallimento del journey con messaggio esplicito ("email di verifica non
  ricevuta in Mailpit entro Ns") — mai un timeout anonimo del browser.
- **Flakiness**: vietato dormire a tempo fisso; solo attese su condizioni (API Mailpit, stati UI, polling DB). Un
  journey instabile è un difetto da correggere, non da ritentare a oltranza (retry Playwright ≤ 1, e il retry che
  passa va comunque segnalato).
- **Esecuzione parziale**: `--journey <id>` (o grep Playwright) per lanciare un solo percorso in sviluppo.
- **CI**: la suite gira in CI come job dedicato; **fuori dal gate rapido per-change** (come `tooling` e `smoke`),
  dentro l'esecuzione completa `./run-tests.sh`.

## 6. Risorse & runbook

- **Nuova cartella `tools/platform-e2e/`** (proposta): progetto Playwright autonomo e cross-app (backoffice + admin,
  anche due sessioni browser nello stesso test per i casi cross-tenant di UC 0092), con:
  - `run.sh` — orchestratore (passi 1–6 del flusso), riusa `dev/lib/services.sh` e la logica di
    `tools/smoke/stack-headless.sh` (estratta in funzioni condivise dove serve, senza duplicarla);
  - `helpers/` — **libreria condivisa dei journey**, da costruire robusta fin dal primo giorno:
    `tenant()` (registrazione o creazione via API di un tenant fresco), `login()`, `mailbox()` (attesa/lettura/
    estrazione link via API Mailpit), `paddle()` (interazione col fake overlay + webhook sintetici firmati di
    UC 0023), `db()` (assert leak-detector), `totp()` (generazione codici a tempo per il 2FA);
  - `journeys/` — un file per journey, nome = ID del registro di copertura (UC 0093).
- **Mailpit**: già nel compose dev; la suite ne usa l'API HTTP (lista messaggi per destinatario, corpo, link).
- **`run-tests.sh`**: nuova area `platform` (stesso commit, come da regola non negoziabile).
- **Runbook diagnosi**: dove trovare trace/screenshot/video; come rilanciare un singolo journey; come ispezionare la
  casella Mailpit (UI web) dopo un rosso.

## 7. Dati toccati

Nessun trattamento nuovo di dati personali: la suite crea e usa **soltanto dati sintetici** (email fittizie su dominio
non recapitabile, nomi generati) dentro il database locale/CI usa-e-getta. Nessun manifesto GDPR da aggiornare; nessun
campo `@PersonalData` nuovo.

## 8. Permessi & gate

La suite non introduce superfici runtime: **verifica** i presidi esistenti invece di aggiungerne. In particolare i
journey devono assertare (qui e negli UC figli) gli invarianti architetturali: `tenant_id` solo dal JWT (mai
passato dal client), filtro row-level (leak detector a livello DB), gate di entitlement/ruolo/quota osservati dal
browser come li vede l'utente. Le utenze sintetiche hanno i ruoli minimi necessari al journey.

## 9. Requisiti di test

- La suite **è** il test; i suoi requisiti di qualità: determinismo (0 dipendenze esterne), journey paralleli e
  indipendenti (tenant fresco ciascuno), diagnosi immediata (trace + messaggi di fallimento parlanti), tempo totale
  target < 10 minuti in CI per la batteria iniziale.
- J-REG verde in locale e in CI, incluse le assert su email reale (Mailpit) e leak detector DB.
- Le aree esistenti restano verdi: `./run-tests.sh` completo non regredisce.
- Collaudo dell'orchestratore: doppia esecuzione consecutiva senza pulizia manuale (idempotenza), esecuzione con
  stack dev acceso (convivenza porte).

## 10. Riferimenti & Definition of Done

- **Decisioni**: #10 (filosofia: test veri, leak detector, piramide), #11 (stack locale, Mailpit ≈ SES, fake Paddle),
  #09 dec. 20 (livelli L1/L2/L3 pagamenti — questa suite non li sostituisce: si inserisce tra L2 e L3).
- **DoD**:
  1. `./run-tests.sh platform` esiste, orchestra stack vero + frontend costruiti + Mailpit e ritorna verdetto unico;
  2. journey J-REG completo e verde (email di verifica **realmente ricevuta e usata**);
  3. helper condivisi documentati e pronti per gli UC 0091/0092;
  4. suite eseguita in CI (job dedicato, fuori dal gate rapido per-change);
  5. `run-tests.sh` aggiornato nello stesso commit; questo file marcato 🟢 implementato in `_INDEX.md` dalla change.

## Punti aperti / decisioni differite

- **Come servire le SPA contro lo stack della suite** (server statico con inoltro `/api/*` derivato dalla scoperta
  servizi — raccomandato — vs riuso completo di Caddy/TLS come `app-start`): da decidere nella change di questo UC;
  il riuso di Caddy porta con sé la fragilità (TLS, `/etc/hosts`) che lo smoke headless ha volutamente evitato.
- **Estrazione delle funzioni comuni** tra `tools/smoke/stack-headless.sh` e l'orchestratore della suite (avvio
  compose, migrate+seed, attesa servizi): appartiene a questa change, ma senza rompere lo smoke (area a sé).
- **Tempo simulato** (scadenze abbonamento, grace GDPR): la leva per "far passare il tempo" nei journey (webhook
  sintetici di stato vs orologio manipolabile nei servizi in profilo dev) è decisa in UC 0091, che ne è il primo
  consumatore.
