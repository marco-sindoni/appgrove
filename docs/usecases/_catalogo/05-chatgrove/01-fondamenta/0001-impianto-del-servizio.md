# 0001 — Impianto del servizio

**Applicazione**: 05 — ChatGrove (`chat_commerce`) · **Epica**: 01 — Fondamenta
**Storia**: `0001` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: nessuna: è la prima dell'epica
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore di piattaforma
> voglio che il servizio `chat_commerce` esista, si avvii e risponda su una rotta propria
> così da avere un contenitore vivo in cui mettere le funzioni di vendita in chat, invece di decidere ogni
> volta dove metterle.

**Contesto.** Oggi ChatGrove non esiste: non c'è un servizio, non c'è uno schema, non c'è una rotta. È la
prima cosa da fare perché ogni storia successiva presuppone un posto dove scrivere il codice. Il lavoro non è
inventivo: l'app **non si scaffolda a mano**, nasce dalla skill `new-application`
([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §16), che genera il servizio, il modulo frontend,
l'istanza del modulo di infrastruttura, il manifesto dei dati e il file di listino. Il valore di questa storia
sta nelle **risposte date prima di lanciarla** (identificativo, modello utente, porta, metrica, colore) e nella
verifica che l'esito parta davvero.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il servizio `services/chat_commerce/`, che dipende da `services/commons` e si avvia senza
   errori con la configurazione locale.
2. **RF-2** — Il servizio espone una rotta di stato `GET /api/chat_commerce/v1/health` che risponde `200` con
   la versione dell'applicazione.
3. **RF-3** — Le risposte di errore escono in formato `application/problem+json`, comprese quelle prodotte dai
   varchi di piattaforma.
4. **RF-4** — La definizione delle interfacce (OpenAPI) è generata e versionata nel repository.
5. **RF-5** — L'infrastruttura dell'app è un'istanza del modulo comune `microsaas_app`, prodotta dallo script
   di aggiunta del servizio: nessuna risorsa scritta a mano.
6. **RF-6** — Il file di listino `chat_commerce.yaml` esiste, è registrato nell'indice dei listini e riporta
   `category: teal`, `userModel: multi_user` e la metrica `messaggi_template` di natura `flow`, **con i valori
   di prezzo lasciati alla decisione dello sviluppatore**.

## 3. Requisiti tecnici

- **RT-1 — Struttura del backend (§2).** Quarkus 3.20.6, Java 21, Quarkus REST + Hibernate ORM bloccante,
  pacchetto radice `app.appgrove.chat_commerce`, accesso ai dati con il modello *repository*. Rotte pubbliche
  sotto `/api/chat_commerce/v1/`. L'app non chiama altre app: l'unica via fra servizi è asincrona a eventi.
- **RT-2 — Infrastruttura (§9).** L'infrastruttura nasce dall'istanza del modulo `microsaas_app` prodotta
  dallo scaffolding; nessuna risorsa parallela e nessuna modifica a mano del blocco generato.
- **RT-3 — Avvio locale (§15).** Le proprietà in `services/chat_commerce/src/main/resources/application.properties`
  dichiarano identificativo, porta `8105` e schema `app_chat_commerce`: la scoperta automatica dei servizi
  deriva da lì tutto il resto. Nessuna riga incollata negli script di avvio.
- **RT-4 — Listino come codice (§7).** Il listino vive in
  `services/core/src/main/resources/pricing/chat_commerce.yaml`, con una **sola** metrica dichiarata e la sua
  natura; i limiti non stanno dal fornitore di pagamento.
- **RT-5 — Registrazione eventi (§14).** L'avvio del servizio e le richieste registrano `tenant_id`, `app_id`,
  `user_id` e identificativo di correlazione; nessun dato personale nei registri.
- **RT-6 — Dati personali (§10).** Nessun dato personale nuovo: questa storia non introduce campi che
  riguardano una persona. Il manifesto `docs/compliance/manifests/chat_commerce.yaml` nasce qui **vuoto di
  voci** ma esistente, in italiano e inglese, e si popola dalla storia `0002`.

## 4. Criteri di accettazione

**CA-1 — Il servizio si avvia**
- **Dato** il repository con il ramo della storia
- **Quando** si esegue `./app-start.sh`
- **Allora** il servizio `chat_commerce` risulta in ascolto sulla porta `8105` e `./dev.sh services` lo elenca
  con il suo identificativo e il suo schema

**CA-2 — La rotta di stato risponde**
- **Dato** il servizio avviato · **Quando** si chiama `GET /api/chat_commerce/v1/health` · **Allora** la
  risposta è `200` con la versione dell'applicazione

**CA-3 — Gli errori hanno la forma di piattaforma**
- **Dato** il servizio avviato · **Quando** si chiama una rotta inesistente sotto `/api/chat_commerce/v1/`
- **Allora** la risposta è `404` con corpo `application/problem+json`

**CA-4 — Nessuna infrastruttura su misura**
- **Dato** il ramo della storia
- **Quando** si esegue la validazione dell'infrastruttura (`./run-tests.sh infra`)
- **Allora** è verde e il blocco dell'app è un'istanza del modulo `microsaas_app`, non risorse scritte a mano

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend, infra e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla logica introdotta e di **integrazione** sulla rotta di stato;
- [ ] prova di **isolamento fra account**: non applicabile qui (nessuna risorsa con dati), da introdurre in `0002`;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-CHAT-COMMERCE]` nasce nella storia `0029`, perché prima
      della `0003` non esiste superficie utente da percorrere; voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) con motivo e storia proprietaria;
- [ ] **traduzioni**: non applicabile (nessun testo visibile in questa storia);
- [ ] **manifesto dei dati** creato in italiano e inglese, senza voci, pronto per la storia `0002`;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con annotate le risposte del
      varco d'identità (identificativo, modello utente, porta, metrica, colore);
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione introdotta, nulla da dichiarare;
- [ ] `./dev.sh services` e l'avvio locale funzionano senza passi manuali;
- [ ] documentazione aggiornata: `run-tests.sh` conosce il nuovo modulo.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Skill `new-application` (UC 0046) | È il generatore da cui l'app nasce: non si scaffolda a mano |
| Decisione sui prezzi (§5 della descrizione) | Il file di listino esiste, ma i valori sono una fermata dello sviluppatore |

## 7. Fuori ambito

- il modello dati del dominio: è la storia `0002`;
- il modulo frontend: è la storia `0003`;
- qualunque collegamento al canale di messaggistica: è l'epica 02.

## 8. Punti aperti

- **Prezzi e limiti** del file di listino: fermata di escalation dello sviluppatore (punto 4 del §11 della
  descrizione dell'applicazione).
- **Chi paga il canale** (Via A o Via B, §5.1 della descrizione): non blocca questa storia, ma va deciso prima
  della storia `0006`.
