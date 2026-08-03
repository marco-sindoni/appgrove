# 0001 — Impianto del servizio

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Epica**: 01 — Fondamenta
**Storia**: `0001` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: nessuna: è la prima dell'epica e dell'applicazione
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore di piattaforma
> voglio che il servizio `einvoicing` esista, risponda e sia raggiungibile alle sue rotte
> così da poter costruire tutto il resto dell'app sopra una base identica a quella delle altre app della suite.

**Contesto.** Oggi l'app non esiste: non c'è servizio, non c'è rotta, non c'è infrastruttura. Questa storia non
consegna nulla all'utente finale ed è l'unica dell'applicazione che può permetterselo, perché è il presupposto di
ogni altra. Va fatta adesso e con lo scaffolding, non a mano: un'app scaffoldata a mano nasce già diversa dalle
altre e la differenza non si recupera più ([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §16).

## 2. Requisiti funzionali

1. **RF-1** — Esiste il servizio `services/einvoicing/`, generato dalla skill `new-application`, che dipende da
   `services/commons` e si avvia sulla porta locale confermata (proposta `8101`).
2. **RF-2** — Il servizio espone una rotta di stato `GET /api/einvoicing/v1/health` che risponde `200` con la
   versione e il nome dell'app, senza richiedere autenticazione.
3. **RF-3** — Il servizio espone la propria definizione OpenAPI, generata e versionata nel repository.
4. **RF-4** — L'infrastruttura dell'app nasce dall'istanza del modulo `microsaas_app` prodotta dallo scaffolding,
   senza risorse scritte a mano.
5. **RF-5** — Il servizio è registrato nello script unico dei test `run-tests.sh` come modulo dell'area backend.
6. **RF-6** — Ogni rotta autenticata dell'app risponde `401` senza token valido, prima ancora che esistano risorse.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Non ci sono ancora entità, ma il contesto del tenant di
  `services/commons` è cablato: ogni rotta autenticata legge `tenant_id` e `sub` **solo** dal token verificato. La
  rotta di stato è l'unica esente e non tocca dati.
- **RT-2 — Interfaccia di programmazione (§2).** Quarkus 3.20.6, Java 21, Quarkus REST con Hibernate ORM
  **bloccante**, pacchetto radice `app.appgrove.einvoicing`, rotte sotto `/api/einvoicing/v1/`, errori in
  `application/problem+json`, definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** La storia crea il ruolo del database e lo schema `app_einvoicing` vuoto; le tabelle
  arrivano con la storia `0002`. Migrazioni Flyway, mai applicate all'avvio in produzione.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata in questa storia: il modulo arriva con la storia `0003`.
- **RT-5 — Cinque lingue (§4).** Nessun testo visibile introdotto.
- **RT-6 — Varchi e quota (§6, §7).** Solo il primo varco: token valido, altrimenti `401`. Abilitazione e quota
  arrivano con la storia `0004`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia: il contratto nasce con
  l'epica 06. Dipendenza dichiarata verso UC 0061-0063, non implementati.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo. Il manifesto
  `docs/compliance/manifests/einvoicing.yaml` viene creato **vuoto ma valido** (identificativo, nome e descrizione
  in italiano e inglese, nessuna voce), così che il controllo di parità delle lingue trovi il file dalla prima
  storia in poi.
- **RT-9 — Registrazione eventi (§14).** Il registro strutturato è attivo e ogni riga porta `tenant_id`, `app_id`,
  `user_id` e identificativo di correlazione, senza dati personali.
- **RT-10 — Avvio locale (§15).** La scoperta automatica ricava servizio, identificativo, porta e schema dal solo
  `services/einvoicing/src/main/resources/application.properties`. Nessuna riga incollata a mano negli script.

## 4. Criteri di accettazione

**CA-1 — Il servizio si avvia e risponde**
- **Dato** lo stack locale avviato con `./app-start.sh`
- **Quando** si chiama `GET /api/einvoicing/v1/health`
- **Allora** la risposta è `200` e contiene `app_id = einvoicing`

**CA-2 — L'app è scoperta senza cablaggi**
- **Dato** il repository dopo l'unione del ramo
- **Quando** si esegue `./dev.sh services`
- **Allora** l'app compare con identificativo `einvoicing`, porta `8101` e schema `app_einvoicing`, e nessuno
  script di avvio è stato modificato a mano

**CA-3 — Senza token si nega**
- **Dato** una rotta autenticata di prova
- **Quando** la si chiama senza intestazione di autorizzazione
- **Allora** la risposta è `401` in formato `application/problem+json`

**CA-4 — L'infrastruttura è quella condivisa**
- **Dato** la cartella `infra/`
- **Quando** si esegue la validazione dell'infrastruttura
- **Allora** esiste una sola istanza del modulo `microsaas_app` per `einvoicing` e nessuna risorsa scritta a mano
  per questa app

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e infra; l'intera suite prima del commit);
- [ ] prove di **unità** sulla logica introdotta e di **integrazione** sulla rotta di stato, con database effimero
      e migrazioni vere;
- [ ] prova di **isolamento fra account**: non applicabile, nessuna risorsa con dati — dichiarato esplicitamente;
- [ ] **prova end-to-end**: *rimando* — non c'è ancora superficie utente; il percorso `[J-EINVOICING]` nasce con
      la storia `0030`, che è la proprietaria della copertura;
- [ ] **traduzioni**: nessun testo visibile introdotto;
- [ ] **manifesto dei dati** creato vuoto e valido in italiano e inglese;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, in particolare con la porta
      effettivamente assegnata e il motivo;
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia;
- [ ] `./dev.sh services` e l'avvio locale funzionano senza passi manuali;
- [ ] `run-tests.sh` aggiornato con il modulo nuovo, nello stesso commit.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Varco d'identità della descrizione dell'applicazione (§3) | La skill `new-application` chiede identificativo, modello utente, porta, metrica, natura e colore **prima** di generare qualunque cosa |
| Decisione dello sviluppatore su prezzi e dati personali | La skill si ferma su entrambe: sono fermate di escalation |

## 7. Fuori ambito

- Le tabelle di dominio: le crea la storia `0002`.
- Il modulo frontend: lo crea la storia `0003`.
- Quota e abilitazione: li crea la storia `0004`.
- Qualunque logica di validazione, trasmissione o conservazione.

## 8. Punti aperti

- La **porta definitiva** non la decide questa storia: `8101` è la convenzione del kit, va confermata con
  `./dev.sh services` al momento dello scaffolding.
- Il listino `pricing/einvoicing.yaml` che lo scaffolding pretende contiene prezzi: è una **fermata di escalation
  dello sviluppatore** (descrizione dell'applicazione §5) e va risolta prima di lanciare il generatore, non dopo.
