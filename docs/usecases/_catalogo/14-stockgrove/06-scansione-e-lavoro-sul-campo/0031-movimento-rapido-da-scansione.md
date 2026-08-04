# 0031 — Movimento rapido da scansione

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 06 — Scansione e lavoro sul campo
**Storia**: `0031` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0014`, `0015`, `0017`, `0030`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetto che riceve o preleva la merce
> voglio passare dalla lettura del codice al movimento registrato in due tocchi, restando dentro la stessa sessione
> così da svuotare un bancale o preparare una spedizione senza tornare indietro a ogni pezzo.

**Contesto.** Con la storia `0030` la lettura arriva all'articolo, ma poi bisogna uscire, aprire la schermata dei
movimenti, riscegliere l'articolo e il deposito e compilare un modulo: sono cinque passaggi per un gesto che ne
vale uno, e al terzo pezzo l'addetto smette. Il magazzino si registra **mentre** si lavora o non si registra
affatto — e un saldo che nessuno alimenta è esattamente il difetto che l'app esiste per togliere (descrizione §1).
È il momento giusto adesso perché carico (`0014`), scarico (`0015`) e storno (`0017`) esistono già: questa storia
non introduce nessuna regola nuova sul registro, ne costruisce la via veloce.

**Il vincolo che governa la storia.** La via rapida **non è una via privilegiata**: passa esattamente dalle stesse
regole della registrazione ordinaria — movimento in sola aggiunta, aritmetica nella base di dati con aggiornamento
condizionato in una sola transazione, chiave di idempotenza per account. Una scorciatoia che scrivesse la giacenza
in modo diverso sarebbe una seconda verità, e le due divergerebbero il primo giorno di lavoro vero.

## 2. Requisiti funzionali

1. **RF-1** — All'inizio della sessione rapida si scelgono **una volta sola** il tipo di movimento (carico o
   scarico) e il **deposito**; entrambi restano validi per tutte le letture successive e sono sempre visibili a
   schermo, così che nessuno registri venti scarichi sul deposito sbagliato.
2. **RF-2** — Da una lettura risolta su un articolo (storia `0030`), la registrazione del movimento richiede **due
   tocchi**: conferma della quantità — predefinita a **uno** — e conferma del movimento. La quantità si può
   correggere prima di confermare.
3. **RF-3** — A movimento registrato la sessione **continua**: si torna al lettore pronto per il pezzo successivo,
   senza passaggi intermedi e senza perdere il tipo di movimento né il deposito scelti.
4. **RF-4** — L'elenco di ciò che si è appena fatto nella sessione resta visibile in coda alla schermata, in ordine
   dal più recente, con articolo, quantità con segno e ora.
5. **RF-5** — Ogni riga dell'elenco si può **stornare** con un tocco, generando lo storno della storia `0017` (un
   movimento contrario che rimanda l'originale, con motivo «correzione in sessione rapida»); la riga resta
   nell'elenco marcata come stornata, non sparisce.
6. **RF-6** — Se la merce non basta, la registrazione è rifiutata con la **quantità residua** dichiarata, la
   sessione non si interrompe e la lettura resta a schermo per essere corretta o abbandonata.
7. **RF-7** — La sessione rapida è un contesto dell'interfaccia, non un'entità salvata: chiudere la schermata la
   termina e non lascia niente in sospeso sul servizio. Ciò che era stato confermato è già nel registro.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La registrazione del movimento e la lettura dell'elenco di sessione
  filtrano per `tenant_id` preso dal token verificato; un `tenant_id` che arrivasse dal corpo della richiesta viene
  ignorato. L'articolo e il deposito indicati devono appartenere all'account, altrimenti la risposta è `404` — non
  si rivela l'esistenza di risorse altrui.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta nuova: si usa
  `POST /api/magazzino/v1/movimenti` della storia `0014`/`0015` con la chiave di idempotenza generata dal
  dispositivo, e `POST /api/magazzino/v1/movimenti/{id}/storno` della storia `0017`. Errori in
  `application/problem+json`; definizione OpenAPI invariata, salvo la documentazione dell'origine del movimento.
- **RT-3 — Persistenza (§8).** **Nessuna migrazione nuova.** Il movimento porta l'origine `scansione` nel campo
  già previsto dalla storia `0013`, che serve a distinguere ciò che è stato registrato sul campo da ciò che è
  arrivato da un evento o da un'importazione. Nessuna tabella di sessione: la sessione vive nell'interfaccia.
- **RT-4 — Modulo frontend (§3, §5).** La sessione rapida è un percorso della sezione `movimenti` del modulo
  `magazzino`, non una sezione nuova del manifesto: pensata per lo schermo stretto, con i bersagli di tocco grandi
  abbastanza da essere colpiti con i guanti, sui soli token del sistema di design, in tema chiaro e scuro. I dati
  passano dal client generato dalla definizione OpenAPI.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe — compresi il messaggio di giacenza insufficiente con la
  quantità residua e il motivo di storno predefinito — passano dallo spazio-nomi `magazzino` e sono presenti in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** **Il movimento non consuma quota e non risponde mai `429`**: impedire di
  registrare uno scarico perché il piano è finito corromperebbe il saldo del cliente (descrizione §5). Il tetto
  `articoli_gestiti` (natura `stock`) resta sulla sola creazione di articoli. Con abbonamento in `past_due` la
  registrazione resta accessibile; con `canceled` risponde `402`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo: carico e scarico da chat sono
  `registra_carico` e `registra_scarico`, dichiarati nella storia `0035` con bozza e conferma umana obbligatorie.
  Va garantito che il percorso rapido e lo strumento conversazionale scrivano attraverso **lo stesso** servizio di
  registrazione, non due strade parallele. Server conversazionale di piattaforma, non ancora implementato
  (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo.** L'autore del movimento (`created_by`) è già
  dichiarato nel manifesto dalla storia `0010`. **Nessuna immagine della fotocamera viene inviata al servizio né
  conservata**: la lettura avviene sul dispositivo e verso il servizio viaggia solo il codice, come nella storia
  `0030`.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `movimento registrato da scansione`, `movimento respinto per
  giacenza insufficiente` e `storno da sessione rapida` sono registrati con `tenant_id`, `app_id`, `user_id` e
  identificativo di correlazione, senza dati personali e senza il testo delle note. **Nessun conteggio aggregato
  per persona**: il dato serve alla tracciabilità della merce, non a misurare chi lavora (descrizione §6).

## 4. Criteri di accettazione

**CA-1 — Due tocchi, sessione che continua**
- **Dato** una sessione rapida aperta in scarico sul deposito «Furgone», e un articolo con giacenza 5 su quel
  deposito
- **Quando** si legge il codice dell'articolo, si conferma la quantità predefinita 1 e si conferma il movimento
- **Allora** esiste un movimento di scarico di −1 con origine `scansione`, la giacenza sul «Furgone» è 4, la riga
  compare in cima all'elenco di sessione e il lettore è già pronto per il pezzo successivo, ancora in scarico sul
  «Furgone»

**CA-2 — Giacenza insufficiente**
- **Dato** la stessa sessione e un articolo con giacenza 2
- **Quando** si tenta di scaricarne 3
- **Allora** la risposta è `409` in `application/problem+json` con la quantità residua 2, nessun movimento viene
  creato, la giacenza resta 2 e la sessione prosegue con la lettura ancora a schermo

**CA-3 — Storno di una riga appena registrata**
- **Dato** una riga di sessione con uno scarico di −1 già registrato
- **Quando** la si storna
- **Allora** esiste un **secondo** movimento di +1 che rimanda il primo, il primo movimento è intatto nel registro
  (non modificato, non cancellato), la giacenza torna al valore precedente e la riga compare come stornata

**CA-4 — Due addetti sullo stesso articolo nello stesso istante**
- **Dato** un articolo con giacenza 5 e due addetti dello stesso account che confermano contemporaneamente uno
  scarico di 3 ciascuno
- **Quando** le due richieste arrivano insieme
- **Allora** una sola va a buon fine e la giacenza resta 2; l'altra riceve `409` con la quantità residua, e in
  nessun caso la giacenza scende sotto zero né si perde uno dei due movimenti

**CA-5 — Invio ripetuto della stessa conferma**
- **Dato** una conferma inviata due volte con la **stessa** chiave di idempotenza (il tocco ripetuto, la rete che
  ritenta)
- **Quando** il servizio riceve la seconda richiesta
- **Allora** non crea un secondo movimento, risponde con quello già registrato e la giacenza è cambiata **una**
  volta sola

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri articoli e depositi
- **Quando** un utente di `A` tenta di registrare un movimento indicando un articolo di `B`
- **Allora** riceve `404`, nessun movimento viene creato e nessuna informazione sull'articolo di `B` compare nella
  risposta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla sessione dell'interfaccia (tipo e deposito che persistono, quantità predefinita,
      elenco) e di **integrazione** sulla registrazione con database effimero e migrazioni vere, compresa la prova
      di concorrenza con due transazioni contemporanee;
- [ ] prova di **isolamento fra account** sulla registrazione del movimento da sessione rapida;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-MAGAZZINO]` è di proprietà della storia `0036`, che
      include il passo «registra un movimento dalla via rapida» e i due scarichi simultanei; la voce nel registro
      di copertura [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) si scrive lì;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova; l'autore del movimento è già dichiarato dalla storia `0010`;
- [ ] **registro delle decisioni** compilato, con la scelta di non introdurre nessuna rotta né tabella nuova e di
      passare dalla stessa registrazione ordinaria;
- [ ] contratto degli **strumenti conversazionali**: nessuno introdotto qui; la via da chat è della storia `0035`
      e condivide il medesimo servizio di registrazione;
- [ ] verifica manuale su schermo stretto, in tema chiaro e scuro, con venti letture consecutive;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0030` | La lettura del codice e la risoluzione sull'articolo sono il punto di partenza |
| `0014` | Il carico, con le sue regole e il suo aggiornamento del costo medio |
| `0015` | Lo scarico, con l'aggiornamento condizionato e il rifiuto a giacenza insufficiente |
| `0017` | Lo storno: senza di esso una riga sbagliata non si può correggere |
| `0008` | I depositi, fra cui si sceglie quello della sessione |

## 7. Fuori ambito

- **Lavorare senza rete**: qui ogni conferma richiede una risposta del servizio; la coda locale e l'invio
  differito sono della storia `0032`.
- **Trasferimento fra depositi da scansione**: il trasferimento (`0016`) chiede due depositi e non entra nel gesto
  a due tocchi; resta sulla schermata ordinaria.
- **Rettifica da sessione rapida**: la rettifica cambia il saldo dichiarando che il registro era sbagliato e
  pretende un motivo scritto (`0021`): non è un gesto da fare di corsa, e resta fuori di proposito.
- **Conteggio di inventario da scansione**: è la sessione di inventario della storia `0022`, che ha uno stato suo
  e un atteso congelato.
- **Etichette per la merce senza codice**: storia `0033`.

## 8. Punti aperti

- **Quantità predefinita a uno**: è la scelta giusta per chi preleva pezzo per pezzo, meno per chi riceve bancali
  interi. Un valore predefinito configurabile per sessione è una raffinatezza plausibile ma non richiesta da
  nessuna fonte: si lascia allo sviluppatore, dopo il primo uso reale.
- **Storno con motivo predefinito**: il motivo «correzione in sessione rapida» è proposto per non chiedere un
  testo a chi ha le mani occupate. Se la revisione dei motivi (`0021`) stabilisse che ogni storno pretende un
  motivo scelto da elenco, questa storia si adegua.
