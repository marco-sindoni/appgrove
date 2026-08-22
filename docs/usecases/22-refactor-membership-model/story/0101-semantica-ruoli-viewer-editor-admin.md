# UC 0101 — Semantica dei tre ruoli (viewer, editor, admin) come contratto di piattaforma

**Area**: 22-refactor-membership-model · **Fase**: evo · **Stato**: ✅ implementato (change 0095)
**Epica**: [E22.1 Fondamenta](../epic/E22-01-fondamenta-modello-centralizzato.md)
**Dipendenze**: UC 0098 (modello dati), UC 0099 (varco riusabile)
**Sostituisce**: UC 0072 dell'epica 14 (semantica della gestione utenti per applicazione)
**Piano di lavoro**: [task/0101](../task/0101-semantica-ruoli-viewer-editor-admin.md)
**Ultimo aggiornamento**: 2026-08-19

## 1. Obiettivo / Scope

Definire una volta per tutte **che cosa può fare** ognuno dei tre ruoli, in modo che due applicazioni
diverse lo interpretino allo stesso modo, e rendere quella definizione **verificabile** invece di
affidarla alla buona volontà di chi scrive l'applicazione successiva.

**Incluso**: la definizione di comportamento dei tre ruoli; la regola di classificazione di una
operazione (che ruolo minimo richiede); la sua applicazione alle due applicazioni esistenti; il modo in
cui l'interfaccia esprime «non puoi» (assente contro disabilitato); il documento per-applicazione che
dichiara le sue operazioni dispositive.

**Escluso**: dove vive il ruolo → UC 0098; come si fa rispettare tecnicamente → UC 0099; la schermata di
gestione utenti → UC 0111; il copilota che fa questa domanda a chi crea un'applicazione → UC 0112.

## 2. Attori & ruoli

- **`viewer`**: legge tutto ciò che l'applicazione mostra; non cambia nulla.
- **`editor`**: legge tutto e compie **ogni** operazione prevista dall'applicazione.
- **`admin`**: come `editor`, più l'abilitazione all'applicazione di persone già esistenti e attive e il
  cambio dei loro ruoli su quella applicazione.
- **`owner`**: sopra tutti, su tutte le applicazioni.
- **Chi sviluppa un'applicazione**: classifica le proprie operazioni secondo questa regola.

## 3. Precondizioni

- Il ruolo esiste come dato (UC 0098) e si fa rispettare (UC 0099).
- L'applicazione ha un elenco delle proprie operazioni (le sue chiamate di rete e i comandi
  dell'interfaccia).

## 4. Flusso principale — la regola di classificazione

Ogni operazione di un'applicazione riceve **una** delle tre etichette, secondo questa domanda in
cascata:

1. **L'operazione cambia dati, invia qualcosa fuori, o consuma quota?** Se sì → richiede almeno
   `editor`. Sono le operazioni **dispositive**: creazione, modifica, cancellazione, invio, esportazione
   che genera un documento, importazione, cambio di stato.
2. **L'operazione governa *chi* usa l'applicazione?** Se sì → richiede `admin`. Sono l'abilitazione, la
   revoca e il cambio di ruolo dentro quella applicazione.
3. **Altrimenti** → basta `viewer`. Sono le letture: elenchi, dettagli, ricerche, riepiloghi, conteggi di
   quota.

Tre chiarimenti che evitano le discussioni ricorrenti:

- **L'esportazione dei propri dati letta come lettura**: scaricare in foglio di calcolo ciò che si vede
  già è una **lettura** (basta `viewer`). Diventa dispositiva solo se produce un effetto verso l'esterno
  (per esempio l'invio di una email al cliente).
- **Le preferenze personali** (tema, lingua, colonne visibili) non sono dati dell'applicazione: le può
  cambiare chiunque, anche un `viewer`.
- **Il `viewer` vede tutti i dati** che l'**ambito** dell'applicazione gli attribuisce, non un
  sottoinsieme scelto a piacere. Il requisito è esplicito: «permette di vedere tutti i dati
  dell'applicazione». La precisazione serve perché [UC 0115](0115-ambito-dati-applicazione.md) introduce
  applicazioni i cui dati sono **della persona** che li ha creati: là «tutti i dati» significa «tutti i
  propri». **L'ambito precede il ruolo**: l'ambito dice su *quali* dati, il ruolo dice *che cosa* puoi
  farci. Fuori da questi due presidi non esistono restrizioni silenziose: nascondere dati a qualcuno
  dentro il proprio ambito sarebbe un ruolo nuovo, da discutere.

## 5. Flussi alternativi / edge / errori

- **Edge — operazione a metà strada** (per esempio «segna come letto»): tecnicamente cambia un dato,
  quindi `editor`. La regola sta nella prima domanda e non si negozia caso per caso: le eccezioni
  renderebbero il modello inspiegabile.
- **Edge — un'applicazione che ha bisogno di poteri intermedi**: si dichiara come punto aperto della sua
  storia, non si inventa un quarto ruolo.
- **Edge — operazioni di conformità sui propri dati personali** (accesso e portabilità): **esenti** dai
  ruoli, come già stabilito per i diritti dell'interessato. Un `viewer` scarica i propri dati.
- **Errore — comando dispositivo invocato da un `viewer`**: rifiuto tipizzato che nomina il ruolo
  richiesto; l'interfaccia non doveva permetterlo, ma la difesa vera sta nel servizio.

## 6. Schermate & stati — come l'interfaccia esprime il limite

Regola in due parti, valida per tutte le applicazioni:

- **Comando dispositivo per chi non ha il ruolo** → **presente ma disabilitato**, con una spiegazione al
  passaggio del puntatore e leggibile dagli strumenti di assistenza: «serve il ruolo Editor: chiedi
  all'owner o a un amministratore dell'applicazione». Motivo: nascondere farebbe credere che la funzione
  non esista.
- **Intere sezioni che governano l'applicazione** (per esempio la gestione utenti per un `editor`) →
  **visibili in sola lettura**. Il `viewer` e l'`editor` vedono *chi* ha accesso; non lo cambiano.
- **Ambiti che non competono al ruolo di piattaforma** (fatturazione, gestione delle persone
  dell'account) → **assenti** dalla navigazione. Questa parte è di UC 0107.

Stati richiesti su ogni schermata: caricamento (il ruolo non è ancora noto → i comandi restano
disabilitati, mai abilitati «in attesa»), pronto, errore di lettura del ruolo (si nega e si spiega).

## 7. Dati toccati

Nessuna tabella nuova. Nasce un **documento per applicazione** — un file dentro il servizio, leggibile da
un programma — che dichiara le operazioni dell'applicazione e il ruolo minimo di ognuna. Serve a tre
cose: farlo verificare da un collaudo, farlo leggere dal copilota della skill (UC 0112) e darlo a chi
scrive la documentazione utente. Nessun dato personale.

## 8. Permessi & gate

- Il **contratto** è questa storia; l'**applicazione** del contratto è il varco di UC 0099.
- **Nessuna applicazione decide da sé** che cosa significa `editor`: la classificazione segue la cascata
  del §4 e viene registrata nel documento dell'applicazione.
- **Il collaudo è parte del contratto**: esiste una prova che confronta le operazioni dichiarate nel
  documento con quelle effettivamente protette nel codice, e va in rosso se una operazione dispositiva
  non richiede almeno `editor`.

## 9. Requisiti di test

- **Unità**: la funzione di confronto fra ruoli, con l'ordinamento e la posizione dell'owner.
- **Verifica strutturale** (dentro i collaudi del servizio): ogni operazione di scrittura esposta
  dall'applicazione dichiara un ruolo minimo; nessuna operazione di scrittura è priva di dichiarazione.
  È il collaudo che rende il contratto reale invece di scritto.
- **Integrazione, per ognuna delle due applicazioni esistenti**: un `viewer` legge e non scrive; un
  `editor` scrive; un `admin` abilita e cambia ruoli; le operazioni sui propri dati personali passano per
  tutti.
- **Percorsi end-to-end**: coperti insieme a UC 0111 e UC 0107 (percorso «stessa applicazione vista dai
  quattro ruoli»). In registro come *da coprire*, con proprietario UC 0113.

## 10. Riferimenti & Definition of Done

- **Riferimenti**: [Epica 22 §2](../epic/E22-00-rifacimento-modello-appartenenza.md);
  [Roles.java del Mini-CRM](../../../../services/crm/src/main/java/app/appgrove/crm/Roles.java) — oggi
  dichiara i ruoli di piattaforma e va riscritto sui ruoli di applicazione;
  [prototipi per ruolo](../prototype/README.md), che mostrano la regola applicata.
- **Definition of Done**:
  1. la regola di classificazione è scritta e applicata alle due applicazioni esistenti;
  2. ogni applicazione ha il suo documento delle operazioni con il ruolo minimo;
  3. esiste il collaudo che coglie una operazione di scrittura non protetta;
  4. l'interfaccia distingue *disabilitato con spiegazione* da *assente*;
  5. i diritti dell'interessato restano esenti dai ruoli;
  6. `run-tests.sh backend frontend` verde.

## Punti aperti / decisioni differite

- ~~Sorte di `App.user_model`~~ — **risolta** dopo la revisione dello sviluppatore: la categoria B2C/B2B
  si **ritira** ([UC 0114](0114-ritiro-categoria-b2c-b2b.md)) e al suo posto nasce l'**ambito dei dati**
  ([UC 0115](0115-ambito-dati-applicazione.md)), che al contrario di quella ha conseguenze verificabili nel
  codice.
- **Un quarto ruolo per casi intermedi**: rimandato. Si affronta se e quando un'applicazione reale
  dimostrerà che tre non bastano.
- **Ruoli e assistente automatico (interfacce per intelligenza artificiale)**: quando l'epica 12 maturerà,
  le sue chiamate dovranno passare dagli stessi ruoli. Annotato là come dipendenza.
