# 0019 — Solleciti automatici

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 04 — Incassi e solleciti
**Storia**: `0019` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0018`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare a cui pesa telefonare per chiedere i propri soldi
> voglio che il sistema mandi un promemoria educato quando una scadenza passa
> così da farmi pagare prima senza dover fare io la parte di quello che insiste, e senza dimenticarmi di nessuno.

**Contesto.** Il sollecito automatico è nella scheda di catalogo dell'app ed è la funzione con il ritorno più
immediato: chi sollecita incassa prima. Il confine con CashGrove (3) va tenuto stretto: qui c'è il **sollecito
semplice** — una regola a soglie di giorni, un messaggio, un registro degli invii — non la sequenza multicanale con
punteggio di rischio. Va dopo lo scadenzario perché senza scadenze non c'è nulla da sollecitare.

## 2. Requisiti funzionali

1. **RF-1** — L'account definisce fino a tre regole di sollecito, ciascuna con una soglia di giorni di ritardo e un
   testo del messaggio.
2. **RF-2** — Il sollecito parte automaticamente per le scadenze che superano la soglia, una sola volta per regola e
   per scadenza.
3. **RF-3** — I solleciti si possono **sospendere** per un singolo cliente o per un singolo documento, con un motivo
   scritto.
4. **RF-4** — Esiste un registro degli invii, consultabile dalla scheda del cliente e da quella del documento.
5. **RF-5** — Prima di attivare l'automatismo, l'account deve confermarlo esplicitamente una volta: nessun messaggio
   parte verso un cliente senza che qualcuno l'abbia voluto.
6. **RF-6** — Un sollecito si può mandare anche a mano, senza aspettare la soglia.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Regole, sospensioni e registro degli invii filtrano per `tenant_id` preso
  dal token verificato. La lavorazione programmata elabora **un account alla volta**, con il contesto del tenant
  impostato in modo esplicito: è il punto in cui una lavorazione in blocco scritta male scavalca l'isolamento.
- **RT-2 — Interfaccia di programmazione (§2).** `GET|PUT /api/billing/v1/dunning-rules`,
  `POST /api/billing/v1/documents/{id}/dunning` (invio a mano),
  `POST /api/billing/v1/customers/{id}/dunning-hold` (sospensione); errori in `application/problem+json`;
  definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V10__dunning.sql` sullo schema `app_billing`: tabelle `dunning_rule`,
  `dunning_hold` e `dunning_log` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e
  cancellazione logica. `dunning_log` porta il destinatario dell'invio: è un dato personale.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Solleciti» nelle Impostazioni per le regole; sulla scheda del
  documento il registro degli invii e il pulsante di sospensione. Solo token del sistema di design; tema chiaro e
  scuro.
- **RT-5 — Cinque lingue (§4).** L'interfaccia è nelle cinque lingue. Il **testo del messaggio** al cliente è invece
  scritto dall'account: non si traduce, ma la lingua del destinatario conta (storia `0023`).
- **RT-6 — Varchi e quota (§6).** Il sollecito **non consuma** la metrica `documenti`. Ma la lavorazione
  programmata deve rispettare l'abilitazione: per un account con abbonamento `canceled` non parte nessun sollecito.
  Le regole si modificano con ruolo `admin`.
- **RT-7 — Esposizione conversazionale (§12).** **Nessuno strumento di scrittura**: mandare un messaggio a un
  cliente è un effetto verso l'esterno, e in questa stesura si comanda solo dall'interfaccia. La lettura del
  registro degli invii è coperta da `leggi_documento` (epica 06). Va dichiarato, perché è una scelta e non una
  dimenticanza.
- **RT-8 — Dati personali (§10).** **Voce nuova nel manifesto** in italiano e inglese: `dunning_log.destinatario`,
  con finalità «sollecito di pagamento», base giuridica «esecuzione del contratto» e conservazione limitata.
  `dunning_log` va aggiunta a `exportData` e `purgeData`: è la tabella che ci si dimentica, ed è quella che contiene
  un indirizzo. L'invio del messaggio richiede un **fornitore esterno di posta elettronica**, che tratta dati per
  nostro conto: va dichiarato prima di attivarlo (vedi storia `0025`).
- **RT-9 — Registrazione eventi (§14).** Gli eventi `sollecito inviato`, `sollecito sospeso` e `sollecito non
  inviato per abbonamento non attivo` sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di
  correlazione, **senza l'indirizzo del destinatario**.

## 4. Criteri di accettazione

**CA-1 — Sollecito automatico**
- **Dato** una regola a 7 giorni di ritardo e una scadenza scaduta da 8 giorni
- **Quando** la lavorazione programmata gira
- **Allora** parte un sollecito, e il registro degli invii lo riporta con data e regola

**CA-2 — Una volta sola**
- **Dato** la stessa scadenza e lo stesso sollecito già inviato
- **Quando** la lavorazione gira di nuovo il giorno dopo
- **Allora** non parte un secondo sollecito per quella regola

**CA-3 — Sospensione**
- **Dato** un cliente con solleciti sospesi e una sua scadenza oltre la soglia
- **Quando** la lavorazione gira
- **Allora** non parte alcun sollecito, e il motivo della sospensione resta consultabile

**CA-4 — Abbonamento non attivo**
- **Dato** un account con abbonamento `canceled` e scadenze oltre soglia
- **Quando** la lavorazione gira · **Allora** non parte nulla per quell'account

**CA-5 — Isolamento fra account**
- **Dato** due account con regole diverse
- **Quando** la lavorazione programmata gira
- **Allora** ciascun account riceve solo i solleciti delle proprie scadenze, e nessun documento dell'uno compare nel
  registro dell'altro

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sulla scelta delle scadenze da sollecitare e di **integrazione** sulla lavorazione
      programmata, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla lavorazione in blocco: è la prova più importante di questa storia;
- [ ] **prova end-to-end**: *rimando* — la lavorazione programmata non si presta al percorso end-to-end
      interattivo; è coperta da prove di integrazione. Proprietaria del rimando: storia `0031`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con `dunning_log.destinatario`, tabella presente in esportazione e
      cancellazione;
- [ ] **registro delle decisioni** compilato, con annotata la scelta di non esporre solleciti alla chat;
- [ ] contratto degli **strumenti conversazionali**: nessuno di scrittura, con la motivazione scritta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0018` | Senza scadenze non c'è nulla da sollecitare |
| storia `0025` | L'invio vero passa dall'adattatore esterno costruito lì; finché non c'è, il sollecito si limita a produrre il messaggio e a registrarlo |

## 7. Fuori ambito

- i solleciti su più canali (messaggistica, telefono): sono di CashGrove (3);
- l'aumento progressivo del tono e le lettere di messa in mora: idem;
- la promessa di pagamento registrata: idem.

## 8. Punti aperti

L'attivazione dell'automatismo è un **effetto verso l'esterno**: manda messaggi a persone. La conferma esplicita
una tantum prevista dal requisito RF-5 è la protezione che propongo, ma se sia sufficiente — o se serva una conferma
per ogni regola, o un periodo di prova in cui i messaggi non partono davvero — è una decisione di prodotto che
spetta allo sviluppatore.
