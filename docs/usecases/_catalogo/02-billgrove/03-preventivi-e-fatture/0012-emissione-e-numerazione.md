# 0012 — Emissione e numerazione progressiva

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 03 — Preventivi e fatture
**Storia**: `0012` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0004`, `0006`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che risponde in prima persona di ciò che emette
> voglio che il numero della fattura venga assegnato dal sistema, senza salti e senza doppioni
> così da non trovarmi a fine anno con una numerazione irregolare, che è un problema fiscale e non un fastidio.

**Contesto.** È la storia più delicata dell'app. La legge impone una numerazione progressiva e univoca per anno e
per sezionale, senza salti né riuso (§2.3 della descrizione): il numero va assegnato al momento dell'**emissione**,
non della creazione della bozza, altrimenti ogni bozza abbandonata lascia un buco. Da qui in poi il documento è
congelato: si rettifica con una nota di credito, non si modifica. È anche il punto in cui si consuma la quota.

## 2. Requisiti funzionali

1. **RF-1** — Il passaggio `bozza → emesso` assegna il numero, congela la data e scrive la copia dei dati del
   cliente nel documento.
2. **RF-2** — Il numero è progressivo e univoco per **(sezionale, anno)**, senza salti né riuso, anche con più
   utenti che emettono nello stesso istante.
3. **RF-3** — Esiste almeno un sezionale predefinito; l'account può aggiungerne altri e ciascuno ha il proprio
   contatore.
4. **RF-4** — L'emissione **prenota una unità** della metrica `documenti`: a quota esaurita risponde `429` e il
   documento resta in bozza, senza numero.
5. **RF-5** — Un documento emesso non è modificabile né cancellabile, e il tentativo viene rifiutato con la
   spiegazione della via corretta (nota di credito).
6. **RF-6** — La data del documento non può essere precedente a quella dell'ultimo documento emesso nello stesso
   sezionale: la numerazione e la cronologia devono concordare.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il contatore è per `(tenant_id, sezionale, anno)` e il `tenant_id` viene
  dal token verificato: due account non condividono mai un contatore, e nessuno può leggere o influenzare quello di
  un altro.
- **RT-2 — Interfaccia di programmazione (§2).** `POST /api/billing/v1/documents/{id}/issue`; errori in
  `application/problem+json` (`409` per stato o data incoerenti, `429` per quota esaurita); definizione OpenAPI
  aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V7__numbering.sql` sullo schema `app_billing`: tabella `numbering` con
  `tenant_id`, sezionale, anno e ultimo numero assegnato, con colonne di controllo. **L'assegnazione avviene in una
  sola transazione con un blocco sulla riga del contatore**: è il punto in cui una implementazione ingenua produce
  doppioni sotto carico concorrente.
- **RT-4 — Modulo frontend (§3, §5).** Azione «Emetti» con conferma esplicita che dica che l'operazione non si
  disfa; l'avviso di quota compare **prima**, non dopo. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili, compresi i messaggi di rifiuto, passano dallo
  spazio-nomi `billing` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Prima di emettere un documento il servizio prenota una unità della metrica
  `documenti` (natura `flow`); a quota esaurita risponde `429` con un messaggio che dice come rimediare. Con
  abbonamento non attivo risponde `402`. La prenotazione e l'assegnazione del numero stanno nella **stessa
  transazione**: non deve esistere il caso «quota consumata, numero non assegnato».
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato: `emetti_documento(id) → numero assegnato`,
  marcato **scrittura irreversibile**: richiede conferma umana **obbligatoria**. La chat può preparare il documento
  ma non può emetterlo da sola. Dipendenza dichiarata: UC 0061-0063.
- **RT-8 — Dati personali (§10).** La copia congelata dei dati del cliente viene **scritta qui**: la voce
  `document.dati_cliente_congelati` del manifesto (dichiarata nella storia `0002`) diventa concreta, e da questo
  momento è coperta dall'obbligo di conservazione decennale, non più dalla sola esecuzione del contratto.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `documento emesso` (con sezionale, anno e numero), `emissione
  respinta per quota` e `emissione respinta per data incoerente` sono registrati con `tenant_id`, `app_id`,
  `user_id` e identificativo di correlazione, senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Emissione**
- **Dato** una fattura in `bozza` e un account con quota disponibile
- **Quando** si emette
- **Allora** il documento passa a `emesso`, riceve il numero successivo del suo sezionale, porta la copia dei dati
  del cliente e consuma una unità di quota

**CA-2 — Nessun salto sotto concorrenza**
- **Dato** due utenti dello stesso account che emettono nello stesso istante sullo stesso sezionale
- **Quando** entrambe le emissioni vanno a buon fine
- **Allora** i numeri assegnati sono consecutivi, distinti, senza salti

**CA-3 — Quota esaurita**
- **Dato** un account che ha raggiunto il tetto di `documenti`
- **Quando** tenta di emettere
- **Allora** riceve `429` con il rimedio, **il documento resta in bozza e nessun numero viene consumato**

**CA-4 — Documento emesso immutabile**
- **Dato** un documento `emesso` · **Quando** si tenta di modificarne una riga o di cancellarlo
- **Allora** la risposta è `409` con l'indicazione di usare una nota di credito

**CA-5 — Data incoerente**
- **Dato** un sezionale il cui ultimo documento è del 20 luglio
- **Quando** si tenta di emettere un documento datato 15 luglio
- **Allora** la risposta è `409` con la spiegazione, e nulla viene emesso

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` che emettono sullo stesso nome di sezionale
- **Quando** entrambi emettono
- **Allora** ciascuno riparte dal proprio contatore, e i numeri non interferiscono

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sull'assegnazione del numero e di **integrazione** sull'emissione concorrente, con
      database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sui contatori;
- [ ] **prova end-to-end**: *coprire ora* — passo «emetti la fattura» del percorso `[J-BILLING]`, compreso il caso
      di quota esaurita; registro di copertura aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato: la copia congelata cambia base giuridica e conservazione;
- [ ] **registro delle decisioni** compilato, con annotata la scelta della transazione unica quota + numero;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `emetti_documento`, con conferma obbligatoria;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | Servono il documento e la macchina a stati |
| storia `0004` | Serve la prenotazione della quota |
| storia `0006` | Serve il cliente da congelare nel documento |

## 7. Fuori ambito

- il calcolo delle imposte: storia `0013` (qui i totali si assumono già calcolati);
- la rettifica: storia `0014`;
- la conservazione decennale e il blocco della cancellazione: storia `0026`;
- l'annullamento di un documento emesso per errore: **non esiste** ed è una scelta, non una mancanza; si rettifica
  con nota di credito.

## 8. Punti aperti

Quanti sezionali servono al cliente tipo e chi li configura resta aperto (punto 6 del §11 della descrizione): troppi
sezionali confondono la micro-impresa, troppo pochi bloccano chi ne ha bisogno per legge, e non ho trovato un dato
di mercato che lo dica. Questa storia costruisce il meccanismo per un numero qualsiasi di sezionali e lascia la
scelta della configurazione predefinita allo sviluppatore.
