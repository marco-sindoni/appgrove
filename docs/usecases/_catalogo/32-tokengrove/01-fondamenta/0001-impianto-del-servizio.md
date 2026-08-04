# 0001 — Impianto del servizio

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 01 — Fondamenta
**Storia**: `0001` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: nessuna: è la prima dell'epica e dell'applicazione
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore della piattaforma
> voglio che il servizio `spesa_modelli` esista, risponda e sia raggiungibile con le sue rotte
> così da poter costruire tutto il resto sopra qualcosa che parte, invece che su un accordo verbale.

**Contesto.** Oggi l'app non esiste. Questa storia è ciò che la skill `new-application` produce quando le si danno
le sei risposte del varco d'identità (§3 del documento capofila): identificativo `spesa_modelli`, modello utente a
più persone, porta locale 8132, metrica `misure_registrate` di natura a consumo, colore verde acqua. Va fatta per
prima perché ogni storia successiva presuppone un servizio che risponde e una definizione delle interfacce da cui
il frontend genera il proprio client.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio `services/spesa_modelli` esiste, si compila e si avvia, e risponde alla verifica di
   stato con l'identificativo dell'app e la versione.
2. **RF-2** — È esposta la rotta di lettura `GET /api/spesa_modelli/v1/riepilogo` che, in assenza di dati,
   restituisce un riepilogo vuoto ben formato (totale zero, periodo richiesto, nessuna fonte collegata) e non un
   errore: l'app appena accesa non è un'app rotta.
3. **RF-3** — La definizione OpenAPI del servizio è generata, versionata nel repository e contiene la rotta di
   RF-2 con i suoi schemi.
4. **RF-4** — L'infrastruttura dell'app nasce dall'istanza del modulo Terraform comune prodotta dallo scaffolding,
   senza risorse scritte a mano.
5. **RF-5** — Una richiesta senza gettone di accesso valido riceve `401`; una richiesta con gettone valido ma
   account non abilitato all'app riceve `402`. La catena completa dei varchi è della storia `0004`; qui esistono i
   primi due gradini.
6. **RF-6** — `run-tests.sh` conosce il nuovo modulo backend e lo esegue.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il contesto del cliente si legge **solo** dal gettone verificato: il
  filtro `tenant_id` è già in piedi nel servizio anche se in questa storia non c'è ancora nessuna tabella di
  dominio da filtrare. Un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** Servizio Quarkus 3.20.6 su Java 21, pacchetto radice
  `app.appgrove.spesa_modelli`, dipendenza da `services/commons`, accesso ai dati con il modello *repository*,
  Hibernate bloccante. Rotte pubbliche sotto `/api/spesa_modelli/v1/*`; errori in `application/problem+json`;
  definizione OpenAPI aggiornata nello stesso commit. Gli oggetti di trasferimento stanno al bordo: nessuna entità
  esposta.
- **RT-3 — Persistenza (§8).** In questa storia si crea lo schema `app_spesa_modelli` e la prima migrazione
  Flyway `V1__schema_iniziale.sql`, anche se contiene solo lo schema: le tabelle sono della storia `0002`.
- **RT-4 — Infrastruttura (§9).** L'istanza del modulo `microsaas_app` è prodotta dallo script di aggiunta del
  servizio; nessuna risorsa parallela scritta a mano, nessuna modifica manuale al blocco generato.
- **RT-5 — Avvio locale (§15).** Le proprietà in `services/spesa_modelli/src/main/resources/application.properties`
  dichiarano identificativo dell'app, porta `8132` e schema: da lì la scoperta automatica dei servizi ricava tutto
  il resto. Nessuna riga incollata a mano negli script di avvio.
- **RT-6 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia; il contratto degli strumenti
  nasce nell'epica 07 (storie `0032` e `0033`). Dipendenza dichiarata verso il livello conversazionale di
  piattaforma (UC 0061-0063), non ancora implementato.
- **RT-7 — Dati personali (§10).** Nessun dato personale nuovo: la storia non introduce campi che riguardino una
  persona. Il manifesto `docs/compliance/manifests/spesa_modelli.yaml` viene però **creato**, vuoto di voci ma con
  intestazione in italiano e inglese, perché le storie successive lo accrescano.
- **RT-8 — Registrazione eventi (§14).** Ogni riga di registro porta `tenant_id`, `app_id`, `user_id` e
  l'identificativo di correlazione della richiesta. Nessun dato personale nei registri.

## 4. Criteri di accettazione

**CA-1 — Il servizio parte e risponde**
- **Dato** lo stack locale avviato con `./app-start.sh`
- **Quando** si interroga la verifica di stato del servizio sulla porta 8132
- **Allora** risponde con esito positivo, identificativo `spesa_modelli` e la versione dell'artefatto

**CA-2 — L'app appena accesa non sembra rotta**
- **Dato** un account abilitato all'app e nessuna fonte collegata
- **Quando** chiede `GET /api/spesa_modelli/v1/riepilogo` per il mese corrente
- **Allora** riceve `200` con totale `0`, periodo richiesto ed elenco delle fonti vuoto

**CA-3 — Accesso negato senza gettone**
- **Dato** una richiesta priva di gettone di accesso
- **Quando** chiama una qualunque rotta dell'app
- **Allora** riceve `401` in formato `problem+json`, senza rivelare se l'account esista

**CA-4 — Nessuna infrastruttura scritta a mano**
- **Dato** il ramo della change
- **Quando** si eseguono la formattazione, la validazione e le prove del modulo di infrastruttura
- **Allora** sono verdi e il blocco dell'app risulta generato dallo script, non modificato a mano

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e infra; l'intera suite prima del commit);
- [ ] prove di **unità** sulla logica introdotta e di **integrazione** sulla rotta, con database effimero e
      migrazioni vere;
- [ ] prova di **isolamento fra account** predisposta sull'infrastruttura di prova, anche se non ci sono ancora
      entità da isolare;
- [ ] **prova end-to-end**: nessun impatto in questa storia (non c'è ancora superficie utente); il percorso
      `[J-SPESA-MODELLI]` nasce nella storia `0034`, che è la proprietaria della voce di registro;
- [ ] **traduzioni**: nessun testo visibile introdotto; il modulo frontend nasce nella storia `0003`;
- [ ] **manifesto dei dati** creato, in italiano e inglese, senza voci;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato con le sei risposte del varco
      d'identità e il perché;
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia, dichiarato esplicitamente;
- [ ] `./dev.sh services` mostra l'app con porta `8132` e schema `app_spesa_modelli`; `./app-start.sh` la avvia
      senza modifiche manuali agli script;
- [ ] `run-tests.sh` aggiornato con il nuovo modulo backend.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Varco d'identità approvato (§3 del documento capofila) | Identificativo, porta, metrica e colore finiscono nello schema, nelle rotte e nelle code: cambiarli dopo è una migrazione di dati |
| Proposta di listino approvata (§5) | Il generatore scrive il file del listino dell'app: senza i piani non parte |
| Proposta di classificazione dei dati personali approvata (§6) | Il generatore crea il manifesto dei dati |

## 7. Fuori ambito

- le tabelle di dominio: sono della storia `0002`;
- la catena completa dei varchi con quota e ruoli: è della storia `0004`;
- qualunque schermata: è della storia `0003`.

## 8. Punti aperti

Nessuno che appartenga a questa storia. I punti P1 (prezzi) e P3 (ruolo sul trattamento dei dati) del documento
capofila vanno però **chiusi prima** di eseguirla, perché il generatore scrive listino e manifesto: sono fermate di
escalation dello sviluppatore.
