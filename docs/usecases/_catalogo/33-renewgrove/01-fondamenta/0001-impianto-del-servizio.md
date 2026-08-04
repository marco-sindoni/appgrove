# 0001 — Impianto del servizio

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 01 — Fondamenta
**Storia**: `0001` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: nessuna: è la prima dell'epica e dell'applicazione
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore di piattaforma
> voglio che il servizio `fidelizzazione` esista, risponda su una rotta propria e si sappia avviare
> così da avere un posto dove mettere il primo pezzo di dominio senza inventarmi convenzioni nuove.

**Contesto.** Oggi l'app non esiste. Un'app di appgrove **non si scaffolda a mano**: nasce dalla skill
`new-application`, che esegue un generatore deterministico e poi co-pilota le due decisioni che un generatore non
può prendere — listino e dati personali. Questa storia è il momento in cui le risposte del varco d'identità (§3
della [descrizione](../application-description.md)) diventano un servizio vero. Qui c'è una cosa da non sbagliare:
l'identificativo `fidelizzazione` finisce nello schema del database, nella rotta pubblica, nel nome della coda
dedicata e nell'istanza del modulo di infrastruttura — cambiarlo dopo non è una rinomina, è una migrazione di dati.
E la scelta di **non** chiamarlo `rinnovi` va difesa adesso, perché è esattamente il fraintendimento che il §0 della
descrizione esiste per spegnere: il rinnovo è di `abbonati` (19 SubGrove), qui si lavora sul rapporto.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il servizio `services/fidelizzazione`, dipendente da `services/commons`, con pacchetto radice
   `app.appgrove.fidelizzazione`, che si compila e si avvia.
2. **RF-2** — Il servizio espone una rotta di verifica dello stato di salute sotto `/api/fidelizzazione/v1/` e
   nient'altro: il dominio arriva con le storie successive.
3. **RF-3** — La definizione delle interfacce (OpenAPI) è generata e versionata nel repository, e il client
   frontend si genera da essa.
4. **RF-4** — L'infrastruttura dell'app nasce da un'istanza del modulo comune `microsaas_app`, prodotta dallo
   script `infra/scripts/service-add`; nessuna risorsa scritta a mano, nessuna modifica manuale al blocco generato.
5. **RF-5** — `run-tests.sh` conosce il modulo nuovo e lo esegue nell'area `backend`, nello stesso commit.

## 3. Requisiti tecnici

- **RT-1 — Struttura del backend (§2).** Quarkus 3.20.6 e Java 21, Quarkus REST con Hibernate ORM **bloccante**
  (la variante reattiva è vietata), accesso ai dati con il modello *repository*, oggetti di trasferimento al
  bordo, errori in `application/problem+json`, paginazione a pagina/dimensione con totale.
- **RT-2 — Infrastruttura (§9).** L'app istanzia il modulo Terraform `microsaas_app` tramite lo script
  `infra/scripts/service-add`; l'area `infra` di `run-tests.sh` resta verde.
- **RT-3 — Avvio locale automatico (§15).** Le proprietà in
  `services/fidelizzazione/src/main/resources/application.properties` dichiarano identificativo app, porta `8133`
  e schema `app_fidelizzazione`: da lì la scoperta automatica ricava rotte del proxy locale, migrazioni e avvii,
  senza toccare gli script. La verifica d'insieme è della storia `0005`.
- **RT-4 — Isolamento fra account (§1).** Il contesto del tenant arriva da `services/commons` e legge `tenant_id`
  **solo** dal token verificato: nessuna rotta accetta un identificativo di account dall'esterno.
- **RT-5 — Nessuna chiamata fra app (§2).** `fidelizzazione` non chiama altre app: l'unica via è asincrona a
  eventi. Il divieto vale in modo particolarmente stretto qui, perché quest'app **vive di dati altrui** (§4.2
  della descrizione) e la tentazione di interrogare SubGrove o `fatture` in sola lettura è la scorciatoia numero
  due dell'elenco dei divieti.
- **RT-6 — Registrazione eventi (§14).** Ogni riga di registro porta `tenant_id`, `app_id`, `user_id` e
  l'identificativo di correlazione della richiesta; nessun dato personale.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento introdotto: la storia non porta funzioni di
  dominio. Il contratto degli strumenti nasce con le storie che introducono le funzioni e vive dentro il servizio;
  il server conversazionale è di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: la storia non introduce campi riferiti a persone.
  Si crea però il manifesto `docs/compliance/manifests/fidelizzazione.yaml` vuoto, con identificativo, nome e
  descrizione in italiano e inglese, e lo scheletro del contratto dati `FidelizzazioneDataContract`.
- **RT-9 — Prove (§11).** Prova di integrazione che avvia il servizio e verifica la rotta di stato; le aree
  `backend` e `infra` di `run-tests.sh` restano verdi.

## 4. Criteri di accettazione

**CA-1 — Il servizio esiste e risponde**
- **Dato** lo stack locale avviato
- **Quando** si interroga la rotta di stato di `fidelizzazione`
- **Allora** risponde in modo positivo sulla porta `8133`

**CA-2 — Identificativo coerente ovunque**
- **Dato** il servizio generato · **Quando** si ispezionano schema, rotta pubblica, coda dedicata e istanza di
  infrastruttura
- **Allora** tutti portano `fidelizzazione`, e nessuno porta `rinnovi`, `retention` o `renewgrove`

**CA-3 — Nessun identificativo di account dall'esterno**
- **Dato** una richiesta con un `tenant_id` nel corpo o nei parametri
- **Quando** il servizio la elabora
- **Allora** quel valore viene ignorato e vale solo quello del token verificato

**CA-4 — La suite conosce il modulo**
- **Dato** il repository · **Quando** si esegue `./run-tests.sh backend`
- **Allora** il modulo `fidelizzazione` compare fra quelli eseguiti e l'esito è verde

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `infra`; l'intera suite prima del commit);
- [ ] prova di **integrazione** sull'avvio del servizio e sulla rotta di stato;
- [ ] prova di **isolamento fra account**: *nessun impatto* — non ci sono ancora risorse di dominio, ma il
      contesto del tenant è cablato e la prova arriva con la storia `0002`;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-FIDELIZZAZIONE]` nasce con la storia `0030`, e il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) porta la voce `da-coprire` con
      motivo («superficie non ancora esistente») e storia proprietaria `0030`;
- [ ] **traduzioni**: nessun testo visibile in questa storia;
- [ ] **manifesto dei dati**: creato vuoto con identificativo, nome e descrizione in italiano e inglese;
- [ ] **registro delle decisioni** compilato: identificativo `fidelizzazione`, perché **non** `rinnovi`, e la
      porta effettivamente assegnata;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione introdotta;
- [ ] `./dev.sh services` mostra l'app con porta e schema; `./app-start.sh` la avvia senza modifiche manuali;
- [ ] `run-tests.sh` aggiornato nello stesso commit.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| skill `new-application` (use case 0046) | l'app non si scaffolda a mano |
| decisioni del varco d'identità (§3 della descrizione) | identificativo, modello utente, porta, metrica e colore vanno dati **prima** di generare |
| decisione di prodotto sul perimetro (§0.3, punto aperto n. 1 della descrizione) | se RenewGrove diventasse l'epica 08 di SubGrove, questa storia non si fa affatto: va chiusa **prima** |

## 7. Fuori ambito

- le tabelle di dominio: storia `0002`;
- il modulo frontend: storia `0003`;
- il varco della quota: storia `0004`;
- il listino vero e proprio: è una decisione dello sviluppatore (§5 della descrizione), qui si consuma soltanto.

## 8. Punti aperti

- **Porta `8133` da confermare.** La convenzione del kit (8100 + numero di catalogo) evita le collisioni fra le
  sessanta proposte, ma la porta definitiva la si verifica con `./dev.sh services` al momento dello scaffolding.
  Chiude: lo sviluppatore, in fase di generazione.
- **Colore-categoria `teal` già proposto da 02 BillGrove e 12 DeskGrove** (punto aperto n. 7 della descrizione).
  Va dichiarato allo scaffolding perché finisce nel listino e nel manifesto del modulo, e i due devono coincidere.
  Chiude: la piattaforma, quando i colori si assegnano davvero.
