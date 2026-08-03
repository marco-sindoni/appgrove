# 0026 — Avvisi di scadenza

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 05 — Tempi di risposta e livello di servizio
**Storia**: `0026` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0010`, `0020`, `0025`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come operatore che ha in mano dodici richieste
> voglio essere avvertito **prima** che una scadenza passi, non dopo
> così da poter ancora fare qualcosa, invece di scoprire a cose fatte che la promessa è saltata.

**Contesto.** Dopo la storia `0025` ogni richiesta porta due scadenze e un tempo residuo: chi le guarda le vede. Il
problema è precisamente questo — *chi le guarda*. È un punto già toccato con mano nel repository: sull'assistenza
interna della piattaforma (UC 0075, change `0084`) la scadenza era visibile nella coda ma **nessuno la ricordava
attivamente**, e il punto è rimasto aperto proprio lì. Qui va chiuso, perché il valore che il cliente compra è
«non mi dimentico più di nessuno» (capofila §2.5, prima voce dell'elenco). La chiusura ha tre pezzi: l'evidenza in
coda, l'ordinamento per urgenza e un avviso che arriva a una persona con un nome — non a una casella generica, che
è esattamente il problema da cui il cliente sta scappando.

## 2. Requisiti funzionali

1. **RF-1** — Ogni richiesta con almeno una scadenza attiva ha uno stato di urgenza calcolato e leggibile: in
   tempo, in avvicinamento, scaduta.
2. **RF-2** — La soglia che fa passare una richiesta «in avvicinamento» è una frazione dell'obiettivo, impostabile
   per account (proposta predefinita: quando è stato consumato il 75% del tempo lavorativo promesso).
3. **RF-3** — L'elenco delle richieste (storia `0010`) mostra l'urgenza a colpo d'occhio, si può ordinare per
   scadenza più vicina e offre una vista «in scadenza e scadute» accanto alle viste già esistenti.
4. **RF-4** — Quando una richiesta entra in «in avvicinamento» l'assegnatario riceve **un solo** avviso; se la
   richiesta non è assegnata, l'avviso va agli operatori che presidiano la coda. Un secondo e ultimo avviso parte
   al momento della violazione.
5. **RF-5** — L'avviso non contiene il contenuto della conversazione: dice che una scadenza si avvicina, riporta il
   numero della richiesta e porta alla pagina, dove l'accesso è controllato dai varchi soliti.
6. **RF-6** — Ogni operatore può silenziare per sé gli avvisi via posta elettronica, senza che questo spenga
   l'evidenza dell'urgenza nell'elenco né gli avvisi degli altri.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il calcolo dell'urgenza, la selezione dei destinatari e la scrittura degli
  avvisi filtrano per `tenant_id` preso dal token verificato; la lavorazione periodica elabora account per account
  con filtro `WHERE tenant_id = :tid`. Un avviso non può in nessun caso riferirsi a una richiesta di un altro
  account, nemmeno quando due account hanno lo stesso numero progressivo di richiesta.
- **RT-2 — Interfaccia di programmazione (§2).** L'urgenza e il tempo residuo viaggiano dentro l'oggetto di
  trasferimento della richiesta già esposto dall'elenco e dal dettaglio; si aggiungono
  `GET|PUT /api/helpdesk/v1/impostazioni/soglia-di-avviso` (soglia dell'account) e
  `GET|PUT /api/helpdesk/v1/operatori/me/preferenze-di-avviso` (silenziamento personale); corpo validato (soglia
  fra il 25% e il 100%); errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__deadline_alerts.sql` sullo schema `app_helpdesk`: colonna della
  soglia sulla configurazione dell'account, tabella `agent_alert_pref` (riferimento all'operatore, avvisi via posta
  attivi o silenziati) e tabella `deadline_alert` (richiesta, tipo di scadenza, livello — avvicinamento o
  violazione —, destinatario, istante di invio), entrambe con `tenant_id`, chiave primaria UUID versione 7, colonne
  di controllo e cancellazione logica. Vincolo di unicità su (`tenant_id`, richiesta, tipo di scadenza, livello,
  destinatario) che garantisce l'unicità dell'avviso di RF-4.
- **RT-4 — Modulo frontend (§3, §5).** Indicatore di urgenza nell'elenco e nel dettaglio della richiesta,
  ordinamento per scadenza, vista «in scadenza e scadute», riquadro della soglia in *Impostazioni → Livello di
  servizio* e interruttore personale nella pagina dell'operatore. Dati letti con il client generato; solo token del
  sistema di design; funziona in tema chiaro e scuro; l'urgenza è sempre accompagnata da un testo e non affidata al
  solo colore.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili e **i testi dei messaggi di avviso** passano dallo
  spazio-nomi `helpdesk` e sono presenti in `en, it, fr, es, de`; l'avviso è scritto nella lingua dell'operatore
  destinatario, non in quella dell'account.
- **RT-6 — Varchi e quota (§6, §7).** Gli avvisi non consumano la metrica `agents` (natura `stock`): sono una
  conseguenza dei posti già occupati. Con abbonamento `canceled` la lavorazione periodica salta l'account e non
  manda avvisi, registrandolo; con `past_due` gli avvisi continuano, coerentemente con il periodo di tolleranza
  della piattaforma.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo. Le richieste in avvicinamento e scadute si
  ottengono con `elenca_richieste(oltre_scadenza)` (storia `0034`), di sola lettura e senza conferma. Mandare un
  avviso non è un'operazione richiamabile: parte dalla lavorazione periodica, non da una chat. Dipendenza di
  piattaforma dichiarata: UC 0061-0063, non ancora implementati.
- **RT-8 — Dati personali (§10).** Nessun campo nuovo che descriva un cliente finale. La tabella `agent_alert_pref`
  porta il riferimento all'operatore, e `deadline_alert` porta il destinatario: entrambe vanno dichiarate nel
  manifesto `docs/compliance/manifests/helpdesk.yaml` in italiano e inglese sotto la voce dell'operatore, con i
  campi annotati `@PersonalData` dove riferiscono una persona, e aggiunte a `exportData` e `purgeData` del contratto
  `HelpdeskDataContract`. Vincolo esplicito, ereditato dalla decisione 14 della change `0084`: **il contenuto della
  conversazione non entra nel messaggio di avviso**, e qui vale di più perché la posta esce dal perimetro dell'app.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «avviso di avvicinamento inviato», «avviso di violazione
  inviato», «avviso saltato per silenziamento», «avviso saltato per abbonamento non attivo» sono registrati con
  `tenant_id`, `app_id`, `user_id` del destinatario e identificativo di correlazione, senza recapiti, senza nomi e
  senza oggetto della richiesta.
- **RT-10 — Invio dei messaggi.** L'avviso via posta elettronica usa il generatore unificato dei messaggi di
  `services/commons` e la sorgente unica `shared/email-templates` (capofila §10, punto 2): DeskGrove aggiunge il
  proprio modello, non duplica il meccanismo. Il destinatario è un **operatore dell'account**, non un cliente
  finale: questo avviso non esce mai verso l'esterno.

## 4. Criteri di accettazione

**CA-1 — L'avviso arriva prima, una volta sola**
- **Dato** una richiesta assegnata con obiettivo di prima risposta di 4 ore lavorative e soglia di avviso al 75%
- **Quando** sono trascorse 3 ore lavorative senza risposta e la lavorazione periodica viene eseguita due volte
- **Allora** l'assegnatario ha ricevuto **un solo** avviso di avvicinamento e la richiesta risulta «in
  avvicinamento» nell'elenco

**CA-2 — Richiesta non assegnata**
- **Dato** una richiesta in coda «Assistenza» senza assegnatario che raggiunge la soglia
- **Quando** la lavorazione periodica viene eseguita
- **Allora** l'avviso raggiunge gli operatori che presidiano quella coda, e nessun operatore estraneo alla coda lo
  riceve

**CA-3 — L'avviso non contiene la conversazione**
- **Dato** una richiesta il cui primo messaggio contiene testo delicato scritto dal cliente finale
- **Quando** parte l'avviso di avvicinamento
- **Allora** il messaggio riporta il numero della richiesta, il tipo di scadenza e il collegamento alla pagina, e
  **nessun** frammento del corpo dei messaggi né l'oggetto della richiesta

**CA-4 — Silenziamento personale**
- **Dato** un operatore che ha silenziato per sé gli avvisi via posta · **Quando** una sua richiesta entra in
  avvicinamento · **Allora** non riceve alcun messaggio, la richiesta risulta comunque «in avvicinamento»
  nell'elenco e gli altri destinatari ricevono il proprio avviso

**CA-5 — Vista e ordinamento**
- **Dato** una coda con richieste in tempo, in avvicinamento e scadute
- **Quando** l'operatore apre la vista «in scadenza e scadute» ordinata per scadenza più vicina
- **Allora** vede in cima la scaduta con il ritardo maggiore e non vede alcuna richiesta in tempo né alcuna
  richiesta con l'orologio in pausa perché in attesa del cliente

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con operatori e richieste in scadenza
- **Quando** la lavorazione periodica viene eseguita
- **Allora** ogni operatore riceve avvisi solo per le richieste del proprio account, e un tentativo di leggere le
  preferenze di avviso di un operatore di `B` da parte di un utente di `A` risponde come se non esistessero

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend e compliance; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo dell'urgenza e sulla scelta dei destinatari, di **integrazione** sulla
      lavorazione periodica con database effimero, migrazioni vere e verifica che un secondo passaggio non produca
      un secondo avviso;
- [ ] prova di **isolamento fra account** su urgenza, destinatari e preferenze;
- [ ] **prova end-to-end**: *rimando* alla storia `0037`, proprietaria del percorso `[J-HELPDESK]`, dove l'avviso di
      scadenza è un passo del percorso; motivo e storia proprietaria annotati nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), compresi i modelli dei messaggi di
      avviso;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con le due tabelle nuove, campi annotati
      `@PersonalData`, tabelle presenti in esportazione e cancellazione;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, in particolare sulla soglia
      predefinita e sul divieto di mettere il contenuto nella notifica;
- [ ] contratto degli **strumenti conversazionali**: nessuna aggiunta, motivo annotato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata: il punto aperto di UC 0075 sull'avviso di scadenza va marcato come chiuso qui, per
      l'app `helpdesk`, senza toccare il comportamento dell'assistenza interna.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0025` (orologi e violazioni) | Senza scadenza e tempo residuo non c'è nulla da anticipare |
| storia `0020` (presa in carico e assegnazione) | L'avviso va a una persona: serve l'assegnatario |
| storia `0010` (elenco, ricerca e viste) | L'evidenza e l'ordinamento si innestano sull'elenco esistente |
| generatore unificato dei messaggi di posta di `services/commons` (UC 0085) | L'avviso via posta riusa il meccanismo esistente e i modelli condivisi |

## 7. Fuori ambito

- **La registrazione della violazione**: già fatta dalla storia `0025`. Qui si manda solo l'avviso.
- **Gli avvisi al cliente finale** («la tua richiesta è in ritardo»): non si fanno, e non per dimenticanza — dire a
  un cliente che siamo in ritardo prima ancora di aver risposto peggiora la situazione. Se servisse, sarebbe una
  decisione di direzione di prodotto e una storia delle epiche del portale.
- **L'inoltro automatico al responsabile alla scadenza** (`inoltra_richiesta` eseguito da solo): rimandato perché è
  un'automazione a più rami, quella famiglia di funzioni che il capofila §2.5 esclude. L'inoltro resta un gesto di
  una persona.
- **La riassegnazione automatica** della richiesta scaduta a un altro operatore: stessa ragione.
- **I canali di avviso diversi dalla posta elettronica e dall'evidenza in coda** (messaggio breve, notifica sul
  telefono, integrazione con strumenti di squadra): rimandati, ciascuno introdurrebbe un fornitore esterno.

## 8. Punti aperti

- **La soglia predefinita del 75%** è una proposta: su un obiettivo di 2 ore lavorative lascia 30 minuti di
  reazione, che potrebbero essere pochi. Un'alternativa è una soglia mista («al 75% oppure a un'ora dalla scadenza,
  la prima che si verifica»). **Decide lo sviluppatore.**
- **Se l'avviso di violazione debba andare anche al titolare dell'account** oltre che all'assegnatario: è una scelta
  di rapporto fra le persone dell'azienda cliente, non una scelta tecnica, e può essere vissuta come sorveglianza.
  **Decide lo sviluppatore.**
- **La chiusura formale del punto aperto di UC 0075** (l'avviso attivo sull'assistenza interna della piattaforma):
  qui si chiude **solo** per l'app `helpdesk`. Se convenga riportare la stessa soluzione anche sull'assistenza
  interna lo decide chi possiede UC 0075, ed è fuori dal perimetro di questa applicazione.
