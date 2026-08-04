# 0002 — Modello dati multi-account

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Epica**: 01 — Fondamenta
**Storia**: `0002` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore di piattaforma
> voglio lo schema del database dell'app con la sua prima tabella e le sue regole di isolamento
> così da poter scrivere il dominio sapendo che nessun account vedrà mai i preventivi di un altro.

**Contesto.** Il servizio esiste ma non ha memoria. Questa storia posa le fondamenta della persistenza: lo schema
dedicato, la prima migrazione, la tabella dei preventivi nella sua forma minima (numero, destinatario ancora come
testo libero, stato, totali) e — soprattutto — la prova automatica che il filtro per account non si può aggirare.
Si fa adesso perché ogni tabella successiva erediterà queste convenzioni: sbagliarle qui costa una migrazione a
ogni storia dell'app.

## 2. Requisiti funzionali

1. **RF-1** — Esiste la migrazione `V1__preventivi_base.sql` sullo schema `app_preventivi` che crea la tabella
   `preventivo` con numero progressivo per account, stato, valuta, totali e date di validità.
2. **RF-2** — Il numero del preventivo è **progressivo per account e per anno** e non si riusa nemmeno dopo una
   cancellazione logica.
3. **RF-3** — Ogni riga porta `tenant_id`, chiave primaria UUID versione 7 generata dall'applicazione, colonne di
   controllo (`created_at`, `updated_at`, `created_by`, `updated_by`) e cancellazione logica (`deleted_at`).
4. **RF-4** — Esiste una risorsa di sola lettura `GET /api/preventivi/v1/preventivi` che restituisce l'elenco
   paginato del solo account chiamante (a questo punto vuoto).
5. **RF-5** — Gli stati ammessi sono esattamente quelli della macchina a stati della descrizione
   dell'applicazione: `bozza`, `inviato`, `visto`, `accettato`, `rifiutato`, `in_revisione`, `scaduto`.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura della tabella `preventivo` filtra per
  `tenant_id` preso dal token verificato; un `tenant_id` che arrivasse dal corpo o dai parametri viene ignorato.
  Il controllo strutturale che fa fallire la compilazione quando qualcuno aggira il filtro deve restare verde.
- **RT-2 — Interfaccia di programmazione (§2).** `GET /api/preventivi/v1/preventivi` con paginazione a
  pagina/dimensione e totale; oggetti di trasferimento al bordo, mai l'entità; errori in `problem+json`;
  definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione Flyway in SQL sotto `services/preventivi/src/main/resources/db/migration`,
  non applicata all'avvio in produzione; nessuna chiave esterna verso altri schemi (`tenant_id` è un riferimento
  logico); indice su `(tenant_id, stato)` e vincolo di unicità su `(tenant_id, anno, numero)`.
- **RT-4 — Cinque lingue (§4).** Non applicabile: nessun testo visibile in questa storia.
- **RT-5 — Registrazione eventi (§14).** Gli eventi `preventivo creato` e `elenco richiesto` sono registrati con
  `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza dati personali.
- **RT-6 — Dati personali (§10).** Nessun dato personale nuovo: il destinatario in questa storia è un testo libero
  che l'interfaccia non espone ancora; l'anagrafica vera arriva con `0006` e con essa le voci del manifesto.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento: comincia con l'epica 06.

## 4. Criteri di accettazione

**CA-1 — La migrazione gira su un database vuoto**
- **Dato** un PostgreSQL 17 effimero · **Quando** parte il servizio in prova · **Allora** lo schema
  `app_preventivi` esiste con la tabella `preventivo` e tutte le colonne di controllo

**CA-2 — Numerazione progressiva per account**
- **Dato** due account `A` e `B`, entrambi senza preventivi
- **Quando** ciascuno crea il suo primo preventivo dell'anno
- **Allora** entrambi ricevono il numero `1`, e i due documenti restano distinti

**CA-3 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri preventivi
- **Quando** un utente di `A` chiede l'elenco dei preventivi
- **Allora** vede solo i propri, anche se forza l'identificativo dell'altro account nel corpo o nei parametri

**CA-4 — Cancellazione logica**
- **Dato** un preventivo con `deleted_at` valorizzato · **Quando** si chiede l'elenco · **Allora** non compare, e
  il suo numero non viene riassegnato al preventivo successivo

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla numerazione e di **integrazione** sulla risorsa, con database effimero e migrazioni
      Flyway vere;
- [ ] prova di **isolamento fra account** sulla risorsa nuova, con tentativo di forzare `tenant_id` dall'esterno;
- [ ] **prova end-to-end**: nessun impatto (nessuna superficie utente ancora);
- [ ] **traduzioni**: non applicabile;
- [ ] **manifesto dei dati**: nessuna voce nuova, motivata sopra;
- [ ] **registro delle decisioni** compilato (forma della numerazione, elenco degli stati, indici);
- [ ] contratto degli **strumenti conversazionali**: nessuno;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare;
- [ ] la macchina a stati della descrizione dell'applicazione è aggiornata se è cambiata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0001` | senza il servizio non c'è niente da migrare |

## 7. Fuori ambito

- righe, totali calcolati e destinatario strutturato: epiche 02 e 03;
- la scrittura da interfaccia: storia `0012`.

## 8. Punti aperti

Nessuno.
