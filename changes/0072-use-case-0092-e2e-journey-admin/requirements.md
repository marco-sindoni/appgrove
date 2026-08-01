# Change 0072: Batteria journey end-to-end lato amministratore + guasti di piattaforma

**Branch**: `change/0072-use-case-0092-e2e-journey-admin`
**Aree**: `tools/platform-e2e` (suite end-to-end di piattaforma) · `docs/usecases` (indici e rimandi)
**Data**: 2026-08-01
**Autore**: Platform Engineering (modalità fast, orchestrata da `go-fast`)
**Use case sorgente**: [`docs/usecases/20-test-e2e-piattaforma/0092-e2e-platform-journey-admin.md`](../../docs/usecases/20-test-e2e-piattaforma/0092-e2e-platform-journey-admin.md)
**Tocca dati personali?**: No — la suite crea soltanto identità sintetiche su dominio non recapitabile
(`test.appgrove.local`), come già fanno UC 0090 e UC 0091. Nessun trattamento nuovo, nessun manifesto dati da
aggiornare, nessuna versione di informativa da far avanzare.

## Problema / Obiettivo

La suite end-to-end di piattaforma copre oggi i percorsi **dell'utente** (nove journey, UC 0090 + UC 0091).
Manca completamente l'altra metà: quello che fa **l'amministratore di piattaforma** dalla propria console, e
quello che l'utente **vede quando qualcosa si rompe davvero**.

Sono due vuoti con conseguenze concrete e diverse fra loro:

1. **Effetti che attraversano due attori.** Quando l'amministratore disabilita un'applicazione, o applica una
   limitazione del trattamento, l'effetto non si vede dove l'azione è stata compiuta: si vede *dall'altra
   parte*, nella sessione del cliente. Nessun collaudo automatico osserva oggi le due parti nello stesso
   momento, quindi una rottura di quel legame — l'azione dell'amministratore che non arriva al cliente, o che
   arriva al cliente sbagliato — passerebbe inosservata fino alla segnalazione di un utente.
2. **Guasti prodotti sul serio.** I collaudi con backend simulato sanno fingere un errore, ma non sanno
   fermare un servizio. La regola più delicata della piattaforma — «un guasto di lettura non deve mai
   presentarsi all'utente come mancanza di diritto» (use case 0077) — è proprio quella che una simulazione
   verifica peggio: è nell'insieme di stati reali che nasce la confusione fra "non riesco a leggere" e "non
   hai comprato".

Obiettivo: al termine, `./run-tests.sh platform` dà quattro certezze in più, tutte osservate su stack vero, e
la batteria resta **integralmente ripristinata** dopo essersi rotta da sola.

## Scope

Quattro journey nuovi in `tools/platform-e2e/journeys/`, con identificatori stabili (sono l'ingresso del
registro di copertura, UC 0093): **`A-CONSOLE`**, **`A-GDPR`**, **`A-ENTITLE`**, **`F-DEGRADE`**.

### A-CONSOLE — console di piattaforma e disabilitazione applicazione (fra due attori)

Due sessioni browser isolate nello stesso collaudo: il **cliente** sul backoffice, l'**amministratore** sulla
console admin, ciascuno con i propri cookie e la propria memoria.

- Il cliente parte da un'applicazione attivata e funzionante, con un dato applicativo suo dentro.
- L'amministratore entra nella console (utenza `platform-admin` del seed), vede gli indicatori di sintesi
  della pagina iniziale, raggiunge il catalogo applicazioni e **disabilita** l'applicazione passando dal
  dialogo di conferma, con la motivazione facoltativa compilata.
- Lo stato mostrato in console dopo l'azione è l'**etichetta tradotta** («Disabilitata» / «Disabled»), non il
  valore grezzo del campo: è ciò che l'operatore legge davvero (regola fissata da UC 0076).
- Dalla parte del cliente: l'applicazione **sparisce** dal menu laterale e la sua rotta **nega l'accesso** —
  qui il diniego è corretto, perché il diritto è realmente venuto meno. I **dati non sono cancellati**:
  verificato leggendo il database, non l'interfaccia.
- L'amministratore **riabilita**: il cliente ritrova l'applicazione **e il dato di prima**.
- Il **registro delle transizioni** contiene entrambe le azioni, con operatore e motivazione, sia nel
  database sia nella tabella della console.

**Freschezza del menu laterale.** L'aggiornamento lato cliente è osservato dopo un ricaricamento della pagina,
che è una delle vie di aggiornamento previste da UC 0077. La via basata sul *ritorno sulla scheda del browser*
non è osservata qui: in un browser senza finestre non è pilotabile in modo deterministico, e ha già la propria
copertura fra i collaudi del frontend. Un journey che passa "quasi sempre" è un difetto, non una copertura.

### A-GDPR — console Diritti GDPR da un capo all'altro

- Il cliente apre un **ticket privacy** dalla pagina Supporto e avvia un **export** dei propri dati.
- La console dell'amministratore **aggrega** entrambe le richieste; l'export vi compare con la prova di
  completamento (stato concluso e data), il ticket con il suo oggetto.
- L'amministratore **risponde nel filo di conversazione** del ticket; il cliente **vede la risposta** nella
  sua sessione.
- L'amministratore applica la **limitazione del trattamento (art. 18)** con conferma, sull'**utente** che ha
  aperto il ticket.
- Effetto osservabile nella sessione del cliente: alla ripresa della sessione l'accesso è **chiuso in modo
  pulito** (si torna alla pagina di accesso) e un nuovo tentativo di accesso viene **rifiutato con il
  messaggio dedicato all'utenza sospesa**.
- L'amministratore **rimuove** la limitazione: il cliente può rientrare. La reversibilità è parte della
  promessa della funzionalità e va vista, non dedotta.
- Verifiche sul database: il **registro delle limitazioni** contiene applicazione e rimozione con l'attore;
  ticket ed export appartengono **soltanto** al tenant giusto (un secondo tenant "canarino", creato dal
  journey, non deve comparire da nessuna parte).

### A-ENTITLE — coerenza della matrice dei diritti d'accesso

Tre tenant portati in tre stati diversi, tutti per vie reali:

1. uno con **acquisto attivo** (checkout vero + evento di attivazione firmato);
2. uno con la **sola fascia gratuita di base** (nessun acquisto);
3. uno **in eliminazione**, ottenuto con la richiesta di cancellazione dell'account che l'utente stesso può
   fare dalla propria pagina dei dati — nessuna leva artificiosa serve.

Per ciascuno, l'insieme delle applicazioni con diritto d'accesso letto nella **matrice della console** deve
coincidere **esattamente** con quello che il tenant vede nel proprio menu laterale. È il confronto che UC 0077
rende possibile — regola unica di accesso condivisa fra le due viste — e che qui viene messo alla prova con
tenant veri invece che con un ragionamento.

### F-DEGRADE — guasti di piattaforma visti dall'utente

- Con il cliente autenticato e l'applicazione funzionante, l'orchestratore **ferma davvero** il servizio che
  risponde per i diritti d'accesso. Non una risposta finta: il processo non c'è più.
- Il menu laterale mostra **l'errore con la possibilità di riprovare**, mai «nessuna applicazione» e mai un
  diniego d'accesso.
- La rotta di un modulo mostra **l'errore**, non la pagina di accesso negato: è la distinzione fra guasto e
  diniego che UC 0077 ha introdotto e che questa batteria deve difendere.
- Il servizio viene **riavviato**; l'utente preme **riprova** e la shell torna normale **senza ricaricare la
  pagina**.
- **Variante sessione**: con la sessione **invalidata dal lato server**, la ripresa fallisce e l'applicazione
  esce **pulita** verso la pagina di accesso — una sola volta, senza rimbalzi, senza restare appesa a
  «ripristino sessione in corso».
- A fine journey **tutti i servizi sono di nuovo su**, qualunque sia stato l'esito: il ripristino non dipende
  dal fatto che il collaudo sia andato bene.

### Infrastruttura della suite

- L'orchestratore espone una **leva di controllo dei servizi** (ferma / avvia / stato) utilizzabile dai
  journey. Ciò che serve per riavviare un servizio identico a com'era — porta, registro, variabili d'ambiente
  — è **derivato dalla stessa scoperta automatica** che avvia lo stack, non riscritto a mano: una nuova
  applicazione deve restare governabile senza toccare nulla.
- **Ordine di esecuzione**: i journey che modificano stato **globale** (il catalogo delle applicazioni; un
  servizio fermo) non possono girare insieme agli altri. Girano **in coda**, uno alla volta. I journey che
  restano confinati al proprio tenant continuano a girare in parallelo, come oggi.
- La **pulizia finale** della suite verifica che nessun servizio sia rimasto giù e che il catalogo sia tornato
  al suo stato di riposo.
- Il documento della suite (`tools/platform-e2e/README.md`) elenca i nuovi journey, la nuova leva e l'ordine
  di esecuzione, con la sua ragione.

## Fuori scope

- **Nuove funzionalità di prodotto**: nessuna. La batteria collauda ciò che esiste; se qualcosa manca lo si
  annota, non lo si costruisce.
- **Il registro di copertura** e il suo controllo meccanico: sono di UC 0093, di cui gli identificatori
  stabili di questa change sono l'ingresso.
- **La console di assistenza nativa** (UC 0075): quando sostituirà l'attuale flusso di supporto, il journey
  A-GDPR andrà aggiornato — rimando già scritto nei punti aperti di UC 0092.
- **Osservabilità dei guasti** (allarmi, raccolta errori): F-DEGRADE osserva l'esperienza dell'utente e basta;
  le verifiche su allarmi e telemetria appartengono all'area osservabilità (#08).
- **Rimozione dei collaudi di livello 2 esistenti** della console admin: restano: sono un percorso felice
  minimo su backend simulato e coprono stati che uno stack vero non sa produrre (export fallito, collegamenti
  alle console del fornitore cloud). Il criterio di riparto di UC 0091 §1 li ammette esplicitamente.

## Criteri di accettazione

- [ ] I quattro journey `A-CONSOLE`, `A-GDPR`, `A-ENTITLE`, `F-DEGRADE` esistono, girano dentro
      `./run-tests.sh platform` e sono **verdi al primo tentativo** — nessun journey segnalato instabile.
- [ ] A-CONSOLE osserva l'effetto della disabilitazione **nella sessione del cliente** e la conservazione dei
      dati sul database, e ritrova entrambe le transizioni nel registro; lo stato in console è letto
      sull'etichetta tradotta.
- [ ] A-GDPR percorre ticket, export, risposta visibile al cliente, limitazione applicata **e rimossa**, con
      le prove nel registro e l'isolamento verificato su un tenant canarino.
- [ ] A-ENTITLE dimostra, per tre tenant in tre stati diversi, che la matrice della console e il menu laterale
      del cliente dicono la **stessa cosa**.
- [ ] F-DEGRADE ferma davvero un servizio, verifica che l'utente veda un **errore** e non un diniego, lo
      riavvia e verifica il rientro **senza ricaricare la pagina**; a fine esecuzione **tutti i servizi sono
      su**.
- [ ] Doppia esecuzione consecutiva della suite verde, senza pulizia manuale in mezzo e senza processi
      residui.
- [ ] `./run-tests.sh` completo verde (tutte le aree): nessuna regressione altrove.

## Invarianti appgrove toccati

- **Tenant dal token verificato / filtro per tenant**: la change non introduce superfici di esecuzione — i
  journey *verificano* gli invarianti invece di toccarli. A-CONSOLE e A-GDPR contengono un rilevatore di
  travaso: l'azione dell'amministratore deve colpire il solo tenant bersaglio, e i tenant canarino creati dal
  journey restano intatti.
- **Modulo Terraform `microsaas_app`**: non pertinente (nessuna infrastruttura).
- **Logging strutturato**: non si aggiungono log; A-CONSOLE verifica che l'azione amministrativa lasci la
  propria traccia persistita con applicazione, operatore e motivazione.
- **Avvio locale di nuove applicazioni**: la leva di controllo dei servizi si appoggia alla scoperta
  automatica esistente, quindi una nuova applicazione entra nella batteria senza cablaggi manuali.

## Requisiti di test

- Nessuna attesa a tempo fisso: solo attese su condizione (stato dell'interfaccia, disponibilità del servizio,
  righe sul database), come già impone UC 0090.
- Ogni journey crea da zero i propri tenant; nessuno dipende dallo stato lasciato da un altro.
- F-DEGRADE ripristina il servizio anche quando fallisce a metà.
- Fallimenti parlanti: un servizio che non riparte deve dirlo, non scadere in un timeout anonimo del browser.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | No — nessun contratto fra aree cambia; cambia soltanto l'orchestrazione interna della suite |
| Version bump | nessuno |
