# 0027 — Indagine di soddisfazione

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 05 — Tempi di risposta e livello di servizio
**Storia**: `0027` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0009`, `0012`, `0015`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha appena chiuso trenta richieste in un mese
> voglio sapere se i clienti sono rimasti contenti di come li abbiamo trattati
> così da accorgermi che un modo di rispondere non funziona prima che il cliente smetta di comprare.

**Contesto.** Fin qui l'app misura **noi**: quanto ci mettiamo, quante scadenze saltiamo. Nessuno di quei numeri dice
se la risposta era *buona*. L'indagine di soddisfazione è l'unica misura che viene da fuori, ed è considerata dal
mercato una funzione «da piano superiore» (capofila §2.6, fonte 2: Zoho Desk non la mette nel piano d'ingresso) —
motivo in più per averla in tutti i piani, perché costa poco ed è la sola voce del cliente finale che entra nel
prodotto. Il vincolo che ne disegna la forma è che **il richiedente non è un utente di appgrove**: non ha un account,
non ha un token, non si registrerà mai (capofila §2.5, quarta voce). Da qui il collegamento monouso a scadenza. E da
qui il rischio: un commento libero scritto da una persona esterna è una delle quattro sorgenti di testo non
presidiato dell'app, con tutto ciò che ne consegue sull'articolo 9 (capofila §6).

## 2. Requisiti funzionali

1. **RF-1** — Alla chiusura di una richiesta, se l'indagine è attiva per l'account, il servizio genera un invito con
   un collegamento monouso e a scadenza e lo manda al richiedente sul recapito della richiesta.
2. **RF-2** — Il collegamento apre una pagina pubblica che non richiede registrazione e non crea alcun utente:
   chiede un voto su tre livelli — insoddisfatto, neutro, soddisfatto — e un commento libero facoltativo.
3. **RF-3** — Il voto si può esprimere **una volta sola** per richiesta: dopo l'invio il gettone è consumato e una
   seconda apertura mostra il ringraziamento senza permettere di votare di nuovo.
4. **RF-4** — Nessun sollecito: se il richiedente non vota, non riceve altri messaggi per quella richiesta.
5. **RF-5** — Il gettone scade dopo un periodo breve (proposta: 14 giorni di calendario); un gettone scaduto,
   consumato o inesistente produce la **stessa** risposta indistinguibile, senza mostrare alcun dato della
   richiesta.
6. **RF-6** — Il commento ricevuto viene passato al riconoscitore deterministico delle categorie particolari e, se
   dà segnale, la richiesta viene contrassegnata «da guardare con attenzione» **senza registrare quale** categoria
   sarebbe stata riconosciuta.
7. **RF-7** — Voto e commento sono visibili all'operatore sulla richiesta; l'indagine si può disattivare per
   l'intero account e per la singola richiesta prima della chiusura.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La pagina del voto è una superficie raggiungibile **senza token**: il
  `tenant_id` non arriva da un token ma è **legato al gettone** e verificato lato servizio, mai accettato da
  parametri o corpo. Tutte le letture e scritture nel backoffice filtrano per `tenant_id` preso dal token
  verificato. Prova di isolamento rafforzata: il gettone di un account non deve mostrare né scrivere nulla su un
  altro account, nemmeno per errore di indice.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte pubbliche `GET /api/helpdesk/v1/pubblico/soddisfazione/{gettone}`
  e `POST /api/helpdesk/v1/pubblico/soddisfazione/{gettone}`, con limitazione della frequenza per gettone e per
  indirizzo di provenienza; rotte interne `GET /api/helpdesk/v1/richieste/{id}/soddisfazione` e
  `GET|PUT /api/helpdesk/v1/impostazioni/soddisfazione`; corpo validato (voto fra i tre ammessi, commento con
  lunghezza massima); errori in `application/problem+json` **senza rivelare** se un gettone è scaduto, consumato o
  inesistente; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__satisfaction_survey.sql` sullo schema `app_helpdesk`: tabella
  `satisfaction_survey` (richiesta, richiedente, voto, commento, istante dell'invito, istante del voto, scadenza
  del gettone, istante del consumo) con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e
  cancellazione logica. Il valore del gettone **non si conserva in chiaro**: si conserva la sua impronta, e il
  valore esiste solo dentro il collegamento inviato. Vincolo di unicità su (`tenant_id`, richiesta): al più
  un'indagine per richiesta.
- **RT-4 — Modulo frontend (§3, §5).** Due superfici distinte: la **pagina pubblica** del voto, minima, senza barra
  laterale e senza accesso alla shell del backoffice; e, dentro il modulo `helpdesk`, il riquadro del voto sul
  dettaglio della richiesta più l'interruttore in *Impostazioni*. Entrambe usano solo i token del sistema di design
  e funzionano in tema chiaro e scuro; la pagina pubblica dichiara di non voler essere indicizzata dai motori di
  ricerca.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili e il testo del messaggio di invito passano dallo
  spazio-nomi `helpdesk` e sono presenti in `en, it, fr, es, de`. La pagina pubblica e l'invito sono nella lingua
  preferita del richiedente (`requester.locale`, storia `0012`), con ripiego sulla lingua predefinita dell'account.
- **RT-6 — Varchi e quota (§6, §7).** L'indagine non consuma la metrica `agents` (natura `stock`) e la pagina
  pubblica non consuma quota. La storia **non fissa prezzi**: l'indagine sta in tutti i piani, secondo la proposta
  del capofila §5. Con abbonamento `canceled` non si generano nuovi inviti e i collegamenti già emessi smettono di
  funzionare, dicendolo senza esporre dati.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento di scrittura: generare o rimandare un invito è un
  atto verso una persona esterna e non si comanda da una chat. In lettura, il voto medio entra in
  `stato_del_servizio(periodo)` (storia `0028`) e il singolo voto è parte di `leggi_richiesta(numero)` (storia
  `0034`), entrambi di sola lettura e senza conferma. Esclusione della scrittura deliberata, annotata nel registro
  delle decisioni; dipendenza di piattaforma dichiarata: UC 0061-0063, non ancora implementati.
- **RT-8 — Dati personali (§10).** Questa storia introduce dati di persone e va trattata di conseguenza: voci nuove
  nel manifesto `docs/compliance/manifests/helpdesk.yaml` **in italiano e inglese** per `survey.rating`,
  `survey.comment` e per l'impronta del gettone; campi annotati `@PersonalData`; tabella `satisfaction_survey`
  aggiunta a `exportData` e a `purgeData` del contratto `HelpdeskDataContract`, con cancellazione **fisica** —
  sostituire il testo con un codice non è cancellare. Il ruolo è quello di **responsabile del trattamento** per
  conto dell'azienda cliente, che resta titolare: il commento è di una persona che con appgrove non ha alcun
  rapporto. Vale integralmente l'avvertenza dell'articolo 9 del capofila §6: il commento è testo libero non
  presidiato, si **segnala** senza classificare (RF-6), non si manda **mai** a un servizio esterno di analisi del
  testo, e la difesa vera è la conservazione breve governata dal cliente (storia `0036`).
- **RT-9 — Registrazione eventi (§14).** Gli eventi «invito all'indagine generato», «voto ricevuto», «gettone
  scaduto», «gettone già consumato», «commento contrassegnato per revisione» sono registrati con `tenant_id`,
  `app_id`, `user_id` (o «sistema») e identificativo di correlazione; **mai** il gettone, mai il commento, mai il
  recapito del richiedente, e mai quale categoria avrebbe fatto scattare il contrassegno.
- **RT-10 — Riuso del riconoscitore.** Il riconoscitore delle categorie particolari è quello già scritto per
  l'assistenza interna della piattaforma (`SpecialCategoryScreening`), spostato in `services/commons` area
  `privacy` dalla storia `0002` (capofila §10, punto 1 e punto aperto n. 7 del §11): qui si **usa**, non si
  riscrive, estendendone semmai l'elenco delle radici per le lingue dei richiedenti.

## 4. Criteri di accettazione

**CA-1 — Il richiedente vota senza registrarsi**
- **Dato** una richiesta chiusa su un account con indagine attiva, e l'invito ricevuto dal richiedente
- **Quando** il richiedente apre il collegamento e sceglie «soddisfatto» aggiungendo un commento
- **Allora** il voto risulta registrato sulla richiesta, l'operatore lo vede nel dettaglio, e nessun utente nuovo è
  stato creato

**CA-2 — Un solo voto per richiesta**
- **Dato** un gettone già usato per votare · **Quando** il richiedente riapre lo stesso collegamento e prova a
  votare di nuovo · **Allora** vede il ringraziamento, il voto registrato resta quello iniziale e la seconda
  richiesta non modifica nulla

**CA-3 — Gettone scaduto indistinguibile**
- **Dato** un gettone scaduto e un gettone mai emesso
- **Quando** si aprono entrambi i collegamenti
- **Allora** si ottiene la stessa identica risposta, senza rivelare quale dei due casi sia e senza mostrare né
  l'oggetto né alcun dato della richiesta

**CA-4 — Nessun sollecito, e indagine disattivabile**
- **Dato** un account che ha disattivato l'indagine, e un secondo account con l'indagine attiva ma una richiesta
  marcata «senza indagine» prima della chiusura
- **Quando** entrambe le richieste vengono chiuse, e passano dieci giorni senza voto sulle altre richieste
- **Allora** nessun invito è stato generato per le due richieste escluse e nessun messaggio di sollecito è partito
  per nessuna

**CA-5 — Commento con segnale di categoria particolare**
- **Dato** un richiedente che scrive nel commento un riferimento al proprio stato di salute
- **Quando** invia il voto
- **Allora** la richiesta risulta contrassegnata «da guardare con attenzione», il contrassegno è un valore
  booleano, e da nessuna parte — né in tabella, né nei registri eventi — risulta **quale** categoria sia stata
  riconosciuta

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` con richieste chiuse e inviti attivi
- **Quando** si apre il gettone emesso da `A`, anche manipolando il percorso o forzando l'identificativo di una
  richiesta di `B`
- **Allora** si vede e si vota soltanto la richiesta di `A`, e nessuna informazione di `B` è raggiungibile

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend e compliance; l'intera suite prima del commit);
- [ ] prove di **unità** sulla generazione e verifica del gettone (scadenza, consumo, impronta) e sul contrassegno
      del commento, di **integrazione** sulle rotte pubbliche compresa la limitazione della frequenza, con database
      effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** rafforzata: è una delle superfici senza token dell'app;
- [ ] **prova end-to-end**: *rimando* alla storia `0037`, proprietaria del percorso `[J-HELPDESK]`, che si chiude
      esattamente con il passo «il richiedente vota dalla pagina pubblica»; motivo e storia proprietaria annotati
      nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), pagina pubblica e messaggio di
      invito compresi;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con voto, commento e impronta del gettone, campi
      annotati `@PersonalData`, tabella presente in esportazione e in cancellazione fisica;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, in particolare sulla scala a tre
      livelli, sulla durata del gettone e sul divieto di sollecito;
- [ ] contratto degli **strumenti conversazionali**: esclusione della scrittura annotata con il motivo, letture
      dichiarate;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0009` (ciclo di vita degli stati) | L'invito nasce dalla chiusura: serve lo stato «chiusa» |
| storia `0012` (canali e anagrafica del richiedente) | Servono il recapito e la lingua preferita del richiedente |
| storia `0015` (posta elettronica in uscita) | L'invito parte come messaggio di posta per conto del cliente |
| storia `0002` (modello dati multi-account) | Il riconoscitore delle categorie particolari deve già essere in `services/commons` |
| epica di piattaforma non implementata (UC 0061-0063) | Le letture del voto saranno esposte quando il livello conversazionale esisterà |

## 7. Fuori ambito

- **La media della soddisfazione e i suoi andamenti**: storia `0028`, che ne fa uno dei quattro numeri del
  cruscotto.
- **Il sollecito a chi non vota**: deliberatamente escluso. Insistere con una persona che non è nostra utente,
  su un rapporto che si è appena chiuso, è invadenza e abbassa la qualità della misura più di quanto alzi il numero
  di risposte.
- **Le indagini periodiche di relazione** (la domanda «ci consiglieresti?» mandata a tutti i clienti ogni tre
  mesi): un altro prodotto, con un'altra base giuridica. Non si aggiunge di soppiatto.
- **Le domande personalizzabili dell'indagine**: rimandate — un editore di questionari è una funzione che vive da
  sola e sposterebbe l'app fuori dal perimetro.
- **La pubblicazione dei voti verso l'esterno** (valutazioni sul sito del cliente, recensioni): fuori ambito e con
  conseguenze proprie; il capofila §2.4 esclude anche i moduli di soddisfazione esterni.
- **La cancellazione per singolo richiedente** che porta via anche il suo voto: storia `0036`.

## 8. Punti aperti

- **La scala del voto** (proposta: tre livelli) contro le cinque stelle diffuse nel mercato. Tre livelli producono
  risposte più affidabili su volumi piccoli — con trenta risposte al mese una media a cinque stelle è rumore — ma
  cinque stelle sono ciò che il cliente si aspetta di vedere. **Decide lo sviluppatore**, ed è una scelta difficile
  da cambiare dopo, perché i voti già raccolti non si riscalano.
- **La durata di validità del gettone** (proposta: 14 giorni) è una scelta di sicurezza: troppo corta rende
  l'indagine inutile, troppo lunga allarga la finestra di esposizione di una pagina raggiungibile senza
  autenticazione. **Decide lo sviluppatore.**
- **La classificazione del commento nel manifesto dei dati** non la chiude un agente: è testo libero di una persona
  esterna che può contenere categorie particolari, e la valutazione d'impatto è già segnalata come probabile.
  **Decidono lo sviluppatore e la revisione legale pre-go-live**
  ([docs/_REVISIONE-LEGALE.md](../../../../_REVISIONE-LEGALE.md)), punto aperto n. 2 del capofila §11.
- **Chi scrive l'informativa mostrata sulla pagina del voto**: il testo è del **titolare**, cioè dell'azienda
  cliente; l'app deve rendere possibile inserirlo, non scriverlo al posto suo (capofila §6, punto 5). Come e dove si
  configuri quel testo va deciso insieme alle storie del portale (epica 06).
