# 0019 — Code di lavoro

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 04 — Organizzazione del lavoro
**Storia**: `0019` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`, `0012`, `0018`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di una micro-impresa
> voglio separare le richieste di assistenza da quelle amministrative e dire chi presidia ciascun gruppo
> così da non far leggere ogni messaggio a tutti e non far scoprire una fattura contestata a chi ripara i prodotti.

**Contesto.** Fino a qui esiste una sola pila di richieste e tutti guardano tutto: funziona con due persone,
smette di funzionare appena la casella `info@` e la casella `amministrazione@` finiscono nello stesso posto. La
coda è **l'unico** meccanismo di organizzazione che questa applicazione si concede. La descrizione
dell'applicazione ([application-description.md](../application-description.md) §2.5) è netta sul perché: le fonti
consultate convergono sul fatto che l'instradamento per competenza, i livelli di supporto e i flussi di
automazione a più rami sono esattamente ciò che travolge una squadra di tre persone. Qui quindi **non c'è alcun
motore di regole**: una richiesta entra nella coda dichiarata dal canale che l'ha portata, e una persona può
spostarla. Due frasi, e finisce lì — la semplicità è una scelta di prodotto difesa, non una funzione mancante.

## 2. Requisiti funzionali

1. **RF-1** — Un amministratore dell'account può creare, rinominare e disattivare una coda, con nome e colore.
   All'attivazione dell'app esiste già una coda predefinita «Assistenza», che non si può disattivare.
2. **RF-2** — A ogni coda si associano gli operatori che la presidiano; un operatore può presidiare più code, e un
   operatore **sospeso** (storia `0018`) resta associato ma non compare fra i presidianti attivi.
3. **RF-3** — Ogni canale d'ingresso (storia `0012`) dichiara la propria coda di destinazione: le richieste che
   arrivano da quel canale nascono in quella coda. Un canale senza coda dichiarata alimenta la coda predefinita.
4. **RF-4** — Ogni richiesta appartiene a **esattamente una** coda e può essere spostata da un operatore in
   un'altra coda, con la traccia dello spostamento (chi, quando, da dove a dove) visibile sul dettaglio.
5. **RF-5** — Una coda che contiene richieste non chiuse **non si cancella**: si disattiva. Una coda disattivata
   non riceve nuove richieste e non compare nei moduli di scelta, ma le richieste che contiene restano leggibili e
   lavorabili finché non vengono chiuse o spostate.
6. **RF-6** — Il numero di code per account è limitato (proposta: **cinque**); al superamento la creazione è
   rifiutata con `422` e un messaggio che spiega il limite e il perché.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura delle entità `Queue` e delle associazioni di
  presidio filtra per `tenant_id` preso dal token verificato; un `tenant_id` che arrivasse dal corpo della
  richiesta o dai parametri viene ignorato. Una coda di un account non è raggiungibile da un altro nemmeno
  conoscendone l'identificativo.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/helpdesk/v1/queues`,
  `PATCH|DELETE /api/helpdesk/v1/queues/{id}`, `PUT /api/helpdesk/v1/queues/{id}/agents` per il presidio e
  `PATCH /api/helpdesk/v1/tickets/{id}` per lo spostamento di coda; corpo validato; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__queues.sql` sullo schema `app_helpdesk`: tabella `queue` (nome,
  colore, stato, indicatore di coda predefinita) e tabella di associazione `queue_agent`, entrambe con `tenant_id`,
  chiave primaria UUID versione 7, colonne di controllo (`created_at`, `updated_at`, `created_by`, `updated_by`) e
  cancellazione logica (`deleted_at`); colonna `queue_id` su `ticket` con indice a partire da `tenant_id`; unicità
  del nome della coda per account fra le righe non cancellate. **Nessuna chiave esterna verso altri schemi**.
- **RT-4 — Modulo frontend (§3, §5).** Sezione Impostazioni → Code del modulo `helpdesk` per la gestione e il
  presidio, più il selettore di coda nel filtro dell'elenco (storia `0010`) e nel dettaglio della richiesta. Dati
  letti con il client generato; solo token del sistema di design, con il colore della coda scelto fra i token
  disponibili e **mai** scritto a mano; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `helpdesk` e sono presenti in
  `en, it, fr, es, de`. Il **nome** della coda lo scrive il cliente e resta nella sua lingua; il nome della coda
  predefinita creata all'attivazione si genera nella lingua dell'account.
- **RT-6 — Varchi e quota (§6, §7).** Le code **non** consumano la metrica `agents`: il tetto di RF-6 è un limite
  di prodotto e produce `422`, non `429`. La catena dei varchi resta quella di piattaforma
  (`401 → 403 → 402 → 403 → 429`): con abbonamento `canceled` o `paused` le rotte rispondono `402`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo: questa storia dà **contenuto** al
  parametro `coda` dello strumento di lettura `elenca_richieste(stato?, coda?, operatore?, oltre_scadenza?,
  periodo?)` già dichiarato al §7 della descrizione dell'applicazione, che accetta il nome della coda e resta di
  sola lettura. La creazione di una coda è configurazione e non si automatizza. Il contratto vive dentro il
  servizio; il server conversazionale è di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo**: la coda è configurazione (nome, colore, stato)
  e il presidio contiene solo identificativi interni di operatori già dichiarati nel manifesto dalla storia `0018`.
  Nulla da aggiungere a `docs/compliance/manifests/helpdesk.yaml`; le tabelle `queue` e `queue_agent` **non**
  entrano in `exportData` né in `purgeData` per questo motivo, e la scelta va annotata nel registro delle decisioni
  perché sia verificabile.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «coda creata», «coda disattivata», «presidio modificato» e
  «richiesta spostata di coda» sono registrati con `tenant_id`, `app_id`, `user_id`, identificativo della coda e
  identificativo di correlazione, **senza** l'oggetto della richiesta né alcun dato personale.

## 4. Criteri di accettazione

**CA-1 — Il canale alimenta la sua coda**
- **Dato** un account con le code «Assistenza» e «Amministrazione», e un canale di posta collegato alla seconda
- **Quando** arriva una richiesta da quel canale
- **Allora** la richiesta nasce nella coda «Amministrazione» e compare nel filtro di quella coda

**CA-2 — Canale senza coda dichiarata**
- **Dato** un canale a cui non è stata dichiarata alcuna coda di destinazione
- **Quando** arriva una richiesta da quel canale
- **Allora** la richiesta nasce nella coda predefinita «Assistenza», senza errori e senza restare senza coda

**CA-3 — Spostamento con traccia**
- **Dato** una richiesta aperta nella coda «Assistenza»
- **Quando** un operatore la sposta in «Amministrazione»
- **Allora** la richiesta compare nella nuova coda e il dettaglio mostra chi l'ha spostata, quando e da quale coda

**CA-4 — Disattivazione di una coda che contiene richieste**
- **Dato** una coda con tre richieste non chiuse
- **Quando** l'amministratore prova a cancellarla
- **Allora** riceve `422` con un messaggio che propone la disattivazione; se disattiva, la coda smette di ricevere
  nuove richieste e sparisce dai moduli di scelta, ma le tre richieste restano leggibili e lavorabili

**CA-5 — Tetto delle code raggiunto**
- **Dato** un account con cinque code attive
- **Quando** l'amministratore ne crea una sesta
- **Allora** riceve `422` con un messaggio che spiega il limite e come liberare una coda, e nulla viene creato

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con le proprie code
- **Quando** un utente di `A` chiede l'elenco delle code, e poi prova a spostare una propria richiesta in una coda
  di `B` forzandone l'identificativo
- **Allora** vede solo le code di `A` e lo spostamento è rifiutato: la coda di `B` per lui non esiste

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla scelta della coda di destinazione (canale dichiarato / ripiego sulla predefinita) e
      di **integrazione** sulla risorsa `queues`, con database effimero e migrazioni Flyway vere;
- [ ] prova di **isolamento fra account** su code, presidio e spostamento, compreso il tentativo di spostare una
      richiesta in una coda di un altro account;
- [ ] **prova end-to-end**: **rimando** alla storia `0037`, proprietaria del percorso `[J-HELPDESK]`, che
      attraverserà una coda esplicita; qui la copertura resta alle prove d'integrazione. Voce `da-coprire` nel
      **registro di copertura** [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) con
      motivo e storia proprietaria;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), con la nota che il nome delle code
      resta nella lingua del cliente;
- [ ] **manifesto dei dati**: nessuna voce nuova, con la motivazione scritta;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con annotato perché non esiste un
      motore di regole di instradamento e perché il tetto delle code è cinque;
- [ ] contratto degli **strumenti conversazionali**: parametro `coda` di `elenca_richieste` documentato;
- [ ] controllo automatico di **accessibilità** verde sulla sezione Code;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove (dati di prova della
      storia `0005`, che devono nascere con la coda predefinita).

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0002` — modello dati multi-account | Serve la tabella `ticket` a cui agganciare la coda |
| Storia `0003` — guscio del modulo frontend | Serve la sezione Impostazioni dove gestire le code |
| Storia `0012` — canali e anagrafica del richiedente | La coda di destinazione è una proprietà del canale: senza canali non c'è nulla che alimenti la coda |
| Storia `0018` — operatori e posti | Il presidio si dichiara su operatori che devono già esistere |

## 7. Fuori ambito

- **L'instradamento automatico per contenuto, competenza, lingua o mittente**: non si fa, e non è un rimando ma una
  scelta di prodotto motivata al §2.5 della descrizione dell'applicazione. Se un giorno servisse, sarebbe una
  storia nuova e una decisione di direzione di prodotto, non un'aggiunta silenziosa.
- **L'assegnazione della richiesta a una persona**: la fa la storia `0020`. Coda e assegnatario sono due cose
  diverse: la coda dice *dove* sta il lavoro, l'assegnatario dice *di chi* è.
- **La politica di servizio applicata alla coda**: la fa la storia `0024` dell'epica 05; qui la coda non ha ancora
  obiettivi di tempo.
- **La visibilità ristretta per coda** («l'operatore vede solo le code che presidia»): rimandata di proposito. Con
  da uno a dieci operatori la separazione utile è quella del *lavoro*, non quella dei *permessi*; introdurla ora
  significherebbe portare dentro una matrice di autorizzazioni che il segmento non usa. Se emergesse come
  necessità, è una decisione dello sviluppatore (§8).
- **Le viste di lavoro e i filtri dell'elenco**: sono della storia `0010`; qui si aggiunge soltanto il criterio
  «coda» a un filtro che esiste già.

## 8. Punti aperti

- **Tetto di cinque code per account** — è una proposta di prodotto, non un vincolo tecnico. Lo chiude lo
  sviluppatore, e va deciso **prima**: abbassarlo dopo significa togliere qualcosa a clienti che ci stanno sopra.
- **La coda deve limitare la visibilità?** Oggi ogni operatore vede tutte le code. È difendibile per una squadra di
  tre persone, molto meno per una di dieci in cui la coda «Amministrazione» contiene contestazioni di pagamento.
  È una scelta di direzione di prodotto e la chiude lo sviluppatore; se la risposta fosse sì, il posto giusto è una
  storia nuova dell'epica 04, non un'aggiunta a questa.
