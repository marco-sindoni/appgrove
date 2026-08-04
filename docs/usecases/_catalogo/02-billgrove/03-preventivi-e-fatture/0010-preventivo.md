# 0010 — Preventivo

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 03 — Preventivi e fatture
**Storia**: `0010` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0007`, `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come artigiano a cui un cliente chiede «quanto mi costa?»
> voglio preparare un preventivo con le mie voci e i miei prezzi, mandarlo e sapere se è stato accettato
> così da non perdere il lavoro perché mi sono dimenticato di richiamare, e da non dover riscrivere tutto quando
> il cliente dice di sì.

**Contesto.** Il preventivo è il primo anello della catena preventivo → ordine → fattura → incasso, che il catalogo
indica come l'argomento di vendita più forte della suite (§6). È anche il caso d'uso di **ingresso**: molte micro
attività cominciano a usare un prodotto di fatturazione proprio perché devono mandare un preventivo. Va prima della
fattura perché la fattura, nella storia successiva, nasce da qui.

## 2. Requisiti funzionali

1. **RF-1** — Si può creare un preventivo per un cliente, con righe libere o prese dal catalogo, prezzi risolti dal
   listino, validità (data di scadenza dell'offerta) e testo introduttivo.
2. **RF-2** — Il preventivo ha una numerazione **propria e non fiscale**, separata da quella dei documenti fiscali.
3. **RF-3** — Il ciclo di vita è `bozza → inviato → accettato | rifiutato | scaduto`; i passaggi non ammessi vengono
   rifiutati.
4. **RF-4** — Un preventivo `inviato` che supera la data di validità passa a `scaduto` da sé, senza intervento.
5. **RF-5** — Un preventivo `inviato` non è più modificabile: per cambiarlo se ne crea una **revisione**, legata
   all'originale.
6. **RF-6** — L'elenco mostra separatamente i preventivi in attesa di risposta e quelli scaduti da richiamare.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura del preventivo filtra per `tenant_id` preso dal
  token verificato; nessuna forzatura dall'esterno viene accettata.
- **RT-2 — Interfaccia di programmazione (§2).** `GET|POST /api/billing/v1/quotes`,
  `GET|PUT /api/billing/v1/quotes/{id}`, `POST /api/billing/v1/quotes/{id}/status`; corpo validato; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Il preventivo usa la tabella `document` con tipo `preventivo` (storia `0002`), più
  una migrazione per i campi propri: validità, testo introduttivo, riferimento alla revisione precedente.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Preventivi» del modulo `billing`: elenco con filtro di stato,
  composizione delle righe, invio. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `billing` e sono presenti in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** **Il preventivo non consuma la metrica `documenti`**: è lavoro commerciale,
  non documento fiscale, e farlo pagare scoraggerebbe l'uso della parte dell'app che porta dentro i clienti. È una
  decisione di posizionamento dichiarata nel §3 della descrizione dell'applicazione e va annotata nel registro delle
  decisioni.
- **RT-7 — Esposizione conversazionale (§12).** Strumenti dichiarati: `crea_preventivo(cliente, righe) → bozza`,
  marcato **scrittura**, che produce una bozza e richiede conferma umana; `elenca_documenti(tipo='preventivo', …)`,
  marcato **lettura**. Il contratto vive dentro il servizio; il server conversazionale è di piattaforma e non ancora
  implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Nessun campo personale nuovo: il preventivo cita un cliente già dichiarato. Il
  testo introduttivo è però un **campo libero**: va aggiunto al manifesto insieme agli altri campi liberi e
  accompagnato dall'avviso nell'interfaccia.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `preventivo creato`, `preventivo inviato`, `preventivo
  accettato`, `preventivo scaduto` sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di
  correlazione, senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Creazione e invio**
- **Dato** un cliente con listino associato
- **Quando** si crea un preventivo con due voci di catalogo e lo si porta a `inviato`
- **Allora** i prezzi sono quelli del listino, il preventivo ha un numero non fiscale e non è più modificabile

**CA-2 — Revisione**
- **Dato** un preventivo `inviato` · **Quando** si chiede di modificarlo
- **Allora** viene creata una revisione in stato `bozza`, legata all'originale, e l'originale resta invariato

**CA-3 — Scadenza automatica**
- **Dato** un preventivo `inviato` con validità fino a ieri
- **Quando** si consulta l'elenco oggi
- **Allora** risulta `scaduto` e compare fra quelli da richiamare

**CA-4 — Passaggio non ammesso**
- **Dato** un preventivo in stato `bozza` · **Quando** si tenta di portarlo ad `accettato`
- **Allora** la risposta è `409` con l'elenco dei passaggi ammessi, e lo stato non cambia

**CA-5 — Nessun consumo di quota**
- **Dato** un account che ha esaurito la quota `documenti`
- **Quando** crea un preventivo
- **Allora** il preventivo viene creato normalmente: la quota riguarda i documenti emessi, non i preventivi

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` con i propri preventivi
- **Quando** un utente di `A` chiede l'elenco, anche forzando l'identificativo di `B`
- **Allora** vede solo i propri

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sul ciclo di vita e sulla scadenza, di **integrazione** sulla risorsa, con database
      effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sui preventivi;
- [ ] **prova end-to-end**: *coprire ora* — primo passo di dominio del percorso `[J-BILLING]`: crea preventivo →
      invia; registro di copertura aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato per il campo libero del testo introduttivo;
- [ ] **registro delle decisioni** compilato, con annotata la scelta di non far consumare quota al preventivo;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `crea_preventivo`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0006` | Il preventivo si intesta a un cliente |
| storia `0007` | Le righe possono nascere da voci di catalogo |
| storia `0008` | I prezzi proposti vengono dal listino del cliente |

## 7. Fuori ambito

- la conversione in fattura: storia `0011`;
- l'invio al cliente per posta elettronica: la storia porta il preventivo allo stato `inviato`, ma la spedizione
  vera è nella storia `0025`;
- la stampa: storia `0016`;
- l'accettazione da parte del cliente in autonomia (una pagina pubblica dove il cliente clicca «accetto»):
  rimandata, perché apre una superficie non autenticata che merita una storia propria.

## 8. Punti aperti

La sovrapposizione con l'app 06 del catalogo (Preventivi) è aperta: il preventivo non può stare in due app. È il
punto 2 del §11 della descrizione dell'applicazione, e lo chiude lo sviluppatore insieme all'agente dell'app 06.
