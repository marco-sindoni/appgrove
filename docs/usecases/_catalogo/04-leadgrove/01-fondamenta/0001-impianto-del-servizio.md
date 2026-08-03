# 0001 — Impianto del servizio

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 01 — Fondamenta
**Storia**: `0001` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: nessuna: è la prima dell'epica e dell'applicazione
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore di piattaforma
> voglio che LeadGrove esista come servizio avviabile, con le sue rotte e la sua infrastruttura
> così da poterci costruire sopra le storie di dominio senza inventare impianto ogni volta.

**Contesto.** Oggi LeadGrove è solo un documento di catalogo. Prima di qualunque contatto o trattativa serve il
guscio: il servizio Quarkus, il pacchetto radice, le rotte pubbliche, l'istanza del modulo di infrastruttura e i
controlli di salute. È la storia che va fatta per prima perché tutte le altre la presuppongono; è anche la sola
che tocca l'infrastruttura, ed è il momento in cui si spendono le risposte del varco d'identità
([application-description.md](../application-description.md) §3).

## 2. Requisiti funzionali

1. **RF-1** — Esiste il servizio `services/sales/`, generato dalla skill `new-application` con identificativo
   `sales`, modello utente `multi`, porta `8104`, metrica `seats` di natura `stock`, colore-categoria `blue`.
2. **RF-2** — Il servizio risponde ai controlli di salute e di prontezza, e restituisce la propria definizione
   OpenAPI generata.
3. **RF-3** — Esiste una rotta di sonda `GET /api/sales/v1/ping` che richiede un token valido e restituisce
   l'identificativo dell'account ricavato dal token, così che l'impianto sia verificabile prima che esista dominio.
4. **RF-4** — L'infrastruttura dell'app nasce dall'istanza del modulo Terraform comune, prodotta dallo script di
   aggiunta servizio; nessuna risorsa scritta a mano.
5. **RF-5** — `run-tests.sh` include il nuovo modulo nell'area `backend` senza modifiche manuali agli elenchi.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La rotta di sonda ricava `tenant_id` **solo** dal token verificato; un
  `tenant_id` nel corpo o nei parametri viene ignorato. È il primo posto dove l'invariante si prova.
- **RT-2 — Interfaccia di programmazione (§2).** Servizio Maven `services/sales/` dipendente da
  `services/commons`, pacchetto radice `app.appgrove.sales`, Quarkus REST + Hibernate ORM **bloccante**, rotte
  `/api/sales/v1/...`, errori in `application/problem+json`, definizione OpenAPI generata e versionata nello
  stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella in questa storia oltre a quanto crea lo scaffolding: il modello
  dati è la storia 0002. Si crea però lo schema `app_sales` con la prima migrazione Flyway
  `V1__create_app_sales_schema.sql`, non applicata all'avvio in produzione.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata in questa storia: il guscio del modulo è la storia 0003.
- **RT-5 — Cinque lingue (§4).** Nessun testo visibile introdotto.
- **RT-6 — Varchi e quota (§6, §7).** La rotta di sonda attraversa i varchi 1 (token valido → altrimenti `401`) e
  2 (app non spenta → altrimenti `403`); abilitazione e quota arrivano con la storia 0004.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia. Si predispone il pacchetto
  `app.appgrove.sales.tools` che ospiterà il contratto (epica 07). Dipendenza dichiarata: UC 0061-0063, livello
  conversazionale non ancora implementato.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: la sonda restituisce identificativi, non nomi. Si
  crea però il file `docs/compliance/manifests/sales.yaml` con intestazione, nome e descrizione in italiano e
  inglese e l'elenco delle voci **vuoto**, perché la storia 0002 lo riempia.
- **RT-9 — Registrazione eventi (§14).** Ogni riga di registro del servizio porta `tenant_id`, `app_id` (`sales`),
  `user_id` e identificativo di correlazione; nessun dato personale.
- **RT-10 — Infrastruttura (§9).** L'infrastruttura nasce dall'istanza del modulo `microsaas_app` prodotta dallo
  scaffolding; il blocco generato non si modifica a mano.

## 4. Criteri di accettazione

**CA-1 — Il servizio si avvia e risponde**
- **Dato** il servizio `sales` compilato
- **Quando** lo si avvia in locale sulla porta `8104`
- **Allora** i controlli di salute e prontezza rispondono positivamente e la definizione OpenAPI è raggiungibile

**CA-2 — La sonda richiede un token**
- **Dato** una richiesta a `GET /api/sales/v1/ping` senza token
- **Quando** la richiesta arriva al servizio
- **Allora** riceve `401` con corpo in `application/problem+json`

**CA-3 — Isolamento fra account**
- **Dato** due account `A` e `B` con due token validi
- **Quando** ciascuno interroga la sonda passando nel corpo l'identificativo dell'altro account
- **Allora** ognuno riceve **il proprio** identificativo, ricavato dal token, e quello passato viene ignorato

**CA-4 — L'infrastruttura è validata**
- **Dato** l'istanza del modulo prodotta dallo scaffolding
- **Quando** si esegue l'area `infra` di `run-tests.sh`
- **Allora** formattazione, validazione e analisi statica sono verdi e nessuna risorsa risulta scritta a mano

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `infra`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla configurazione del contesto dell'account e di **integrazione** sulla sonda, con
      database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla rotta di sonda;
- [ ] **prova end-to-end**: nessun impatto — la storia non introduce superficie utente; il percorso `[J-SALES]`
      nasce nella storia 0037;
- [ ] **traduzioni**: nessun testo visibile introdotto;
- [ ] **manifesto dei dati** creato con intestazione in italiano e inglese e voci vuote;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con annotate le sei risposte del
      varco d'identità e il motivo dell'identificativo `sales` invece di `crm`;
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia, pacchetto predisposto;
- [ ] `./dev.sh services` mostra `sales` con porta `8104` e schema `app_sales`; `./app-start.sh` la avvia senza
      modifiche manuali agli script;
- [ ] documentazione aggiornata: `run-tests.sh` e l'indice degli use case.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Decisione di prodotto sul rapporto con il Mini-CRM ([application-description.md](../application-description.md) §11.1) | Se lo sviluppatore scegliesse di ereditare `crm`, cambierebbero identificativo, schema, rotte e istanza di infrastruttura: cambiarli dopo non è una rinomina, è una migrazione |
| Skill `new-application` (UC 0046) | L'app non si scaffolda a mano |

## 7. Fuori ambito

- il modello dati di dominio: è la storia 0002;
- il modulo frontend: è la storia 0003;
- abilitazione e quota: è la storia 0004;
- il listino `pricing/sales.yaml`: lo scrive la skill con lo sviluppatore, dopo la conferma dei prezzi (§5 della
  descrizione dell'applicazione).

## 8. Punti aperti

- **Identificativo dell'app** — `sales` è una proposta; l'alternativa (ritirare il Mini-CRM ed ereditare `crm`) è
  una decisione dello sviluppatore e va presa **prima** di questa storia.
- **Prezzi e limiti dei piani** — fermata di escalation: il file di listino non si scrive senza conferma.
