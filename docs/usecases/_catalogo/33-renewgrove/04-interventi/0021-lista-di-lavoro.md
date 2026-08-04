# 0021 — Lista di lavoro per la persona

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 04 — Interventi con conferma umana
**Storia**: `0021` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0019`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un'attività dove il cliente non si trattiene con un messaggio ma con una telefonata
> voglio un elenco corto di persone da chiamare oggi, con scritto **perché** e **che cosa dire**
> così da fare in venti minuti, la mattina, il lavoro che oggi rimando finché non è troppo tardi.

**Contesto.** È la via che la [descrizione](../application-description.md) §4.3 chiama «il caso più frequente
nelle micro-imprese, dove l'intervento giusto è una telefonata», e finché il contratto dell'evento di richiesta di
comunicazione non esiste (punto aperto n. 2) è anche **l'unica via percorribile** — il che la rende, nei fatti, la
funzione più usata dell'epica 04 e non il ripiego che il nome suggerisce.

Ha una proprietà che vale la pena dire forte, perché cambia la natura del prodotto: in questa via **appgrove non
compie alcun effetto verso l'esterno**. Nessun messaggio parte, nessun sistema contatta nessuno. È un **elenco di
lavoro per una persona**, che poi alza la cornetta. Tutto ciò che l'app fa è dire a chi lavora chi chiamare,
perché, che cosa dire, e offrire uno spazio per segnare com'è andata — che è anche il modo in cui l'esito rientra
nella misura dell'efficacia (epica 05).

Il segmento a cui parliamo, secondo il §2.1, non è servito da nessuno proprio qui: gli strumenti concorrenti sono
verticali sul momento della disdetta cliccata, che nel nostro mercato quasi non esiste, perché «il cliente di una
micro-impresa non disdice cliccando: smette di chiamare».

## 2. Requisiti funzionali

1. **RF-1** — Esiste una sezione **Da fare**: l'elenco degli interventi confermati che nessuna applicazione può
   inviare, ordinato per rischio decrescente e con il numero di giorni da cui aspettano.
2. **RF-2** — Ogni voce mostra quattro cose e nient'altro: **chi chiamare** (l'etichetta leggibile del rapporto e
   il rimando alla riga d'origine nell'app che la possiede), **perché** (i tre fatti principali che hanno formato
   il punteggio, storia `0017`), **che cosa dire** (il testo dell'intervento, dal piano e modificato da chi l'ha
   preparato), e **quando è stato confermato e da chi**.
3. **RF-3** — Chi esegue segna **com'è andata**: esito da un elenco corto (`fatto — risposta positiva`, `fatto —
   risposta negativa`, `fatto — nessuna risposta`, `non riuscito a contattare`, `rimandato`) e una **nota libera**
   facoltativa. Registrare l'esito porta l'intervento in stato `eseguito` (macchina a stati del §4.4).
4. **RF-4** — Una voce si può **prendere in carico**: chi la prende compare accanto a essa, così che in un ufficio
   di tre persone nessuno chiami due volte lo stesso cliente. La presa in carico si rilascia, e i due gesti sono
   tracciati.
5. **RF-5** — L'esito registrato diventa un **segnale interno** sul rapporto, così che il punteggio ne tenga conto
   e la misura dell'efficacia (epica 05) abbia una data e un fatto da cui partire.
6. **RF-6** — La sezione dichiara in una riga che **da qui non parte nulla verso il cliente**: è un elenco di
   lavoro, la chiamata la fa una persona. Detto una volta, sopra l'elenco, senza note legali.
7. **RF-7** — La **nota libera** è scritta da un nostro utente e porta l'avvertenza esplicita, visibile prima di
   scrivere: **non inserire dati sulla salute** né altre informazioni delle categorie particolari dell'articolo 9
   del regolamento europeo.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura delle voci di lista filtra per `tenant_id`
  preso dal token di accesso verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri
  viene ignorato. Prendere in carico o chiudere una voce di un altro account restituisce `404`.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/fidelizzazione/v1/lista-di-lavoro` (paginata, con
  filtri per stato e per chi l'ha presa in carico),
  `POST /api/fidelizzazione/v1/lista-di-lavoro/{id}/presa-in-carico` e la sua revoca,
  `POST /api/fidelizzazione/v1/lista-di-lavoro/{id}/esito` (esito dall'elenco chiuso più nota facoltativa). Corpo
  validato; errori in `application/problem+json`, con `409` se l'esito viene registrato due volte. Definizione
  OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V17__voce_lista_di_lavoro.sql` sullo schema `app_fidelizzazione`:
  tabella `voce_lista_di_lavoro` con `tenant_id`, intervento, stato (`da fare` / `presa in carico` / `chiusa`),
  chi l'ha presa in carico e quando, esito, nota, momento della chiusura; chiave primaria UUID versione 7, colonne
  di controllo `created_at`, `updated_at`, `created_by`, `updated_by` e cancellazione logica `deleted_at`. Nessuna
  chiave esterna verso altri schemi. **Nessuna colonna di recapito**, coerentemente con la via A del §4.3: il
  numero da chiamare lo cerca chi chiama, nell'app che lo possiede, seguendo il rimando alla riga d'origine.
- **RT-4 — Modulo frontend (§3, §5).** Sezione `Da fare` del modulo `fidelizzazione`: elenco a schede, ciascuna
  con i tre fatti principali (componente della storia `0017`), il testo da dire, i pulsanti di presa in carico e
  di esito. Pensata per essere usata **la mattina, in dieci minuti**: niente cruscotto, niente indicatori, una
  colonna sola. Dati letti e scritti con il client generato; solo token del sistema di design; funziona in tema
  chiaro e scuro; controllo automatico di accessibilità.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe — le cinque voci dell'elenco degli esiti, la riga «da qui non
  parte nulla», l'avvertenza dell'articolo 9, i testi della presa in carico — passano dallo spazio-nomi
  `fidelizzazione` e sono presenti in `en, it, fr, es, de`. Il **testo dell'intervento** resta nella lingua in cui
  il cliente lo ha scritto.
- **RT-6 — Varchi e quota (§6, §7).** Catena dei varchi completa: `401`, `403` ad app spenta, `402` ad account non
  abilitato o abbonamento `canceled`, `403` a ruolo insufficiente. Prendere in carico e registrare un esito sono
  aperti a `owner`, `admin` e `member`: è il lavoro quotidiano di chi tiene la relazione. **Nessun consumo di
  quota nuovo**: la metrica `rapporti_sorvegliati` (natura `stock`) conta i rapporti, non le telefonate — e non
  deve contarle, per la stessa ragione per cui non conta gli interventi (§3 della descrizione).
- **RT-7 — Esposizione conversazionale (§12).** **Nessuno strumento nuovo**: la tabella degli strumenti (§7 della
  descrizione) non ne prevede per la lista di lavoro, e la scelta è motivata — l'elenco si legge con
  `elenca_rapporti_a_rischio` (`0013`) e la singola voce con `stato_rapporto`, mentre registrare un esito è un
  gesto che si fa mentre si riattacca il telefono, non dettando a una chat. Il livello conversazionale è di
  piattaforma e **non è ancora implementato** (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Sì.** Voce nuova nel manifesto
  `docs/compliance/manifests/fidelizzazione.yaml`, in **italiano e inglese**:
  `lista_di_lavoro.esito_e_nota` — dove vive: tabella `voce_lista_di_lavoro`; di chi è: cliente del nostro cliente
  (per riferimento) e utente del nostro cliente per i campi di autore; che dato è: comportamentale più prova; a
  cosa serve: sapere se la telefonata è stata fatta e com'è andata, e alimentare la misura dell'efficacia; base
  giuridica: esecuzione del rapporto commerciale fra il nostro cliente e il suo cliente; conservazione: 24 mesi
  come l'intervento (punto aperto n. 9). Campi annotati `@PersonalData`: esito, nota, autore. Tabella
  `voce_lista_di_lavoro` aggiunta a `exportData` e a `purgeData` di `FidelizzazioneDataContract`. **Da correggere
  nella descrizione**: il §6 elenca due soli punti di testo libero (contestazione e nota dell'intervento) mentre
  questa storia ne introduce un terzo — l'elenco va aggiornato. Il presidio resta lo stesso: **contrattuale, non
  tecnico**, avvertenza a schermo, nessun rilevamento automatico del contenuto.
- **RT-9 — Registrazione eventi (§14).** `voce presa in carico`, `presa in carico rilasciata`,
  `esito registrato (tipo di esito)`, con `tenant_id`, `app_id`, `user_id`, identificativo di correlazione e
  identificativo dell'intervento; **mai la nota**, mai l'etichetta del rapporto.

## 4. Criteri di accettazione

**CA-1 — L'elenco della mattina**
- **Dato** un account con quattro interventi confermati e non inviabili da alcuna applicazione
- **Quando** l'utente apre la sezione Da fare
- **Allora** vede quattro voci in ordine di rischio decrescente, ciascuna con etichetta del rapporto, i tre fatti
  principali, il testo da dire, chi ha confermato e da quanti giorni la voce aspetta

**CA-2 — Registrare com'è andata chiude l'intervento**
- **Dato** una voce presa in carico
- **Quando** l'utente registra l'esito «fatto — risposta positiva» con una nota
- **Allora** la voce risulta chiusa, l'intervento passa in stato `eseguito`, e sul rapporto compare un segnale
  interno con la data della telefonata

**CA-3 — Nessun doppio lavoro**
- **Dato** una voce già presa in carico dall'utente Anna
- **Quando** l'utente Luca apre l'elenco
- **Allora** vede la voce marcata «in carico ad Anna» e, se tenta di prenderla, riceve `409` con l'indicazione di
  chi la sta lavorando

**CA-4 — Esito registrato due volte**
- **Dato** una voce già chiusa con esito
- **Quando** si tenta di registrare un secondo esito
- **Allora** la richiesta è respinta con `409` in `problem+json`, l'esito originale e il suo momento restano
  invariati, e l'intervento resta `eseguito` una volta sola

**CA-5 — Da qui non parte nulla**
- **Dato** un utente che apre la sezione Da fare
- **Quando** legge l'intestazione
- **Allora** trova in chiaro che nessun messaggio parte da questa sezione e che la chiamata la fa una persona, in
  tutte e cinque le lingue; e nessuna azione della sezione pubblica eventi verso applicazioni destinatarie

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con la propria lista di lavoro
- **Quando** un utente di `A` tenta di registrare l'esito di una voce di `B` usandone l'identificativo
- **Allora** riceve `404`, nessuna riga di `B` cambia e nessun intervento di `B` passa di stato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sull'ordinamento dell'elenco e sulla transizione dell'intervento all'esito; prove di
      **integrazione** sulla risorsa, con database effimero e migrazioni Flyway vere, compresa la presa in carico
      contesa fra due utenti;
- [ ] prova che **nessun evento verso applicazioni destinatarie** viene pubblicato dalle azioni di questa sezione;
- [ ] prova di **isolamento fra account** sulla risorsa `voce_lista_di_lavoro`;
- [ ] **prova end-to-end**: *rimandare* — il percorso `[J-FIDELIZZAZIONE]` nasce nella storia `0030` e dovrà
      coprire il tratto «confermo senza destinatario → la voce compare nella lista → registro l'esito →
      l'intervento è eseguito»; voce `da-coprire` con motivo e storia proprietaria `0030` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), elenco degli esiti e avvertenza
      dell'articolo 9 compresi;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con `lista_di_lavoro.esito_e_nota`, campi annotati
      `@PersonalData`, tabella in `exportData` e in `purgeData`; **elenco dei punti di testo libero** della
      descrizione §6 corretto;
- [ ] **registro delle decisioni** compilato con: perché la lista di lavoro non è un ripiego ma la via prevalente,
      perché l'elenco degli esiti è chiuso e corto, perché nessun recapito è conservato nemmeno qui;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo, con la motivazione scritta;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0019` — intervento con conferma umana | una voce di lista nasce da un intervento confermato e ne chiude la macchina a stati con l'esito |
| storia `0017` — il punteggio non decide da solo | fornisce il componente dei tre fatti principali, che è il «perché» di ogni voce |
| storia `0020` — consegna all'app proprietaria | decide **quando** si ripiega qui; finché il contratto di piattaforma non esiste, si ripiega sempre |
| storia `0011` — rimando alla riga d'origine nella fonte | serve a chi chiama per trovare il recapito nell'app che lo possiede, dato che qui non c'è |
| epica di piattaforma non implementata, UC 0061-0063 | nessuno strumento proprio; l'elenco si legge con gli strumenti di lettura della storia `0028` |

## 7. Fuori ambito

- **l'invio di un messaggio** da questa sezione: **escluso per scelta**, è la proprietà che la definisce;
- **la conservazione del recapito** per comodità di chi chiama: escluso, è la via B del §4.3 e riscriverebbe il
  manifesto dei dati;
- **la registrazione della telefonata** o la sua trascrizione: fuori, introdurrebbe contenuti liberi non
  controllabili e, molto probabilmente, categorie particolari dell'articolo 9;
- **l'assegnazione automatica delle voci** alle persone del gruppo di lavoro: rimandata, perché in un'attività da
  tre persone non serve e in una da trenta serve un modello di carichi di lavoro che questa app non ha;
- **la misura dell'efficacia** a partire dagli esiti registrati: epica 05 (`0024`-`0027`).

## 8. Punti aperti

- **Se le voci di lista debbano scadere.** Una voce confermata e mai lavorata per tre settimane è, di fatto, un
  intervento che non è avvenuto — e lasciarla nell'elenco sporca la misura dell'efficacia, perché non si capisce
  più se il rapporto è stato perso nonostante l'intervento o **senza** l'intervento. **Raccomandazione**: dopo una
  finestra dichiarata la voce si marca «non lavorata» e l'intervento passa a un esito proprio, distinto da
  «eseguito». Chiude: **sviluppatore**, con la direzione di prodotto, in coordinamento con la storia `0024`.
- **Quante voci mostrare per volta.** L'elenco è utile se si esaurisce in venti minuti; su un account con 1.200
  rapporti sorvegliati potrebbe essere lunghissimo, e un elenco lungo è un elenco che nessuno apre.
  **Raccomandazione**: un tetto giornaliero suggerito, con il resto raggiungibile ma non in faccia. Chiude:
  **sviluppatore**.
