# Change 0030: Console "Diritti GDPR" (admin) + ticketing in-house

**Branch**: `change/0030-use-case-0034-console-diritti-gdpr`
**Aree**: services/core · frontend (apps/admin, apps/backoffice, packages/api-client) · docs
**Data**: 2026-07-04
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/08-compliance-gdpr/0034-console-diritti-gdpr.md](../../docs/usecases/08-compliance-gdpr/0034-console-diritti-gdpr.md)
**Tocca dati personali?**: Sì — nuova entità ticket di supporto (user_id/tenant_id, oggetto, thread; dati personali minimizzati, niente allegati) e nuovo trattamento "gestione diritti/supporto" nel manifesto dati. Si applica il gate privacy/RoPA di step-03 (UC 0031: scanner + classificazione MAJOR/MINOR).

## Problema / Obiettivo

Oggi le richieste di esercizio dei diritti GDPR (export, recessi per-app, eliminazioni account) esistono nel
servizio core ma non c'è alcun punto unico dove il platform-admin possa vederle e gestirle; inoltre non esiste
alcun ticketing di supporto, per cui un export fallito non ha presa in carico tracciata (rimando aperto dalle
change 0028/0029) e la limitazione del trattamento (art. 18, rimando dalla change 0029) non è gestibile da nessuno.

Obiettivo: console **"Diritti GDPR"** nella SPA admin — un unico pannello in **aggregazione** (non un nuovo
store) su richieste export, recessi per-app, eliminazioni account (grace 14gg) e ticket privacy, con puntatori
all'accessorio (deep-link CloudWatch Logs Insights, oggetto S3, prova di purge) — più **ticketing in-house
completo** (utente + admin, thread, notifiche email) con ticket privacy speciali auto-creati dagli eventi e
gestione operativa della limitazione art. 18.

## Scope

**Backend — `services/core`:**
1. **Ticketing in-house** (da zero): entità `support_ticket` + `support_ticket_message` (migration `V7`),
   con tipo (`support` | `privacy`), priorità, stato (open/in_progress/resolved/closed), `tenant_id`/`user_id`,
   oggetto, thread di messaggi (autore utente o admin). Ticket privacy = tipo speciale con scadenza legale
   1 mese (SLA art. 12) esposta come `due_at`. Niente allegati (MVP).
2. **Endpoint utente** (`/api/platform/v1/tickets`, ruoli OWNER/ADMIN dell'account, tenant dal JWT):
   crea ticket, lista i propri, dettaglio con thread, aggiungi messaggio.
3. **Endpoint admin** (`/api/platform/v1/admin/...`, `@RolesAllowed(platform-admin)`, query native
   cross-tenant come `AdminResource`):
   - aggregazione console: lista unificata delle richieste diritti (export job con stato/progress/scadenza
     link, recessi per-app da subscription soft-deleted + export COMPLETED collegato, account in
     `pending_deletion` con countdown grace, ticket privacy) con filtri per tipo/stato;
   - dettaglio per singolo oggetto con timeline e puntatori (chiave S3/link download, riga di
     `gdpr_purge_audit`, deep-link Logs Insights);
   - ticket: lista/dettaglio/risposta nel thread/cambio stato-priorità.
4. **Auto-creazione ticket privacy**: in `GdprExportResultsConsumer`, ramo `JOB_FAILED` → crea ticket
   privacy con riferimento al job (idempotente: un solo ticket per job fallito).
5. **Limitazione art. 18**: azione admin applica/rimuove la sospensione riusando la meccanica esistente
   di sospensione utente/account, con **causale distinta** `gdpr_restriction` che la separa da quella
   amministrativa; l'azione è collegabile a un ticket privacy e lascia **prova nell'audit** (log evento
   tipizzato + registrazione persistente sul modello `gdpr_purge_audit`).
6. **Notifiche email**: Mailer Quarkus (pattern `auth-local/EmailService`, Mailpit in `%dev`, `MockMailbox`
   nei test): all'utente quando l'admin risponde/chiude; all'indirizzo di supporto piattaforma quando nasce
   un ticket (incluso auto-creato). SES reale in cloud = infrastruttura, tracciata (vedi rimandi).
7. **Retention ticket 24 mesi** (#13 E): sweeper orario sul modello `AccountDeletionSweeper` che elimina
   i ticket chiusi/risolti oltre finestra; la prova di evasione resta nell'audit.
8. **Deep-link CloudWatch Logs Insights**: costruito da config per-ambiente (region + log group); query
   pre-filtrata per `request_id`/`tenant_id`/`user_id`/`job_id`. Se la config manca (locale), il link non compare.

**Frontend:**
9. **Admin SPA** (`apps/admin`): pagina "Diritti GDPR" (tabella aggregata: tipo, soggetto, stato, timeline,
   scadenza, link accessorio; dettaglio richiesta; vista ticket con thread e azioni; azione limitazione art. 18
   con conferma). Route sotto `requireRole('platform-admin')`, voce sidebar, i18n, stati loading/empty/error.
10. **Backoffice utente** (`apps/backoffice`): pagina "Supporto" — apri ticket, lista dei propri, thread con
    risposta. Link dalla pagina privacy esistente (sostituisce l'invito "contatta il supporto" su export FAILED).
11. **api-client**: rigenerazione OpenAPI + `npm run gen` + build.

**Trasversale:** manifesto dati/RoPA (nuovo trattamento "gestione diritti/supporto", retention 24 mesi),
OpenAPI committato, logging strutturato su tutti i nuovi flussi, aggiornamento indice `_INDEX.md` 0034 → ✅.

## Fuori scope

- **Cambi consenso**: nessun modello consensi esiste (postura no-consent); la voce della console è omessa.
  → rimando tracciato su **UC 0039** (alimentare la console quando nasce il centro consensi).
- **Framework export/erasure** (UC 0032) e **self-service diritti** (UC 0033): invariati, solo letti.
- **Job retention generali** (UC 0035): qui solo lo sweeper ticket 24 mesi; il resto (retention finestre
  console, purge audit 12 mesi, trigger cloud) resta a UC 0035.
- **Registro breach** (UC 0049): la console non lo linka finché non esiste → rimando tracciato su UC 0049.
- **SES reale in cloud** (infra Terraform): tracciato, non implementato qui.
- **Impersonation**: esclusa per invariante (#03 15) — l'admin agisce solo dai dati della console.
- **Allegati ai ticket**: esclusi (MVP, minimizzazione).

## Criteri di accettazione

- [ ] La console admin aggrega export/recessi/eliminazioni/ticket privacy con stato, timeline, scadenze e
      puntatori (S3, audit purge, Logs Insights quando configurato); accesso solo platform-admin.
- [ ] Un export FAILED genera automaticamente un solo ticket privacy con scadenza a 1 mese e notifica email;
      l'utente può aprire/seguire ticket dal backoffice e l'admin rispondere dalla console (thread + email).
- [ ] L'admin può applicare/rimuovere la limitazione art. 18 con causale dedicata e prova in audit;
      i ticket oltre retention 24 mesi vengono eliminati dallo sweeper.
- [ ] Suite verdi: `mvn test` (core), `npm test` + Playwright e2e (admin e backoffice), gate privacy UC 0031 eseguito.

## Invarianti appgrove toccati

- **Tenant ID solo dal JWT**: gli endpoint utente dei ticket scope-ano su `tenant_id` del token; mai da body/params.
- **Filtro row-level**: entità ticket tenant-scoped (`BaseTenantEntity`); le query admin cross-tenant sono
  native e gated `platform-admin`, stessa eccezione documentata di `AdminResource`.
- **Logging strutturato**: ogni evento (ticket creato/risposto, limitazione applicata, sweep) con
  `tenant_id`/`app_id`/`user_id`.
- Modulo Terraform: non toccato (nessuna nuova app).

## Requisiti di test

- Backend: API ticket utente (isolamento tenant: un tenant non vede i ticket altrui), API admin
  (403 senza platform-admin), auto-ticket su `JOB_FAILED` idempotente, limitazione art. 18 (applica/rimuovi +
  audit), sweeper retention con "adesso" iniettabile, aggregazione console (fixture multi-tipo).
- Frontend: component test pagina console e pagina Supporto; e2e admin (aggregazione + ticket + limitazione)
  ed e2e backoffice (apri ticket, thread).
- Security: nessun endpoint console accessibile senza platform-admin; nessuna impersonation.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No (solo aggiunte) |
| Contratto cross-area | Sì (frontend ↔ nuovi endpoint core; OpenAPI/api-client rigenerati) |
| Version bump | minor |
