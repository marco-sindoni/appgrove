# 0022 — Sessione di inventario fisico

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 04 — Inventario fisico, rettifiche e valore
**Storia**: `0022` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0008`, `0013`, `0021`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di una piccola impresa
> voglio aprire una sessione di conteggio su un deposito e contare la merce in più persone, anche in giorni diversi
> così da fare l'inventario di fine anno senza fermare il lavoro e senza ricominciare da capo se mi interrompono.

**Contesto.** Contare il magazzino è la cosa che il nostro cliente fa una volta l'anno e detesta: le fonti raccolte
nell'analisi in rete lo dicono con parole sue — «contare una volta l'anno senza impazzire» è una delle quattro
richieste esplicite del segmento (descrizione dell'applicazione, §2.5). Con la storia `0021` si può già correggere
un articolo alla volta, ma nessuno conta quattrocento referenze aprendo quattrocento moduli. Serve un contenitore:
una sessione che sa cosa c'è da contare, si ricorda cosa è già stato contato e resta aperta fra il venerdì e il
lunedì. Questa storia costruisce il contenitore e il conteggio; la chiusura e le differenze sono la storia `0023`.

## 2. Requisiti funzionali

1. **RF-1** — Si apre una sessione di inventario su **un** deposito, con un ambito scelto fra: tutto il deposito,
   una categoria di articoli, una ubicazione. All'apertura la sessione riceve le righe da contare — un articolo per
   riga — e passa in stato `aperta`.
2. **RF-2** — All'apertura ogni riga **congela la quantità attesa**, cioè la giacenza dell'articolo in quel deposito
   in quell'istante, insieme al momento del congelamento. Il valore congelato non cambia più per tutta la vita
   della sessione.
3. **RF-3** — Si registra la quantità contata su una riga alla volta; ogni riga conserva **chi** l'ha contata e
   **quando**. Una riga può essere contata più volte: vale l'ultimo conteggio, e i precedenti restano visibili nel
   suo storico.
4. **RF-4** — La sessione distingue tre condizioni di riga: **non contata**, **contata** (con un numero, anche
   zero), **saltata** (dichiarata esplicitamente fuori conteggio, con nota facoltativa). Una riga non contata non è
   una riga contata a zero.
5. **RF-5** — La sessione si può interrompere e riprendere senza limiti di tempo: chiuderla è un atto esplicito
   (storia `0023`). Si può **annullare** una sessione aperta, che passa in stato `annullata` senza produrre alcun
   movimento.
6. **RF-6** — I movimenti ordinari **continuano** durante il conteggio: carichi, scarichi e trasferimenti non sono
   bloccati. Ogni riga mostra, accanto all'atteso congelato, la **giacenza corrente** e segnala quando le due
   divergono, perché significa che quell'articolo si è mosso mentre si contava.
7. **RF-7** — Una sessione mostra in ogni momento il proprio avanzamento: righe contate, saltate, mancanti, e chi
   ha contato cosa **come informazione di tracciabilità della merce**, senza alcuna classifica né conteggio per
   persona.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `inventario` e `riga_inventario` filtra per
  `tenant_id` preso dal token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri
  viene ignorato. Prova di isolamento fra due account su entrambe le risorse.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/magazzino/v1/inventari`,
  `GET /api/magazzino/v1/inventari/{id}`, `GET|PATCH /api/magazzino/v1/inventari/{id}/righe` con paginazione a
  pagina/dimensione e totale; oggetti di trasferimento al bordo (le entità non si espongono mai); errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V14__inventario.sql` sullo schema `app_magazzino`: tabelle `inventario`
  (deposito, ambito, stato, apertura, chiusura, autore) e `riga_inventario` (articolo, ubicazione,
  `quantita_attesa_congelata`, `momento_congelamento`, `quantita_contata`, `contato_da`, `contato_il`, condizione,
  nota), entrambe con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica.
  Nessuna chiave esterna verso altri schemi. Indice su `(tenant_id, inventario_id, articolo_id)` con unicità: lo
  stesso articolo non compare due volte nella stessa sessione.
- **RT-4 — Modulo frontend (§3, §5).** Nuova sezione `inventari` nel manifesto del modulo `magazzino`: elenco delle
  sessioni, apertura guidata, schermata di conteggio pensata **per il telefono** (una riga grande, tastiera
  numerica, avanzamento sempre visibile). Dati letti con il client generato; solo token del sistema di design,
  colore-categoria `amber`; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Etichette degli stati, delle condizioni di riga, dei tipi di ambito e dei messaggi
  passano dallo spazio-nomi `magazzino` e sono presenti in `en, it, fr, es, de`; nessun testo visibile scritto a
  mano nei componenti.
- **RT-6 — Varchi e quota (§6, §7).** Aprire una sessione e contare **non consumano quota e non vengono mai
  respinti con `429`**: il tetto `articoli_gestiti` (natura `stock`) colpisce solo la creazione di articoli nuovi.
  Un inventario è la verifica di ciò che si possiede già. Valgono gli altri varchi: `401`, `402` con abbonamento
  `canceled`, `403` per ruolo insufficiente.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento di scrittura dichiarato qui: aprire un inventario
  a voce non è un gesto che qualcuno fa. Lo strumento `chiudi_inventario`, che è il momento pericoloso, appartiene
  alla storia `0023` e al contratto della storia `0035`; gli strumenti di lettura sull'avanzamento restano fuori
  perimetro. Il server conversazionale è di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** `inventario.created_by` e `riga_inventario.contato_da` sono **dati sull'attività
  di un lavoratore** già dichiarati nel manifesto `docs/compliance/manifests/magazzino.yaml` dalla storia `0010`,
  in italiano e inglese, con i campi annotati `@PersonalData`: questa storia verifica che le due tabelle nuove
  siano presenti in `exportData` e `purgeData` del contratto `MagazzinoDataContract`. La nota di riga è testo
  libero e porta l'avviso «campo a testo libero: non inserire dati sensibili». **Niente indicatori di produttività
  per persona**: nessuna schermata mostra «righe contate per operatore» (art. 4 della legge 300/1970, Statuto dei
  lavoratori; descrizione, §6).
- **RT-9 — Registrazione eventi (§14).** Gli eventi `inventario aperto`, `riga contata`, `riga saltata`,
  `inventario annullato` sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione,
  con articolo e deposito come identificativi e **senza** le note a testo libero.

## 4. Criteri di accettazione

**CA-1 — Apertura con atteso congelato**
- **Dato** un deposito con tre articoli in giacenza 10, 4 e 0
- **Quando** un utente apre una sessione di inventario sull'intero deposito
- **Allora** la sessione è in stato `aperta` con tre righe, ciascuna con `quantita_attesa_congelata` pari a 10, 4 e
  0 e il momento del congelamento valorizzato

**CA-2 — Il congelato non si muove, il corrente sì**
- **Dato** una sessione aperta con una riga il cui atteso congelato è 10
- **Quando** nel frattempo qualcuno registra uno scarico di 3 sullo stesso articolo e deposito
- **Allora** la riga mostra ancora atteso congelato 10, giacenza corrente 7, e segnala che l'articolo si è mosso
  durante il conteggio

**CA-3 — Conteggio ripetuto**
- **Dato** una riga già contata a 9 · **Quando** la stessa riga viene contata a 8 da un'altra persona
- **Allora** la riga vale 8, porta il nome di chi ha fatto l'ultimo conteggio e il momento, e il conteggio
  precedente resta leggibile nel suo storico

**CA-4 — Riga non contata e riga contata a zero**
- **Dato** una sessione con una riga mai toccata e una riga contata con il valore 0
- **Quando** si legge l'avanzamento della sessione
- **Allora** la prima risulta `non_contata` e la seconda `contata` con quantità 0, e le due non sono mai
  conteggiate insieme

**CA-5 — Ripresa dopo interruzione**
- **Dato** una sessione `aperta` con dieci righe di cui quattro contate, lasciata da due giorni
- **Quando** un utente la riapre
- **Allora** ritrova esattamente le quattro righe contate con i loro valori e le sei mancanti, senza alcuna
  ricostruzione né perdita

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con le proprie sessioni di inventario
- **Quando** un utente di `A` chiede l'elenco delle sessioni o tenta di leggere una sessione di `B`
- **Allora** vede solo le proprie e riceve `404` sulla sessione altrui, anche forzando l'identificativo dell'altro
  account nella richiesta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend, frontend e compliance; l'intera suite prima del commit);
- [ ] prove di **unità** sulla generazione delle righe per ambito e sulle condizioni di riga, di **integrazione**
      sulle rotte delle sessioni e delle righe, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su `inventario` e `riga_inventario`;
- [ ] **prova end-to-end**: *rimando* — il percorso completo «conta, trova una differenza, rettifica» è di proprietà
      della storia `0037`; il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve lì la voce;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati** verificato: `inventario` e `riga_inventario` presenti in esportazione e cancellazione,
      campi dell'autore annotati;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con la scelta del congelamento
      all'apertura e delle scritture non bloccate durante il conteggio;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione esposta in questa storia, e detto perché;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0008` | Depositi e ubicazioni: l'ambito della sessione si esprime su di essi |
| `0013` | Registro e proiezione della giacenza: l'atteso si congela leggendo la proiezione |
| `0021` | La rettifica esiste già ed è la sola via per cambiare un saldo: la chiusura (`0023`) la riuserà riga per riga |

## 7. Fuori ambito

- **La chiusura della sessione, il calcolo delle differenze e la generazione delle rettifiche**: sono la storia
  `0023`. Qui la sessione si apre, si conta, si interrompe, si riprende e al massimo si annulla.
- **Il conteggio con la fotocamera**, cioè inquadrare un codice per posizionarsi sulla riga giusta: epica 06,
  storie `0030`-`0032`. Qui l'articolo si cerca per codice o descrizione.
- **Il valore in denaro di quello che si conta**: storia `0025`.
- **Il blocco dei movimenti durante l'inventario**: deliberatamente non fatto, vedi i punti aperti.

## 8. Punti aperti

- **Perché la differenza si calcola sulla giacenza corrente e non sull'atteso congelato.** È la scelta di progetto
  di questa storia e va detta per esteso, perché è controintuitiva. Il congelato serve a due cose: mostrare a chi
  conta quale numero il sistema si aspettava, e riconoscere gli articoli che si sono mossi durante il conteggio.
  Ma la rettifica prodotta alla chiusura deve portare il saldo a coincidere con quello che **c'è davvero adesso**:
  se la calcolassimo sul congelato, i movimenti regolari avvenuti durante il conteggio verrebbero cancellati due
  volte — una perché già registrati, una perché inglobati nella differenza. La conseguenza da accettare è che un
  articolo movimentato **dopo** essere stato contato produce una differenza che non è un errore di magazzino ma un
  effetto del tempo trascorso: per questo la riga lo segnala e la chiusura (`0023`) mostra la segnalazione prima di
  confermare. La proposta alternativa — congelare tutto e bloccare i movimenti per la durata del conteggio — è
  praticabile in un magazzino che chiude, non in un negozio che vende: **non la adottiamo**, ma la decisione
  spetta allo sviluppatore.
- **Sessioni parallele sullo stesso deposito.** La proposta è di ammettere una sola sessione aperta per deposito,
  per non avere due conteggi contraddittori sullo stesso articolo. Chi ha un magazzino grande e vuole contare per
  corsie userà l'ambito per ubicazione. Da confermare.
- **Durata massima di una sessione aperta.** Non c'è: una sessione dimenticata da sei mesi resta aperta e il suo
  congelato diventa privo di senso. Serve almeno un avviso, forse una scadenza. Punto di prodotto, non di questa
  storia.
