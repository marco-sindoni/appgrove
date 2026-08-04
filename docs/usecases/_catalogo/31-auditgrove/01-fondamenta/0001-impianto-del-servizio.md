# 0001 — Impianto del servizio

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 01 — Fondamenta
**Storia**: `0001` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: nessuna: è la prima dell'epica e dell'applicazione
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore di piattaforma
> voglio che l'applicazione AuditGrove esista come servizio avviabile, con la sua rotta pubblica e la sua
> infrastruttura generata dal modulo comune
> così da poter costruire tutto il resto sopra qualcosa che parte, risponde e si distribuisce come le altre app.

**Contesto.** Oggi AuditGrove non esiste: c'è una descrizione. Questa storia è l'atto di nascita — l'esecuzione
della skill `new-application`, che genera il servizio Quarkus, il modulo frontend vuoto, l'istanza del modulo
Terraform e i file di listino e manifesto. È la prima storia perché ogni altra ha bisogno di un servizio dove
mettere il proprio codice. Non introduce nessuna funzione di dominio: chi la esegue non deve avere la tentazione
di anticipare il registro (storia 0002).

## 2. Requisiti funzionali

1. **RF-1** — Esiste il servizio `services/agentaudit` con pacchetto radice `app.appgrove.agentaudit`, che si
   avvia e risponde alla rotta di stato di salute.
2. **RF-2** — La rotta pubblica dell'app è `/api/agentaudit/v1/*` e restituisce `401` senza token valido, `402`
   per un account non abilitato all'app.
3. **RF-3** — La definizione OpenAPI del servizio è generata e versionata nel repository, anche se al momento
   dichiara le sole rotte tecniche.
4. **RF-4** — L'infrastruttura dell'app nasce da un'istanza del modulo Terraform comune, prodotta dallo script di
   aggiunta del servizio; nessuna risorsa scritta a mano.
5. **RF-5** — Il file di listino `pricing/agentaudit.yaml` esiste con i piani proposti al §5 della descrizione
   dell'applicazione, **come proposta approvata dallo sviluppatore** e non come valore inventato dalla storia.
6. **RF-6** — Il manifesto dei dati `docs/compliance/manifests/agentaudit.yaml` esiste, in italiano e inglese,
   inizialmente vuoto di voci ma con identificativo, nome e descrizione dell'app.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il contesto del tenant arriva da `services/commons`: `tenant_id` dal
  token verificato, mai dal corpo o dai parametri. Nessuna rotta di dominio ancora esposta, ma il filtro è già
  cablato nel livello di accesso ai dati.
- **RT-2 — Interfaccia di programmazione (§2).** Quarkus 3.20.6 su Java 21, Quarkus REST + Hibernate ORM
  bloccante, modello *repository*; errori in `application/problem+json`; definizione OpenAPI aggiornata nello
  stesso commit.
- **RT-3 — Persistenza (§8).** Schema `app_agentaudit` creato dalla migrazione `V1__baseline.sql`; ruolo di
  database dedicato al servizio, con privilegi solo sul proprio schema. Nessuna tabella di dominio in questa
  storia.
- **RT-4 — Modulo frontend (§3, §5).** Non in questa storia: è la 0003. Qui si genera solo la cartella del modulo
  con il manifesto minimo prodotto dallo scaffolding.
- **RT-5 — Cinque lingue (§4).** Le descrizioni del listino sono nelle cinque lingue `en, it, fr, es, de`, come
  prescritto dal formato del listino come codice.
- **RT-6 — Varchi e quota (§6, §7).** La catena dei varchi è quella comune: `401` senza token, `403` ad app
  spenta, `402` senza abilitazione, `403` per ruolo insufficiente, `429` a quota esaurita. La metrica `actions`
  è dichiarata nel listino; il consumo effettivo è la storia 0004.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia. Lo scaffolding lascia il
  luogo dove il contratto degli strumenti vivrà (epica 07), coerente con UC 0063 e UC 0066.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: il manifesto nasce con l'intestazione e senza
  voci. Il contratto dati dell'app (`AgentauditDataContract`) è generato con `exportData` e `purgeData` vuoti e
  **deve fallire il collaudo se resta vuoto quando arriveranno le tabelle** (storia 0033).
- **RT-9 — Registrazione eventi (§14).** Il registro strutturato del servizio porta `tenant_id`, `app_id`,
  `user_id` e identificativo di correlazione fin dal primo avvio, senza dati personali.
- **RT-10 — Infrastruttura (§9).** L'infrastruttura dell'app nasce dall'istanza del modulo `microsaas_app`
  prodotta dallo scaffolding; il blocco generato non si modifica a mano.

## 4. Criteri di accettazione

**CA-1 — Il servizio esiste e risponde**
- **Dato** il repository dopo l'esecuzione della skill di scaffolding
- **Quando** si avvia lo stack locale
- **Allora** il servizio `agentaudit` risulta avviato sulla porta assegnata e la sua rotta di stato di salute
  risponde con esito positivo

**CA-2 — La rotta pubblica è protetta**
- **Dato** un chiamante senza token
- **Quando** interroga `/api/agentaudit/v1/…`
- **Allora** riceve `401` in formato `problem+json`, e nessuna informazione sull'esistenza delle risorse

**CA-3 — L'app non abilitata è respinta**
- **Dato** un utente autenticato di un account **non** abbonato ad AuditGrove
- **Quando** interroga una rotta dell'app
- **Allora** riceve `402` con l'indicazione di come attivare l'abbonamento

**CA-4 — L'infrastruttura è generata, non scritta**
- **Dato** il codice dell'infrastruttura dopo la storia
- **Quando** si esegue la validazione statica dell'infrastruttura
- **Allora** l'app risulta come istanza del modulo comune e non compare nessuna risorsa su misura

**CA-5 — Il listino è coerente**
- **Dato** il file di listino dell'app
- **Quando** si eseguono i controlli dell'area di conformità e del listino
- **Allora** la metrica dichiarata è `actions` di natura `flow`, il colore-categoria è `violet` e coincide con
  quello del modulo frontend, e le descrizioni esistono in tutte e cinque le lingue

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla logica introdotta e di **integrazione** sull'avvio del servizio, con database
      effimero e migrazioni vere;
- [ ] prova di **isolamento fra account**: non applicabile in questa storia (nessuna risorsa di dominio), e la
      cosa è dichiarata esplicitamente invece che sottintesa;
- [ ] **prova end-to-end**: risposta «nessun impatto» — l'app non ha ancora superficie utente; il percorso
      `[J-AGENTAUDIT]` nasce alla storia 0037, e il registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve la voce dell'app con
      esenzione motivata fino ad allora;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`) per le descrizioni del listino;
- [ ] **manifesto dei dati** creato in italiano e inglese, ancora senza voci;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato con le scelte fatte e il perché — in
      particolare l'identificativo dell'app e il motivo per cui non è `audit`;
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia, dichiarato;
- [ ] `./dev.sh services` mostra l'app con la sua porta e il suo schema, e `./app-start.sh` la avvia senza
      modifiche manuali agli script;
- [ ] `run-tests.sh` aggiornato con il nuovo modulo backend nello stesso commit.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Skill `new-application` (UC 0046) | È la via unica per creare un'app: non si scaffolda a mano |
| Decisione sul perimetro (§0 della descrizione) | Se il perimetro fosse quello della piattaforma invece che dei sistemi del cliente, il modello dati sarebbe un altro e questa storia genererebbe la cosa sbagliata |
| Conferma di listino e classificazione dati personali | Sono le due fermate di escalation che la skill pretende prima di generare |

## 7. Fuori ambito

- il registro e la catena delle impronte: storia 0002;
- qualunque schermata: storia 0003;
- il consumo effettivo della quota: storia 0004;
- l'ingresso delle azioni: epica 02.

## 8. Punti aperti

- **Il perimetro dell'app** (§0 della descrizione dell'applicazione) è direzione di prodotto e va chiuso **prima**
  di eseguire questa storia: chiude lo sviluppatore.
- **La porta locale** `8131` è una proposta del kit: va verificata con `./dev.sh services` al momento dello
  scaffolding.
- **Prezzi e limiti** del listino sono una proposta (§5 della descrizione): li conferma lo sviluppatore.
