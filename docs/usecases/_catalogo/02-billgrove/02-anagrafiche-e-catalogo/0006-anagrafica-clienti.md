# 0006 — Anagrafica clienti

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 02 — Anagrafiche e catalogo
**Storia**: `0006` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetto all'amministrazione di una micro-impresa
> voglio tenere in un posto solo i dati dei clienti a cui fatturo
> così da non ricopiare partita IVA, indirizzo e codice destinatario a ogni documento, e da non sbagliarli.

**Contesto.** Oggi il cliente vive nel documento precedente: si apre l'ultima fattura fatta a quel cliente e si
copia. È il modo in cui nascono gli errori di indirizzo e di codice fiscale, che sul documento fiscale non sono
innocui. L'anagrafica clienti è anche la prima delle **entità condivise** individuate dal catalogo (§6): la stessa
scheda servirà a LeadGrove, a CashGrove e ai verticali, quindi va progettata pensando che un giorno sarà letta da
altri — a eventi, mai con una interrogazione fra schemi.

## 2. Requisiti funzionali

1. **RF-1** — Si può creare, modificare, cercare e cancellare (in modo logico) un cliente, con: denominazione o
   nome e cognome, tipo (persona fisica o giuridica), partita IVA, codice fiscale, indirizzo completo, posta
   elettronica, telefono, codice destinatario e nota libera.
2. **RF-2** — L'elenco è paginato, ordinabile e ricercabile per denominazione, partita IVA e posta elettronica.
3. **RF-3** — I campi obbligatori dipendono dal tipo: una persona giuridica richiede la denominazione, una persona
   fisica richiede nome e cognome; la validazione è dichiarativa e il messaggio dice quale campo manca.
4. **RF-4** — Un cliente con documenti collegati non è cancellabile: si può solo archiviare, e l'archiviazione lo
   toglie dagli elenchi di scelta senza toccare i documenti.
5. **RF-5** — Il cliente porta i propri **termini di pagamento predefiniti** (giorni e modalità), che le storie
   successive useranno per calcolare la scadenza.
6. **RF-6** — Accanto alla nota libera compare l'avviso di non inserire dati sensibili.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `customer` filtra per `tenant_id` preso dal
  token verificato; un `tenant_id` che arrivasse dal corpo o dai parametri viene ignorato. Prova di isolamento fra
  due account.
- **RT-2 — Interfaccia di programmazione (§2).** `GET|POST /api/billing/v1/customers`,
  `GET|PUT|DELETE /api/billing/v1/customers/{id}`; corpo validato in modo dichiarativo; errori in
  `application/problem+json`; paginazione a pagina e dimensione con totale; definizione OpenAPI aggiornata nello
  stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V3__customer.sql` sullo schema `app_billing`: tabella `customer` con
  `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica; indice di ricerca su
  `(tenant_id, legal_name)`.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Clienti» del modulo `billing`: elenco con ricerca, scheda di
  dettaglio, modulo di inserimento con React Hook Form e validazione condivisa. Solo token del sistema di design;
  funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `billing` e sono presenti in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6).** I clienti **non** consumano quota: la metrica è `documenti`. Il ruolo `member`
  può creare e modificare; la cancellazione richiede `admin`.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato: `cerca_cliente(testo) → elenco minimizzato`,
  marcato **lettura**. La creazione del cliente da chat non è prevista in questa stesura. Il contratto vive dentro
  il servizio; il server conversazionale è di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **La storia introduce i primi dati personali dell'app.** Voci nuove nel manifesto
  `docs/compliance/manifests/billing.yaml` in italiano e inglese per denominazione, nome e cognome, codice fiscale,
  indirizzo, posta elettronica, telefono e nota; campi annotati `@PersonalData`; tabella `customer` aggiunta a
  `exportData` e `purgeData` del contratto `BillingDataContract`.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `cliente creato`, `cliente archiviato` sono registrati con
  `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, **senza** nome né posta elettronica: si scrive
  l'identificativo del cliente, non chi è.

## 4. Criteri di accettazione

**CA-1 — Creazione di un cliente**
- **Dato** un utente abilitato con ruolo `member`
- **Quando** crea un cliente persona giuridica con denominazione e partita IVA
- **Allora** il cliente è creato e compare nell'elenco, ricercabile per denominazione

**CA-2 — Validazione**
- **Dato** un cliente persona fisica senza cognome · **Quando** si tenta di salvarlo
- **Allora** la risposta è `400` in `problem+json` con l'indicazione del campo mancante, e nulla viene creato

**CA-3 — Cliente con documenti**
- **Dato** un cliente con almeno una fattura emessa · **Quando** si tenta di cancellarlo
- **Allora** la risposta è `409` con la spiegazione, e viene proposta l'archiviazione

**CA-4 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri clienti
- **Quando** un utente di `A` chiede l'elenco dei clienti
- **Allora** vede solo i propri, anche se forza l'identificativo dell'altro account nella richiesta

**CA-5 — Esportazione dei dati dell'interessato**
- **Dato** un cliente presente in anagrafica
- **Quando** si esegue l'esportazione dei dati dell'account
- **Allora** tutti i campi del cliente dichiarati nel manifesto compaiono nell'esportazione

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sulla validazione condizionata al tipo e di **integrazione** sulla risorsa, con database
      effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su `customer`, con tentativo di forzatura del `tenant_id`;
- [ ] **prova end-to-end**: *coprire ora* — passo «crea un cliente» del percorso `[J-BILLING]`; registro di
      copertura aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, campi annotati `@PersonalData`, tabella presente in
      esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `cerca_cliente`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | Il documento deve poter puntare a un cliente |
| storia `0003` | Serve il guscio del modulo per appendere la sezione «Clienti» |

## 7. Fuori ambito

- l'importazione da file: storia `0009`;
- la copia congelata dei dati del cliente sul documento: già prevista dalla storia `0002`, valorizzata dalla `0012`;
- la condivisione dell'anagrafica con le altre app della suite: fuori da questa stesura, dipende dall'infrastruttura
  a eventi.

## 8. Punti aperti

Nessuno. La classificazione dei dati personali introdotta qui è la prima proposta concreta del manifesto e resta,
come tutto il §6 della descrizione, **da confermare** con lo sviluppatore prima della produzione.
