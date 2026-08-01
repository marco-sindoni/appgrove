# Log di implementazione — Change 0078: Dashboard operativa del workspace

**Branch**: `change/0078-use-case-0097-dashboard-operativa` · **Modalità**: fast (autopilot senza gate di workflow)
**Use case**: [0097](../../docs/usecases/21-catalogo-app-backoffice/0097-dashboard-operativa.md) · **Data**: 2026-08-02
**Registro decisioni**: [`decisions.json`](decisions.json) — 17 voci

## Che cosa è cambiato

### `services/auth` — lo stato del secondo fattore diventa leggibile

Nuova lettura `GET /api/auth/2fa/status`, autenticata, sul soggetto del token: `{ "enabled": <sì/no> }`. Il contratto
comune ai due provider di identità guadagna `totpEnabled(bearerToken, sub)` — il provider locale legge la propria riga
di credenziali (un utente senza credenziali risulta «non attivo», non un errore), quello Cognito guarda l'elenco dei
fattori **confermati** restituito da `GetUser`. Nessuna scrittura, nessun dato nuovo conservato.

Serviva perché fino a ieri il prodotto **non sapeva** se un utente avesse il secondo fattore: l'invito ad attivarlo era
un banner cieco, chiudibile per sempre nella memoria del browser. Un avviso permanente sulla pagina d'atterraggio che
dicesse il falso a chi ha già fatto la cosa giusta sarebbe stato peggio dell'avviso che sostituisce.

### `frontend` — la Dashboard

`src/pages/dashboard/` sostituisce la vecchia `pages/Dashboard.tsx`:

- **`dashboardModel.ts`** — la logica che può sbagliare in silenzio, in funzioni **pure**: composizione e ordine degli
  avvisi, derivazione della barra di quota (percentuale, soglia d'avviso, tetto illimitato, tetto zero, consumo oltre il
  tetto), app davvero in uso, prossimo rinnovo;
- **`quotaApi.ts`** — la lettura di quota di **una** app, dal servizio dell'app stessa (percorso dichiarato dal manifest
  del modulo). Riusa la configurazione di autenticazione della shell (token in memoria, un solo rinnovo su 401) invece di
  aprire un secondo modo di parlare col backend. Una lettura per card: un'app che risponde male non spegne le altre;
- **`DashboardPage.tsx`** — intestazione con saluto e nome del workspace, striscia degli avvisi, griglia delle app in uso
  con barra di consumo e azioni, colonna «At a glance» + scorciatoie. **Ogni sezione possiede i propri stati**.

Attorno alla pagina: il descrittore di quota facoltativo nel manifest dei moduli (`ModuleQuota`), dichiarato da `fatture`
e `crm`; le letture di membri e inviti condizionate al ruolo **nella loro definizione**; il banner del secondo fattore
tolto dal guscio; la pagina **Security** che smette di proporre l'attivazione a chi ce l'ha già; la pagina **Account**
con la sezione Workspace (nome, identificativo in carattere a larghezza fissa, pulsante di copia con conferma).

Testi nuovi nelle **5 lingue** (`dashboard.*`, `account.*` nel catalogo della shell; `quotaUnit` nei bundle di `fatture`
e nelle stringhe di `crm`).

### `tools/new-application` — parità dei modelli

Il varco di parità è scattato (toccato `frontend/apps/backoffice/src/modules/fatture`) e si è scelta la via
**«aggiorna i modelli»**: il modello del manifest dichiara il descrittore di quota e i cinque bundle generati portano
l'unità consumata, derivata dalla metrica dell'app. Un'app nuova nasce quindi con la sua barra in Dashboard.

## Collaudo

| Area | Che cosa | Esito |
|---|---|---|
| `services/auth` | stato del secondo fattore: falso prima dell'iscrizione, vero dopo la conferma, 401 senza token | ✅ |
| `frontend` unità | 22 asserzioni sulla logica pura (avvisi, barra, app in uso, rinnovo) | ✅ |
| `frontend` componente | Dashboard: saluto, card con barra, tetto illimitato, guasto di una quota, avvisi in ordine, workspace vuoto, member senza azioni riservate, guasto della vetrina; Account: identificativo e copia | ✅ |
| `frontend` e2e L2 | `L2-DASHBOARD` — 7 percorsi (`e2e/dashboard.spec.ts`) | ✅ |
| piattaforma | `J-REG` esteso: la panoramica su stack vero e l'identificativo del workspace confrontato col valore del database | ✅ |
| **suite completa** | `./run-tests.sh` — backend, frontend, infra, compliance, tooling, smoke, platform, site | ✅ |

**Nota sulla suite.** Due esecuzioni complete precedenti sono state rosse per la stessa causa d'ambiente e su test
**diversi** ogni volta: il guardiano dei contenitori di collaudo non si avviava e la base dati di prova non partiva
(«Could not connect to Ryuk»). I due test coinvolti sono stati rieseguiti isolatamente — verdi — e la terza esecuzione
completa è verde su tutte le aree. Nulla è stato reso più tollerante per farla passare.

## Gate

- **Privacy/RoPA (UC 0031)**: nessun segnale. L'unica informazione nuova che circola è un sì/no sullo stato del secondo
  fattore dell'utente in sessione: attributo di sicurezza già conservato, nessuna finalità/base giuridica/categoria/
  conservazione nuove, nessuna voce di manifesto da aggiungere.
- **Copertura end-to-end (UC 0093/0094)**: coperta ora. Nuovo percorso `L2-DASHBOARD`, `J-REG` esteso a UC 0097,
  esenzione `non-implementato` di 0097 rimossa. `tools/e2e-coverage` verde.
- **Parità scaffolding (UC 0046)**: via 1 — modelli aggiornati. Collaudo di parità verde.
- **`run-tests.sh`**: nessuna modifica necessaria (nessun modulo nuovo, nessun comando di test cambiato).

## Note per il revisore

- **Promemoria landing stale (UC 0057)**: l'euristica scatta — la change tocca il modulo frontend di `fatture`, che ha
  una landing **pubblicata** — ma la valutazione è che la landing non sia invecchiata: le due modifiche sono
  un'etichetta interna dell'unità consumata e il percorso dove leggere la quota. Nessuna funzionalità, nessun prezzo,
  nessuna schermata dell'app cambiano. Nessuna ri-esecuzione di `finalize-landing` richiesta.
- **Quattro asserzioni end-to-end preesistenti su «Your apps»** sono state ristrette al menu laterale invece di
  rinominare la sezione della Dashboard: il testo compare ora due volte nella stessa schermata, come nel riferimento
  visivo approvato, e quelle asserzioni cercavano nell'intera pagina. Nessuna asserzione è stata indebolita.
- **Decisione 8 corretta dalla 11**: gli avvisi sono due, non tre. L'avviso legale non è stato duplicato perché una
  presa visione non si può registrare da nessuna parte — limite tracciato nei punti aperti di **UC 0056**.

## Rimandi tracciati

- **UC 0056** — gli aggiornamenti legali minori si possono solo chiudere, mai prendere in carico: manca la schermata
  «rivedi i documenti aggiornati» (il contratto server esiste già).
- **UC 0097** — una lettura di quota per app in uso (da unificare se il numero di app crescesse); un'app attiva senza
  modulo impacchettato non avrebbe barra e avrebbe un «Open» che non porta da nessuna parte (vale identicamente per la
  vetrina, UC 0095); il saluto non conosce l'ora del giorno; l'avviso legale deliberatamente non duplicato.
