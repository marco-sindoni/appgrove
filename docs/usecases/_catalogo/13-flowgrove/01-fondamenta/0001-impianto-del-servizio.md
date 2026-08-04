# 0001 — Impianto del servizio

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 01 — Fondamenta
**Storia**: `0001` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: nessuna: è la prima dell'applicazione
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore di piattaforma
> voglio che il servizio `progetti` esista, si avvii e risponda su una rotta di salute
> così da avere un posto dove mettere tutto il resto, generato dal modello e non scritto a mano.

**Contesto.** Nel repository non esiste ancora nulla di FlowGrove. Un'app non si scaffolda a mano
([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §16): nasce dalla skill `new-application`, che esegue un
generatore deterministico e poi chiede le due decisioni che un generatore non può prendere — listino e dati
personali. Questa storia è il momento in cui le risposte già preparate nel varco d'identità
([application-description.md](../application-description.md) §3) vengono usate davvero. Se l'esito del generatore
è sbagliato si corregge il modello e si rigenera: non si toppa l'uscita, perché la toppa nessuno se la ricorda e
l'app successiva eredita il difetto.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il servizio Maven `services/progetti/` con pacchetto radice `app.appgrove.progetti`, prodotto
   dal generatore e non scritto a mano.
2. **RF-2** — Il servizio si avvia in locale sulla porta `8113` e risponde alla rotta di salute
   `GET /api/progetti/v1/health` con lo stato del servizio e della connessione al database.
3. **RF-3** — `./dev.sh services` mostra il servizio `progetti` con il proprio identificativo, porta e schema,
   ricavati dalla sola scoperta automatica.
4. **RF-4** — L'istanza del modulo di infrastruttura `microsaas_app` è creata dallo script `infra/scripts/service-add`;
   nessuna risorsa scritta a mano.
5. **RF-5** — `./run-tests.sh` conosce il modulo nuovo e la sua suite è verde (anche se contiene per ora solo la
   prova sulla rotta di salute).
6. **RF-6** — La definizione delle interfacce (OpenAPI) è generata e versionata nel repository.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Nessuna entità ancora, ma il filtro per account è già attivo nel
  contesto: il servizio dipende da `services/commons` e prende `tenant_id` **solo** dal token verificato. La rotta
  di salute è l'unica non autenticata.
- **RT-2 — Interfaccia di programmazione (§2).** Quarkus 3.20.6, Java 21, Quarkus REST + Hibernate ORM
  **bloccante**; rotte sotto `/api/progetti/v1/`; errori in `application/problem+json`; nessuna chiamata sincrona
  verso altre app.
- **RT-3 — Persistenza (§8).** La proprietà dello schema (`app_progetti`) è dichiarata in
  `services/progetti/src/main/resources/application.properties`; le migrazioni Flyway esistono come cartella
  vuota, la prima tabella è della storia 0002.
- **RT-4 — Infrastruttura (§9).** L'infrastruttura nasce dall'istanza del modulo `microsaas_app` prodotta dallo
  scaffolding; il blocco `module` generato non si modifica a mano.
- **RT-5 — Avvio locale (§15).** La mappa servizio → identificativo → porta → schema si ricava **solo** da
  `application.properties`: nessuna riga incollata negli script di avvio, nel proxy locale o nelle migrazioni.
- **RT-6 — Registrazione eventi (§14).** Il formato di registro strutturato è quello comune: ogni riga porta
  `tenant_id`, `app_id`, `user_id` e identificativo di correlazione; JSON in produzione, testo leggibile in
  sviluppo.
- **RT-7 — Dati personali (§10).** Nessun dato personale in questa storia. Il manifesto
  `docs/compliance/manifests/progetti.yaml` nasce vuoto ma **esistente**, con nome e descrizione in italiano e
  inglese, pronto per la storia 0002.
- **RT-8 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia: il contratto nasce con le
  funzioni (epica 06).

## 4. Criteri di accettazione

**CA-1 — Il servizio si avvia**
- **Dato** un ambiente locale appena preparato
- **Quando** si esegue `./app-start.sh`
- **Allora** il servizio `progetti` risulta in ascolto sulla porta `8113` e `GET /api/progetti/v1/health`
  restituisce `200` con lo stato del database

**CA-2 — Scoperta automatica**
- **Dato** il file `application.properties` del servizio
- **Quando** si esegue `./dev.sh services`
- **Allora** compare la riga `progetti` con porta `8113` e schema `app_progetti`, senza che nessuno script sia
  stato modificato a mano

**CA-3 — Rotta protetta senza token**
- **Dato** una qualunque rotta diversa da quella di salute
- **Quando** la si chiama senza token di accesso
- **Allora** la risposta è `401` in formato `application/problem+json`

**CA-4 — Suite verde**
- **Dato** il repository con il modulo nuovo
- **Quando** si esegue `./run-tests.sh`
- **Allora** l'esito è verde e l'area `backend` comprende il modulo `progetti`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (aree `backend`, `infra`, `tooling`; l'intera suite prima del commit);
- [ ] prova di **integrazione** sulla rotta di salute con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account**: non applicabile (nessuna risorsa) — dichiarato esplicitamente;
- [ ] **prova end-to-end**: rimando — non c'è ancora superficie utente; la copre la storia 0031 (`[J-PROGETTI]`);
- [ ] **traduzioni**: non applicabile (nessun testo visibile in questa storia);
- [ ] **manifesto dei dati** creato, vuoto di voci ma con intestazione in italiano e inglese;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con annotate identificativo, porta e
      colore-categoria effettivamente scelti;
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia, dichiarato;
- [ ] `./dev.sh services` e `./app-start.sh` funzionano senza passi manuali;
- [ ] `run-tests.sh` aggiornato con il modulo nuovo nello stesso commit.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Skill `new-application` (UC 0046) | È il modo previsto per creare un'app; questa storia la esegue, non la sostituisce |
| Varco d'identità ([application-description.md](../application-description.md) §3) | Identificativo, porta, modello utente, metrica e colore devono essere decisi **prima** di generare |
| Decisioni su listino e dati personali (§5, §6) | La skill le chiede allo sviluppatore: sono fermate di escalation |

## 7. Fuori ambito

- le tabelle di dominio: sono della storia 0002;
- il modulo frontend: è della storia 0003;
- il file di listino con i prezzi veri: la storia 0004 lo usa, ma i numeri li conferma lo sviluppatore.

## 8. Punti aperti

- L'identificativo `progetti` va verificato libero al momento dello scaffolding con `./dev.sh services`; se fosse
  occupato, cambiarlo **prima** di generare, perché dopo non è una rinomina ma una migrazione di dati.
