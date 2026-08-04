# 0007 — Lettura automatica dei dati della ricevuta

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 02 — Cattura e lettura della ricevuta
**Storia**: `0007` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come collaboratore che ha appena fotografato uno scontrino
> voglio che l'app provi a leggere da sola data, esercente, imponibile, imposta e totale, dicendomi quanto è sicura
> di ciascun valore
> così da dover solo controllare invece di ricopiare, ma sapendo dove guardare per primo.

**Contesto.** È la funzione che il catalogo mette al centro dell'app («OCR + estrazione») ed è anche la più facile
da raccontare male. **Non è una funzione che restituisce fatti: è una funzione che restituisce ipotesi.** Le
rassegne di prodotto riportano lentezza e incostanza dell'estrazione fra le lamentele ricorrenti, e chi la vende
promette «oltre il 99% di accuratezza» — cioè ammette che sbaglia (descrizione dell'applicazione, §2.5). Da qui
discendono i due requisiti che rendono questa storia diversa da un semplice collegamento a un servizio esterno: la
**fiducia dichiarata campo per campo** e il fatto che l'esito **non diventa mai un dato confermato da solo**.

## 2. Requisiti funzionali

1. **RF-1** — Caricata una ricevuta, la lettura parte **in modo asincrono** e la spesa passa da `caricata` a
   `letta` quando l'esito arriva; l'interfaccia mostra nel frattempo uno stato di lavorazione, non una schermata
   ferma.
2. **RF-2** — L'esito contiene, per **ciascun** campo estratto (data, esercente, partita IVA dell'esercente,
   imponibile, imposta, totale, valuta, tipo di documento, mezzo di pagamento se leggibile): il valore proposto e
   una **fiducia da 0 a 100**.
3. **RF-3** — L'esito è conservato **integralmente e separatamente** dai valori della spesa: si deve poter
   rispondere per sempre alla domanda «che cosa aveva letto la macchina, e che cosa ha corretto la persona».
4. **RF-4** — La spesa passa a `da_rivedere` con i valori proposti precompilati, **mai** a `confermata`: nessun
   percorso, nemmeno di configurazione, permette di saltare la revisione umana.
5. **RF-5** — Se la lettura fallisce o riesce solo in parte, la spesa passa comunque a `da_rivedere` con i campi
   vuoti e un motivo leggibile («immagine troppo scura», «nessun totale riconosciuto»): un fallimento non blocca il
   ciclo, lo rende manuale.
6. **RF-6** — La lettura si può **ripetere** su richiesta (per esempio dopo aver ricaricato una foto migliore) e la
   ripetizione non consuma quota e non cancella l'esito precedente, che resta nello storico.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'esito della lettura è legato alla ricevuta e filtra per `tenant_id`
  preso dal token verificato; il lavoro asincrono porta con sé l'account e non lo desume dal contenuto.
- **RT-2 — Interfaccia di programmazione (§2).** `POST /api/notespese/v1/ricevute/{id}/lettura` avvia o ripete la
  lettura; `GET /api/notespese/v1/ricevute/{id}/lettura` restituisce l'ultimo esito con le fiducie. Errori in
  `application/problem+json`, con distinzione fra «fornitore non raggiungibile» (riprovabile) e «documento non
  interpretabile» (definitivo). Definizione OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V4__esito_lettura.sql`: tabella `esito_lettura` con `tenant_id`, chiave
  UUID versione 7, riferimento logico alla ricevuta, valori estratti, fiducia per campo, fornitore e versione del
  modello, tempo di risposta, stato, colonne di controllo e cancellazione logica. Più esiti per ricevuta sono
  ammessi: **non si sovrascrive**, si aggiunge.
- **RT-4 — Modulo frontend (§3, §5).** Nella sezione *Spese*, lo stato «in lettura» e l'esito con i campi
  precompilati. La schermata di revisione vera e propria è della storia `0008`.
- **RT-5 — Cinque lingue (§4).** I motivi di fallimento sono **codici**, non frasi, e la frase mostrata all'utente
  vive nello spazio-nomi `notespese` in tutte e cinque le lingue: un messaggio che arriva già scritto dal fornitore
  è un messaggio che sarà sempre in una lingua sola.
- **RT-6 — Varchi e quota (§6, §7).** La lettura **non** consuma quota (si consuma alla conferma, storia `0004`).
  Esiste però un limite tecnico sul numero di ripetizioni per ricevuta, perché ogni chiamata al fornitore ha un
  costo reale.
- **RT-7 — Esposizione conversazionale (§12).** La storia dichiara lo strumento
  `leggi_ricevuta(riferimento del file caricato) → bozza di spesa con la fiducia per campo`, marcato **scrittura**:
  produce una bozza in `da_rivedere` e **richiede conferma umana**; non esiste un percorso in cui l'assistente
  confermi la propria estrazione. Il contratto vive dentro il servizio; il server conversazionale è di piattaforma e
  non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** 🛑 **La storia introduce un responsabile esterno del trattamento**: il fornitore
  della lettura riceve **l'immagine intera** del documento, cioè anche ciò che non abbiamo chiesto (descrizione,
  §6). Requisiti non negoziabili: il fornitore sta **dietro un'interfaccia interna** (`EstrattoreRicevuta`) con
  almeno due realizzazioni — quella vera e quella simulata usata in locale e nelle prove — così da poterlo
  sostituire senza toccare il resto; il contratto di responsabile, il trattamento in Unione Europea, il divieto di
  riuso per addestramento e la cancellazione dopo l'elaborazione sono **prerequisiti dell'attivazione**, non
  dettagli successivi. Voci nuove nel manifesto in italiano e inglese per i campi estratti; il fornitore entra
  nell'elenco dei responsabili esterni e nell'informativa.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `lettura avviata`, `lettura riuscita`, `lettura fallita`,
  `lettura ripetuta` portano `tenant_id`, `app_id`, `user_id`, identificativo di correlazione, fornitore, tempo di
  risposta e **fiducia media** — **mai** i valori estratti, che sono dati personali.

## 4. Criteri di accettazione

**CA-1 — Lettura riuscita con fiducia dichiarata**
- **Dato** una ricevuta caricata e il fornitore simulato che risponde con valori completi
- **Quando** la lettura termina
- **Allora** la spesa è in `da_rivedere`, i campi sono precompilati e ogni campo mostra la sua fiducia; **nessun**
  campo risulta confermato

**CA-2 — Lettura parziale**
- **Dato** una ricevuta di cui il fornitore riconosce solo data e totale
- **Quando** la lettura termina
- **Allora** la spesa è in `da_rivedere`, esercente e imposta sono vuoti con fiducia 0, e il motivo dice quali campi
  non sono stati riconosciuti

**CA-3 — Fornitore non raggiungibile**
- **Dato** il fornitore che non risponde · **Quando** si carica una ricevuta
- **Allora** dopo i tentativi previsti la spesa passa comunque a `da_rivedere` con i campi vuoti e un motivo
  riprovabile; il ciclo di lavoro dell'utente **non** si blocca

**CA-4 — Lo storico dell'esito non si perde**
- **Dato** una ricevuta già letta una volta · **Quando** si ripete la lettura
- **Allora** esistono due esiti, il più recente è quello mostrato, e resta possibile sapere cosa diceva il primo

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` · **Quando** un utente di `A` chiede l'esito di lettura di una ricevuta di `B`
- **Allora** riceve `404`, e nessun dato dell'account `B` compare in risposta

**CA-6 — Nessuna scorciatoia sulla conferma**
- **Dato** un esito con fiducia 100 su tutti i campi
- **Quando** la lettura termina
- **Allora** la spesa resta in `da_rivedere`: la fiducia alta cambia il colore del campo, non lo stato della spesa

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend, frontend e compliance; l'intera suite prima del commit);
- [ ] prove di **unità** sulla mappatura esito → campi e sulla soglia di fiducia; di **integrazione** sul ciclo
      asincrono con database effimero, migrazioni vere e **fornitore simulato** (in locale il fornitore è sempre
      simulato: nessuna chiamata reale nelle prove);
- [ ] prova di **isolamento fra account** su `esito_lettura`;
- [ ] **prova end-to-end**: *coprire ora* il passo «la ricevuta viene letta e la spesa attende revisione» nel
      percorso `[J-NOTESPESE]`, con esito simulato deterministico; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** dei motivi di fallimento in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese; **fornitore esterno dichiarato** fra i responsabili
      del trattamento, con base contrattuale verificata prima dell'attivazione;
- [ ] **registro delle decisioni** compilato: quale fornitore, perché, dove tratta i dati, e perché sta dietro
      un'interfaccia;
- [ ] contratto dello strumento `leggi_ricevuta` dichiarato dentro il servizio, marcato scrittura con conferma;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0006` | Senza il file archiviato non c'è nulla da leggere |
| Scelta del fornitore di lettura automatica | È insieme una decisione economica e di conformità: punto aperto n. 2 della descrizione dell'applicazione |
| UC 0061-0063 (livello conversazionale) | Lo strumento `leggi_ricevuta` è dichiarato ma non esposto: il server non esiste ancora |

## 7. Fuori ambito

- La schermata di revisione e correzione: storia `0008`.
- La categorizzazione automatica: storia `0010`. Qui si leggono i campi del documento, non se ne interpreta il
  significato aziendale.
- Il riconoscimento delle singole righe dello scontrino (che cosa si è comprato): non serve al ciclo della nota
  spese e moltiplicherebbe i dati personali raccolti. Deliberatamente escluso.

## 8. Punti aperti

- **Quale fornitore, a che costo per documento, con quale trattamento dei dati** — è il punto aperto n. 2 della
  descrizione dell'applicazione, e va chiuso **prima** di implementare, non durante: incide sul margine del piano
  base e sull'elenco dei responsabili esterni.
- **Elaborazione in casa** invece che da terzi: eliminerebbe il responsabile esterno più delicato ma sposta il costo
  sull'infrastruttura. Da valutare, non da decidere qui.
- **Soglia di fiducia sotto la quale un campo si mostra come «da controllare»**: proposta 80, ma è un numero da
  tarare sui dati veri, non da fissare a tavolino.
