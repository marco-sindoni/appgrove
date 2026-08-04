# 0001 — Impianto del servizio

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Epica**: 01 — Fondamenta
**Storia**: `0001` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: nessuna: è la prima dell'epica e dell'applicazione
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore di piattaforma
> voglio che il servizio `preventivi` esista, risponda e sia raggiungibile come tutte le altre app
> così da poterci costruire sopra il dominio senza rifare ogni volta il lavoro di impianto.

**Contesto.** Oggi l'applicazione non esiste. Nel monorepo una app nuova **non si scaffolda a mano**: nasce dalla
skill `new-application`, che genera il servizio Quarkus, il modulo frontend, l'istanza del modulo di
infrastruttura, il manifesto dei dati e il file di listino. Questa storia è il momento in cui si eseguono le
risposte del varco d'identità già scritte nella descrizione dell'applicazione (`preventivi`, modello `multi`,
porta `8106`, metrica `preventivi_inviati` di natura `flow`, colore `violet`) e si verifica che l'esito sia sano.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il servizio `services/preventivi` generato dalla skill `new-application`, con pacchetto
   radice `app.appgrove.preventivi` e dipendenza da `services/commons`.
2. **RF-2** — Il servizio espone una risorsa di stato su `/api/preventivi/v1/health` che risponde `200` a un
   utente autenticato e abilitato, e la definizione OpenAPI è generata e versionata.
3. **RF-3** — L'infrastruttura dell'app nasce da una istanza del modulo Terraform `microsaas_app` prodotta dallo
   scaffolding: nessuna risorsa scritta a mano.
4. **RF-4** — Il file di listino `services/core/src/main/resources/pricing/preventivi.yaml` esiste, è registrato
   in `pricing/index.yaml` e dichiara `category: violet`, `userModel: multi_user` e la metrica
   `preventivi_inviati` di tipo `flow`.
5. **RF-5** — `run-tests.sh` riconosce il nuovo modulo backend e lo esegue senza modifiche manuali agli script.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Anche la risorsa di stato passa dal contesto del tenant: legge
  `tenant_id` dal token verificato e non accetta nessun identificativo dal corpo o dai parametri.
- **RT-2 — Interfaccia di programmazione (§2).** Quarkus 3.20.6 su Java 21, Quarkus REST + Hibernate ORM
  **bloccante**, accesso ai dati con il modello *repository*, errori in `application/problem+json`, rotte sotto
  `/api/preventivi/v1/`. La definizione OpenAPI è aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Questa storia dichiara lo schema `app_preventivi` e il ruolo di database dedicato;
  le tabelle arrivano con la storia `0002`.
- **RT-4 — Infrastruttura (§9).** Istanza del modulo `microsaas_app` creata da `infra/scripts/service-add`; il
  blocco `module` generato non si modifica a mano.
- **RT-5 — Avvio locale (§15).** Il file `services/preventivi/src/main/resources/application.properties` dichiara
  identificativo app, porta `8106` e schema: da lì la scoperta automatica ricava tutto il resto.
- **RT-6 — Registrazione eventi (§14).** Ogni riga di registro porta `tenant_id`, `app_id`, `user_id` e
  identificativo di correlazione, senza dati personali.
- **RT-7 — Dati personali (§10).** Nessun dato personale nuovo: questa storia non introduce campi di persone. Il
  manifesto vuoto viene creato e riempito dalla storia `0007`.
- **RT-8 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia: il contratto comincia con
  l'epica 06.

## 4. Criteri di accettazione

**CA-1 — Il servizio parte e risponde**
- **Dato** lo stack locale avviato con `./app-start.sh`
- **Quando** un utente autenticato di un account abilitato chiama `GET /api/preventivi/v1/health`
- **Allora** riceve `200` e il corpo dichiara identificativo dell'app e versione

**CA-2 — L'app è scoperta senza cablaggi**
- **Dato** il solo file `application.properties` del servizio
- **Quando** si esegue `./dev.sh services`
- **Allora** l'app compare con `app_id` `preventivi`, porta `8106` e schema `app_preventivi`, e la rotta
  `/api/preventivi/v1/*` è presente nel blocco rigenerato del proxy locale

**CA-3 — Utente non autenticato**
- **Dato** una richiesta senza token · **Quando** chiama la risorsa di stato · **Allora** riceve `401` in
  `application/problem+json`

**CA-4 — Infrastruttura validata**
- **Dato** l'istanza del modulo generata · **Quando** si esegue `./run-tests.sh infra` · **Allora** formattazione,
  validazione e prove del modulo sono verdi

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e infra; l'intera suite prima del commit);
- [ ] prove di **unità** sulla configurazione e di **integrazione** sulla risorsa di stato, con database effimero;
- [ ] prova di **isolamento fra account** sulla risorsa introdotta;
- [ ] **prova end-to-end**: nessun impatto in questa storia (nessuna superficie utente) — il percorso
  `[J-PREVENTIVI]` nasce con la storia `0029`, il registro
  [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) si aggiorna lì;
- [ ] **traduzioni**: non applicabile, nessun testo visibile;
- [ ] **manifesto dei dati** creato vuoto, riempito dalla storia `0007`;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, comprese le risposte del varco
      d'identità (identificativo, modello utente, porta, metrica, colore);
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia;
- [ ] `./dev.sh services` e l'avvio locale funzionano senza passi manuali;
- [ ] descrizione dell'applicazione aggiornata se un valore del varco d'identità è stato cambiato in corsa.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| skill `new-application` (UC 0046) | è il solo modo ammesso di creare una app nuova |
| decisione dello sviluppatore su listino e dati personali | la skill si ferma e chiede: sono le due fermate di escalation |

## 7. Fuori ambito

- le tabelle di dominio: storia `0002`;
- il modulo frontend: storia `0003`;
- il consumo della quota: storia `0004`.

## 8. Punti aperti

Il colore-categoria `violet` è proposto sapendo che nel repository il listino e il manifesto delle due app reali
oggi non concordano sul colore (punto 7 dei rischi della descrizione dell'applicazione): prima di generare va
stabilito quale delle due fonti comanda. Lo chiude lo sviluppatore.
