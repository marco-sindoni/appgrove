# 0018 — Catalogo degli strumenti osservati

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 04 — Regole e approvazione umana
**Storia**: `0018` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che risponde di ciò che fanno gli agenti della propria azienda
> voglio vedere l'elenco degli strumenti che i miei agenti usano davvero, senza doverlo compilare io
> così da scoprire cosa hanno per le mani prima di dover decidere che cosa concedergli.

**Contesto.** Prima di poter dire *«questo strumento richiede un'approvazione»* bisogna sapere che quello
strumento esiste. E il modo peggiore per saperlo è chiederlo al cliente: chi ha messo insieme tre agenti con
strumenti diversi, spesso costruiti da persone diverse in momenti diversi, **non ha quell'elenco** — e se ce
l'avesse non avrebbe bisogno di noi.

L'elenco si costruisce da solo: ogni azione che arriva porta il nome di uno strumento, e la prima volta che quel
nome compare nasce la sua scheda. È l'inverso del solito lavoro di configurazione, ed è anche il primo momento in
cui il prodotto restituisce valore senza chiedere niente: si collega una sorgente, si aspetta un giorno, e si
guarda l'elenco. Nella maggior parte dei casi contiene almeno una voce che il cliente non si aspettava.

Questa storia costruisce il catalogo e la classificazione del rischio. **Non** costruisce le regole: quelle sono la
storia 0019, e vanno tenute separate perché la prima è osservazione e la seconda è decisione.

## 2. Requisiti funzionali

1. **RF-1** — Alla prima comparsa di un nome di strumento in un'azione registrata, nasce la **scheda dello
   strumento** per quell'account: nome, sorgente che l'ha dichiarato, momento della prima comparsa.
2. **RF-2** — La scheda si aggiorna a ogni comparsa successiva: ultima comparsa, conteggio delle azioni per esito,
   classe di effetto prevalente, elenco delle sorgenti che l'hanno usato.
3. **RF-3** — L'app propone una **classe di rischio** per ogni strumento nuovo, ricavata dalla classe di effetto
   dichiarata nell'azione (lettura, scrittura, cancellazione, pagamento, invio verso l'esterno) e dal nome dello
   strumento quando è riconoscibile; la proposta è dichiarata come tale e non si confonde con una scelta umana.
4. **RF-4** — Una persona con ruolo adeguato può **cambiare la classe di rischio** di uno strumento; il cambio è
   una riga del registro con chi e quando, non una modifica silenziosa.
5. **RF-5** — Uno **strumento mai visto prima** è messo in evidenza nell'elenco per un periodo, e non si mescola
   con quelli noti: la comparsa di uno strumento nuovo è di per sé un fatto notevole, che si tratti di una
   funzione appena rilasciata o di qualcosa che non doveva esserci.
6. **RF-6** — Esiste una schermata «Strumenti» che elenca gli strumenti osservati con i loro conteggi e la loro
   classe di rischio, ordinabile e filtrabile per sorgente e per classe.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il catalogo degli strumenti è **per account**: due account che usano uno
  strumento con lo stesso nome hanno due schede distinte, con conteggi e classificazioni proprie. Ogni lettura e
  scrittura filtra per `tenant_id` preso dal token verificato; un `tenant_id` che arrivasse dal corpo della
  richiesta viene ignorato. Non esiste un catalogo globale condiviso, e non deve esistere: sarebbe una fuga di
  informazioni su come lavorano i clienti.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/agentaudit/v1/tools` (elenco paginato con
  filtri), `GET /api/agentaudit/v1/tools/{id}` (scheda) e `PATCH /api/agentaudit/v1/tools/{id}` (classe di
  rischio); corpo validato; errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso
  commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__strumenti.sql` sullo schema `app_agentaudit`: tabella `tools` con
  `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica, nome, prima e ultima
  comparsa, conteggi, classe di effetto prevalente, classe di rischio proposta e classe di rischio confermata.
  A differenza delle azioni, **questa tabella si aggiorna**: è una vista di lavoro, non una prova. La distinzione
  va scritta nel registro delle decisioni, perché in un'app costruita sul divieto di modifica una tabella
  modificabile è una scelta che va motivata.
- **RT-4 — Modulo frontend (§3, §5).** Nuova sezione «Strumenti» nel manifesto del modulo `agentaudit`; la
  schermata legge i dati con il client generato e non accede al token se non tramite il contesto della shell; solo
  token del sistema di design; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `agentaudit` e sono presenti
  in `en, it, fr, es, de`, comprese le denominazioni delle classi di rischio e di effetto — che vanno tradotte con
  cura, perché sono i termini su cui il cliente prenderà le decisioni della storia 0019. **Il nome dello
  strumento non si traduce**: è un identificativo tecnico e va mostrato come arriva.
- **RT-6 — Varchi e quota (§6, §7).** La scoperta di uno strumento **non consuma** la metrica `actions`: è un
  effetto della registrazione di un'azione, che ha già consumato la propria unità, e contarla due volte sarebbe
  scorretto. Il cambio della classe di rischio richiede un ruolo amministrativo dell'account (`403` altrimenti).
  Con abbonamento non attivo risponde `402`.
- **RT-7 — Esposizione conversazionale (§12).** Il catalogo rientra nella risposta di `riepiloga_attivita(periodo)
  → conteggi per strumento, esito, sorgente`, marcato **lettura**. Il cambio della classe di rischio **non** è
  esposto come strumento: è una decisione che precede le regole, e le regole non si comandano da una chat senza
  conferma (storia 0035). Il contratto vive dentro il servizio; il server conversazionale è di piattaforma e non
  ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo**: la scheda di uno strumento contiene un nome
  tecnico, date e conteggi. Va però verificato un caso: se un cliente chiamasse i propri strumenti con nomi che
  contengono dati di persone — `invia_email_a_mario_rossi` — quel nome finirebbe in una tabella e in una
  schermata. Non si può impedire; si può dichiarare nel manifesto che il nome dello strumento è materiale fornito
  dal cliente e che il cliente è responsabile di ciò che ci scrive dentro.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `strumento scoperto` e `classe di rischio modificata` sono
  registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza dati personali. Il primo
  è anche una riga del registro dell'app: la comparsa di uno strumento nuovo è un fatto che deve restare
  dimostrabile.

## 4. Criteri di accettazione

**CA-1 — Lo strumento si scopre da solo**
- **Dato** un account senza strumenti noti
- **Quando** arriva un'azione che dichiara lo strumento `cancella_cliente`
- **Allora** nasce la scheda di quello strumento con la prima comparsa valorizzata, una classe di rischio
  proposta coerente con la classe di effetto dichiarata, e lo strumento compare in evidenza come mai visto prima

**CA-2 — La scheda si aggiorna**
- **Dato** uno strumento già noto con dodici azioni registrate
- **Quando** ne arrivano altre cinque, di cui due negate
- **Allora** i conteggi riflettono diciassette azioni con la ripartizione corretta per esito, e l'ultima comparsa
  è aggiornata

**CA-3 — La classificazione umana prevale e resta tracciata**
- **Dato** uno strumento con classe di rischio proposta dall'app
- **Quando** una persona con ruolo adeguato la cambia
- **Allora** la scheda mostra la classe confermata dalla persona, la proposta resta visibile come tale, e nel
  registro compare la riga con chi ha cambiato e quando

**CA-4 — Chi non ha il ruolo non classifica**
- **Dato** un utente con il solo permesso di lettura
- **Quando** tenta di cambiare la classe di rischio di uno strumento
- **Allora** riceve `403` e nulla viene modificato

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` che usano entrambi uno strumento chiamato `invia_pagamento`
- **Quando** un utente di `A` consulta il catalogo e la scheda di quello strumento
- **Allora** vede esclusivamente i conteggi e la classificazione del proprio account, anche forzando
  l'identificativo dell'altro account nella richiesta, e la classificazione fatta da `B` non influenza `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla proposta automatica della classe di rischio e sull'aggiornamento dei conteggi, e di
      **integrazione** sulla scoperta a partire da un'azione, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sul catalogo e sulle schede, compreso il caso di nomi di strumento
      identici in due account;
- [ ] **prova end-to-end**: risposta «coprire ora» — la schermata «Strumenti» è superficie utente; il percorso
      `[J-AGENTAUDIT]` riceve il passo «dichiara un'azione, apri Strumenti, trova lo strumento scoperto», e il
      registro di copertura [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) viene
      aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`) per le classi di rischio e di
      effetto;
- [ ] **manifesto dei dati**: nessuna voce nuova, con la nota su chi risponde del contenuto dei nomi degli
      strumenti;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con la voce obbligatoria sulla
      tabella modificabile in un'app costruita sul divieto di modifica, e sul perché la distinzione regge;
- [ ] contratto degli **strumenti conversazionali**: `riepiloga_attivita` in sola lettura; il cambio di classe non
      è esposto, e il motivo è dichiarato;
- [ ] controllo automatico di **accessibilità** sulla schermata «Strumenti»;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0007` | Il contratto dell'azione è ciò che dichiara nome dello strumento e classe di effetto: senza, non c'è niente da catalogare |
| storia `0008` | Il catalogo si popola dalle azioni che entrano dalla rotta di ingresso |
| storia `0003` | Serve il guscio del modulo frontend per appendere la sezione «Strumenti» |

## 7. Fuori ambito

- **le regole**: consenti, nega, richiedi approvazione sono la storia 0019. Qui si osserva e si classifica, non si
  decide cosa fare;
- l'ordinamento degli strumenti per rischio effettivo osservato (quante volte è stato negato, quante volte ha
  prodotto uno scostamento): dipende dalla storia 0023 e viene dopo;
- la dichiarazione anticipata degli strumenti da parte della sorgente, prima che vengano usati: sarebbe utile e
  non è necessaria, vedi punti aperti.

## 8. Punti aperti

- **Se uno strumento debba poter essere dichiarato in anticipo.** Oggi si scopre solo usandolo: chi vuole
  preparare le regole prima di mettere un agente in produzione non può. Una dichiarazione anticipata dalla
  sorgente risolverebbe, ma aggiunge un'interfaccia e un modo per popolare il catalogo con strumenti mai usati.
  Chi chiude: sviluppatore.
- **Come si riconosce che due nomi sono lo stesso strumento.** Se una sorgente rinomina uno strumento, nasce una
  scheda nuova e la storia precedente resta appesa alla vecchia; le regole vanno rifatte. Un meccanismo di
  identità più forte del nome (per esempio un identificativo stabile dichiarato dalla sorgente) sarebbe migliore,
  ma richiede disciplina al cliente. Chi chiude: sviluppatore, insieme al contratto della storia 0007.
- **La proposta automatica di classe di rischio non deve dare falsa sicurezza.** Una classificazione sbagliata che
  sembra autorevole è peggio di nessuna classificazione: per questo la proposta resta visibile come proposta anche
  dopo la conferma. Se le prove d'uso mostrassero che le persone la accettano senza guardarla, andrebbe resa più
  scomoda, non più intelligente.
