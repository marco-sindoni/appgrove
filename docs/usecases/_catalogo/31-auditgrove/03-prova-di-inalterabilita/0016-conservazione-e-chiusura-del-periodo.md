# 0016 — Conservazione e chiusura del periodo

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 03 — Prova di inalterabilità
**Storia**: `0016` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0013`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che paga per un registro
> voglio sapere esattamente per quanto tempo le mie righe restano consultabili, e che cosa resta quando non lo
> sono più
> così da non scoprire il giorno del bisogno che ciò che mi serviva è stato eliminato in silenzio.

**Contesto.** Un registro che tiene tutto per sempre non esiste: la conservazione costa, e cresce nel tempo invece
che con il consumo. È infatti l'unica leva che distingue davvero i piani (§5 della descrizione
dell'applicazione), perché la metrica di quota è una sola — `actions`, natura `flow` — e la durata di
conservazione è una **funzionalità del piano**, non una seconda metrica.

La proposta è: **30 giorni** nel piano gratuito, **13 mesi** nel piano Pro, **25 mesi** nel piano Team. I mesi
dispari non sono un vezzo: tredici mesi coprono un anno solare intero più il tempo di accorgersene. Va detto per
onestà che le fonti secondarie consultate indicano una soglia normativa di **almeno sei mesi** per la
conservazione delle registrazioni dei sistemi ad alto rischio, ma **quel numero non è stato verificato sul testo
primario** (§2.3 e §2.7 della descrizione dell'applicazione): è un ordine di grandezza che orienta il listino, non
un requisito che possiamo citare in un testo commerciale.

Il punto difficile di questa storia non è cancellare: è **cosa resta**. Se allo scadere le righe spariscono e
basta, il cliente perde anche la prova di aver avuto un registro. Perciò prima di lasciar uscire un periodo si
produce un **sigillo di chiusura** che ne conserva la prova aggregata: quante azioni, in che intervallo di
sequenze, con quale impronta di testa. Le righe se ne vanno; il fatto che ci fossero e che fossero quelle resta
dimostrabile.

## 2. Requisiti funzionali

1. **RF-1** — Ogni azione porta la **durata di conservazione** che le compete, determinata dal piano attivo **al
   momento in cui è stata registrata**, non dal piano attivo oggi.
2. **RF-2** — Una lavorazione programmata individua i periodi in scadenza e, **prima** di lasciarli uscire dalla
   consultazione, produce un **sigillo di chiusura**: intervallo di sequenze, conteggio, impronta di testa alla
   chiusura, riferimento ai sigilli giornalieri che il periodo conteneva.
3. **RF-3** — Le azioni uscite dal periodo di conservazione **non sono più consultabili né esportabili**; restano
   il sigillo di chiusura e le righe di catena che ne attestano l'esistenza, così che la catena non presenti buchi
   inspiegati.
4. **RF-4** — Il cliente riceve un **avviso in anticipo** delle azioni prossime alla scadenza, con l'invito a
   esportarle o a produrne un pacchetto di prova se gli servono.
5. **RF-5** — Il passaggio a un piano con conservazione più corta **non accorcia retroattivamente** la
   conservazione delle azioni già registrate: la riduzione decorre in avanti. Il passaggio mostra prima un avviso
   che dice esattamente cosa cambia e propone l'esportazione.
6. **RF-6** — La sezione «Integrità» mostra, per ogni periodo, se è aperto, chiuso o scaduto, e con quale sigillo
   di chiusura.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La lavorazione di chiusura opera per account e ogni interrogazione
  filtra per `tenant_id`; un errore su un account non deve poter chiudere il periodo di un altro, e la prova lo
  verifica esplicitamente. Le letture dei periodi filtrano per `tenant_id` preso dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/agentaudit/v1/retention-periods` che espone lo
  stato dei periodi; nessuna rotta che permetta di anticipare o posticipare una scadenza dall'esterno. Errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__conservazione.sql` sullo schema `app_agentaudit`: colonna della
  durata di conservazione sulle azioni, tabella `retention_periods` con `tenant_id`, chiave primaria UUID versione
  7, colonne di controllo, stato, riferimento al sigillo di chiusura. **Qui e solo qui** l'app rimuove fisicamente
  righe della tabella delle azioni, ed è l'unica eccezione al divieto della storia 0002: va scritta nel registro
  delle decisioni con la sua motivazione, perché è precisamente il genere di potere che, se non è delimitato,
  distrugge il prodotto. La rimozione avviene per interi intervalli di sequenze contigue, mai per singole righe
  scelte.
- **RT-4 — Modulo frontend (§3, §5).** Lo stato dei periodi si mostra nella sezione «Integrità» già esistente
  (storia 0014); l'avviso di scadenza imminente usa i componenti di avviso comuni. Solo token del sistema di
  design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili — compreso il testo dell'avviso di scadenza e quello
  che accompagna il passaggio a un piano inferiore — passano dallo spazio-nomi `agentaudit` e sono presenti in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** La durata di conservazione arriva dalla **funzionalità del piano** pubblicata
  dall'abilitazione, non da una configurazione dell'app: la storia non fissa durate, le legge. La chiusura di un
  periodo e il sigillo di chiusura non consumano la metrica `actions`. Con abbonamento in `past_due` la
  consultazione resta accessibile; con `canceled` l'accesso all'app cessa ma **l'esportazione dei dati resta
  accessibile in ogni caso**, ed è precisamente il momento in cui il cliente ne ha più bisogno.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo. Lo stato dei periodi rientra nella
  risposta di `verifica_integrita` e di `riepiloga_attivita` (storia 0034), marcati **lettura**. Nessuno strumento
  può chiudere o prolungare un periodo: sarebbe un effetto irreversibile comandabile da una chat.
- **RT-8 — Dati personali (§10).** **Nessun campo nuovo che riguardi una persona**, ma la storia cambia il regime
  di conservazione dei campi esistenti: le voci del manifesto che dichiarano «per quanto si tiene» vanno
  aggiornate in italiano e inglese per riflettere la durata per piano e il fatto che la durata è quella del piano
  vigente al momento della registrazione. È il genere di aggiornamento che si dimentica e che rende il manifesto
  falso.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `periodo chiuso`, `azioni rimosse per scadenza` (con il solo
  conteggio) e `avviso di scadenza inviato` sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo
  di correlazione, senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Il periodo si chiude prima di scadere**
- **Dato** un account nel piano Pro con azioni registrate tredici mesi fa
- **Quando** la lavorazione di conservazione viene eseguita
- **Allora** viene prodotto un sigillo di chiusura con intervallo di sequenze, conteggio e impronta di testa, e
  **solo dopo** quelle azioni escono dalla consultazione

**CA-2 — La catena non presenta buchi inspiegati**
- **Dato** un periodo chiuso e le sue azioni non più consultabili
- **Quando** si verifica l'integrità del periodo successivo
- **Allora** la verifica riesce, perché il sigillo di chiusura documenta l'intervallo mancante, e l'esito non
  segnala una divergenza dove c'è invece una scadenza

**CA-3 — Scendere di piano non distrugge il passato**
- **Dato** un account nel piano Team con dodici mesi di azioni registrate
- **Quando** passa al piano Pro, che ha conservazione più corta
- **Allora** vede prima un avviso che spiega cosa cambia e propone l'esportazione, e dopo il passaggio le azioni
  già registrate mantengono la conservazione del piano sotto cui sono nate, mentre quelle nuove seguono la nuova
  durata

**CA-4 — L'avviso arriva in tempo**
- **Dato** un account con azioni prossime alla scadenza
- **Quando** manca il preavviso previsto
- **Allora** il cliente riceve un avviso che indica quante azioni e quale intervallo stanno per uscire, con il
  rimando alla produzione di un pacchetto di prova

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con periodi in scadenza nello stesso giorno
- **Quando** la lavorazione di conservazione viene eseguita e fallisce sull'account `A`
- **Allora** il periodo di `B` viene comunque chiuso correttamente, nessuna riga di `A` viene rimossa, e un
  utente di `A` non vede mai i periodi di `B` nemmeno forzando l'identificativo dell'altro account

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo della durata di conservazione per azione e sul sigillo di chiusura, e di
      **integrazione** sulla lavorazione programmata, con database effimero e migrazioni vere;
- [ ] prova che la rimozione avviene **solo** per intervalli contigui scaduti e **mai** per singole righe scelte:
      è il presidio che impedisce a questa storia di diventare una porta di servizio per cancellare ciò che dà
      fastidio;
- [ ] prova di **isolamento fra account** sulla chiusura dei periodi e sulla loro lettura;
- [ ] **prova end-to-end**: risposta «rimando» — lo stato dei periodi è superficie utente ma dipende dal passare
      del tempo, che un percorso end-to-end non può simulare in modo pulito; il passo viene coperto dalla storia
      0037 con dati preparati, e il registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve la voce `da-coprire` con
      motivo e storia proprietaria;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati** aggiornato in italiano e inglese nelle voci che dichiarano la durata di
      conservazione;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con **due voci obbligatorie**:
      l'eccezione al divieto di rimozione e i suoi limiti, e la scelta di non accorciare retroattivamente la
      conservazione al passaggio a un piano inferiore;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo, e il divieto di chiudere o prolungare un
      periodo da una chat è dichiarato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | La rimozione per scadenza è l'unica eccezione al registro in sola aggiunta: va costruita sopra quel divieto, non prima |
| storia `0013` | Il sigillo di chiusura è un sigillo, e riusa la macchina che li produce e li firma |
| Funzionalità del piano pubblicate dall'abilitazione | La durata di conservazione arriva dal listino come codice, non da una configurazione dell'app |

## 7. Fuori ambito

- la cancellazione richiesta da una persona i cui dati compaiono nel registro: è un'altra cosa e ha altre regole,
  storia 0032 dell'epica 06. Qui si parla di scadenza per contratto, non di diritti dell'interessato;
- la conservazione dei **contenuti allegati**, che ha durata più corta e regime proprio: storia 0031;
- l'archiviazione a lungo termine presso un fornitore di conservazione a norma: punto aperto di prodotto (§11
  della descrizione dell'applicazione).

## 8. Punti aperti

- ⚠️ **Fermata di escalation — cosa succede alla conservazione quando si scende di piano.** La proposta di questa
  storia (avviso, esportazione proposta, riduzione solo in avanti) costa complessità: significa che la durata di
  conservazione va portata sulla riga e non dedotta dal piano corrente. L'alternativa — la riduzione vale subito
  per tutto — è più semplice e **distrugge prove già acquisite e già pagate**. Non è una scelta che spetta a un
  agente che scrive documenti. Chi chiude: sviluppatore (§5 della descrizione dell'applicazione).
- **Le durate proposte sono una proposta.** 30 giorni, 13 mesi, 25 mesi discendono dal listino proposto, che è a
  sua volta una fermata di escalation. Chi chiude: sviluppatore.
- **La soglia normativa dei sei mesi non è verificata.** Se la verifica sul testo primario la confermasse,
  il piano gratuito a 30 giorni resterebbe fuori da qualunque uso di conformità, e questo va detto nei testi
  commerciali invece che lasciato intuire. Chi chiude: revisione legale (§2.7 della descrizione
  dell'applicazione).
- **Cosa succede alla chiusura dell'account.** La cessazione dell'abbonamento e la chiusura dell'account non sono
  la stessa cosa della scadenza di un periodo, e il registro è precisamente ciò che il cliente vorrà portarsi via
  in quel momento. La procedura di uscita non è definita in questa storia. Chi chiude: sviluppatore.
