# UC 0111 — Schermata «Gestione utenti» dentro ogni applicazione

**Area**: 22-refactor-membership-model · **Fase**: evo · **Stato**: 🟢 scritto (da implementare)
**Epica**: [E22.4 Dentro le applicazioni](../epic/E22-04-app-e-industrializzazione.md)
**Dipendenze**: UC 0098 (modello dati), UC 0099 (autorizzazione), UC 0101 (semantica dei ruoli), UC 0107 (ruolo nel contratto della shell)
**Sostituisce**: UC 0074 dell'epica 14 (schermata membri per applicazione)
**Piano di lavoro**: [task/0111](../task/0111-schermata-gestione-utenti-app.md)
**Prototipi**: tutti e quattro (è la schermata che mostra meglio la differenza fra i ruoli)
**Ultimo aggiornamento**: 2026-08-19

## 1. Obiettivo / Scope

Dare a **ogni** applicazione la stessa schermata di gestione degli utenti: chi ha accesso, con quale ruolo,
e — per chi ne ha il potere — aggiungere, rimuovere, cambiare ruolo.

**Incluso**: la schermata come componente condiviso, riusato da tutte le applicazioni; le regole di chi può
cosa; il ritiro dei posti locali del Mini-CRM; l'aggiunta della sezione ai due moduli esistenti.

**Escluso**: il modello dati → UC 0098; il varco → UC 0099; l'invito di persone nuove, che è dell'owner e
sta in «Members» → UC 0100 e UC 0103.

## 2. Attori & ruoli

| Chi | Che cosa può fare in questa schermata |
|---|---|
| **Owner** | tutto: aggiungere qualunque persona attiva, rimuovere, cambiare qualunque ruolo |
| **`admin`** dell'applicazione | aggiungere persone **già esistenti e attive**, rimuoverle, cambiare i loro ruoli su **questa** applicazione |
| **`editor`** | **sola lettura**: vede chi ha accesso e con che ruolo |
| **`viewer`** | **sola lettura** |

## 3. Precondizioni

- Esiste `platform.app_access` con la sua interfaccia (UC 0098).
- Il modulo dell'applicazione conosce il ruolo di chi guarda (UC 0107).
- Esiste l'elenco delle persone dell'account, per scegliere chi aggiungere (UC 0100).

## 4. Flusso principale

1. La persona apre l'applicazione e la voce «Utenti» del menu dell'applicazione.
2. Vede l'**elenco di chi ha accesso**: nome, email, ruolo su questa applicazione, e l'**owner sempre in
   testa**, marcato come titolare dell'account e non modificabile.
3. Se è owner o `admin`, vede il comando **«Aggiungi utente»**: sceglie fra le persone dell'account che
   **non** hanno ancora accesso, con la loro email, e assegna il ruolo. Chi non ha il potere vede il comando
   **disabilitato con la spiegazione** (regola di UC 0101).
4. Sulle righe, owner e `admin` possono **cambiare il ruolo** (selettore) e **rimuovere l'accesso**
   (conferma esplicita).
5. Ogni operazione aggiorna l'elenco e produce la traccia di controllo (UC 0098). Chi perde l'accesso lo
   sente entro pochi secondi (UC 0099).
6. Un riquadro in testa dice, in una riga, che cosa significano i tre ruoli in **questa** applicazione,
   prendendo il testo dal documento delle operazioni dell'applicazione (UC 0101). Non un testo generico:
   quello dell'applicazione che si sta guardando.

## 5. Flussi alternativi / edge / errori

- **Edge — nessuna persona da aggiungere** (tutte già abilitate): il comando spiega la situazione e, per
  l'owner, rimanda a «Members» per invitarne di nuove. Per un `admin` **non** c'è quel rimando: gli si dice
  che le persone nuove le invita il titolare dell'account. È la differenza che il prototipo `admin.html`
  mette in evidenza.
- **Edge — l'ultima persona rimossa**: ammesso. Un'applicazione con solo l'owner è legittima.
- **Errore — rimozione di sé stessi**: consentita a un `admin`? **No**: si blocca, perché produrrebbe una
  perdita di accesso involontaria e irreversibile per quella persona (non potrebbe più rientrare da sola).
  L'owner può farlo su chiunque tranne sé stesso.
- **Errore — tentativo di toccare l'owner**: rifiutato con messaggio chiaro.
- **Errore — ruolo non valido o persona non attiva**: rifiuto tipizzato dal core (UC 0098); l'interfaccia
  mostra il messaggio.
- **Edge — la persona è indicata per la cessazione** (UC 0104): compare con l'etichetta di stato e resta
  gestibile; togliergli l'accesso qui è proprio la via per escluderlo subito.
- **Stati**: caricamento, elenco (mai vuoto: c'è l'owner), errore con possibilità di riprovare, esito
  riuscito.

## 6. Schermate & stati

**Un solo componente condiviso**, non una schermata per applicazione: vive nel pacchetto dei componenti o
nella shell e riceve l'identificativo dell'applicazione e il ruolo di chi guarda. Motivo: quattro copie
della stessa tabella divergerebbero in un mese, e il difetto sarebbe invisibile.

Composizione: intestazione con il nome dell'applicazione e la spiegazione dei tre ruoli; comando
«Aggiungi utente» (attivo, disabilitato con spiegazione, o assente per ambito); tabella con nome, email,
ruolo (selettore o testo), azione di rimozione.

Testi nelle cinque lingue. Ogni comando raggiungibile da tastiera; il selettore di ruolo con etichetta
esplicita per gli strumenti di assistenza, come già si fa nella schermata dei membri attuale.

## 7. Dati toccati

- **`platform.app_access`**: letta e scritta (UC 0098).
- **`platform.membership` ⋈ `platform.identity`** (UC 0116): lette per comporre l'elenco delle persone aggiungibili — si parte dalle appartenenze dell'account e si prendono **solo** indirizzo e nome dall'identità, il
  minimo necessario.
- **Ritiro dei posti del Mini-CRM**: la tabella `app_crm.seat`, il suo varco e la sua schermata vanno
  **rimossi**, sostituiti dal meccanismo di piattaforma. La quota `seats` del suo listino perde significato:
  o si toglie, o si trasforma in un limite diverso. Va deciso in implementazione con chi cura i prezzi, e va
  fatto **nella stessa change**, altrimenti restano due contatori dello stesso concetto.
- **Dati personali**: email e nome delle persone dell'account, **già dichiarati** (UC 0013). Nessun nuovo
  trattamento: l'elenco è una vista su dati già trattati, entro lo stesso account, col minimo dei campi.

## 8. Permessi & gate

- **Owner e `admin` scrivono; `editor` e `viewer` leggono.** Presidio nel core (UC 0098), riflesso
  nell'interfaccia.
- **L'`admin` è circoscritto alla sua applicazione**: non può usare questa schermata per toccare un'altra
  applicazione.
- **Nessuno può invitare persone nuove da qui.** È il confine economico dell'intero modello e va reso
  evidente nel testo, non solo nel codice.
- **Account solo dal token verificato**; l'elenco delle persone aggiungibili è sempre dentro l'account.

## 9. Requisiti di test

- **Componente, per i quattro ruoli**: chi vede il comando attivo, chi disabilitato, chi non lo vede;
  chi può cambiare i ruoli; l'owner sempre in testa e non modificabile.
- **Integrazione**: un `admin` aggiunge una persona esistente e ne cambia il ruolo; un `editor` che chiama
  le stesse operazioni riceve un rifiuto; un `admin` di un'altra applicazione riceve un rifiuto.
- **Integrazione**: rimozione di sé stessi bloccata; owner non toccabile.
- **Ritiro dei posti del Mini-CRM**: le sue vecchie chiamate non esistono più; nessun riferimento residuo
  nel frontend; la sua quota è coerente.
- **Percorso di piattaforma**: è il cuore del percorso «stessa applicazione vista dai quattro ruoli»
  (UC 0113).

## 10. Riferimenti & Definition of Done

- **Riferimenti**: [MembersScreen.tsx del Mini-CRM](../../../../frontend/apps/backoffice/src/modules/crm/screens/MembersScreen.tsx)
  come antenato da generalizzare e poi ritirare;
  [SeatResource.java](../../../../services/crm/src/main/java/app/appgrove/crm/SeatResource.java);
  [UC 0054](../../11-apps/0054-app2-b2b-via-new-application.md); i quattro prototipi.
- **Definition of Done**:
  1. esiste **un** componente condiviso, usato da tutte le applicazioni;
  2. i quattro ruoli si comportano come da tabella, provato per ognuno;
  3. i posti locali del Mini-CRM sono ritirati nella stessa change, senza doppio conteggio;
  4. la spiegazione dei ruoli è quella dell'applicazione che si guarda;
  5. le cinque lingue sono complete;
  6. `run-tests.sh frontend backend` verde più il percorso di piattaforma.

## Punti aperti / decisioni differite

- **Ruolo predefinito all'aggiunta**: proposto `viewer`, il più prudente. Da confermare al primo uso reale.
- **Sorte della quota `seats` nel listino del Mini-CRM**: da decidere con chi cura i prezzi
  contestualmente all'implementazione (togliere il limite o sostituirlo con un limite sui contatti).
  Proprietario: questa storia, in accordo con UC 0047.
- **Dove vive il componente condiviso** (pacchetto dei componenti o shell): decisione di implementazione;
  la seconda è più semplice, la prima più riusabile. Proprietario: questa storia.
- **Aggiunta di più persone in una volta**: comoda per un'applicazione con molti utenti. Rimandata.

### Lasciato da UC 0098 (change 0091)

- **Il ruolo predefinito quando si concede un accesso** è una scelta dell'**interfaccia**, non del servizio:
  il servizio pretende un valore esplicito, perché un potere concesso per omissione di un campo è il modo
  peggiore di concederlo. Il valore prudente da proporre resta `viewer`, e la conferma è di questa storia.
- **Le operazioni di rete esistono già** (`/api/platform/v1/apps/{appId}/access`: elenco, concessione,
  cambio di ruolo, revoca) e nessuna schermata le consuma: sono il contratto su cui questa storia
  costruisce. L'elenco **aggiunge l'owner in testa** con `implicit: true`, perché non ha righe proprie —
  è il costo dell'accesso implicito, ed è già pagato dal servizio.
- **L'elenco espone indirizzo e nome** delle persone abilitate anche a chi ha ruolo `viewer` su quella
  applicazione: sono i colleghi del proprio gruppo di lavoro, e un elenco di accessi senza nomi sarebbe
  inutilizzabile. Se questa storia volesse restringerlo (per esempio mostrando i nomi solo a chi governa
  gli accessi), è la sua schermata a stabilirlo.

### Lasciato da UC 0099 (change 0092)

- **Nel Mini-CRM convivono DUE varchi delle persone**, e la fine della convivenza è di questa storia. Il
  varco nuovo è quello di piattaforma (`@RequiresAppRole` in `commons`: letture `viewer`, scritture
  `editor` su contatti e interazioni); quello vecchio è il **posto** (`SeatAccess` + tabella `seat` +
  `SeatResource`), il meccanismo che quell'applicazione si era costruita da sé. Due varchi contemporanei
  sono accettabili per una change, **non per due**: quando questa storia rifà la schermata di gestione delle
  persone dentro l'applicazione, il posto va **ritirato** — varco, tabella, API e schermata — e non
  affiancato una terza volta. Conseguenza pratica da non dimenticare: oggi, per usare il Mini-CRM, servono
  *entrambi* (un posto **e** un ruolo).
- **Il campo «Identificativo utente» della schermata dei posti è inusabile, e va via col posto.** Trovato
  il 2026-08-21 durante il collaudo manuale della change 0092: la schermata «Membri» del Mini-CRM chiede a
  mano un identificativo che è il `sub` del token (`seed-acme-owner`, …), mentre chi guarda ha davanti un
  elenco di persone con nome e indirizzo. Chi ha provato ha scritto l'indirizzo email — la sola cosa che
  conosceva — il posto è stato creato, **e non serviva a nulla**: il server confronta quel valore col `sub`
  di chi chiede, quindi ogni operazione rispondeva `403` «nessun posto assegnato», per un posto che
  nell'elenco risultava assegnato. Un campo che accetta in silenzio un valore che non potrà mai combaciare
  è peggio di un campo che rifiuta. Non lo si corregge dov'è, perché la schermata e il posto sono ciò che
  questa storia **ritira**: la sostituta deve far **scegliere** la persona da un elenco (nome + indirizzo)
  e mandare al server l'identificativo dell'identità, mai farlo digitare.
- **Il ruolo della persona è già disponibile all'interfaccia**: la lettura
  `GET /api/platform/v1/me/app-access` restituisce `appId`, `appSlug`, `appName` e `role`, e i rifiuti del
  varco portano identificativi stabili (`urn:appgrove:app-role:no-access`, `:insufficient`, `:unavailable`)
  più i campi `requiredRole` e `role`. La schermata può quindi disabilitare i comandi invece di far
  scoprire il rifiuto premendo un pulsante.
