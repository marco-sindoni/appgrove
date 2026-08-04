# 0001 — Impianto del servizio

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 01 — Fondamenta
**Storia**: `0001` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: nessuna: è la prima dell'epica e dell'applicazione
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore di piattaforma
> voglio che ReachGrove esista come servizio avviabile, con le sue rotte e la sua infrastruttura
> così da poterci costruire sopra le storie del consenso e dell'invio senza inventare impianto ogni volta.

**Contesto.** Oggi ReachGrove è solo un documento di catalogo. Prima di qualunque iscritto, consenso o campagna
serve il guscio: il servizio Quarkus, il pacchetto radice, le rotte pubbliche, l'istanza del modulo di
infrastruttura e i controlli di salute. È la storia che va fatta per prima perché tutte le altre la presuppongono;
è anche la sola che tocca l'infrastruttura, ed è il momento in cui si spendono le risposte del varco d'identità
([application-description.md](../application-description.md) §3).

## 2. Requisiti funzionali

1. **RF-1** — Esiste il servizio `services/campaigns/`, generato dalla skill `new-application` con identificativo
   `campaigns`, modello utente `multi`, porta locale `8116`, metrica di quota `messages_sent` di natura `flow`,
   colore-categoria `violet`.
2. **RF-2** — Il servizio risponde ai controlli di salute e di prontezza, e restituisce la propria definizione
   OpenAPI generata.
3. **RF-3** — Esiste una rotta di sonda `GET /api/campaigns/v1/ping` che richiede un token valido e restituisce
   l'identificativo dell'account ricavato dal token, così che l'impianto sia verificabile prima che esista dominio.
4. **RF-4** — L'infrastruttura dell'app nasce dall'istanza del modulo Terraform comune, prodotta dallo script di
   aggiunta servizio; nessuna risorsa scritta a mano.
5. **RF-5** — Esiste lo schema `app_campaigns`, creato dalla prima migrazione Flyway; nessuna tabella di dominio
   in questa storia.
6. **RF-6** — `run-tests.sh` include il nuovo modulo nell'area `backend` senza modifiche manuali agli elenchi.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La rotta di sonda ricava `tenant_id` **solo** dal token verificato; un
  `tenant_id` nel corpo o nei parametri viene ignorato. È il primo posto dove l'invariante si prova, ed è
  l'invariante su cui poggia tutto il resto dell'app: qui il perimetro dell'account decide **a chi si può
  scrivere**, non solo cosa si vede.
- **RT-2 — Interfaccia di programmazione (§2).** Servizio Maven `services/campaigns/` dipendente da
  `services/commons`, pacchetto radice `app.appgrove.campaigns`, Quarkus REST + Hibernate ORM **bloccante**, rotte
  `/api/campaigns/v1/...`, errori in `application/problem+json`, definizione OpenAPI generata e versionata nello
  stesso commit.
- **RT-3 — Persistenza (§8).** Prima migrazione Flyway `V1__create_app_campaigns_schema.sql` sullo schema
  `app_campaigns`, non applicata all'avvio in produzione. Nessuna tabella di dominio: il modello dati è la storia
  0002.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata in questa storia: il guscio del modulo è la storia 0003.
- **RT-5 — Cinque lingue (§4).** Nessun testo visibile introdotto.
- **RT-6 — Varchi e quota (§6, §7).** La rotta di sonda attraversa i varchi 1 (token valido → altrimenti `401`) e
  2 (app non spenta dalla piattaforma → altrimenti `403`); abilitazione dell'account e quota sulla metrica
  `messages_sent` arrivano con la storia 0004.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia. Si predispone il pacchetto
  `app.appgrove.campaigns.tools` che ospiterà il contratto degli strumenti (epica 07). Dipendenza dichiarata:
  UC 0061-0063, livello conversazionale di piattaforma non ancora implementato.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: la sonda restituisce identificativi, non recapiti.
  Si crea però il file `docs/compliance/manifests/campaigns.yaml` con intestazione, nome e descrizione in italiano
  e inglese e l'elenco delle voci **vuoto**, perché la storia 0002 lo riempia. Si predispone la classe
  `CampaignsDataContract` con `appId()`, `exportData(scope)`, `purgeData(scope)` e `manifest()` ancora senza
  tabelle: aggiungerle una per una è il lavoro della storia 0002 e delle storie di dominio.
- **RT-9 — Registrazione eventi (§14).** Ogni riga di registro del servizio porta `tenant_id`, `app_id`
  (`campaigns`), `user_id` e identificativo di correlazione della richiesta; nessun dato personale — in questa app
  significa in particolare: **mai** un indirizzo di posta o un numero di telefono in un registro, nemmeno in un
  messaggio di errore.
- **RT-10 — Infrastruttura (§9).** L'infrastruttura nasce dall'istanza del modulo `microsaas_app` prodotta dallo
  scaffolding; il blocco generato non si modifica a mano.

## 4. Criteri di accettazione

**CA-1 — Il servizio si avvia e risponde**
- **Dato** il servizio `campaigns` compilato
- **Quando** lo si avvia in locale sulla porta `8116`
- **Allora** i controlli di salute e di prontezza rispondono positivamente e la definizione OpenAPI è raggiungibile

**CA-2 — La sonda richiede un token**
- **Dato** una richiesta a `GET /api/campaigns/v1/ping` senza token
- **Quando** la richiesta arriva al servizio
- **Allora** riceve `401` con corpo in `application/problem+json`

**CA-3 — Isolamento fra account**
- **Dato** due account `A` e `B` con due token validi
- **Quando** ciascuno interroga la sonda passando nel corpo l'identificativo dell'altro account
- **Allora** ognuno riceve **il proprio** identificativo, ricavato dal token, e quello passato viene ignorato

**CA-4 — Lo schema esiste ed è vuoto**
- **Dato** un database effimero con le migrazioni vere applicate
- **Quando** si interroga l'elenco delle tabelle dello schema `app_campaigns`
- **Allora** lo schema esiste e non contiene tabelle di dominio, oltre a quelle di controllo delle migrazioni

**CA-5 — L'infrastruttura è validata**
- **Dato** l'istanza del modulo prodotta dallo scaffolding
- **Quando** si esegue l'area `infra` di `run-tests.sh`
- **Allora** formattazione, validazione e analisi statica sono verdi e nessuna risorsa risulta scritta a mano

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `infra`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla configurazione del contesto dell'account e di **integrazione** sulla sonda, con
      database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla rotta di sonda;
- [ ] **prova end-to-end**: nessun impatto — la storia non introduce superficie utente; il percorso
      `[J-CAMPAIGNS]` nasce nella storia 0037 e da lì entra nel registro di copertura;
- [ ] **traduzioni**: nessun testo visibile introdotto;
- [ ] **manifesto dei dati** `docs/compliance/manifests/campaigns.yaml` creato con intestazione in italiano e
      inglese e voci vuote; contratto dati dell'app predisposto senza tabelle;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con annotate le sei risposte del
      varco d'identità e il motivo dell'identificativo `campaigns` invece di `reach` o `marketing`;
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia, pacchetto predisposto;
- [ ] `./dev.sh services` mostra `campaigns` con porta `8116` e schema `app_campaigns`; `./app-start.sh` la avvia
      senza modifiche manuali agli script;
- [ ] documentazione aggiornata: `run-tests.sh` e l'indice degli use case.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Skill `new-application` (UC 0046) | L'app non si scaffolda a mano: il generatore produce servizio, modulo frontend, istanza di infrastruttura e file di listino |
| Conferma del varco d'identità ([application-description.md](../application-description.md) §3) | Identificativo, modello utente, porta, metrica e colore-categoria si spendono qui: cambiarli dopo non è una rinomina, è una migrazione di schema, rotte e infrastruttura |

## 7. Fuori ambito

- il modello dati di dominio: è la storia 0002;
- il guscio del modulo frontend: è la storia 0003;
- abilitazione dell'account e quota degli invii: è la storia 0004;
- il popolamento con dati di prova: è la storia 0005;
- il listino `services/core/src/main/resources/pricing/campaigns.yaml`: lo scrive la skill con lo sviluppatore,
  dopo la conferma dei prezzi ([application-description.md](../application-description.md) §5);
- la scelta e la configurazione del fornitore di consegna della posta elettronica: nessun invio parte in questa
  epica. È un prerequisito dell'epica 04 (storie 0017 e 0019), non di questa storia.

## 8. Punti aperti

- **Prezzi, limiti dei piani e durata della prova** — fermata di escalation dello sviluppatore
  ([application-description.md](../application-description.md) §5): il file di listino non si scrive senza
  conferma. Lo scaffolding può creare il file, ma i numeri li mette una persona.
- **Fornitore di consegna della posta elettronica** — l'unico prezzo verificato in analisi è quello di un
  fornitore statunitense con regione europea disponibile ma casa madre fuori dall'Unione europea
  ([application-description.md](../application-description.md) §11.2). È insieme una scelta di fornitore, di costo
  e di conformità, e va chiusa **prima** dell'epica 04. Non blocca questa storia. Chiude lo sviluppatore.
- **Colore-categoria `violet`** — già proposto da 06 QuoteGrove e 13 FlowGrove: la collisione fra sei colori e
  sessanta app è strutturale ([application-description.md](../application-description.md) §11.7). Chiude la
  piattaforma quando i colori si assegnano sul serio.
