# 0001 — Impianto del servizio `notespese`

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 01 — Fondamenta
**Storia**: `0001` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: nessuna: è la prima dell'epica e dell'applicazione
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore di piattaforma
> voglio che l'applicazione delle note spese esista come servizio a sé, con la sua rotta e la sua infrastruttura
> così da poter costruire le funzioni di dominio senza dover prima decidere ogni volta dove vivono.

**Contesto.** Oggi non esiste nulla: nessuno schema, nessuna rotta, nessuna istanza di infrastruttura. Questa storia
non produce niente di visibile per il cliente finale, ed è l'unica del catalogo a cui è concesso: è il gradino zero
che tutte le altre presuppongono. Va fatta adesso perché ogni scelta successiva (schema, rotte, coda degli eventi)
prende il nome dall'identificativo dell'app, e cambiarlo dopo è una migrazione di dati, non una rinomina
(descrizione dell'applicazione, §3).

## 2. Requisiti funzionali

1. **RF-1** — Esiste il servizio `services/notespese` generato dalla skill `new-application`, con pacchetto radice
   `app.appgrove.notespese`, che si compila e passa le prove vuote.
2. **RF-2** — Il servizio risponde su `/api/notespese/v1/*` e pubblica una definizione di interfaccia versionata, con
   almeno una rotta di stato di salute e una rotta di elenco vuota.
3. **RF-3** — L'infrastruttura dell'app nasce da un'istanza del modulo Terraform comune, aggiunta dallo script di
   piattaforma: nessuna risorsa scritta a mano.
4. **RF-4** — Il file `application.properties` dichiara identificativo dell'app (`notespese`), porta (`8108` da
   confermare) e schema (`app_notespese`), così che la scoperta automatica dei servizi trovi l'app da sola.
5. **RF-5** — Il servizio dipende da `services/commons` per il contesto dell'account, la mappatura degli errori, le
   entità di base e la paginazione, e non reimplementa nessuna di queste cose.
6. **RF-6** — Lo script unico delle prove riconosce la nuova area ed esegue le prove del servizio.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Nessuna entità di dominio ancora, ma il contesto dell'account è cablato
  fin da qui: il servizio legge `tenant_id` **solo** dal token verificato tramite il filtro di `commons`, e un
  `tenant_id` che arrivasse dal corpo o dai parametri viene ignorato. La prova che lo dimostra nasce con la prima
  risorsa (storia `0002`).
- **RT-2 — Interfaccia di programmazione (§2).** Quarkus 3.20.6 su Java 21, Quarkus REST con Hibernate ORM
  **bloccante**; rotte `/api/notespese/v1/*`; errori in `application/problem+json`; definizione OpenAPI generata e
  versionata nello stesso commit.
- **RT-3 — Persistenza (§8).** Solo la predisposizione: schema `app_notespese`, ruolo del database dedicato al
  servizio con privilegi sul solo schema, cartella delle migrazioni Flyway creata e vuota. Le tabelle sono della
  storia `0002`.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata in questa storia: il guscio del modulo è la storia `0003`.
- **RT-5 — Cinque lingue (§4).** Nessun testo visibile introdotto.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota: la catena dei varchi si aggancia nella storia `0004`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento dichiarato qui; il contratto vive nel servizio e
  si popola dall'epica 06 (dipendenza da UC 0061-0063, non ancora implementati).
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: nessuna tabella di dominio esiste ancora. Il file
  del manifesto `docs/compliance/manifests/notespese.yaml` viene però **creato** con intestazione e descrizione in
  italiano e inglese e nessuna voce, così che le storie successive lo accrescano invece di doverlo inventare.
- **RT-9 — Registrazione eventi (§14).** Il registro strutturato è attivo dall'avvio: ogni riga porta `tenant_id`,
  `app_id`, `user_id` e identificativo di correlazione della richiesta, senza dati personali.
- **RT-10 — Infrastruttura (§9).** L'istanza del modulo `microsaas_app` è prodotta dallo script di piattaforma; il
  blocco generato non si modifica a mano.
- **RT-11 — Avvio locale (§15).** L'app compare in `./dev.sh services` e si avvia con gli script comuni **senza**
  che nessuno tocchi a mano gli script: se serve toccarli, è un difetto della scoperta automatica.

## 4. Criteri di accettazione

**CA-1 — Il servizio si avvia e risponde**
- **Dato** il repository con la nuova app generata
- **Quando** si avvia lo stack locale
- **Allora** `GET /api/notespese/v1/health` risponde `200` e `./dev.sh services` elenca `notespese` con porta `8108`
  e schema `app_notespese`

**CA-2 — Errori nel formato di piattaforma**
- **Dato** il servizio avviato · **Quando** si chiama una rotta inesistente sotto `/api/notespese/v1/`
- **Allora** la risposta è `404` con corpo `application/problem+json`, non una pagina di errore del contenitore

**CA-3 — Nessuna infrastruttura su misura**
- **Dato** la cartella dell'infrastruttura
- **Quando** si esamina ciò che la storia ha aggiunto
- **Allora** c'è **una sola** istanza del modulo comune e nessuna risorsa dichiarata fuori dal modulo; la validazione
  e le prove del modulo passano

**CA-4 — Chiamata senza token**
- **Dato** il servizio avviato · **Quando** si chiama una rotta di dominio senza token di accesso
- **Allora** la risposta è `401`, e nel registro compare la riga con l'identificativo di correlazione e nessun dato
  personale

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e infra; l'intera suite prima del commit);
- [ ] prove di **unità** sulla configurazione e di **integrazione** sull'avvio con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account**: rimandata alla storia `0002`, che introduce la prima risorsa con dati —
      qui non esiste ancora nessuna risorsa da isolare;
- [ ] **prova end-to-end**: *nessun impatto* — la storia non tocca la superficie utente; il percorso `[J-NOTESPESE]`
      nasce con la storia `0031`, e il registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve la voce corrispondente lì;
- [ ] **traduzioni**: nessuna stringa visibile introdotta;
- [ ] **manifesto dei dati** creato, vuoto, con intestazione in italiano e inglese;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, in particolare con la scelta
      dell'identificativo `notespese` e il perché non è `spendgrove`;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione introdotta, nessuno strumento da dichiarare;
- [ ] `./dev.sh services` e l'avvio locale funzionano senza passi manuali;
- [ ] `run-tests.sh` aggiornato con la nuova area di servizio nello stesso commit.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Skill `new-application` (UC 0046) | L'app non si scaffolda a mano: il generatore produce servizio, modulo, istanza di infrastruttura, manifesto e listino |
| Decisione sul listino (§5 della descrizione) | Il generatore chiede piani e quota: senza la conferma dello sviluppatore non si genera |

## 7. Fuori ambito

- Le tabelle di dominio: sono della storia `0002`.
- Il modulo frontend e le sue sezioni: storia `0003`.
- La catena dei varchi e il conteggio della quota: storia `0004`.
- I dati di prova: storia `0005`.

## 8. Punti aperti

- **Porta locale definitiva**: `8108` è la convenzione del kit (8100 + numero di catalogo); la porta vera si conferma
  con `./dev.sh services` al momento dello scaffolding.
- **Listino e quota** sono una fermata di escalation dello sviluppatore: il generatore non parte finché non sono
  confermati (descrizione dell'applicazione, §5).
