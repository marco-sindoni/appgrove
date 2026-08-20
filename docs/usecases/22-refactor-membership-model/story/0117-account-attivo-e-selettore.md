# UC 0117 — Account attivo nella sessione e selettore

**Area**: 22-refactor-membership-model · **Fase**: evo · **Stato**: 🟢 scritto (da implementare)
**Epica**: [E22.5 Identità e appartenenze](../epic/E22-05-identita-e-appartenenze.md)
**Dipendenze**: [UC 0116](0116-identita-e-appartenenze.md), UC 0010 (accesso locale), UC 0017 (flussi di autenticazione)
**Piano di lavoro**: [task/0117](../task/0117-account-attivo-e-selettore.md)
**Ultimo aggiornamento**: 2026-08-20

## 1. Obiettivo / Scope

Se una persona appartiene a più account, il token non può più dedurre l'account da lei: deve portare
**l'account attivo** in quella sessione, e la persona deve poterlo cambiare.

È la storia **più delicata dell'intera epica**, perché tocca il punto in cui si stabilisce chi sei e per
conto di chi stai agendo. Un errore qui non è un difetto di interfaccia: è un varco fra due aziende.

**Incluso**: dove vive l'account attivo; come il token lo trasporta senza violare l'invariante «account
solo dal token verificato»; la verifica che l'appartenenza esista e sia attiva; il cambio di account; il
selettore nell'interfaccia; la parità fra il fornitore di identità reale e quello locale.

**Escluso**: il modello dati → [UC 0116](0116-identita-e-appartenenze.md); i percorsi d'ingresso →
[UC 0118](0118-inviti-e-registrazione-con-identita-esistente.md).

## 2. Attori & ruoli

- **Persona con una sola appartenenza** (il caso normale, oggi tutti): non deve accorgersi di nulla. Nessun
  selettore, nessun passaggio in più.
- **Persona con più appartenenze**: sceglie l'account su cui lavorare e lo cambia quando serve.
- **Sistema**: rifiuta se l'account chiesto non corrisponde a un'appartenenza attiva.

## 3. Precondizioni

- Identità e appartenenze esistono (UC 0116).
- La funzione che costruisce il token interroga già la banca dati
  ([handler.py](../../../../infra/modules/platform_shared/lambda/pre_token_gen/handler.py)) e il fornitore
  locale replica gli stessi claim ([TokenService.java](../../../../services/auth/src/main/java/app/appgrove/auth/local/TokenService.java)).

## 4. Flusso principale

1. L'account attivo è **un dato conservato in banca dati**, sull'appartenenza (o sull'identità come
   riferimento all'appartenenza corrente) — **non** un attributo dell'utente presso il fornitore di
   identità. Ragione tecnica verificata: il gruppo di utenti di Cognito non dichiara attributi
   personalizzati ([auth.tf](../../../../infra/modules/platform_shared/auth.tf)) e aggiungerne uno per via
   dichiarativa rischia di **ricreare il gruppo**, cioè di perdere gli utenti. La funzione che costruisce il
   token interroga già la banca dati: leggere una colonna in più costa nulla e non tocca l'infrastruttura.
2. Alla creazione del token, il sistema: trova le appartenenze **attive** dell'identità; se l'account attivo
   conservato ne indica una, usa quella; se non lo indica o indica un'appartenenza non più valida, usa
   l'unica disponibile; se ce n'è più di una e nessuna è indicata, **nega** e chiede di scegliere.
3. Il claim dell'account continua a essere iniettato dal solo lato server e verificato: **l'invariante 1
   non si tocca**. Cambia la funzione che lo calcola, non chi se ne fida.
4. **Cambio di account**: la persona sceglie dal selettore; il sistema scrive il nuovo account attivo,
   **rinnova il token** e ricarica l'applicazione. Il cambio non è una preferenza di visualizzazione: è un
   cambio di sessione.
5. **Selettore nell'interfaccia**: nella **barra laterale, sotto il marchio** — non fra i controlli
   dell'intestazione. L'account è il **contesto** in cui si sta lavorando, come il menu che gli sta sotto,
   non un comando accessorio come la lingua o il tema. Mostra il nome dell'account corrente ed elenca gli
   altri. **Se l'appartenenza è una sola, il selettore non esiste** — non «esiste disabilitato»: un comando
   che non serve a nulla è rumore, ed è lo stesso principio già adottato per i menu dei non-owner
   (UC 0107). Il **nome** dell'account resta però sempre visibile, anche con una sola appartenenza.
6. **Nessuna etichetta di ruolo nell'interfaccia di piattaforma.** Il ruolo è per applicazione: una
   etichetta globale sarebbe falsa appena una persona è abilitata a più di una applicazione («Admin del
   Mini-CRM, Viewer delle Note, Editor di Teams…» non è un'informazione, è un elenco). Il ruolo si legge
   dove è vero — sulla scheda dell'applicazione nel cruscotto e in testa alle sue schermate (UC 0101).

## 5. Il nodo della storia: il token vecchio dopo il cambio

Rinnovare il token non annulla quello precedente: per il tempo che gli resta, un token con l'account A
continua a essere valido. Va guardato in faccia, perché somiglia a un varco e non lo è:

- **non è un varco**: quel token vale per un account a cui la persona **appartiene davvero**. Non le
  concede nulla che non avesse già;
- **è invece un problema di revoca**, che esiste già oggi e non nasce qui: se la persona viene rimossa da
  un account, il suo token resta valido fino alla scadenza. La stretta appartiene a UC 0099, che introduce
  la rilettura dal core per le operazioni che modificano dati;
- **la conseguenza pratica**: la durata del token è il ritardo massimo con cui una revoca ha effetto, e va
  scritta dove la si sceglie, non lasciata implicita.

Ciò che questa storia **deve** garantire è più stretto e verificabile: nessun token può portare un account a
cui l'identità **non** appartiene. Vale in ogni caso, compreso un account attivo scritto da un percorso
manomesso — perché l'appartenenza si **riverifica** al momento della creazione del token, e non si crede al
valore conservato.

## 6. Flussi alternativi / edge / errori

- **Edge — una sola appartenenza**: nessun selettore, nessun cambio, comportamento identico a oggi. È il
  caso di tutti gli utenti attuali e deve restare a costo zero.
- **Edge — l'account attivo punta a un'appartenenza revocata**: si ignora il valore conservato e si
  ricalcola. Se resta una sola appartenenza si entra lì; se ne restano più di una si chiede di scegliere.
- **Edge — l'account attivo punta a un account sospeso o cancellato**: come sopra. Un account non
  utilizzabile non è un account attivo.
- **Edge — nessuna appartenenza attiva**: nessun token valido, come già oggi («a chiusura in caso di
  dubbio»). Il messaggio dice cosa fare, non «errore».
- **Edge — la persona è owner del proprio account e member di un altro**: nel primo vede tutti i menu, nel
  secondo soltanto le applicazioni a cui è abilitata (UC 0107). Lo **stesso** essere umano, due esperienze
  diverse: è il collaudo end-to-end più utile di questa storia.
- **Errore — cambio verso un account a cui non si appartiene**: rifiuto `404` (non `403`: non si rivela
  l'esistenza dell'account).
- **Edge — schede del navigatore aperte su due account**: dopo il cambio, la scheda vecchia ha un token
  che punta ancora ad A. Non è un varco (vedi §5) ma è **confusione**, e la confusione su chi stai
  guardando è essa stessa un rischio: l'interfaccia deve accorgersi che l'account attivo è cambiato e
  invitare a ricaricare. Da non lasciare al caso.

## 7. Dati toccati

- **Riferimento all'account attivo**: una colonna sull'identità (`active_membership_id`, annullabile). Non
  è un dato personale nuovo: è una preferenza tecnica di sessione.
- **Nessun attributo nuovo presso il fornitore di identità**: scelta deliberata (§4.1).
- **Traccia di controllo**: il cambio di account attivo si registra, con soli identificativi opachi. È
  un'informazione utile in caso di contestazione su «chi ha fatto cosa e per conto di chi».

## 8. Permessi & gate

- **Account solo dal token verificato** (invariante 1): confermato. L'account attivo conservato è un
  **suggerimento**, non una fonte di verità: la verità è l'appartenenza riverificata al momento della
  creazione del token.
- **A chiusura in caso di dubbio**: qualunque incertezza (nessuna appartenenza, appartenenza non valida,
  più appartenenze e nessuna scelta) produce l'assenza del claim, e i servizi rifiutano. Il comportamento
  esiste già e va conservato tale e quale.
- **Parità obbligatoria fra i due fornitori di identità**: Cognito e locale devono produrre gli stessi
  claim. Una divergenza qui significa collaudi verdi in locale e rotti in ambiente reale, o peggio il
  contrario.

## 9. Requisiti di test

- **Unità**: la funzione che scegli l'account (nessuna appartenenza, una, più di una con e senza scelta
  conservata, scelta conservata non più valida). È il cuore della storia e va provata caso per caso.
- **Sicurezza, la prova che conta**: un account attivo conservato che **non** corrisponde a
  un'appartenenza attiva non produce mai un token con quel claim. Da provare anche manomettendo
  direttamente il valore conservato, perché è l'unica prova che il valore non è creduto.
- **Integrazione**: cambio di account, rinnovo del token, claim nuovo; il vecchio token continua a valere
  per il suo account fino alla scadenza (comportamento **atteso**, quindi va scritto in un collaudo, non
  scoperto in seguito).
- **Parità dei fornitori**: lo stesso collaudo gira su Cognito e sul fornitore locale, e pretende gli
  stessi claim.
- **Percorsi end-to-end**: la stessa persona entra nel proprio account e vede tutti i menu; passa
  all'account dell'azienda e vede solo le applicazioni a cui è abilitata; torna indietro. Con
  un'appartenenza sola, il selettore non appare. Percorsi `J-ACCOUNT-SWITCH` da registrare in
  [copertura-e2e.yaml](../../../testing/copertura-e2e.yaml).

**Prototipo di riferimento**: [prototype/admin.html](../prototype/admin.html) e
[viewer.html](../prototype/viewer.html) mostrano il selettore — nella barra laterale, sotto il marchio —
apribile con due appartenenze; [owner.html](../prototype/owner.html) e
[editor.html](../prototype/editor.html) mostrano al suo posto il **solo nome** dell'account, senza comando. La [documentazione dei prototipi](../prototype/README.md) porta la riga di mappatura verso
`shell/Topbar.tsx` e la tabella degli stati da implementare comunque (caricamento, cambio in corso,
appartenenza revocata a menu aperto).

## 10. Riferimenti & Definition of Done

- **Riferimenti**: [handler.py](../../../../infra/modules/platform_shared/lambda/pre_token_gen/handler.py),
  [TokenService.java](../../../../services/auth/src/main/java/app/appgrove/auth/local/TokenService.java),
  [UserDirectory.java](../../../../services/auth/src/main/java/app/appgrove/auth/local/UserDirectory.java),
  [auth.tf](../../../../infra/modules/platform_shared/auth.tf),
  [docs/02 — Auth & sicurezza](../../../02-auth-sicurezza.md).
- **Definition of Done**:
  1. l'account attivo vive in banca dati e nessun attributo nuovo tocca il gruppo di utenti;
  2. l'appartenenza è **riverificata** a ogni creazione di token, e il valore conservato non è creduto;
  3. con una sola appartenenza nulla cambia per l'utente, provato da un collaudo;
  4. il selettore compare solo quando serve e il cambio rinnova il token;
  5. i due fornitori di identità producono claim identici, provato dallo stesso collaudo;
  6. i percorsi end-to-end del cambio di account sono nel registro di copertura;
  7. `run-tests.sh backend frontend infra` verde.

## Punti aperti / decisioni differite

- **Durata del token e ritardo della revoca**: questa storia rende visibile un legame che c'era già. La
  scelta della durata resta di UC 0017; qui si pretende solo che sia **scritta** e non implicita.
  Proprietario: UC 0017.
- **Ultimo account usato come predefinito, o scelta a ogni accesso?** Si adotta l'ultimo usato, perché con
  una sola appartenenza deve essere invisibile e con più appartenenze è ciò che la persona si aspetta.
  Rivedibile se emergesse un rischio di «lavorare sull'account sbagliato senza accorgersene» — mitigato dal
  nome dell'account sempre visibile nell'intestazione. Proprietario: questa storia.
- ~~**Nome dell'account nell'interfaccia**~~ — **chiuso**: mostrato in permanenza nella **barra laterale,
  sotto il marchio**, anche con una sola appartenenza, perché con più account è un elemento di sicurezza
  percepita e non un ornamento. Sta lì e non nell'intestazione perché l'account è il contesto del lavoro,
  come il menu che gli sta sotto. Reso nei prototipi (`admin.html` e `viewer.html` col selettore,
  `owner.html` e `editor.html` col solo nome) e documentato nella
  [tabella di mappatura](../prototype/README.md) → `shell/Sidebar.tsx`.
