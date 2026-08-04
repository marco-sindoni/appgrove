# 0014 — Spiegazione del punteggio

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 03 — Punteggio di rischio spiegabile e contestabile
**Storia**: `0014` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0013`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che sta per telefonare a un cliente di dieci anni
> voglio aprire il numero e vedere **quali fatti** lo hanno formato, quanto pesa ciascuno e in che verso
> così da sapere di che cosa parlare, e da poter dire «no, quello non c'entra» invece di subire un verdetto.

**Contesto.** Il punteggio esiste e ha la sua serie storica (`0013`), ma finora è un numero nudo — e un numero nudo
è esattamente ciò che la [descrizione](../application-description.md) §1 indica come il difetto peggiore possibile:
«un punteggio plausibile e non tracciabile è peggio di nessun punteggio, perché non si può contraddire». Questa
storia è l'adempimento diretto di un obbligo: la sentenza della Corte di giustizia dell'Unione europea del 7
dicembre 2023, causa C-634/21, impone che chi calcola un punteggio destinato a determinare in modo decisivo la
sorte di un rapporto contrattuale fornisca **informazioni significative sulla logica utilizzata** e comunichi
**l'importanza e le conseguenze previste** del trattamento (§2.3). Non è una schermata di cortesia: è la
differenza fra un prodotto vendibile in Europa e uno no.

C'è anche una ragione commerciale, e la descrizione §7 la dice meglio di così: la domanda che il titolare fa
davvero non è «chi rischio di perdere» — quello lo mostra un elenco — ma **«perché dici così?»**.

## 2. Requisiti funzionali

1. **RF-1** — Ogni punteggio si **apre** in una scheda che mostra i fatti che lo hanno formato: per ciascuno il
   **tipo di segnale** con il suo significato in lingua naturale, la **data del fatto**, **quanto ha pesato** sul
   valore finale, **in che verso** (alza il rischio / abbassa il rischio) e **su quale finestra di osservazione**.
2. **RF-2** — La scheda dichiara la **versione del modello** con cui il valore è stato calcolato e il momento del
   calcolo: una spiegazione senza queste due informazioni non è verificabile.
3. **RF-3** — La scheda dice **che cosa farebbe scendere il punteggio**: per i contributi che lo alzano, quale
   cambiamento nei fatti (e di quanta entità) porterebbe il rapporto nella fascia inferiore. È la parte
   «conseguenze previste» dell'obbligo, ed è anche la sola parte utile a chi deve decidere cosa fare.
4. **RF-4** — La spiegazione è **un dato conservato insieme al punteggio**, non una ricostruzione fatta dopo: si
   legge dai contributi scritti al momento del calcolo (`0013`). Riaprire un punteggio di sei mesi fa mostra la
   spiegazione di allora, con i pesi di allora, **anche se il modello nel frattempo è cambiato**.
5. **RF-5** — Quando il punteggio è marcato **parziale** (fonte in silenzio, `0013`), la scheda lo dice in cima e
   nomina le fonti mancanti: una spiegazione che tace ciò che non ha visto è una spiegazione falsa.
6. **RF-6** — La stessa spiegazione è disponibile come strumento conversazionale `spiega_punteggio`, con lo stesso
   contenuto e la stessa marcatura di parzialità: non esistono due versioni della verità, una a schermo e una in
   chat.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La lettura della spiegazione filtra per `tenant_id` preso dal token di
  accesso verificato, sulle stesse tabelle `punteggio` e `contributo_punteggio`; un `tenant_id` che arrivasse dal
  corpo della richiesta o dai parametri viene ignorato. Chiedere la spiegazione di un punteggio di un altro
  account restituisce `404`, non un elenco vuoto.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta
  `GET /api/fidelizzazione/v1/punteggi/{id}/spiegazione` (e la forma di comodo
  `GET /api/fidelizzazione/v1/rapporti/{id}/punteggio/spiegazione` per il valore corrente). Il risultato porta:
  valore, fascia, versione del modello, momento del calcolo, marcatura di parzialità, elenco dei contributi
  ordinati per peso decrescente, e l'elenco dei cambiamenti che farebbero scendere la fascia. Errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** **Nessuna tabella nuova e nessuna migrazione di struttura**: la spiegazione legge
  `punteggio` e `contributo_punteggio`, scritte dalla storia `0013` sullo schema `app_fidelizzazione`. Se serve,
  una migrazione `V11__indice_contributo_punteggio.sql` aggiunge il solo indice di lettura su
  `(tenant_id, punteggio)`. È una scelta di disegno, non una svista: conservare una seconda copia della
  spiegazione la farebbe divergere dai contributi, ed è precisamente ciò che RF-4 vieta.
- **RT-4 — Modulo frontend (§3, §5).** Nella sezione `Rapporti` del modulo `fidelizzazione`, il punteggio è
  cliccabile e apre il riquadro della spiegazione: contributi in ordine di peso, verso reso con un'icona **e** con
  una parola (mai col solo colore, per l'accessibilità), finestra e data di ciascun fatto, e il blocco «che cosa lo
  farebbe scendere». Dati letti con il client generato; solo token del sistema di design; funziona in tema chiaro
  e scuro; controllo automatico di accessibilità sulla schermata.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe — nomi e significati dei tipi di segnale, i due versi, i testi
  del blocco «che cosa lo farebbe scendere», la marcatura di parzialità — passano dallo spazio-nomi
  `fidelizzazione` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Catena dei varchi completa: `401`, `403` ad app spenta, `402` ad account non
  abilitato o abbonamento `canceled`, `403` a ruolo insufficiente. La spiegazione è in lettura per tutti i ruoli,
  `member` compreso: negarla a chi lavora sui rapporti significherebbe negarla proprio a chi deve poterla
  contestare (`0015`). **Nessun consumo di quota**: leggere una spiegazione non consuma
  `rapporti_sorvegliati` (natura `stock`).
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `spiega_punteggio(rapporto) → valore, fascia, versione del modello, contributi con peso, verso e data, che cosa
  lo farebbe scendere, marcatura di parzialità`, marcato **lettura**, idempotente, senza conferma. È lo strumento
  che la descrizione §7 indica come il più importante dell'app. Il contratto vive dentro il servizio; il server
  conversazionale è di piattaforma e **non è ancora implementato** (UC 0061-0063); la storia `0028` lo assembla
  insieme agli altri strumenti di lettura.
- **RT-8 — Dati personali (§10).** **Nessun campo nuovo**: la spiegazione espone `punteggio` e
  `contributo_punteggio`, già dichiarati nel manifesto `docs/compliance/manifests/fidelizzazione.yaml` dalla
  storia `0013` con la voce `punteggio.valore_e_contributi` in italiano e inglese, già annotati `@PersonalData` e
  già presenti in `exportData` e `purgeData`. Questa storia aggiunge una verifica, non un campo: la
  **forma leggibile** della spiegazione deve comparire nell'esportazione dei dati dell'interessato, perché il
  cliente finale ha diritto a sapere non solo che esiste un punteggio, ma da che cosa nasce (§6 della
  descrizione). Il requisito è reso verificabile dalla storia `0017` e chiuso dalla `0032`.
- **RT-9 — Registrazione eventi (§14).** `spiegazione consultata (identificativo del punteggio, canale: interfaccia
  o strumento conversazionale)`, con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, **senza
  contributi e senza etichetta del rapporto**: si registra che qualcuno ha guardato, non che cosa ha letto.

## 4. Criteri di accettazione

**CA-1 — Il numero si apre**
- **Dato** un rapporto in fascia `a rischio`, con quattro segnali nella finestra
- **Quando** l'utente apre la spiegazione del punteggio
- **Allora** vede i quattro contributi in ordine di peso decrescente, ciascuno con tipo di segnale, data del
  fatto, peso applicato, verso in parola («alza il rischio») e finestra, più la versione del modello e il momento
  del calcolo

**CA-2 — Che cosa lo farebbe scendere**
- **Dato** un rapporto in fascia `a rischio` il cui contributo maggiore è «rate non rientrate negli ultimi 90
  giorni»
- **Quando** l'utente legge il blocco «che cosa lo farebbe scendere»
- **Allora** vede l'indicazione concreta del cambiamento e della sua entità («due incassi regolari nei prossimi 60
  giorni riporterebbero il rapporto in fascia `attenzione`»), non una frase generica

**CA-3 — La spiegazione di ieri resta quella di ieri**
- **Dato** un punteggio calcolato tre mesi fa con la versione 1 del modello, e una versione 3 oggi viva
- **Quando** l'utente apre quel punteggio storico
- **Allora** vede i pesi e i contributi della versione 1, e la scheda dichiara «versione 1»: nessun valore è
  ricalcolato con il modello attuale

**CA-4 — Spiegazione onesta su fonte in silenzio**
- **Dato** un punteggio marcato parziale perché la fonte dei pagamenti tace da oltre il ritardo atteso
- **Quando** l'utente apre la spiegazione
- **Allora** in cima legge che il valore è parziale e quali fonti mancano, prima ancora di vedere i contributi

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri punteggi
- **Quando** un utente di `A` chiede la spiegazione di un punteggio di `B` usandone l'identificativo
- **Allora** riceve `404` in `problem+json` e nessun contributo di `B` compare nella risposta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo del blocco «che cosa lo farebbe scendere» (soglia della fascia inferiore,
      entità del cambiamento) e sull'ordinamento dei contributi; prove di **integrazione** sulla rotta, con
      database effimero e migrazioni vere, che verificano che una spiegazione storica non venga ricalcolata con il
      modello vivo;
- [ ] prova di **isolamento fra account** sulla risorsa della spiegazione;
- [ ] controllo automatico di **accessibilità** sul riquadro: il verso di ogni contributo è leggibile senza
      distinguere i colori;
- [ ] **prova end-to-end**: *rimandare* — il percorso `[J-FIDELIZZAZIONE]` nasce nella storia `0030` e dovrà
      coprire il tratto «apro un punteggio a rischio e leggo i fatti che lo hanno formato»; voce `da-coprire` con
      motivo e storia proprietaria `0030` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), compresi i significati dei tipi
      di segnale;
- [ ] **manifesto dei dati**: nessuna voce nuova, verificato che le tabelle lette siano già dichiarate ed esportate;
- [ ] **registro delle decisioni** compilato con: perché la spiegazione è un dato conservato e non una
      ricostruzione, perché non si conserva una seconda copia, quale obbligo dell'articolo 22 adempie ciascun
      elemento della scheda;
- [ ] contratto dello strumento `spiega_punteggio` dichiarato come **lettura**;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0013` — calcolo e storico del punteggio | i contributi vengono scritti al momento del calcolo: senza, non c'è nulla da spiegare e la spiegazione dovrebbe essere ricostruita, che è ciò che RF-4 vieta |
| storia `0012` — modello del punteggio | i significati dei tipi di segnale, i versi e le soglie delle fasce vengono dal modello |
| storia `0011` — salute e ritardo delle fonti | la marcatura di parzialità mostrata in cima alla scheda |
| epica di piattaforma non implementata, UC 0061-0063 | `spiega_punteggio` è dichiarato e non esposto: finché il server conversazionale non esiste, il contratto vive versionato dentro il servizio e la spiegazione si legge dall'interfaccia |

## 7. Fuori ambito

- **contestare un fatto** e vedere il punteggio rifarsi davanti agli occhi: storia `0015`. Qui si legge, non si
  interviene;
- **cambiare i pesi** perché la spiegazione ha convinto che sono sbagliati: storia `0016`;
- **mostrare la spiegazione dentro la conferma di un intervento** (i tre fatti principali): storia `0017`, che ne
  fa un presidio verificabile;
- **l'esportazione della spiegazione fra i dati dell'interessato**: verificata dalla `0017` e chiusa dalla `0032`;
- **una spiegazione redatta in linguaggio naturale da un modello linguistico**: deliberatamente rimandata e non a
  una storia di questa epica. Sarebbe una riformulazione che nessuno può verificare sopra dati che invece si
  verificano: introdurla ora rovinerebbe l'unica proprietà per cui questa schermata esiste.

## 8. Punti aperti

- **Quanto in là spingere il blocco «che cosa lo farebbe scendere».** Detto in modo troppo preciso diventa un
  bersaglio da colpire e smette di essere una previsione onesta; detto in modo troppo vago è inutile.
  **Raccomandazione**: indicare il cambiamento e la sua entità sui soli contributi che l'utente può influenzare
  (un incasso rientrato, una segnalazione chiusa), e non su quelli che dipendono dal semplice trascorrere del
  tempo. Chiude: **sviluppatore**, con la direzione di prodotto.
- **Se la spiegazione vada mostrata anche al cliente finale, e in quale forma.** Il §6 della descrizione dice che
  entra nell'esportazione dei dati dell'interessato; se e come vada resa comprensibile a chi non conosce il
  modello è materia dell'informativa che il *nostro cliente* deve dare ai *suoi* clienti. Chiude: **revisione
  legale** — punto aperto n. 4 della descrizione.
