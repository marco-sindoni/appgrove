# Change 0092: Autorizzazione per applicazione — varco riusabile in `commons`, ruolo fuori dal token

**Branch**: `change/0092-use-case-0099-autorizzazione-per-applicazione`
**Aree**: `services/commons`, `services/core`, `services/crm`, `services/fatture`, `services/auth`, `infra/`, `tools/new-application`, `docs/`
**Data**: 2026-08-21
**Autore**: Platform Engineering (modalità fast, orchestrata da `go-fast`)
**Use case sorgente**: [`docs/usecases/22-refactor-membership-model/story/0099-autorizzazione-per-applicazione.md`](../../docs/usecases/22-refactor-membership-model/story/0099-autorizzazione-per-applicazione.md)
**Tocca dati personali?**: Sì (in senso minore) — la copia locale del ruolo dentro ogni servizio di
applicazione contiene l'identificativo di autenticazione della persona (nessun nome, nessun indirizzo). È
la copia di un dato già dichiarato nel manifesto della piattaforma (`app_access.identityId`, change 0091);
non nasce alcuna finalità nuova. Classificazione **MINORE**, motivata nel registro delle decisioni; la
copia viene cancellata fisicamente con l'account quando si esercita il diritto di cancellazione.

## Problema / Obiettivo

Oggi il ruolo di una persona **su una applicazione** esiste nel modello dati (`platform.app_access`,
UC 0098) ma **nessuno lo rispetta**: non c'è modo per un servizio di applicazione di saperlo, e chi ha
provato a fare da sé si è costruito un meccanismo proprio — il Mini-CRM ha la propria tabella dei posti e
il proprio varco `SeatAccess`, diverso da qualunque altro. È il difetto che l'epica 22 esiste per chiudere:
ogni applicazione nuova reinventa l'autorizzazione in modo un po' diverso, e le differenze si scoprono a
incidente avvenuto.

Obiettivo osservabile, a change conclusa:

1. il token porta **un ruolo in meno** — solo il ruolo di piattaforma (`owner` o `member`, più
   `platform-admin` per chi amministra la piattaforma), in cloud e in locale, con gli stessi claim;
2. esiste **una** lettura che dice «dove posso entrare, e con che ruolo»;
3. esiste **un** varco dichiarativo in `services/commons` che un'operazione usa scrivendo il ruolo minimo
   richiesto e **nessuna** logica di autorizzazione;
4. un cambio di ruolo deciso nel core **si sente** nel servizio dell'applicazione entro pochi secondi,
   senza che la persona rientri;
5. in assenza di informazione **si nega**, con un messaggio che dice «guasto nostro» e non «permesso
   negato».

## Scope

**`services/commons` — il varco condiviso** (il cuore della change):

- l'enumerazione del ruolo di applicazione e il suo **ordinamento** (`viewer` < `editor` < `admin`)
  vivono qui, in un solo posto, usati sia dal core sia dai servizi delle applicazioni;
- un'**annotazione** che dichiara il ruolo minimo di un'operazione (o di un'intera risorsa) e un
  **filtro** che la interpreta prima che l'operazione parta. L'operazione non confronta ruoli;
- il **servizio di lettura del ruolo**: risponde dalla **copia locale** del servizio; se non ce l'ha o è
  **scaduta**, la chiede al core e la conserva. Con un modo esplicito di **saltare la copia** per le
  operazioni irreversibili;
- la **copia locale**: stessa forma di quella già in uso per i diritti d'accesso (tabella nello schema del
  servizio, marcata «da rinfrescare» a evento, non cancellata), indicizzata sull'**identificativo di
  autenticazione** della persona — nessun nome, nessun indirizzo;
- i **rifiuti tipizzati**: nessun accesso, ruolo insufficiente, informazione non disponibile — tre casi
  distinti, tre messaggi distinti, identificativi stabili nel corpo dell'errore;
- l'**invalidazione a eventi**: un solo canale per servizio, che marca da rinfrescare **tutte** le copie
  locali di quel servizio (diritti d'accesso e ruoli), e la **cancellazione** di tutte quelle copie quando
  un account esercita il diritto di cancellazione.

**`services/core` — la fonte di verità**:

- la lettura `GET /api/platform/v1/me/app-access`: per la persona che chiama, l'elenco delle applicazioni
  in cui può entrare con il ruolo che ha. Solo le applicazioni che hanno **insieme** il diritto dell'account
  e l'accesso della persona; per l'owner, tutte quelle con diritto, col ruolo massimo;
- i tre punti che scrivono l'accesso (concessione, cambio di ruolo, revoca) e l'uscita di una persona
  dall'account **emettono** l'evento di invalidazione: sono i punti che UC 0098 ha lasciato marcati.

**`services/auth` e `infra/` — il token**:

- il claim dei ruoli non può più contenere `admin` come ruolo di piattaforma: dove il valore fosse ancora
  scritto nei dati (la conversione è di UC 0113) viene **letto come `member`**, sia nella funzione che
  compone il token in cloud sia nel fornitore di identità locale, con lo stesso comportamento.

**`services/crm` — la prima applicazione che lo usa**:

- le operazioni su contatti e interazioni dichiarano il ruolo minimo: **letture `viewer`**, **scritture
  `editor`**. Il varco nuovo si aggiunge **accanto** a quello dei posti, che resta.

**`services/fatture` e i modelli di `tools/new-application`**:

- la tabella della copia locale e la configurazione, così che il varco sia **disponibile** in ogni
  applicazione (esistente e futura) invece di essere un privilegio della prima che lo ha chiesto.

## Fuori scope

- **Il significato di comportamento dei tre ruoli** — cosa esattamente può fare un `editor` in una
  applicazione qualunque: è il contratto di **UC 0101**. Qui c'è solo l'ordine e il confronto.
- **Le schermate**: il menu laterale e le rotte per ruolo sono di **UC 0107**, le schermate di gestione
  dentro le applicazioni di **UC 0111**, l'elenco unico dei membri di **UC 0100**. Nessuna riga di frontend
  in questa change.
- **Il ritiro dei posti del Mini-CRM** (tabella, API, schermata) → **UC 0111**. Qui il varco nuovo convive
  con quello vecchio.
- **La conversione dei dati reali `admin` → `member`** e il **ritiro della tolleranza** `Roles.ADMIN` con la
  sua data → **UC 0113**. Qui si smette di *emettere* quel valore, non di *accettarlo*.
- **L'esclusione della voce di catalogo di piattaforma dei posti** dalla lettura «dove posso entrare»: quella
  voce non esiste ancora e nessun attributo la distingue: la crea **UC 0103**, che deve escluderla nello
  stesso momento (rimando scritto là e commento nel punto esatto del codice).
- **Misure e allarmi** dedicati alla copia locale del ruolo (le copie dei diritti d'accesso ne hanno):
  rimandati, con rimando scritto nei punti aperti della storia. Qui restano i log strutturati.
- **L'annotazione delle rotte di `fatture`** col ruolo minimo: la storia chiede *una* prima applicazione.

## Criteri di accettazione

- [ ] Un'operazione di un servizio di applicazione dichiara il ruolo minimo con una **annotazione** e non
      contiene alcun confronto fra ruoli; il filtro condiviso decide al posto suo.
- [ ] Con ruolo **sufficiente** l'operazione procede; con ruolo **insufficiente** riceve un rifiuto che
      **nomina** il ruolo richiesto; **senza alcun accesso** riceve un rifiuto diverso, che dice che serve
      l'abilitazione dell'owner o di un `admin` dell'applicazione.
- [ ] Copia locale assente **e** core non raggiungibile → si **nega**, con il messaggio del guasto e non
      quello del permesso negato (codice di stato distinto).
- [ ] Cambiato il ruolo nel core, il servizio dell'applicazione applica il **nuovo** ruolo dopo il consumo
      dell'evento, senza che la persona rientri; la copia vecchia non sopravvive.
- [ ] Scaduta la durata massima, la copia viene rinfrescata anche **senza** evento.
- [ ] `GET /api/platform/v1/me/app-access` restituisce solo le applicazioni con diritto dell'account **e**
      accesso della persona; l'owner le vede tutte col ruolo massimo; una persona di un altro account non
      vede nulla di questo account.
- [ ] Il token di una persona la cui appartenenza vale ancora `admin` porta `member` nel claim dei ruoli, in
      cloud e in locale, con lo stesso esito.
- [ ] La copia locale del ruolo viene **cancellata** quando l'account esercita il diritto di cancellazione, e
      il conteggio entra nella traccia di controllo che lo prova.
- [ ] `./run-tests.sh` (suite completa) verde.

## Invarianti appgrove toccati

- **Account solo dal token verificato** — la copia locale è indicizzata per account **e** per persona, e
  l'account arriva sempre dal token verificato: né la lettura del core né il filtro accettano un
  identificativo di persona o di account da parametro o da corpo della richiesta. La nuova lettura del core
  non ha parametri: dice dove può entrare **chi chiama**.
- **Filtro per account su ogni lettura** — ogni interrogazione della copia locale porta l'account nella
  condizione, come la copia dei diritti d'accesso; le letture del core restano sull'entità con
  discriminatore automatico.
- **Modulo Terraform `microsaas_app`** — nessuna infrastruttura nuova: si riusa la coda già dichiarata dal
  modulo per l'invalidazione dei diritti d'accesso (una coda per servizio), quindi il modulo non cambia e
  ogni applicazione nuova eredita il canale senza righe in più.
- **Logging strutturato** — ogni decisione del varco che nega, e ogni invalidazione consumata, lascia una
  riga con account, applicazione e persona.

## Requisiti di test

- **Unità** — il confronto fra ruolo posseduto e ruolo richiesto, compreso l'ordinamento completo e la
  posizione dell'owner sopra tutti; la **scadenza** della copia locale; la normalizzazione del ruolo di
  piattaforma per il claim (`admin` → `member`), in Java **e** in Python.
- **Integrazione nel core** — contenuto della lettura «dove posso entrare» per owner e per collaboratore;
  esclusione delle applicazioni senza diritto; separazione fra account.
- **Integrazione in un servizio di applicazione** — i tre esiti del varco (passa / ruolo insufficiente /
  nessun accesso) su una lettura e su una scrittura.
- **Invalidazione** — cambiato il ruolo nel core e consumato l'evento, il servizio applica il ruolo nuovo:
  prova esplicita che la copia vecchia non sopravvive.
- **Fallimento chiuso** — copia assente e core non raggiungibile → rifiuto con il codice del guasto.
- **Cancellazione** — la purga per account cancella anche la copia locale del ruolo e lo dichiara nella
  traccia di controllo.
- **Percorsi end-to-end** — nessuno: la storia non aggiunge alcun comando visibile. La voce del registro di
  copertura passa da `non-implementato` a `senza-superficie`.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No — il varco è opt-in per annotazione; il claim perde un valore che nessuna appartenenza produce più (la tolleranza dei token già emessi resta) |
| Contratto cross-area | Sì — nuova lettura `GET /api/platform/v1/me/app-access` (core → servizi delle applicazioni, e domani → frontend con UC 0107) |
| Version bump | minor |
