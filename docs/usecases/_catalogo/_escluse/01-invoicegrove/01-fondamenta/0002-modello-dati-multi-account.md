# 0002 — Modello dati multi-account

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Epica**: 01 — Fondamenta
**Storia**: `0002` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore di piattaforma
> voglio che lo schema dell'app esista con le sue tabelle di base e il filtro per account già applicato
> così da non dover ricucire l'isolamento fra account dopo, quando ci saranno già dei dati dentro.

**Contesto.** L'isolamento fra account è l'invariante numero uno della piattaforma e non è un accorgimento che si
aggiunge: o è nella forma delle tabelle e nella forma del repository fin dalla prima migrazione, o qualcuno prima o
poi scriverà una interrogazione senza filtro. Questa storia mette a terra lo scheletro di persistenza —
`legal_entity`, `counterparty`, `canonical_document`, `document_line` — con le colonne comuni, e lascia alle
epiche successive il compito di riempirle di significato.

## 2. Requisiti funzionali

1. **RF-1** — Esistono le tabelle `legal_entity`, `counterparty`, `canonical_document`, `document_line` nello
   schema `app_einvoicing`, ciascuna con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e
   cancellazione logica.
2. **RF-2** — Ogni tabella ha un indice che comincia per `tenant_id`, così che nessuna lettura utile possa essere
   efficiente senza il filtro.
3. **RF-3** — Il livello di accesso ai dati (modello *repository*) espone solo metodi che richiedono il
   `tenant_id`; non esiste un metodo «leggi tutto».
4. **RF-4** — Un tentativo di forzare `tenant_id` dal corpo della richiesta o dai parametri viene **ignorato**: il
   valore usato è sempre quello del token verificato.
5. **RF-5** — Esiste il contratto `EinvoicingDataContract` con `appId()`, `exportData(scope)`, `purgeData(scope)`
   e `manifest()`, per ora sulle sole tabelle introdotte qui.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `legal_entity`, `counterparty`,
  `canonical_document` e `document_line` filtra per `tenant_id` preso dal token verificato. Suite di isolamento
  fra account **obbligatoria e mai esclusa**, con almeno due account per ogni tabella introdotta.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta pubblica nuova in questa storia: si tocca solo lo
  strato di persistenza. Le entità non escono mai dal bordo: gli oggetti di trasferimento arrivano con le epiche
  di dominio.
- **RT-3 — Persistenza (§8).** Migrazione `V1__base_schema.sql` sullo schema `app_einvoicing`: quattro tabelle con
  `tenant_id`, chiave primaria UUID versione 7 generata dall'applicazione, `created_at`, `updated_at`,
  `created_by`, `updated_by`, `deleted_at`. **Vietate** le chiavi esterne verso altri schemi: `tenant_id` è un
  riferimento logico. Un ruolo del database per il servizio, con privilegi solo sul proprio schema.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata.
- **RT-5 — Cinque lingue (§4).** Nessun testo visibile.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota: qui non si crea nulla su richiesta dell'utente.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento.
- **RT-8 — Dati personali (§10).** **Sì, e questa è la prima storia che ne introduce.** `legal_entity` e
  `counterparty` contengono denominazione, identificativo fiscale e indirizzo di persone quando la controparte è
  un professionista o una ditta individuale. Voci nuove nel manifesto
  `docs/compliance/manifests/einvoicing.yaml` in italiano **e** inglese, campi annotati `@PersonalData` (un campo
  annotato e non dichiarato fa fallire la compilazione), e le quattro tabelle presenti **sia** in `exportData`
  **sia** in `purgeData`.
- **RT-9 — Registrazione eventi (§14).** Le operazioni di scrittura registrano `tenant_id`, `app_id`, `user_id` e
  identificativo di correlazione, **senza** denominazioni né indirizzi: identificativi soltanto.

## 4. Criteri di accettazione

**CA-1 — Le migrazioni girano su database vero**
- **Dato** un database PostgreSQL 17 effimero
- **Quando** si esegue la suite di integrazione
- **Allora** le migrazioni Flyway si applicano e le quattro tabelle esistono con tutte le colonne di controllo

**CA-2 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con le proprie controparti
- **Quando** un utente di `A` chiede l'elenco delle controparti
- **Allora** vede solo le proprie, anche se forza l'identificativo dell'account `B` nel corpo della richiesta o nei
  parametri

**CA-3 — Nessun metodo senza filtro**
- **Dato** il codice del livello di accesso ai dati
- **Quando** si esegue il controllo strutturale della piattaforma
- **Allora** la compilazione fallisce se esiste un metodo che interroga una tabella con `tenant_id` senza filtrarlo

**CA-4 — Cancellazione logica**
- **Dato** una controparte esistente
- **Quando** la si cancella dall'applicazione
- **Allora** la riga resta con `deleted_at` valorizzato e non compare più in nessuna lettura ordinaria

**CA-5 — Esportazione e cancellazione dichiarate**
- **Dato** un account con dati in tutte e quattro le tabelle
- **Quando** si invoca `exportData` e poi `purgeData` per quell'account
- **Allora** l'esportazione contiene tutte e quattro le tabelle e la cancellazione le svuota fisicamente,
  lasciando una riga di prova nel registro delle purghe

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e compliance; l'intera suite prima del commit);
- [ ] prove di **unità** sul livello di accesso ai dati e di **integrazione** con database effimero e migrazioni
      vere;
- [ ] prova di **isolamento fra account** su tutte e quattro le tabelle;
- [ ] **prova end-to-end**: *nessun impatto* — la storia non tocca la superficie utente;
- [ ] **traduzioni**: nessun testo visibile;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, campi annotati, tabelle in esportazione e
      cancellazione;
- [ ] **registro delle decisioni** compilato, in particolare sulla scelta di non usare chiavi esterne e sul perché;
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0001` | Servono il servizio, lo schema vuoto e il ruolo del database |

## 7. Fuori ambito

- Il significato fiscale dei campi (regime, giurisdizione, recapito elettronico): epica 02, storie `0006`-`0008`.
- Il documento canonico completo allineato alla norma europea: storia `0011`. Qui `canonical_document` è uno
  scheletro con i soli campi comuni.
- `transmission`, `lifecycle_event`, `archive_record`: nascono nelle epiche 04 e 05, quando servono.

## 8. Punti aperti

- **La cancellazione logica non si applicherà ad `archive_record`** (descrizione dell'applicazione §4): un
  documento in conservazione a norma non si cancella su richiesta. La deroga va motivata quando quella tabella
  nasce (storia `0022`), non adesso.
- La classificazione delle voci del manifesto è una **proposta** della descrizione dell'applicazione §6 e va
  confermata dallo sviluppatore: «niente contratto, niente produzione».
