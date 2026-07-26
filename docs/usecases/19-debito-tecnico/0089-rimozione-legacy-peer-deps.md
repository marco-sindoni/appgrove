# UC 0089 — Rimozione legacy-peer-deps nel frontend

**Area**: 19-debito-tecnico · **Fase**: evo · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0020 (shell SPA del backoffice), UC 0019 (design system frontend)
**Fonte**: R19 (Tabella dei residui in `_INDEX.md`); `docs/_BACKLOG.md` §"Backoffice shell (UC 0020)" — voce legacy-peer-deps
**Ultimo aggiornamento**: 2026-07-26

## 1. Obiettivo / Scope
Descrivere l'attività di **manutenzione** che rimuove il rilassamento `legacy-peer-deps=true` dal file di configurazione
dell'installatore di pacchetti del frontend (`frontend/.npmrc`), riportando l'installazione al controllo rigoroso
(strict) delle dipendenze compagne (peer dependencies).

**Contesto**. Il monorepo usa **TypeScript 6** (l'ultima versione principale del linguaggio). Alcune librerie del
frontend — ad esempio `react-i18next` e `openapi-typescript` — dichiarano ancora una dipendenza compagna **opzionale**
su `typescript@^5`, non aggiornata all'ultima versione principale. L'installatore di pacchetti in modalità rigorosa
tratterebbe quella dichiarazione come **conflitto** e rifiuterebbe l'installazione. Per non bloccare il lavoro, il file
`.npmrc` imposta `legacy-peer-deps=true`, che **rilassa solo il controllo dei peer in fase di installazione**: è una
scelta di installazione, **senza alcun effetto a runtime** sul codice spedito.

Questo use case appartiene all'epica **Debito tecnico & feature deprioritizzate**; il file gemello dell'epica è
[UC 0088 — Search globale dal workspace del backoffice](0088-search-globale-workspace.md). L'attività è un **follow-up**:
va eseguita **quando l'ecosistema aggiorna** quei peer opzionali a TypeScript 6, non prima.

**Incluso**: come verificare che i peer siano aggiornati, come togliere il flag, reinstallare da zero e confermare che i
test frontend restino verdi. **Escluso**: aggiornamenti funzionali delle librerie oltre a quanto serve a togliere il
flag; nessuna modifica al codice applicativo.

## 2. Attori & ruoli
- **Sviluppatore / manutentore del frontend**: esegue la verifica dei peer, rimuove il flag, reinstalla e valida.
- **Sistema di integrazione continua**: rilancia l'installazione e la suite di test sul cambiamento, come conferma
  indipendente che l'installazione rigorosa regge.

Nessun utente finale coinvolto; nessun attore esterno.

## 3. Precondizioni
- Il file `frontend/.npmrc` contiene ancora `legacy-peer-deps=true`.
- Il monorepo è su TypeScript 6.
- Esiste una condizione di sblocco: le librerie che dichiarano il peer opzionale `typescript@^5` (almeno `react-i18next`
  e `openapi-typescript`, più eventuali altre emerse) hanno pubblicato versioni che dichiarano un intervallo di peer
  compatibile con TypeScript 6.

## 4. Flusso principale
1. **Verifica dei peer**: controllare, per ogni libreria sospetta, l'intervallo di dipendenza compagna dichiarato nella
   versione installata (ispezione dei metadati del pacchetto / albero delle dipendenze). Confermare che l'intervallo
   ammette la versione di TypeScript in uso.
2. **Allineare le versioni**: se necessario, aggiornare quelle librerie alle versioni che dichiarano il peer aggiornato,
   rispettando i vincoli del design system (UC 0019) e della shell (UC 0020).
3. **Rimuovere il flag**: togliere la riga `legacy-peer-deps=true` da `frontend/.npmrc` (o rimuovere il file se restava
   solo per quello).
4. **Reinstallazione pulita**: eliminare l'albero dei pacchetti installati e il file di blocco delle versioni, poi
   reinstallare da zero in modalità rigorosa, così che un eventuale conflitto residuo emerga subito.
5. **Validazione**: eseguire la suite di test del frontend e verificarla verde, inclusi i test end-to-end di livello L2
   con browser reale (UC 0029), tramite `run-tests.sh frontend`.
6. **Chiusura**: aggiornare la tabella dei residui (R19) e la voce nel backlog come completate; registrare la scelta nel
   registro decisioni della change.

## 5. Flussi alternativi / edge / errori
- **Edge — solo alcune librerie aggiornate**: se anche una sola libreria dichiara ancora il peer vecchio,
  l'installazione rigorosa fallisce. In tal caso **non** si toglie il flag: si sospende l'attività e si annota quale
  libreria manca all'appello, riprendendo il follow-up più avanti.
- **Edge — peer opzionale ma dichiarato male**: alcune librerie dichiarano il peer come opzionale; verificare se il
  conflitto sia effettivo o aggirabile aggiornando solo i metadati (versione della libreria) senza toccarne il codice.
- **Errore — installazione rigorosa fallita**: leggere l'albero dei conflitti riportato dall'installatore, identificare
  la libreria colpevole e il peer richiesto; decidere se aggiornarla o attendere. Non reintrodurre il flag come
  "soluzione" silenziosa: se si rimette, si documenta il perché.
- **Errore — test rossi dopo la reinstallazione**: se l'aggiornamento di una libreria introduce una regressione,
  correggerla o riportare la libreria alla versione precedente compatibile; il flag resta rimosso solo se i test tornano
  verdi.

## 6. Risorse & runbook
- **File toccato**: `frontend/.npmrc` (rimozione della riga `legacy-peer-deps=true`), più eventuali versioni nel file di
  manifesto dei pacchetti del frontend e nel relativo file di blocco.
- **Comandi (runbook)**:
  1. verifica dei peer dichiarati dalle librerie sospette (ispezione metadati / albero dipendenze);
  2. eventuale aggiornamento mirato delle librerie;
  3. rimozione della riga in `frontend/.npmrc`;
  4. reinstallazione pulita in modalità rigorosa (cancellare albero pacchetti + file di blocco, poi reinstallare);
  5. `run-tests.sh frontend` (unit + Playwright end-to-end L2) e conferma verde.
- **Rollback**: se l'installazione rigorosa o i test non reggono, ripristinare `frontend/.npmrc` e il file di blocco allo
  stato precedente e rinviare il follow-up, annotando la libreria che ancora blocca.

## 7. Dati toccati
Nessuno. È un'attività di configurazione dell'installazione delle dipendenze del frontend: **nessun dato personale**,
nessuna entità persistente, nessuna tabella. Nessun effetto a runtime sul codice spedito agli utenti.

## 8. Permessi & gate
Nessun gate applicativo (nessun entitlement, ruolo o quota). Presidio operativo unico: l'installazione rigorosa e la
suite di test del frontend devono restare verdi tramite `run-tests.sh` prima del merge. Come ogni change, restano i tre
presidi dello sviluppatore: approvazione dei requisiti, consenso al commit e consenso al merge.

## 9. Requisiti di test
- **Regressione dell'installazione**: reinstallazione pulita in modalità rigorosa che **completa senza errori di peer** —
  è di per sé il test principale di questo use case.
- **Suite frontend verde**: test unitari (vitest) e test end-to-end L2 con browser reale (Playwright, UC 0029) verdi dopo
  la rimozione del flag.
- **Integrazione continua**: la pipeline ripete installazione e test in ambiente pulito, come conferma indipendente che
  non serve più il rilassamento.
- Nessun nuovo test da scrivere: si riusa la suite esistente; ciò che cambia è la **condizione di installazione** sotto
  cui gira.

## 10. Riferimenti & Definition of Done
- **Dipendenze**: UC 0020 (shell SPA del backoffice, dove il file `.npmrc` è nato) e UC 0019 (design system frontend, i
  cui vincoli di versione vanno rispettati negli aggiornamenti).
- **Fonte**: R19 nella tabella dei residui di `_INDEX.md`; `_BACKLOG.md` §"Backoffice shell (UC 0020)", voce
  legacy-peer-deps.
- **DoD**: `legacy-peer-deps=true` rimosso da `frontend/.npmrc`; reinstallazione pulita in modalità rigorosa completata
  senza conflitti di peer; suite frontend (unit + end-to-end L2) verde via `run-tests.sh frontend`; residuo R19 e voce di
  backlog segnati come chiusi; scelta registrata nel `decisions.json` della change.

## Punti aperti / decisioni differite
- **Momento dello sblocco**: l'attività dipende dall'ecosistema esterno (le librerie che devono aggiornare il peer a
  TypeScript 6). Finché anche una sola libreria dichiara il peer vecchio, il flag resta. Da rivedere periodicamente.
- **Debito tecnico frontend collegato — runtime auth/sessione condiviso (UC 0021 #18)**: nei punti aperti della console
  admin (UC 0021, item #18) è tracciata l'**estrazione di un runtime di autenticazione/sessione condiviso** fra
  backoffice e console admin. Oggi il sottoinsieme minimo (configurazione, autenticazione, client delle API) è
  **duplicato** in `apps/admin`; l'ipotesi è consolidarlo in un pacchetto condiviso `@appgrove/app-runtime`. Resta
  tracciato in UC 0021, promuovibile a storia propria dentro questa epica quando sarà maturo. Lo si annota qui perché è
  dello stesso genere (debito tecnico del frontend), ma **non** è di competenza di questo UC.
