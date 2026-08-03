# 0009 — Modulo pubblico di iscrizione

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 02 — Pubblico e prova del consenso
**Storia**: `0009` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che vuole far crescere la lista dal proprio sito
> voglio un modulo di iscrizione da incorporare, che chieda il consenso nel modo giusto senza che io debba
> sapere come si fa
> così da raccogliere iscritti che sono già in regola nel momento in cui entrano.

**Contesto.** È la porta d'ingresso principale della lista, e la sola che produce da sola una prova completa: il
testo letto, il momento, l'indirizzo di rete, la conferma. La legge chiede che il consenso sia libero, specifico e
informato ([application-description.md](../application-description.md) §2.3 punto 1): «libero» significa che
l'iscrizione non può essere il prezzo di qualcos'altro, «specifico» che il consenso al marketing non si mescola
con altre finalità, «informato» che ci deve essere un'informativa raggiungibile. Il cliente micro non sa niente di
tutto questo, e il valore del prodotto è che non deve saperlo: il modulo **non si può configurare male**, perché
le parti che rendono lecito il consenso non sono opzioni.

## 2. Requisiti funzionali

1. **RF-1** — Il cliente crea un modulo di iscrizione scegliendo i campi (recapito obbligatorio; nome, lingua e
   campi personalizzati facoltativi), il testo del consenso e il collegamento alla **propria** informativa.
2. **RF-2** — Tre elementi sono **obbligatori e non rimovibili**: la casella di consenso **non pre-spuntata**, il
   testo del consenso non vuoto e il collegamento all'informativa del cliente. Senza tutti e tre il modulo non si
   può pubblicare, e il messaggio di errore dice perché.
3. **RF-3** — Il modulo si pubblica in due forme: una pagina ospitata da noi con un indirizzo proprio, e un
   frammento da incorporare nel sito del cliente che mostra il modulo dentro una cornice servita da noi.
4. **RF-4** — L'invio del modulo **non crea mai** un iscritto contattabile: crea una richiesta in
   `in attesa di conferma` e fa partire la doppia conferma (storia 0008).
5. **RF-5** — Ogni invio conserva il **dato grezzo** ricevuto e il testo del consenso **nella versione vigente in
   quel momento**, così che un'iscrizione contestata si possa ricostruire anche dopo che il cliente ha cambiato il
   modulo dieci volte.
6. **RF-6** — Il modulo pubblico è difeso dagli abusi: limite di frequenza per indirizzo di rete, difesa contro
   gli invii automatici che non richieda al visitatore di risolvere enigmi, e nessuna risposta che riveli se un
   recapito è già iscritto.
7. **RF-7** — Il cliente vede, per ogni modulo, quanti invii sono arrivati, quanti sono stati confermati e quanti
   respinti, con i motivi in forma di conteggio.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il modulo pubblico porta una chiave pubblica che identifica il modulo e
  quindi l'account: l'account **non** si legge da un parametro della richiesta né da un campo nascosto. Ogni
  scrittura filtra per il `tenant_id` risolto dalla chiave; nella console del cliente ogni lettura filtra per
  `tenant_id` dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte autenticate
  `GET|POST|PATCH /api/campaigns/v1/subscription-forms` e rotta **pubblica**
  `POST /api/campaigns/v1/public/forms/{publicKey}/submissions`, senza token di accesso, con limite di frequenza
  e risposta sempre identica a prescindere dall'esito. Errori in `application/problem+json`; definizione OpenAPI
  aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabelle `subscription_form` e `form_submission` sullo schema `app_campaigns` con
  `tenant_id`, chiave UUID versione 7, colonne di controllo e cancellazione logica. Il testo del consenso è
  **versionato**: modificarlo crea una versione nuova e non tocca quelle a cui sono agganciate le prove esistenti.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Moduli d'iscrizione» del modulo `campaigns`: elenco, editore con
  anteprima dal vivo, istruzioni per incorporare. La pagina pubblica usa i token del sistema di design, funziona in
  tema chiaro e scuro e non carica **nessuna** risorsa da domini di terzi — né caratteri, né immagini, né
  tracciatori.
- **RT-5 — Cinque lingue (§4).** L'interfaccia di gestione è in `en, it, fr, es, de`. La **pagina pubblica** esce
  nella lingua scelta dal cliente per quel modulo; il **testo del consenso** lo scrive il cliente e non si
  traduce automaticamente — tradurre una dichiarazione altrui significherebbe cambiarne il contenuto.
- **RT-6 — Varchi e quota (§6, §7).** L'invio del modulo non consuma `messages_sent`; lo consuma solo l'eventuale
  messaggio di conferma, che per scelta della storia 0008 è esente. Con abbonamento `canceled` la pagina pubblica
  risponde in modo cortese che le iscrizioni sono sospese e non crea nulla; il piano `free` consente **un** modulo
  attivo, coerentemente con il limite di un dominio mittente (§5 della descrizione).
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento di scrittura: creare o modificare un modulo
  significa fissare il testo di un consenso, che è materia da interfaccia. Lo strumento di lettura
  `elenca_segmenti` non c'entra; se in futuro servirà un `elenca_moduli` sarà di sola lettura. Scelta dichiarata.
- **RT-8 — Dati personali (§10).** Voce `form_submission.payload` del manifesto in italiano e inglese: dato
  grezzo di chiunque compili il modulo pubblico, finalità «ricostruire un'iscrizione contestata», base giuridica
  «prova», conservazione proposta 24 mesi. Campi annotati `@PersonalData`, tabella in `exportData` e `purgeData`.
  Nota importante: il modulo pubblico raccoglie dati **anche di persone che non completeranno mai la conferma**, e
  quelle righe vanno cancellate alla scadenza senza aspettare che qualcuno le chieda.
- **RT-9 — Registrazione eventi (§14).** «Invio del modulo ricevuto», «invio respinto per frequenza», «invio
  respinto come automatico» con `tenant_id`, `app_id`, identificativo del modulo e identificativo di correlazione;
  **mai** il recapito, **mai** il contenuto dei campi.

## 4. Criteri di accettazione

**CA-1 — Un modulo non pubblicabile a metà**
- **Dato** un modulo senza collegamento all'informativa
- **Quando** l'utente tenta di pubblicarlo
- **Allora** riceve `400` con l'indicazione dei tre elementi obbligatori mancanti, e il modulo resta non
  pubblicato

**CA-2 — La casella non è pre-spuntata**
- **Dato** un modulo pubblicato
- **Quando** un visitatore apre la pagina pubblica
- **Allora** la casella di consenso è vuota, e inviando senza spuntarla riceve un errore in linea invece di
  un'iscrizione

**CA-3 — L'invio non produce un contattabile**
- **Dato** un visitatore che compila e invia correttamente
- **Quando** l'invio arriva
- **Allora** nasce un iscritto `in attesa di conferma`, parte il messaggio di conferma, e il conteggio dei
  contattabili dell'account **non** cambia

**CA-4 — Il modulo non dice chi è già iscritto**
- **Dato** un recapito già iscritto e confermato
- **Quando** qualcuno lo reinserisce nel modulo pubblico
- **Allora** la risposta è identica a quella di un recapito nuovo — stesso testo, stesso codice — e non nasce una
  seconda iscrizione

**CA-5 — La prova regge al cambio del testo**
- **Dato** un'iscrizione raccolta con la versione 1 del testo del consenso, e il cliente che pubblica la versione 2
- **Quando** si apre la prova di quell'iscrizione
- **Allora** compare la versione 1, integra, con il momento e l'indirizzo di rete

**CA-6 — Isolamento fra account**
- **Dato** la chiave pubblica del modulo dell'account `A`
- **Quando** si invia una richiesta a quella chiave aggiungendo nel corpo l'identificativo dell'account `B`
- **Allora** l'iscritto nasce in `A` e il valore passato viene ignorato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla validazione dei tre elementi obbligatori e sul versionamento del testo del
      consenso; prove di **integrazione** sulla rotta pubblica, compresi limite di frequenza e risposta uniforme;
- [ ] prova di **isolamento fra account** sulla rotta pubblica e sulla gestione dei moduli;
- [ ] **prova end-to-end**: coprire ora — il percorso `[J-CAMPAIGNS]` (storia 0037) comincia dall'invio del modulo
      pubblico, perché è la porta d'ingresso reale della lista; voce aggiunta al registro di copertura;
- [ ] **traduzioni** in tutte e cinque le lingue per l'interfaccia di gestione; testo del consenso escluso dalla
      traduzione automatica;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per `subscription_form` e `form_submission`, con la
      nota sulle iscrizioni mai confermate e la loro scadenza;
- [ ] **registro delle decisioni** compilato, con annotato perché i tre elementi non sono configurabili e perché
      la risposta pubblica è uniforme;
- [ ] contratto degli **strumenti conversazionali**: nessuno introdotto, con la motivazione scritta;
- [ ] controllo automatico di **accessibilità** verde sulla pagina pubblica, che è la superficie usata da persone
      che non sono clienti nostri;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0008` | Ogni invio del modulo fa partire la doppia conferma |
| Storia `0007` | Il testo del consenso versionato è ciò che la registrazione copia come prova |
| Estensioni della console di amministrazione ([estensioni-admin.md](../estensioni-admin.md)) | Il modulo pubblico è l'unica superficie raggiungibile senza autenticazione: chi amministra deve poterla spegnere per un account sotto abuso |

## 7. Fuori ambito

- le pagine di atterraggio complete con contenuti: qui c'è un modulo d'iscrizione, non un costruttore di siti;
- l'iscrizione tramite codice a barre bidimensionale o parola chiave inviata per messaggio: rimandate, perché
  richiedono un canale che non possediamo (epica 04);
- la personalizzazione grafica avanzata del modulo incorporato: rimandata; per ora colori del sistema di design e
  poche scelte, perché un modulo che si può travestire è un modulo che si può rendere ingannevole.

## 8. Punti aperti

- **Difesa dagli invii automatici.** Le difese diffuse sul mercato sono servizi di terzi che vedono l'indirizzo di
  rete dei visitatori del cliente: sarebbe un fornitore esterno in più e un trattamento in più
  ([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §10). La proposta è partire con difese nostre — limite
  di frequenza, campo esca invisibile, tempo minimo di compilazione — e valutare altro solo se gli abusi reali lo
  richiedono. Chiude lo sviluppatore.
- **Chi risponde dell'informativa collegata.** Il collegamento lo fornisce il cliente e noi non possiamo
  verificarne il contenuto. Che sia obbligatorio è deciso; che basti a soddisfare l'obbligo di informativa è
  materia della revisione legale e del contratto di trattamento.
