# 0001 — Impianto del servizio

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 01 — Fondamenta
**Storia**: `0001` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: nessuna: è la prima dell'epica e la prima dell'applicazione
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore della piattaforma
> voglio che l'applicazione di magazzino esista come servizio a sé, avviabile e raggiungibile
> così da poter costruire il registro dei movimenti sopra un fondamento uguale a quello di tutte le altre app,
> senza infrastruttura scritta a mano e senza passi manuali che nessuno si ricorda.

**Contesto.** Oggi StockGrove è solo un documento. Questa storia porta in vita il servizio `magazzino`: cartella
Maven, dipendenza dalle parti comuni, rotte pubbliche, istanza di infrastruttura, proprietà di configurazione da
cui la scoperta automatica ricava tutto il resto. Va fatta **per prima** e va fatta con il generatore, non a mano:
un'app scaffoldata a mano nasce già diversa dalle altre e il difetto si eredita per anni
([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §16). Qui non c'è ancora nessuna tabella di dominio e
nessuna schermata: c'è il guscio che regge tutto il resto.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il servizio `services/magazzino/`, generato dalla skill `new-application`, con pacchetto
   radice `app.appgrove.magazzino` e dipendenza da `services/commons` (contesto dell'account, mappatura degli
   errori, entità di base con colonne di controllo, paginazione).
2. **RF-2** — Il file `services/magazzino/src/main/resources/application.properties` dichiara identificativo
   dell'app `magazzino`, porta locale `8114` e schema `app_magazzino`: sono le sole tre proprietà da cui la
   scoperta automatica dei servizi ricava avvio, migrazioni, rotte del proxy locale e avvii di collaudo.
3. **RF-3** — Il servizio espone una rotta di stato `GET /api/magazzino/v1/stato` che risponde `200` con la
   versione dell'applicazione e l'esito del collegamento alla base di dati, e **non** richiede un account
   abilitato.
4. **RF-4** — La definizione OpenAPI del servizio è generata e versionata nel repository; il client del frontend
   nascerà da lì (storia `0003`).
5. **RF-5** — L'infrastruttura dell'app nasce dall'istanza del modulo Terraform `microsaas_app` prodotta da
   `infra/scripts/service-add`: nessuna risorsa scritta a mano, nessuna modifica manuale al blocco generato.
6. **RF-6** — Il listino `services/core/src/main/resources/pricing/magazzino.yaml` esiste ed è registrato in
   `pricing/index.yaml` con `userModel: multi_user`, `category: amber` e le descrizioni nelle cinque lingue; i
   piani e i prezzi li compila la storia `0004` dopo la conferma dello sviluppatore.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Nessuna entità di dominio in questa storia, quindi nessuna
  interrogazione da filtrare; il contesto dell'account arriva comunque da `services/commons` e legge `tenant_id`
  **solo** dal token verificato. La rotta di stato non espone nulla che dipenda dall'account.
- **RT-2 — Interfaccia di programmazione (§2).** Quarkus 3.20.6 su Java 21, Quarkus REST + Hibernate ORM
  **bloccante** (la variante reattiva è vietata), accesso ai dati con il modello *repository*. Rotte pubbliche
  sotto `/api/magazzino/v1/...`; errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso
  commit. Il servizio **non chiama** altre app: l'unica via fra servizi è asincrona a eventi.
- **RT-3 — Persistenza (§8).** Lo schema `app_magazzino` viene creato con il ruolo di base di dati dedicato al
  servizio, con privilegi solo sul proprio schema. La cartella `services/magazzino/src/main/resources/db/migration/`
  esiste ma è **vuota**: la prima migrazione `V1__articolo.sql` è della storia `0002`. Le migrazioni non si
  applicano all'avvio in produzione.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata: la storia si ferma al servizio. Il modulo del backoffice
  è della storia `0003`.
- **RT-5 — Cinque lingue (§4).** Nessun testo visibile all'utente in questa storia. Le uniche stringhe rivolte a
  una persona sono le descrizioni del listino, presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** La catena dei cinque varchi arriva dalle parti comuni e dalla proiezione
  locale delle abilitazioni; il suo cablaggio effettivo e la metrica `articoli_gestiti` (natura `stock`) sono della
  storia `0004`. Questa storia non consuma quota e non fissa prezzi.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento dichiarato: il contratto degli strumenti di
  lettura è della storia `0034` e quello degli strumenti di scrittura della `0035`. Il server conversazionale è di
  piattaforma e non è ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo**: non ci sono tabelle di dominio. Il manifesto
  `docs/compliance/manifests/magazzino.yaml` nasce comunque in questa storia con identificativo, nome e
  descrizione in italiano e inglese e **senza voci**; le voci arrivano con le storie `0009` (fornitori) e `0010`
  (manifesto completo e diritti dell'interessato). Il contratto `MagazzinoDataContract` è dichiarato con
  `appId()` e `manifest()`, mentre `exportData` e `purgeData` restano vuoti e vengono riempiti dalla `0010`.
- **RT-9 — Registrazione eventi (§14).** Il servizio registra all'avvio la propria versione e lo schema in uso; ogni
  riga di registro porta `tenant_id`, `app_id`, `user_id` e l'identificativo di correlazione della richiesta, senza
  dati personali. In sviluppo il formato è testo leggibile, in produzione JSON.

## 4. Criteri di accettazione

**CA-1 — Il servizio si avvia e risponde**
- **Dato** lo stack locale in esecuzione
- **Quando** si interroga `GET /api/magazzino/v1/stato`
- **Allora** la risposta è `200` con la versione dell'applicazione e l'esito positivo del collegamento alla base di
  dati

**CA-2 — Nessuna rotta protetta è raggiungibile senza token**
- **Dato** una richiesta senza token di accesso
- **Quando** si interroga una qualunque rotta sotto `/api/magazzino/v1/` diversa da quella di stato
- **Allora** la risposta è `401` in `application/problem+json`

**CA-3 — La scoperta automatica vede l'app**
- **Dato** il repository con il servizio appena generato
- **Quando** si esegue `./dev.sh services`
- **Allora** l'elenco mostra `magazzino` con porta `8114` e schema `app_magazzino`, e nessuno script di avvio è
  stato modificato a mano

**CA-4 — L'infrastruttura è quella del modulo condiviso**
- **Dato** la cartella `infra/`
- **Quando** si eseguono la formattazione, la validazione e le prove del modulo Terraform
- **Allora** tutte passano e il blocco dell'app è un'istanza di `microsaas_app`, senza risorse aggiuntive scritte a
  mano

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend, infra e compliance; l'intera suite prima del commit);
- [ ] prova di **integrazione** sull'avvio del servizio con database effimero, e prova che la definizione OpenAPI
      generata sia allineata al codice;
- [ ] prova di **isolamento fra account**: non applicabile in questa storia, nessuna risorsa di dominio — la prima
      arriva con la `0002` e la porta con sé;
- [ ] **prova end-to-end**: *rimando* — non c'è ancora superficie utente; il percorso `[J-MAGAZZINO]` è di proprietà
      delle storie `0036` (movimenti) e `0037` (inventario), e il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve lì le voci;
- [ ] **traduzioni**: le descrizioni del listino presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati** creato in italiano e inglese, senza voci, con il contratto dell'app dichiarato;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato con l'identificativo scelto, la porta,
      lo schema, il colore-categoria e il modello utente;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione esposta in questa storia;
- [ ] `./dev.sh services` mostra l'app con la sua porta e il suo schema e `./app-start.sh` la avvia **senza**
      modifiche manuali agli script;
- [ ] `run-tests.sh` aggiornato se il nuovo modulo richiede una voce nell'area backend;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| skill `new-application` (UC 0046) | L'app non si scaffolda a mano: il generatore produce servizio, modulo frontend, istanza di infrastruttura, manifesto e listino |
| conferma dello sviluppatore su identificativo, porta e colore-categoria | Sono le risposte del varco d'identità (descrizione dell'applicazione, §3) e vanno confermate prima di generare |

## 7. Fuori ambito

- Tabelle di dominio e migrazioni: storia `0002`.
- Schermate e modulo del backoffice: storia `0003`.
- Piani, prezzi e applicazione della quota: storia `0004` — e i **prezzi** restano comunque una fermata di
  escalation dello sviluppatore.
- Dati di prova e profilo di dimostrazione: storia `0005`.

## 8. Punti aperti

- **Porta locale `8114`**: è la convenzione del kit (8100 + numero di catalogo), ma la porta definitiva si conferma
  con `./dev.sh services` al momento dello scaffolding, perché l'elenco delle porte già prese cambia nel tempo.
- **Colore-categoria `amber`**: nel repository listino e manifesto delle app reali non concordano fra loro sul
  colore; prima di fissarlo va chiarito quale delle due fonti comanda (descrizione dell'applicazione, §11 punto 8).
  Chiude lo sviluppatore, al momento dello scaffolding.
