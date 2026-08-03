# 0005 — Avvio locale e dati di prova

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 01 — Fondamenta
**Storia**: `0005` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`, `0002`, `0003`, `0004`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore che prende in mano ReachGrove
> voglio avviare l'app in locale e trovarci dentro dati sensati, compresi i casi scomodi
> così da poter lavorare sulle storie di dominio senza passare la prima mezz'ora a inventarmi un archivio.

**Contesto.** Un'app nuova dev'essere eseguibile in locale **subito dopo l'unione del ramo**, senza passi manuali:
la mappa servizio → identificativo → porta → schema si ricava dal solo file delle proprietà del servizio, e da lì
discendono da soli avvio, migrazioni, rotte del proxy locale e avvii di collaudo. Il dovere di questa storia è
quindi dichiarare bene quelle proprietà e **verificare** che tutto ne discenda, non incollare righe negli script.

La seconda metà della storia è il popolamento. Un archivio di prova con dieci iscritti tutti attivi è inutile in
questa applicazione: le regole che contano si vedono solo sui casi scomodi. Per questo i dati di prova contengono
fin dal primo giorno un iscritto **in quarantena** (importato senza prova del consenso) e un recapito
**soppresso**: chi sviluppa deve inciampare subito nel comportamento che il prodotto esiste per garantire, non
scoprirlo quando scrive la storia 0018.

## 2. Requisiti funzionali

1. **RF-1** — `./dev.sh services` mostra `campaigns` con porta `8116` e schema `app_campaigns`, ricavati dal solo
   file `services/campaigns/src/main/resources/application.properties`.
2. **RF-2** — `./app-start.sh` avvia il servizio insieme agli altri e `./app-stop.sh` lo ferma, senza alcuna
   modifica manuale agli script; le rotte `/api/campaigns/v1/*` del proxy locale sono generate, non scritte a mano.
3. **RF-3** — Il comando di migrazione dello strumento di sviluppo applica le migrazioni dello schema
   `app_campaigns` su un database locale vuoto senza intervento.
4. **RF-4** — Esiste un comando di popolamento che riempie l'account di prova con dati **inventati**: alcune
   decine di iscritti, due segmenti, un paio di modelli di messaggio, una campagna conclusa con i suoi esiti di
   recapito e un dominio mittente verificato.
5. **RF-5** — Fra i dati di prova ci sono obbligatoriamente: un iscritto **in attesa di conferma**, un iscritto
   **in quarantena** con la riga di importazione che ne spiega l'origine, un recapito **soppresso** per
   disiscrizione e uno per rimbalzo permanente, e una campagna **bloccata**.
6. **RF-6** — Tutti gli indirizzi di posta dei dati di prova stanno sul dominio riservato `.test`, i numeri di
   telefono sono di fantasia e nessun dato proviene da una persona reale. Il comando di popolamento è **rifiutato**
   se eseguito su un ambiente diverso da quello locale.
7. **RF-7** — Il popolamento è ripetibile: eseguirlo due volte non duplica i dati e non lascia l'archivio in uno
   stato incoerente.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** I dati di prova nascono tutti dentro l'account di prova locale, con il
  suo `tenant_id`; il comando popola un secondo account minimo, così che le prove di isolamento abbiano da subito
  due perimetri veri con cui lavorare.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta pubblica nuova. Il popolamento è un comando dello
  strumento di sviluppo, non un'interfaccia esposta: un'app che sa riempirsi di dati da una rotta è un'app che
  qualcuno riempirà in produzione.
- **RT-3 — Persistenza (§8).** Il popolamento scrive attraverso gli archivi dell'app, non con inserimenti diretti,
  così che rispetti le colonne di controllo, le chiavi UUID versione 7 e i divieti sulle tabelle ad accrescimento.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova: con i dati caricati le sei sezioni del guscio
  (storia 0003) smettono di essere vuote, ed è così che si verifica che il modulo parli davvero col servizio.
- **RT-5 — Cinque lingue (§4).** Nessun testo visibile nuovo. Gli iscritti di prova hanno lingue diverse fra loro,
  perché la personalizzazione del messaggio nella lingua del destinatario (storia 0015) va vista subito.
- **RT-6 — Varchi e quota (§6, §7).** L'account di prova nasce abilitato nello stub locale dell'abilitazione, con
  un tetto della metrica `messages_sent` volutamente basso, così che il `429` sia raggiungibile a mano senza
  aspettare migliaia di invii.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo **di persone reali**: i dati di prova sono
  interamente inventati e non escono dall'ambiente locale. Vale comunque il divieto di usare dati veri anche solo
  «per comodità», che in questa app significherebbe caricare una rubrica personale in un archivio di marketing.
- **RT-9 — Registrazione eventi (§14).** Il popolamento registra quante righe ha creato e in quale account, con
  `app_id` e identificativo di correlazione; nessun recapito nei registri, nemmeno se inventato.

## 4. Criteri di accettazione

**CA-1 — L'app si avvia senza cablaggi**
- **Dato** un ambiente locale pulito, appena dopo l'unione del ramo
- **Quando** si esegue `./dev.sh services` e poi `./app-start.sh`
- **Allora** `campaigns` compare con porta `8116` e schema `app_campaigns`, il servizio risponde e le rotte
  `/api/campaigns/v1/*` sono raggiungibili dal proxy locale, senza che nessuno abbia modificato uno script

**CA-2 — Il popolamento riempie l'app**
- **Dato** un database locale con le migrazioni applicate e nessun dato
- **Quando** si esegue il comando di popolamento
- **Allora** le sei sezioni del modulo mostrano dati inventati al posto degli stati vuoti

**CA-3 — I casi scomodi ci sono**
- **Dato** l'archivio popolato
- **Quando** si aprono l'elenco degli iscritti e le impostazioni di invio
- **Allora** si trovano un iscritto in attesa di conferma, uno in quarantena con l'origine dichiarata, un recapito
  soppresso per disiscrizione, uno per rimbalzo permanente e una campagna bloccata

**CA-4 — Ripetibile e innocuo**
- **Dato** un archivio già popolato
- **Quando** si esegue di nuovo il comando
- **Allora** i dati non si duplicano e l'archivio resta coerente

**CA-5 — Non si esegue fuori dal locale**
- **Dato** una configurazione che non è quella dell'ambiente locale
- **Quando** si tenta di eseguire il popolamento
- **Allora** il comando si rifiuta di partire con un messaggio che spiega il motivo, e non scrive nulla

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `smoke`; l'intera suite prima del commit);
- [ ] prove di **unità** sull'idempotenza del popolamento e di **integrazione** sull'applicazione delle migrazioni
      a partire da un database vuoto;
- [ ] prova di **isolamento fra account**: i due account di prova non si vedono a vicenda;
- [ ] **prova end-to-end**: nessun impatto diretto — la storia non introduce superficie utente propria; i dati che
      produce sono però il presupposto del percorso `[J-CAMPAIGNS]` della storia 0037, che deve restare
      deterministico e non dipendere dall'ordine di esecuzione;
- [ ] **traduzioni**: nessun testo visibile nuovo;
- [ ] **manifesto dei dati**: nessuna voce nuova; dichiarato che i dati di prova sono inventati e locali;
- [ ] **registro delle decisioni** compilato, con annotato perché i dati di prova comprendono quarantena e
      soppressione;
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia;
- [ ] `./dev.sh services`, `./app-start.sh` e l'avvio di collaudo funzionano senza passi manuali;
- [ ] documentazione dello sviluppo aggiornata con il comando di popolamento.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0001` | Servono il servizio e il file delle proprietà da cui la scoperta automatica ricava tutto |
| Storia `0002` | Il popolamento scrive nelle tabelle del modello dati |
| Storia `0003` | I dati servono a vedere il modulo pieno invece che vuoto |
| Storia `0004` | L'account di prova nasce abilitato, con un tetto basso per poter provare il `429` |

## 7. Fuori ambito

- l'invio reale di messaggi in locale: nessun messaggio esce davvero: il fornitore di consegna è simulato, come il
  fornitore di pagamento. La simulazione dell'invio è della storia 0019;
- i dati di prova delle prove end-to-end di piattaforma, che il percorso `[J-CAMPAIGNS]` (storia 0037) crea da sé
  per restare deterministico;
- ogni funzione di importazione rivolta al cliente: è la storia 0010, e passa dalla quarantena.

## 8. Punti aperti

- **Nessuno.** La storia è interamente dentro il perimetro dello sviluppo locale e non tocca prezzi, dati di
  persone reali né effetti verso l'esterno.
