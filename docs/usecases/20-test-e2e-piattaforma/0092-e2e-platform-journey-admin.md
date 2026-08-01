# UC 0092 — Batteria journey end-to-end lato amministratore + guasti di piattaforma

**Area**: 20-test-e2e-piattaforma · **Fase**: evo · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0090 (fondamenta suite), UC 0021 (admin console SPA), UC 0034 (console Diritti GDPR), UC 0076 (disabilita applicazione), UC 0077 (provider entitlement reale)
**Fonte decisioni**: #10 (testing)
**Ultimo aggiornamento**: 2026-08-01

## 1. Obiettivo / Scope

Completare la batteria di piattaforma con i **journey lato amministratore di piattaforma** (console admin) e con i
**guasti di piattaforma** osservati dall'utente. Il valore distintivo rispetto all'L2 admin esistente è duplice:
(1) gli effetti **cross-attore** — un'azione dell'amministratore vista *dal tenant*, e viceversa — verificati con
**due sessioni browser nello stesso journey**; (2) i guasti prodotti **davvero** (servizio fermo) e non simulati.

**Incluso**: journey A-CONSOLE, A-GDPR, A-ENTITLE, F-DEGRADE descritti sotto.
**Escluso**: journey utente (UC 0091); funzionalità admin nuove; la console ticketing nativa (UC 0075, quando
maturerà porterà il proprio journey via UC 0094).

## 2. Attori & ruoli

- **platform-admin** sintetico (ruolo dal JWT, seed della suite) sulla console admin.
- **Owner/member** di tenant sintetici creati dai journey (seconda sessione browser).
- **Sistemi**: stack della suite (UC 0090), Mailpit, fake Paddle.

## 3. Precondizioni

UC 0090 implementato. Il seed della suite include un'utenza `platform-admin` (come il seed dev); i tenant osservati
sono creati dal journey stesso via helper.

## 4. Flusso principale — i journey

**A-CONSOLE — Console di piattaforma e disabilitazione app (cross-tenant)**
1. Sessione A (tenant): owner con app attivata e funzionante (via helper di UC 0091).
2. Sessione B (admin): login platform-admin → overview con indicatori → dettaglio app → **disabilita l'app** con
   conferma.
3. Sessione A: l'app sparisce/si blocca per il tenant (sidebar aggiornata secondo le regole di freschezza di
   UC 0077; la rotta del modulo nega l'accesso); i dati NON sono cancellati (assert DB).
4. Sessione B: riabilita → Sessione A: l'accesso torna. Assert: audit dell'azione admin registrato con
   `tenant_id`/`app_id`/`user_id` (logging strutturato).

**A-GDPR — Console Diritti GDPR end-to-end**
1. Sessione A: l'utente apre un ticket privacy dalla pagina Supporto e avvia un export.
2. Sessione B: la console GDPR aggrega la richiesta e l'export (con prova di completamento); l'admin risponde nel
   thread; applica la **limitazione art. 18** con conferma.
3. Sessione A: la risposta è visibile nel thread; gli effetti della limitazione sono osservabili.
4. Assert DB: registro della limitazione; il ticket e l'export appartengono al solo tenant giusto.

**A-ENTITLE — Coerenza della matrice entitlement**
1. Si portano due tenant in stati diversi (uno con acquisto attivo via fake Paddle, uno con sola fascia gratuita,
   uno in eliminazione se disponibile via leva di test).
2. La matrice `account × app` della console admin riflette **esattamente** lo stato reale (stessa regola unica di
   accesso del backend — UC 0077): entitled sì/no coerente con ciò che i tenant vedono nelle rispettive sidebar.

**F-DEGRADE — Guasti di piattaforma osservati dall'utente**
1. Con utente autenticato e shell montata, l'orchestratore **ferma davvero** il servizio degli entitlement.
2. La sidebar mostra l'errore con "riprova" — mai "nessuna app" né un diniego d'accesso (UC 0077); la rotta di un
   modulo mostra errore, non `/forbidden`.
3. Il servizio viene riavviato → "riprova" → la shell torna normale senza ricaricare la pagina.
4. Variante sessione: con refresh-token invalidato lato server, la prima chiamata 401 → tentativo di rinnovo →
   uscita pulita al login (nessun loop, nessuno stato appeso).

## 5. Flussi alternativi / edge / errori

- **Due sessioni browser**: contesti Playwright isolati (cookie/storage separati) nello stesso test; helper dedicato
  in `tools/platform-e2e/helpers/`.
- **Stop/riavvio di un servizio** (F-DEGRADE): l'orchestratore di UC 0090 espone `stopService(app_id)` /
  `startService(app_id)`; il journey è **seriale rispetto a se stesso** ma resta isolato dagli altri (il servizio
  fermato serve solo il suo scopo — se la condivisione dei servizi tra journey paralleli lo impedisce, F-DEGRADE
  gira in coda alla batteria: scelta della change).
- **Leva "account in eliminazione"** (A-ENTITLE): se non esiste una via reale rapida, la variante si limita agli
  stati raggiungibili senza leve artificiose; il caso mancante si traccia nel registro (UC 0093) come `da-coprire`.

## 6. Risorse & runbook

- Journey in `tools/platform-e2e/journeys/` con ID stabili: `A-CONSOLE`, `A-GDPR`, `A-ENTITLE`, `F-DEGRADE`.
- Helper nuovi: doppia sessione (tenant+admin), controllo servizi dell'orchestratore, seed platform-admin.
- La console admin è **costruita e servita** dalla suite accanto al backoffice (stessa meccanica di UC 0090).

## 7. Dati toccati

Solo dati sintetici (come UC 0090/0091). Il journey A-GDPR maneggia richieste privacy **sintetiche**: nessun dato
personale reale, nessun manifesto da aggiornare.

## 8. Permessi & gate

- I journey verificano il gate di ruolo `platform-admin` (console raggiungibile solo col ruolo; senza →
  `/forbidden` — già coperto in L2, qui ri-attraversato con token veri).
- A-CONSOLE/A-GDPR assertano l'isolamento: l'azione admin tocca solo il tenant bersaglio (leak detector sugli altri
  tenant creati dal journey).
- F-DEGRADE verifica il principio "guasto ≠ diniego": un errore di lettura non deve mai presentarsi come mancanza
  di diritto (UC 0077).

## 9. Requisiti di test

- I 4 journey verdi in locale e CI dentro `./run-tests.sh platform`, nel budget di tempo complessivo della suite
  (UC 0090 §9; F-DEGRADE può richiedere serializzazione — misurare e documentare).
- Doppia esecuzione consecutiva verde; nessun residuo (servizi tutti su a fine batteria anche dopo F-DEGRADE).
- L2 admin esistente: eventuali duplicazioni rimosse col criterio di riparto di UC 0091 §1.

## 10. Riferimenti & Definition of Done

- **Decisioni**: #10; UC 0076 (semantica disabilitazione: reversibile, dati preservati); UC 0077 (regola unica di
  accesso + stati della shell); UC 0034 (console GDPR).
- **DoD**:
  1. i 4 journey esistono, sono verdi e girano nel comando unico;
  2. helper doppia-sessione e controllo servizi disponibili e documentati;
  3. ID stabili registrati (input per UC 0093);
  4. `_INDEX.md` aggiornato dalla change.

## Punti aperti / decisioni differite

- **Ticketing nativo (UC 0075)**: quando sostituirà l'attuale flusso di supporto, il journey A-GDPR andrà
  aggiornato — rimando tracciato qui e da registrare in `da-coprire` nel registro alla nascita di quel UC.
- **Osservabilità dei guasti** (allarmi, error-reporter): F-DEGRADE osserva solo l'esperienza utente; le assert
  sugli allarmi/telemetria appartengono all'area observability (#08) e non a questa suite.
