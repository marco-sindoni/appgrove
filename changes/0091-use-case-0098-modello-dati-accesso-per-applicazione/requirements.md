# Change 0091: Modello dati dell'accesso per applicazione e ruolo di piattaforma a due valori

**Branch**: `change/0091-use-case-0098-modello-dati-accesso-per-applicazione`
**Aree**: `services/core`, `frontend/` (conseguenza minima), `dev/seed`, `docs/` (manifesto dati, registro dei trattamenti, copertura end-to-end)
**Data**: 2026-08-21
**Autore**: Platform Engineering (modalità fast, orchestrata da `go-fast`)
**Use case sorgente**: [docs/usecases/22-refactor-membership-model/story/0098-modello-dati-accesso-per-applicazione.md](../../docs/usecases/22-refactor-membership-model/story/0098-modello-dati-accesso-per-applicazione.md)
**Tocca dati personali?**: Sì — classificazione **MINORE**. Nessuna finalità nuova, nessuna base giuridica nuova, nessun responsabile esterno nuovo, nessuna categoria particolare. Un solo dato nuovo dichiarato: il riferimento all'identità dentro la riga di accesso.

## Problema / Obiettivo

Oggi non esiste un posto dove scrivere la frase **«questa persona può usare questa applicazione con questo
ruolo»**. Il ruolo vive sull'appartenenza all'account (`owner` · `admin` · `member`) e vale per *tutto*: chi è
`admin` dell'account lo è di ogni applicazione e anche delle schermate di piattaforma, chi è `member` non lo è
di nulla. Il Mini-CRM ha dovuto inventarsi una tabella di «posti» locale per dire chi può entrare — cioè la
domanda esisteva già e ha trovato una risposta privata, non condivisa.

Questa change crea quel posto — `platform.app_access`, una riga per la terna (account, applicazione, persona)
con il suo ruolo — e **riduce il ruolo di piattaforma ai due soli valori che hanno senso** in un modello dove
il potere sta sull'applicazione: `owner` (chi possiede l'account) e `member` (tutti gli altri). Il valore
`admin` scompare da quel livello e riappare, molto più circoscritto, sull'applicazione.

Osservabile a fine change: si concede, si cambia e si revoca l'accesso di una persona a una applicazione
attraverso il core, con la traccia di controllo; l'owner ce l'ha implicito e nessuno può togliergliela;
l'ultimo owner non è rimovibile, retrocedibile né sospendibile **dal servizio** e non solo dall'interfaccia.

## Scope

**Banca dati** — nuova tabella `platform.app_access` (prima migrazione libera: `V20`), separata per account
(porta `tenant_id`, invariante 2), con riferimento all'**identità** della persona (non all'appartenenza:
l'appartenenza cambia nel tempo, l'identità no) e all'applicazione di catalogo. Vincoli: unicità della terna
sulle **righe vive**, controllo sui valori del ruolo di applicazione, indici per le due domande che servono
(«quali applicazioni vede questa persona?», «chi ha accesso a questa applicazione?»).

**Modello e regole** — entità e repository dell'accesso; enumerazione del ruolo di applicazione (`viewer` ·
`editor` · `admin`) con l'ordinamento fra i tre valori scritto **una** volta; le regole di chi-può-cosa come
funzione pura, senza accesso alla banca dati, così che siano collaudabili in isolamento e non ripetute in ogni
operazione.

**Interfaccia del core** — leggere chi ha accesso a una applicazione, concedere, cambiare ruolo, revocare.
Ogni operazione verifica, nell'ordine: che chi chiede ne abbia il diritto (owner su tutto, `admin` **solo**
sulla propria applicazione), che la persona bersaglio appartenga allo stesso account e sia attiva, che
l'account abbia **diritto** a quella applicazione. Traccia di controllo con soli identificativi opachi.

**Ruolo di piattaforma a due valori** — l'enumerazione ammette `owner` e `member`. Di conseguenza smettono di
offrire e di accettare il valore `admin`: l'invito, il cambio di ruolo della persona, il seme di sviluppo e i
due selettori dell'interfaccia dei membri (che diventano, rispettivamente, un invito senza scelta di ruolo e
una etichetta in sola lettura).

**Vincolo dell'ultimo owner nel servizio** — rimozione, retrocessione e sospensione dell'ultimo owner
diventano rifiuti tipizzati del servizio, non soltanto comandi disabilitati nell'interfaccia.

**Conformità** — la nuova tabella è dichiarata nel manifesto dei dati come dato di **autorizzazione**, entra
nell'esportazione dell'account e nella cancellazione dell'account; registro dei trattamenti rigenerato.

## Fuori scope

- **Come i servizi delle applicazioni fanno rispettare il ruolo** — il varco riusabile in `services/commons`,
  la copia locale con invalidazione a eventi, e l'**uscita del ruolo dal token**: UC 0099, storia successiva.
  Qui i punti di scrittura sono pronti e commentati, ma nessun evento viene emesso.
- **Il claim dei ruoli del token e le annotazioni che lo leggono** (`@RolesAllowed({owner, admin})`): restano
  come sono. La riduzione del claim è di UC 0099; la tolleranza dei token già emessi che portano ancora
  `admin` è di UC 0113 §6. Poiché nessuna appartenenza produce più quel valore, la restrizione avviene per
  costruzione senza riscrivere l'autorizzazione di venti operazioni mentre la storia dopo la rifà.
- **La conversione dei dati esistenti** (`admin` → `member` + accesso `admin` su ogni applicazione
  dell'account) e il vincolo di controllo sui valori del ruolo di piattaforma: UC 0113. Il seme di sviluppo è
  invece aggiornato, perché è un dato di sviluppo che deve dire la verità sul modello del giorno.
- **Il significato di comportamento dei tre ruoli di applicazione** (cosa può fare un `editor`): UC 0101.
- **Le schermate**: l'elenco unico dei membri senza ruolo → UC 0100; la gestione utenti dentro
  l'applicazione, con il suo ruolo predefinito → UC 0111; la visibilità delle voci di menu → UC 0107.

## Criteri di accettazione

- [ ] `platform.app_access` esiste con unicità della terna sulle righe vive, i due indici di lettura e il
      controllo sui valori del ruolo; la migrazione si applica su banca dati vuota e su banca dati esistente.
- [ ] Il ruolo di piattaforma ammette **due soli valori**: una richiesta che tenta di invitare o di portare
      una persona ad `admin` è rifiutata, e nessuna superficie lo offre più.
- [ ] Si concede, si cambia e si revoca l'accesso attraverso il core; un `admin` di una applicazione opera
      **solo** su quella e riceve un rifiuto sulle altre; un `editor`, un `viewer` e una persona esterna
      all'account non scrivono nulla.
- [ ] La persona di un altro account non si concede e non si legge: risposta «non trovato», non «vietato».
- [ ] La persona non attiva (sospesa, o con invito ancora in attesa) non riceve accesso.
- [ ] L'applicazione a cui l'account non ha diritto non riceve accesso.
- [ ] L'owner ha accesso implicito su ogni applicazione dell'account, compare in testa all'elenco di chi ha
      accesso pur senza riga propria, e ogni tentativo di concedergli, cambiargli o revocargli l'accesso è
      rifiutato.
- [ ] L'ultimo owner non è rimovibile, retrocedibile né sospendibile: il rifiuto arriva dal servizio.
- [ ] Due concessioni simultanee sulla stessa terna producono **una** sola riga.
- [ ] Il manifesto dei dati dichiara la nuova tabella; esportazione e cancellazione dell'account la
      comprendono; il registro dei trattamenti è rigenerato.
- [ ] `./run-tests.sh` (suite completa) verde.

## Invarianti appgrove toccati

- **Account solo dal token verificato** — l'account della riga di accesso è sempre il claim verificato. Vale
  anche per la persona bersaglio: si accetta solo se ha un'**appartenenza viva a quell'account**, letta dal
  modello; l'identificativo che arriva dal chiamante non è mai una prova di appartenenza.
- **Filtro riga per riga** — `app_access` è entità di account: il filtro `WHERE tenant_id = ?` è automatico
  (discriminatore), da non riscrivere a mano. Nessuna lettura trasversale agli account è introdotta.
- **Logging strutturato** — ogni operazione registra account, applicazione e persona; la traccia di controllo
  porta soli identificativi opachi, mai indirizzo né nome.
- **Modulo Terraform `microsaas_app`** — non toccato (nessuna infrastruttura nuova).

## Requisiti di test

- **Unità**: le regole di chi-può-cosa come funzione pura — owner, `admin` della stessa applicazione, `admin`
  di un'altra, `editor`, `viewer`, persona esterna; l'ordinamento fra i tre ruoli di applicazione.
- **Integrazione con banca dati reale**: concessione, cambio, revoca; unicità della terna; rifiuto per persona
  non attiva; rifiuto per applicazione senza diritto; l'owner in testa all'elenco di una applicazione appena
  installata; i rifiuti sull'owner; il vincolo dell'ultimo owner.
- **Separazione fra account**: prova dedicata dentro `MultiTenancyTest` — un owner dell'account A non concede
  né legge accessi dell'account B, e una persona dell'account B non è nemmeno visibile.
- **Concorrenza**: due concessioni simultanee sulla stessa terna → una sola riga.
- **Percorsi end-to-end**: **nessuno proprio** — storia senza superficie. Nel registro di copertura la voce
  0098 passa da esenzione `non-implementato` a `senza-superficie`. I percorsi esistenti che esercitavano il
  cambio del ruolo di piattaforma vanno adeguati: quel comportamento non esiste più.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | Sì — il valore `admin` del ruolo di piattaforma non è più accettato da invito e cambio di ruolo (restrizione, non allargamento) |
| Contratto cross-area | Sì — nuove operazioni di piattaforma per gli accessi; l'interfaccia dei membri smette di offrire il ruolo |
| Version bump | minor |
