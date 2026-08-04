# 0022 — Scadenza e mancata risposta

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 04 — Regole e approvazione umana
**Storia**: `0022` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0020`, `0021`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che risponde di ciò che fanno gli agenti della propria azienda
> voglio che una richiesta di approvazione a cui nessuno ha risposto si chiuda con un rifiuto, e che io lo sappia
> così da non ritrovarmi con azioni compiute perché il permesso era stato «dato dal silenzio», e da accorgermi che
> c'è una coda che nessuno guarda.

**Contesto.** Ogni richiesta in attesa ha due destini possibili, non uno: qualcuno decide, oppure nessuno decide.
Il secondo caso è quello che si dimentica di progettare, ed è quello che nella realtà accade più spesso — la
persona è in ferie, l'avviso è finito nella posta indesiderata, la richiesta è arrivata di notte.

La regola è quella rilevata nell'analisi delle aspettative dei clienti piccoli, ed è la stessa che il prodotto
applica altrove: **se un flusso non riesce a produrre l'approvazione, quel flusso resta in sola proposta** (§2.5
della descrizione dell'applicazione). Tradotto: il silenzio vale **no**. Non è una scelta prudente per abitudine —
è l'unica coerente con la ragione per cui la regola diceva `richiedi approvazione`: se quell'azione era abbastanza
grave da meritare una persona, non può essere abbastanza lieve da procedere senza.

C'è però una seconda metà, ed è quella che rende utile la storia invece che solo rigorosa: **una coda che nessuno
guarda è un guasto silenzioso**. Se le richieste scadono e nessuno lo sa, il cliente non sta usando il presidio:
sta subendo un blocco che attribuirà agli agenti, o a noi. Perciò le scadenze si contano e si dicono.

## 2. Requisiti funzionali

1. **RF-1** — Ogni nulla osta in attesa ha una **scadenza**, valorizzata al momento della creazione e visibile
   sia alla sorgente sia in coda.
2. **RF-2** — La scadenza è **configurabile per classe di effetto**: si può volere mezz'ora per un pagamento e
   otto ore per un invio verso l'esterno. Esistono valori predefiniti per account.
3. **RF-3** — Alla scadenza, il nulla osta passa a **scaduto** e l'esito verso la sorgente è **non concesso**; la
   scadenza è **una riga del registro**, non una modifica silenziosa di stato.
4. **RF-4** — Una richiesta scaduta **non si può decidere a posteriori**: chi arriva dopo vede che è scaduta e con
   quale ritardo, e se serve l'agente chiede un nulla osta nuovo.
5. **RF-5** — Il cliente riceve un **avviso ricorrente** che riporta quante richieste sono scadute nel periodo e
   su quali strumenti; l'avviso non è la somma dei singoli avvisi di richiesta, è un riassunto di secondo livello
   destinato a chi sorveglia.
6. **RF-6** — La schermata «Approvazioni» mostra, accanto alla coda in attesa, le richieste **scadute di
   recente**, perché sono la prova che il presidio non sta funzionando come dovrebbe.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La lavorazione di scadenza opera per account e ogni interrogazione
  filtra per `tenant_id`; un errore su un account non deve poter far scadere le richieste di un altro. Le letture
  delle richieste scadute filtrano per `tenant_id` preso dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta nuova per far scadere: la scadenza è un fatto del
  tempo, non un'operazione richiedibile dall'esterno. Si estende `GET /api/agentaudit/v1/clearances` con il filtro
  di stato `expired`; errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__scadenze.sql` sullo schema `app_agentaudit`: colonna della
  scadenza sulla tabella dei nulla osta e tabella `expiry_settings` con `tenant_id`, chiave primaria UUID versione
  7, colonne di controllo, classe di effetto, durata. La transizione a `scaduto` è registrata **come riga di
  catena**, coerentemente con la storia 0002: lo stato del nulla osta è una vista, la prova è la riga.
- **RT-4 — Modulo frontend (§3, §5).** Estensione della sezione «Approvazioni» (storia 0021) con il filtro delle
  richieste scadute e il tempo rimanente su quelle in attesa; nessuna sezione nuova. Il tempo rimanente usa i
  colori funzionali del sistema di design man mano che si avvicina alla scadenza. Solo token del sistema di
  design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `agentaudit` e sono presenti
  in `en, it, fr, es, de`, compreso il testo dell'avviso ricorrente sulle richieste scadute.
- **RT-6 — Varchi e quota (§6, §7).** La scadenza **non consuma** la metrica `actions`: è un fatto della
  piattaforma, non un'azione dell'agente, e il conteggio è già stato fatto al momento della richiesta (storia
  0020). La configurazione delle durate richiede un ruolo amministrativo dell'account (`403` altrimenti); con
  abbonamento non attivo risponde `402`, ma **le scadenze continuano a decorrere**: una richiesta non deve restare
  eternamente in attesa perché l'abbonamento è scaduto.
- **RT-7 — Esposizione conversazionale (§12).** Le richieste scadute compaiono in
  `elenca_approvazioni_in_attesa(sorgente?)` come categoria distinta e in `riepiloga_attivita(periodo)`, entrambi
  marcati **lettura**. Nessuno strumento può prolungare una scadenza né far scadere in anticipo: sarebbe un modo
  per aggirare la decisione umana da una chat. Il contratto vive dentro il servizio; il server conversazionale è
  di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo**: la scadenza aggiunge una data e un conteggio.
  I campi già dichiarati (chi ha chiesto) restano quelli della storia 0020.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `nulla osta scaduto`, `riepilogo delle scadenze inviato` e
  `configurazione delle durate modificata` sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di
  correlazione, senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Il silenzio vale no**
- **Dato** un nulla osta in attesa con scadenza fra trenta minuti e nessuna persona che decide
- **Quando** la scadenza è trascorsa
- **Allora** lo stato è `scaduto`, l'esito verso la sorgente è **non concesso**, e nel registro compare la riga
  della scadenza con il momento esatto

**CA-2 — Non si decide una richiesta scaduta**
- **Dato** un nulla osta scaduto due ore fa
- **Quando** una persona con ruolo adeguato tenta di approvarlo
- **Allora** l'operazione viene rifiutata con l'indicazione che la richiesta è scaduta e da quanto, e nulla
  cambia; per procedere serve una richiesta nuova da parte dell'agente

**CA-3 — Le durate si configurano per classe di effetto**
- **Dato** un account che imposta trenta minuti per la classe pagamento e otto ore per l'invio verso l'esterno
- **Quando** nascono due nulla osta, uno per ciascuna classe
- **Allora** ognuno riceve la scadenza della propria classe, e la scadenza è visibile alla sorgente nella
  risposta

**CA-4 — La coda dimenticata si fa notare**
- **Dato** un account con sette richieste scadute nell'ultima settimana
- **Quando** viene prodotto l'avviso ricorrente
- **Allora** il cliente riceve un riassunto che riporta il numero di scadenze e gli strumenti coinvolti, e la
  schermata «Approvazioni» mostra le richieste scadute di recente

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con richieste in attesa che scadono nello stesso minuto
- **Quando** la lavorazione di scadenza viene eseguita e fallisce sull'account `A`
- **Allora** le richieste di `B` scadono correttamente, quelle di `A` restano in attesa e vengono riprovate, e un
  utente di `A` non vede mai le richieste scadute di `B`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo della scadenza per classe di effetto e sul divieto di decidere una richiesta
      scaduta, e di **integrazione** sulla lavorazione di scadenza, con database effimero e migrazioni vere e
      **senza attese a tempo reale**: il tempo si controlla, non si aspetta;
- [ ] prova di **isolamento fra account** sulla scadenza e sulla lettura delle richieste scadute;
- [ ] **prova end-to-end**: risposta «coprire ora» — il percorso `[J-AGENTAUDIT]` riceve il passo «una richiesta
      scade e la sorgente riceve non concesso», con il tempo controllato dai dati di prova; il registro di
      copertura [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) viene aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati**: nessuna voce nuova, e il fatto è dichiarato;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con la voce obbligatoria: il
      silenzio vale no, e perché non è configurabile in senso contrario;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo; il divieto di prolungare o anticipare una
      scadenza da una chat è dichiarato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0020` | La scadenza è una proprietà del nulla osta e ne chiude il ciclo di vita |
| storia `0021` | Estende la schermata «Approvazioni» e ne completa i due esiti possibili |

## 7. Fuori ambito

- l'**inoltro automatico a un secondo approvatore** prima della scadenza: sarebbe la risposta naturale al problema
  della persona in ferie, e non è in questa storia perché presuppone di aver deciso chi viene avvisato (punto
  aperto della storia 0021);
- la **riapertura** di una richiesta scaduta: deliberatamente esclusa. Riaprire significa approvare a posteriori
  un'azione che nel frattempo l'agente potrebbe aver già rinunciato a compiere, e produce un registro ambiguo;
- il caso in cui l'agente compia comunque l'azione dopo un `non concesso`: è uno scostamento e lo rileva la storia
  0023.

## 8. Punti aperti

- **Le durate predefinite.** Trenta minuti e otto ore sono esempi, non una proposta studiata: la durata giusta
  dipende da quanto rapidamente l'agente ha bisogno di procedere e da quanto è realistico che una persona
  risponda in quell'azienda. Chi chiude: sviluppatore.
- **Se il silenzio debba poter valere sì per qualche classe.** La risposta di questa storia è no, senza
  configurazione contraria: rendere configurabile «alla scadenza procedi» significherebbe offrire un modo elegante
  per disattivare il presidio continuando a pagarlo. Se il mercato lo chiedesse con insistenza, sarebbe una
  decisione di prodotto e non una funzione da aggiungere di slancio. Chi chiude: sviluppatore.
- **Che cosa succede se la lavorazione di scadenza si ferma.** Le richieste resterebbero in attesa oltre il
  dovuto, cioè il presidio diventerebbe un blocco. Serve un allarme di piattaforma sul ritardo della lavorazione,
  che è materia della storia 0026 e dell'osservabilità comune. Chi chiude: sviluppatore.
