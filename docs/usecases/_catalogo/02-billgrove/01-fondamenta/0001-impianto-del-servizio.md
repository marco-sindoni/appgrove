# 0001 — Impianto del servizio

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 01 — Fondamenta
**Storia**: `0001` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: nessuna: è la prima dell'epica
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore della piattaforma
> voglio che il servizio `billing` esista, risponda e sia visibile agli strumenti comuni
> così da poter costruire tutto il resto sopra qualcosa che si avvia, si prova e si distribuisce come le altre app.

**Contesto.** Oggi BillGrove non esiste: non c'è servizio, non c'è rotta, non c'è istanza di infrastruttura. Questa
è la storia che accende la scatola vuota. Va fatta per prima perché ogni altra storia dell'app presuppone una rotta
`/api/billing/v1/*` che risponde e una definizione delle interfacce da cui il frontend genera il proprio client.
L'app **non si scaffolda a mano**: si usa la skill `new-application`, che chiede prima le sei risposte del varco
d'identità già scritte nella descrizione dell'applicazione (§3).

## 2. Requisiti funzionali

1. **RF-1** — Esiste il servizio `services/billing`, avviabile, con pacchetto radice `app.appgrove.billing`.
2. **RF-2** — Esiste una rotta di verifica dello stato `GET /api/billing/v1/health` che risponde senza richiedere
   un token, e almeno una rotta protetta che risponde `401` senza token valido.
3. **RF-3** — La definizione delle interfacce (OpenAPI) è generata e versionata nel repository.
4. **RF-4** — Gli errori escono in formato `application/problem+json`, compresi quelli dei varchi.
5. **RF-5** — L'istanza di infrastruttura dell'app nasce dal modulo comune, senza risorse scritte a mano.
6. **RF-6** — L'app compare nel catalogo delle applicazioni della piattaforma con il suo colore-categoria `teal`.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il contesto del tenant è preso dal token verificato tramite
  `services/commons`; nessuna rotta accetta un `tenant_id` dal corpo o dai parametri. Non ci sono ancora entità, ma
  il filtro è già in piedi: la prima entità lo troverà pronto.
- **RT-2 — Interfaccia di programmazione (§2).** Quarkus 3.20.6 su Java 21, Quarkus REST + Hibernate ORM
  **bloccante**, accesso ai dati con il modello *repository*. Rotte sotto `/api/billing/v1/`. Definizione OpenAPI
  aggiornata nello stesso commit. Il servizio **non chiama** altri servizi in modo sincrono.
- **RT-3 — Persistenza (§8).** In questa storia si crea solo lo schema `app_billing` e la prima migrazione Flyway
  vuota di impianto; le tabelle arrivano con la storia `0002`.
- **RT-6 — Varchi e quota (§6).** La catena dei varchi è cablata nel filtro comune: `401` senza token, `403` ad app
  spenta, `402` senza abilitazione, `403` a ruolo insufficiente. La quota arriva con la storia `0004`.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: il servizio non ha ancora entità. Il manifesto
  `docs/compliance/manifests/billing.yaml` nasce però qui, con identificativo, nome e descrizione in italiano e
  inglese e l'elenco delle voci vuoto.
- **RT-9 — Registrazione eventi (§14).** Ogni riga di registro porta `tenant_id`, `app_id=billing`, `user_id` e
  l'identificativo di correlazione della richiesta; JSON in produzione, testo leggibile in locale.
- **RT-10 — Infrastruttura (§9).** L'infrastruttura dell'app nasce dall'istanza del modulo `microsaas_app` prodotta
  dallo scaffolding, tramite `infra/scripts/service-add`; nessuna risorsa scritta a mano, nessuna modifica manuale
  al blocco generato.
- **RT-11 — Avvio locale (§15).** Le proprietà `quarkus.http.port=8102`, `quarkus.flyway.schemas=app_billing` e
  `appgrove.app-id=billing` sono dichiarate in `services/billing/src/main/resources/application.properties`: da lì
  discende tutta la scoperta automatica.

## 4. Criteri di accettazione

**CA-1 — Il servizio si avvia e risponde**
- **Dato** il repository appena aggiornato e nessun passo manuale eseguito
- **Quando** si lancia `./app-start.sh`
- **Allora** il servizio `billing` risulta avviato sulla porta `8102` e `GET /api/billing/v1/health` risponde `200`

**CA-2 — Rotta protetta senza token**
- **Dato** nessun token di accesso
- **Quando** si chiama una rotta protetta di `/api/billing/v1/`
- **Allora** la risposta è `401` in formato `application/problem+json`

**CA-3 — Account senza abilitazione**
- **Dato** un utente autenticato il cui account non è abbonato a BillGrove
- **Quando** chiama una rotta protetta
- **Allora** riceve `402` con un messaggio che indica come attivare l'app

**CA-4 — L'app è nota agli strumenti comuni**
- **Dato** lo stack locale avviato
- **Quando** si esegue `./dev.sh services`
- **Allora** `billing` compare nell'elenco con porta `8102` e schema `app_billing`, e la rotta
  `/api/billing/v1/*` risulta nel blocco rigenerato del proxy locale

**CA-5 — Infrastruttura validata**
- **Dato** l'istanza del modulo `microsaas_app` per `billing`
- **Quando** si esegue l'area `infra` di `./run-tests.sh`
- **Allora** formattazione, validazione e analisi statica passano, e nessuna risorsa dell'app è definita fuori dal
  modulo

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `infra`, `tooling`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla mappatura degli errori e di **integrazione** sull'avvio del servizio, con database
      effimero e migrazioni vere;
- [ ] prova di **isolamento fra account**: non applicabile in questa storia perché non ci sono ancora risorse — la
      prima è nella storia `0002`, e va dichiarato così nel registro delle decisioni;
- [ ] **prova end-to-end**: *rimando* — non c'è ancora superficie utente; il percorso `[J-BILLING]` nasce nella
      storia `0031`, che è la proprietaria;
- [ ] **traduzioni**: non applicabile, nessun testo visibile in questa storia;
- [ ] **manifesto dei dati** creato in italiano e inglese, con l'elenco delle voci vuoto e la dichiarazione
      esplicita che l'app non tratta ancora dati personali;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato con le scelte fatte e il perché;
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia, e va detto;
- [ ] `./dev.sh services` e l'avvio locale funzionano senza passi manuali;
- [ ] documentazione aggiornata: `run-tests.sh` include il nuovo modulo backend.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Skill `new-application` (UC 0046) | L'app non si scaffolda a mano: il generatore produce servizio, modulo, istanza di infrastruttura, manifesto e listino |
| Decisione sul listino (§5 della descrizione) | Il generatore chiede il listino prima di produrre il file `pricing/billing.yaml`: è una fermata di escalation |
| Punto aperto 1 del §11 (rapporto con l'app reale `fatture`) | Se la decisione fosse «BillGrove è l'evoluzione di `fatture`», questa storia cambia natura e diventa una migrazione |

## 7. Fuori ambito

- le tabelle del dominio: storia `0002`;
- il modulo frontend: storia `0003`;
- la quota e la sua prenotazione: storia `0004`;
- qualunque logica di documento: epica 03.

## 8. Punti aperti

Il listino non è deciso: è una fermata di escalation dello sviluppatore (§5 della descrizione dell'applicazione).
Va chiuso **prima** di eseguire il generatore, perché il file del listino nasce lì. Resta aperto anche il punto 1
del §11 — se BillGrove sia un'app nuova o l'evoluzione dell'app reale `fatture`.
