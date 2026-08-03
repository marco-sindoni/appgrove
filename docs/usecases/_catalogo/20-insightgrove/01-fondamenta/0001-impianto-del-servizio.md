# 0001 — Impianto del servizio `insights`

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 01 — Fondamenta
**Storia**: `0001` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: nessuna: è la prima dell'epica e dell'applicazione
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore di piattaforma
> voglio che l'applicazione InsightGrove esista come servizio avviabile, registrato e raggiungibile
> così da poter costruire tutto il resto sopra qualcosa che parte davvero, invece che sopra un'idea.

**Contesto.** Oggi InsightGrove non esiste. Prima di poter ricevere un solo fatto o mostrare un solo indicatore
serve il servizio: un modulo Maven, la rotta pubblica, l'istanza del modulo di infrastruttura, la coda dedicata
sul bus di eventi. Questa storia non produce niente di visibile al cliente, ma è la prima perché tutte le altre
la presuppongono. L'app **non si scaffolda a mano**: nasce dalla skill `new-application`, e le risposte al varco
d'identità sono già scritte nel §3 della [descrizione dell'applicazione](../application-description.md).

## 2. Requisiti funzionali

1. **RF-1** — Esiste il servizio `services/insights`, generato dalla skill `new-application` con
   `app_id = insights`, modello utente `multi`, porta locale `8120`, colore-categoria `blue`.
2. **RF-2** — Il servizio risponde su `/api/insights/v1/` con almeno una risorsa di verifica dello stato, che non
   richiede autenticazione e non tocca il database.
3. **RF-3** — Il servizio ha la sua istanza del modulo di infrastruttura `microsaas_app`, con lo schema
   `app_insights`, il ruolo del database dedicato e **una coda dedicata sul bus di eventi**, che serve sia alla
   purga per account sia — dalla storia 0007 — alla ricezione dei fatti.
4. **RF-4** — Il servizio dichiara nel proprio `application.properties` tutto ciò che la scoperta automatica dei
   servizi usa per derivare la mappa *servizio → identificativo app → porta → schema*: nessuno script di avvio
   viene modificato a mano.
5. **RF-5** — La definizione OpenAPI del servizio è generata e versionata nel repository.
6. **RF-6** — Il servizio implementa lo scheletro del contratto dati `InsightsDataContract` con `appId()`,
   `exportData(scope)`, `purgeData(scope)` e `manifest()`, anche se al momento non c'è ancora alcuna tabella da
   esportare o cancellare.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Non ci sono ancora entità, ma il contesto del tenant è cablato:
  ogni risorsa futura leggerà `tenant_id` dal gettone verificato tramite `services/commons`, mai dal corpo della
  richiesta o dai parametri.
- **RT-2 — Interfaccia di programmazione (§2).** Quarkus 3.20.6, Java 21, Quarkus REST + Hibernate ORM
  **bloccante**, pacchetto radice `app.appgrove.insights`, dipendenza da `services/commons`; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione Flyway `V1__baseline.sql` che crea lo schema `app_insights` vuoto;
  migrazioni non applicate all'avvio in produzione.
- **RT-9 — Infrastruttura (§9).** L'infrastruttura nasce dall'istanza del modulo `microsaas_app` prodotta da
  `infra/scripts/service-add`; nessuna risorsa scritta a mano, nessuna modifica manuale al blocco `module`
  generato.
- **RT-15 — Avvio locale (§15).** `./dev.sh services` mostra `insights` con la porta `8120` e lo schema
  `app_insights`; `./app-start.sh` lo avvia senza modifiche manuali agli script; il blocco `api-routes` del
  proxy locale si rigenera da solo.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: la storia non introduce tabelle. Il manifesto
  `docs/compliance/manifests/insights.yaml` nasce con identificativo, nome e descrizione in italiano e inglese e
  **nessuna voce**.
- **RT-11 — Prove (§11).** Prova di integrazione che avvia il servizio e verifica la risorsa di stato;
  `./run-tests.sh` aggiornato con la nuova area del servizio.

## 4. Criteri di accettazione

**CA-1 — Il servizio si avvia in locale**
- **Dato** un repository pulito dopo l'unione del ramo
- **Quando** si esegue `./app-start.sh`
- **Allora** il servizio `insights` risponde sulla porta `8120` e `./dev.sh services` lo elenca con lo schema
  `app_insights`

**CA-2 — La rotta pubblica esiste**
- **Dato** lo stack locale avviato
- **Quando** si chiama `GET /api/insights/v1/health` attraverso il proxy locale
- **Allora** la risposta è `200` e proviene dal servizio `insights`, senza che il file del proxy sia stato
  modificato a mano

**CA-3 — Nessun cablaggio manuale**
- **Dato** il ramo di questa storia
- **Quando** si guarda il differenziale
- **Allora** non ci sono modifiche a `app-start.sh`, `app-stop.sh` o al blocco delle rotte del proxy scritte a
  mano: tutto discende da `services/insights/src/main/resources/application.properties`

**CA-4 — L'errore esce nel formato di piattaforma**
- **Dato** lo stack locale avviato
- **Quando** si chiama una rotta inesistente sotto `/api/insights/v1/`
- **Allora** la risposta è `404` con corpo `application/problem+json`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **integrazione** che avviano il servizio con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account**: non applicabile in questa storia (nessuna risorsa con dati), e detto
      esplicitamente nel registro delle decisioni;
- [ ] **prova end-to-end**: *rimando* — non c'è ancora superficie utente; la copre la storia 0034, che possiede
      il percorso `[J-INSIGHTS]`. Voce `da-coprire` nel registro di copertura con motivo e storia proprietaria;
- [ ] **traduzioni**: non applicabile (nessun testo visibile in questa storia);
- [ ] **manifesto dei dati** creato, in italiano e inglese, senza voci;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con almeno la scelta
      dell'identificativo dell'app e la sua motivazione;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione introdotta, e detto esplicitamente;
- [ ] `./dev.sh services` e l'avvio locale funzionano senza passi manuali;
- [ ] `run-tests.sh` aggiornato con il nuovo modulo nello stesso commit.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| nessuna storia di questa app | è la prima |
| skill `new-application` (UC 0046) | l'app non si scaffolda a mano: il generatore produce servizio, modulo frontend, istanza di infrastruttura, manifesto e listino |
| decisione sul listino (§5 della descrizione) | `new-application` chiede prezzi e quota prima di generare: è una fermata di escalation dello sviluppatore |

## 7. Fuori ambito

- il modello dati e la tabella dei fatti: è la storia 0002;
- il modulo frontend: è la storia 0003;
- il consumo degli eventi dalla coda: la coda si crea qui, il consumatore è la storia 0007;
- qualunque indicatore: l'epica 03.

## 8. Punti aperti

- **Il listino e la quota** devono essere confermati dallo sviluppatore prima che `new-application` generi il
  file `pricing/insights.yaml` (§5 della descrizione, punto aperto 1 e 9).
- **Il contratto degli eventi di dominio è di piattaforma o dell'app?** La coda si crea qui, ma che cosa vi
  transita è una decisione che riguarda tutte le app (punto aperto 11 della descrizione). Va chiarita prima
  della storia 0006, non prima di questa.
