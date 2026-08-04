# 0023 — Riconciliazione fra nulla osta ed esito

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 04 — Regole e approvazione umana
**Storia**: `0023` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0020`, `0008`, `0011`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che risponde di ciò che fanno gli agenti della propria azienda
> voglio sapere se un agente ha fatto esattamente quello che gli era stato concesso, e nient'altro
> così da accorgermi quando un agente agisce senza chiedere, agisce dopo un rifiuto, o agisce in modo diverso da
> come aveva dichiarato.

**Contesto.** È la terza e ultima contromisura al limite del registro cooperativo (§0 della descrizione
dell'applicazione), e chiude il ragionamento cominciato con la numerazione di sequenza (storia 0011).

Il registro non intercetta: chiede fiducia all'agente, e la fiducia va verificata. Ci sono quattro modi in cui la
realtà può divergere da ciò che era stato autorizzato, e sono quattro cose diverse:

1. l'agente ha **agito senza chiedere**, su uno strumento che richiedeva un nulla osta;
2. l'agente ha **agito dopo un rifiuto** o dopo una scadenza;
3. l'agente ha **ottenuto un nulla osta e non ha mai dichiarato l'esito** — non sappiamo se ha agito;
4. l'agente ha agito con **parametri diversi** da quelli su cui era stato autorizzato.

I primi due sono violazioni. Il terzo è un buco informativo, e va detto come tale invece di essere interpretato.
Il quarto è il più insidioso, perché in un registro che non conserva i valori sembrerebbe impossibile da rilevare
— e invece si rileva benissimo **confrontando le impronte**, che è esattamente la proprietà per cui le impronte
esistono (§6.3 della descrizione dell'applicazione).

Il limite residuo va dichiarato e non nascosto: **un agente che non dichiara nulla resta invisibile**. Nessuna di
queste verifiche lo cattura; lo catturano solo la disciplina del cliente nel far dichiarare tutti i propri agenti
e, parzialmente, i buchi di sequenza della storia 0011.

## 2. Requisiti funzionali

1. **RF-1** — Ogni azione dichiarata che riguardi uno strumento la cui regola vigente richiedeva un nulla osta
   viene **riconciliata** con il nulla osta corrispondente, individuato per riferimento esplicito o per chiave di
   deduplicazione.
2. **RF-2** — Sono rilevati e distinti quattro tipi di **scostamento**: azione senza nulla osta quando ne serviva
   uno; azione dopo un esito negativo o scaduto; nulla osta concesso senza esito mai dichiarato entro un tempo
   ragionevole; impronte dei parametri dell'azione diverse da quelle del nulla osta.
3. **RF-3** — Ogni scostamento è **una riga del registro** con il proprio tipo, e non una segnalazione volatile:
   uno scostamento è precisamente il genere di fatto che dovrà essere dimostrabile dopo.
4. **RF-4** — Ogni scostamento produce un **avviso** al cliente, con il dettaglio di quale agente, quale
   strumento, quale nulla osta.
5. **RF-5** — La scheda di un'azione (storia 0025) e quella di un nulla osta mostrano l'esito della
   riconciliazione: **conforme**, **scostamento** con il tipo, oppure **in attesa di esito**.
6. **RF-6** — Esiste una vista degli scostamenti del periodo, filtrabile per tipo, per sorgente e per strumento:
   è la vista che si apre dopo un incidente.
7. **RF-7** — Il **limite residuo è dichiarato nell'interfaccia**, non solo nella documentazione: la vista degli
   scostamenti dice esplicitamente che rileva ciò che è stato dichiarato e non ciò che non lo è mai stato.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La riconciliazione opera dentro un solo account: un'azione non può
  essere riconciliata con il nulla osta di un altro account, e ogni interrogazione filtra per `tenant_id` preso
  dal token verificato o dalla credenziale della sorgente. È un caso di prova esplicito, perché un accoppiamento
  sbagliato fra account produrrebbe uno scostamento falso e distruggerebbe la fiducia nella funzione.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/agentaudit/v1/deviations` (elenco paginato con
  filtri per tipo, sorgente, strumento, periodo); nessuna rotta che permetta di cancellare o «risolvere» uno
  scostamento — al più di annotarlo, ma l'annotazione è una riga nuova. Errori in `application/problem+json`;
  definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__scostamenti.sql` sullo schema `app_agentaudit`: tabella
  `deviations` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo, tipo, riferimento
  all'azione e al nulla osta, momento di rilevazione. Tabella **in sola aggiunta**, come le azioni: uno
  scostamento non si cancella, si spiega.
- **RT-4 — Modulo frontend (§3, §5).** Nuova sezione «Scostamenti» nel manifesto del modulo `agentaudit`, e
  arricchimento delle schede di azione e nulla osta con l'esito della riconciliazione; dati letti con il client
  generato; solo token del sistema di design; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `agentaudit` e sono
  presenti in `en, it, fr, es, de`, compresi i nomi dei quattro tipi di scostamento e — in particolare — il testo
  che dichiara il limite residuo, che deve dire la stessa cosa in tutte e cinque le lingue senza attenuarla in
  nessuna.
- **RT-6 — Varchi e quota (§6, §7).** La riconciliazione **non consuma** la metrica `actions`: è un'elaborazione
  su righe già contate. La riga di scostamento è generata dalla piattaforma e non conta come azione dell'agente.
  Con abbonamento non attivo risponde `402`; gli scostamenti già registrati restano leggibili nei limiti della
  conservazione.
- **RT-7 — Esposizione conversazionale (§12).** Gli scostamenti rientrano in `riepiloga_attivita(periodo)` e in
  `elenca_azioni(esito?)`, entrambi marcati **lettura**. Nessuno strumento di scrittura: uno scostamento non si
  chiude e non si archivia da una chat. Il contratto vive dentro il servizio; il server conversazionale è di
  piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Nessun campo nuovo che riguardi una persona**: uno scostamento riferisce
  righe già dichiarate nel manifesto. La tabella degli scostamenti va comunque aggiunta a `exportData` e
  `purgeData` del contratto dati dell'app, perché contiene riferimenti a righe che contengono identificativi di
  persone: dimenticarla è il difetto di conformità più probabile in una storia come questa.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `scostamento rilevato` (con il tipo) e `riconciliazione
  completata` sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza dati
  personali. Il primo merita una soglia di allarme: è il segnale più forte che questa app può emettere.

## 4. Criteri di accettazione

**CA-1 — Azione senza nulla osta**
- **Dato** uno strumento con regola vigente `richiedi approvazione` e nessun nulla osta richiesto
- **Quando** arriva un'azione dichiarata su quello strumento
- **Allora** viene registrato uno scostamento di tipo «azione senza nulla osta», il cliente riceve un avviso, e la
  scheda dell'azione mostra lo scostamento

**CA-2 — Azione dopo un rifiuto**
- **Dato** un nulla osta negato da una persona, oppure scaduto
- **Quando** arriva comunque un'azione che vi corrisponde
- **Allora** viene registrato uno scostamento di tipo «azione dopo esito negativo», distinto dal precedente

**CA-3 — Concesso ma mai dichiarato**
- **Dato** un nulla osta concesso e nessuna azione dichiarata entro il tempo previsto
- **Quando** la riconciliazione viene eseguita
- **Allora** viene registrato uno scostamento di tipo «esito mai dichiarato», e la scheda del nulla osta lo mostra
  come tale — **non** come azione compiuta e **non** come azione non compiuta, perché non lo sappiamo

**CA-4 — Parametri diversi da quelli autorizzati**
- **Dato** un nulla osta concesso con certe impronte dei parametri
- **Quando** arriva l'azione dichiarata con impronte diverse
- **Allora** viene registrato uno scostamento di tipo «parametri difformi», rilevato **confrontando le impronte** e
  senza che l'app abbia mai conservato i valori

**CA-5 — Il conforme non produce rumore**
- **Dato** un nulla osta concesso e l'azione corrispondente dichiarata con le stesse impronte
- **Quando** la riconciliazione viene eseguita
- **Allora** l'esito è `conforme`, non viene registrato nessuno scostamento e non viene inviato nessun avviso

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` con nulla osta e azioni che usano chiavi di deduplicazione identiche
- **Quando** la riconciliazione viene eseguita
- **Allora** ogni azione è riconciliata esclusivamente con nulla osta del proprio account, non viene prodotto
  nessuno scostamento falso, e un utente di `A` non vede gli scostamenti di `B`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sui quattro tipi di scostamento e sull'accoppiamento fra azione e nulla osta, e di
      **integrazione** sulla riconciliazione, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla riconciliazione, compreso il caso di chiavi di deduplicazione
      identiche in due account;
- [ ] **prova end-to-end**: risposta «coprire ora» — il percorso `[J-AGENTAUDIT]` riceve il passo «dichiara
      un'azione su uno strumento che richiedeva approvazione senza chiedere il nulla osta, verifica che compaia
      fra gli scostamenti»; il registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) viene aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), compreso il testo che dichiara
      il limite residuo;
- [ ] **manifesto dei dati**: nessuna voce nuova, ma la tabella degli scostamenti è presente in esportazione e
      cancellazione;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con **due voci obbligatorie**: la
      distinzione fra i quattro tipi e perché «esito mai dichiarato» non si interpreta, e la rilevazione dei
      parametri difformi tramite impronte;
- [ ] contratto degli **strumenti conversazionali**: nessuno di scrittura, e il divieto di chiudere uno
      scostamento da una chat è dichiarato;
- [ ] controllo automatico di **accessibilità** sulla schermata «Scostamenti»;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata: il limite residuo del registro cooperativo va descritto dove si descrive il
      prodotto, non solo qui.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0020` | Serve il nulla osta con le sue impronte e la sua chiave di deduplicazione |
| storia `0008` | Serve l'azione dichiarata con le proprie impronte, da confrontare |
| storia `0011` | La numerazione di sequenza è la contromisura gemella: insieme coprono due facce dello stesso limite |

## 7. Fuori ambito

- **la rilevazione di un agente che non dichiara nulla**: non è possibile con questo modello, e la storia lo
  dichiara invece di fingere il contrario. Si attenua solo con la disciplina del cliente e con i buchi di
  sequenza (storia 0011);
- il blocco automatico di una sorgente che produce scostamenti ripetuti: sarebbe una reazione, e questa app
  registra e avvisa, non esegue (§1 della descrizione dell'applicazione). Vedi punti aperti;
- l'analisi del comportamento dell'agente e i punteggi di rischio predittivi: deliberatamente fuori dal prodotto —
  è ciò che i fornitori vendono e non ciò che i clienti chiedono (§2.5 della descrizione dell'applicazione).

## 8. Punti aperti

- **Sospendere una sorgente che continua a divergere.** Sarebbe la reazione naturale a una sequenza di
  scostamenti gravi, e sarebbe anche il primo passo verso un prodotto che *agisce* invece di registrare — con
  tutte le conseguenze del caso, compreso il fatto che una sospensione sbagliata ferma il lavoro del cliente. È
  direzione di prodotto. Chi chiude: sviluppatore.
- **Entro quanto tempo un esito non dichiarato diventa uno scostamento.** Troppo presto produce falsi allarmi su
  azioni lente; troppo tardi rende inutile la rilevazione. Il valore ragionevole dipende dal tipo di strumento e
  probabilmente va legato alla classe di effetto, come le scadenze della storia 0022. Chi chiude: sviluppatore.
- **Come si annota uno scostamento senza chiuderlo.** Serve un modo per scrivere «era previsto, l'ho fatto io a
  mano» accanto a uno scostamento, senza cancellarlo. La forma proposta è una riga nuova che lo commenta; la
  schermata dovrà mostrare l'annotazione senza far sparire il fatto. Chi chiude: sviluppatore, insieme alla
  storia 0025.
