# 0001 — Impianto del servizio

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 01 — Fondamenta
**Storia**: `0001` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: nessuna: è la prima dell'epica e dell'applicazione
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore di piattaforma
> voglio che il servizio `abbonati` esista, risponda su una rotta propria e si sappia avviare
> così da avere un posto dove mettere il primo pezzo di dominio senza inventarmi convenzioni nuove.

**Contesto.** Oggi l'app non esiste. Un'app di appgrove **non si scaffolda a mano**: nasce dalla skill
`new-application`, che esegue un generatore deterministico e poi co-pilota le due decisioni che un generatore non
può prendere — listino e dati personali. Questa storia è il momento in cui le risposte già preparate nel varco
d'identità (§3 della descrizione) diventano un servizio vero. C'è una cosa da non sbagliare proprio qui:
l'identificativo `abbonati` finisce nello schema del database, nella rotta pubblica, nei nomi delle code e
nell'istanza del modulo di infrastruttura — cambiarlo dopo non è una rinomina, è una migrazione di dati. E la
scelta di **non** chiamarlo `abbonamenti`, per non confonderlo con gli abbonamenti di piattaforma, va difesa
adesso, perché dopo è tardi (§10.1 della descrizione).

## 2. Requisiti funzionali

1. **RF-1** — Esiste il servizio `services/abbonati`, dipendente da `services/commons`, con pacchetto radice
   `app.appgrove.abbonati`, che si compila e si avvia.
2. **RF-2** — Il servizio espone una rotta di verifica dello stato di salute sotto `/api/abbonati/v1/` e nient'altro:
   il dominio arriva con le storie successive.
3. **RF-3** — La definizione delle interfacce (OpenAPI) è generata e versionata nel repository, e il client
   frontend si genera da essa.
4. **RF-4** — L'infrastruttura dell'app nasce da un'istanza del modulo comune, prodotta dallo script previsto;
   nessuna risorsa scritta a mano.
5. **RF-5** — `run-tests.sh` conosce il modulo nuovo e lo esegue nell'area `backend`.

## 3. Requisiti tecnici

- **RT-1 — Struttura del backend (§2).** Quarkus 3.20.6 e Java 21, Quarkus REST con Hibernate ORM **bloccante**
  (la variante reattiva è vietata), accesso ai dati con il modello *repository*, oggetti di trasferimento al
  bordo, errori in `application/problem+json`, paginazione a pagina/dimensione con totale.
- **RT-2 — Infrastruttura (§9).** L'app istanzia il modulo Terraform `microsaas_app` tramite lo script
  `infra/scripts/service-add`; il blocco generato non si modifica a mano.
- **RT-3 — Avvio locale automatico (§15).** Le proprietà in
  `services/abbonati/src/main/resources/application.properties` dichiarano identificativo app, porta `8119` e
  schema `app_abbonati`: da lì la scoperta automatica ricava tutto il resto, senza toccare gli script.
- **RT-4 — Isolamento fra account (§1).** Il contesto del tenant arriva da `services/commons` e legge
  `tenant_id` **solo** dal token verificato: nessuna rotta accetta un identificativo di account dall'esterno.
- **RT-5 — Nessuna chiamata fra app (§2).** `abbonati` non chiama altre app: l'unica via è asincrona a eventi.
  Vale in particolare verso il servizio centrale degli abbonamenti di piattaforma, che **non** va interrogato.
- **RT-6 — Registrazione eventi (§14).** Ogni riga di registro porta `tenant_id`, `app_id`, `user_id` e
  l'identificativo di correlazione della richiesta.
- **RT-7 — Dati personali (§10).** Nessun dato personale: la storia non introduce campi riferiti a persone.
- **RT-8 — Prove (§11).** Prova di integrazione che avvia il servizio e verifica la rotta di stato; l'area
  `backend` di `run-tests.sh` resta verde.

## 4. Criteri di accettazione

**CA-1 — Il servizio esiste e risponde**
- **Dato** lo stack locale avviato
- **Quando** si interroga la rotta di stato di `abbonati`
- **Allora** risponde in modo positivo sulla porta `8119`

**CA-2 — Identificativo coerente ovunque**
- **Dato** il servizio generato · **Quando** si ispezionano schema, rotta, coda e istanza di infrastruttura
- **Allora** tutti portano `abbonati`, e nessuno porta `abbonamenti` né `subgrove`

**CA-3 — Nessun identificativo di account dall'esterno**
- **Dato** una richiesta con un `tenant_id` nel corpo o nei parametri
- **Quando** il servizio la elabora
- **Allora** quel valore viene ignorato e vale solo quello del token

**CA-4 — La suite conosce il modulo**
- **Dato** il repository · **Quando** si esegue `./run-tests.sh backend` · **Allora** il modulo `abbonati` compare
  fra quelli eseguiti e l'esito è verde

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `infra`; l'intera suite prima del commit);
- [ ] prova di **integrazione** sull'avvio del servizio;
- [ ] prova di **isolamento fra account**: *nessun impatto* — non ci sono ancora risorse di dominio, ma il
      contesto del tenant è cablato e coperto dalla prova della storia `0002`;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-ABBONATI]` nasce con la storia `0033`, e il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) porta la voce `da-coprire` con
      motivo e storia proprietaria;
- [ ] **traduzioni**: nessun testo visibile in questa storia;
- [ ] **manifesto dei dati**: creato vuoto con identificativo, nome e descrizione in italiano e inglese;
- [ ] **registro delle decisioni** compilato: identificativo `abbonati` e perché **non** `abbonamenti`;
- [ ] `./dev.sh services` mostra l'app con porta e schema; `./app-start.sh` la avvia senza modifiche manuali;
- [ ] `run-tests.sh` aggiornato nello stesso commit.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| skill `new-application` (use case 0046) | l'app non si scaffolda a mano |
| decisioni del varco d'identità (§3 della descrizione) | identificativo, modello utente, porta, metrica e colore vanno dati **prima** di generare |

## 7. Fuori ambito

- le tabelle di dominio: storia `0002`;
- il modulo frontend: storia `0003`;
- il listino vero e proprio: è una decisione dello sviluppatore (§5 della descrizione), qui si consuma soltanto.

## 8. Punti aperti

**Porta `8119` da confermare.** La convenzione del kit (8100 + numero di catalogo) evita le collisioni fra le
sessanta proposte, ma la porta definitiva la si verifica con `./dev.sh services` al momento dello scaffolding.
Chiude: lo sviluppatore, in fase di generazione.
