# Implementation Log — Change 0030: Console "Diritti GDPR" (admin) + ticketing in-house

**Branch**: `change/0030-use-case-0034-console-diritti-gdpr`
**Aree**: services/core · frontend (apps/admin, apps/backoffice, packages/api-client, packages/i18n) · docs
**Completata**: 2026-07-04

## File modificati

| File | Azione |
|---|---|
| `services/core/src/main/resources/db/migration/V7__support_ticketing.sql` | Creato — tabelle `support_ticket`/`support_ticket_message`, colonna `suspended_reason` su accounts/users, `gdpr_restriction_audit` |
| `services/core/.../core/support/` (10 file: entità, enum, repository, `TicketResource`, `TicketStore`, `TicketNotifier`, `TicketRetentionSweeper`, DTO) | Creato — ticketing in-house completo |
| `services/core/.../gdpr/AdminGdprResource.java` + `AdminGdprDtos.java` | Creato — console aggregata (`/admin/gdpr/*`, gated platform-admin) |
| `services/core/.../gdpr/GdprRestrictionService.java` | Creato — limitazione art. 18 con causale `gdpr_restriction` + audit |
| `services/core/.../observability/AwsConsoleLinks.java` | Creato — deep-link Logs Insights/S3 da config per-ambiente |
| `services/core/.../gdpr/GdprExportResultsConsumer.java` | Modificato — auto-ticket privacy su `JOB_FAILED` (idempotente) |
| `services/core/.../gdpr/PlatformDataContract.java` | Modificato — export/purge dei ticket + manifesto derivato |
| `services/core/.../platform/Account.java`, `User.java` | Modificato — campo `suspendedReason` |
| `services/core/pom.xml`, `application.properties` | Modificato — `quarkus-mailer` (Mailpit in `%dev`), `appgrove.support.inbox`, config `appgrove.aws-console.*` documentata |
| `services/core/.../META-INF/openapi/openapi.{yaml,json}` | Rigenerato |
| `services/core/src/test/...` (6 classi nuove + 3 aggiornate) | Creato/Modificato — vedi Test |
| `frontend/packages/api-client/src/schema.ts` | Rigenerato (`npm run gen`) |
| `frontend/packages/i18n/src/resources/{en,it}.ts` | Modificato — namespace `support` e `admin.gdpr`, `nav.support`, `admin.nav.gdpr`, copy export fallito + CTA |
| `frontend/apps/backoffice/src/pages/support/` (`SupportPage.tsx`, `supportApi.ts`, test) | Creato — pagina "Supporto" (apri ticket, lista, thread) |
| `frontend/apps/backoffice/src/pages/privacy/PrivacyPage.tsx` | Modificato — su export FAILED link "Vai al supporto" (l'auto-ticket nasce lato server) |
| `frontend/apps/backoffice/src/{routing/routes.tsx,shell/Sidebar.tsx}` | Modificato — route + voce nav `/support` |
| `frontend/apps/backoffice/e2e/support.spec.ts` | Creato; `e2e/privacy.spec.ts` esteso (export FAILED → link Supporto) |
| `frontend/apps/admin/src/pages/{GdprRights,GdprTicketDetail,GdprExportDetail}.tsx` + `gdpr.test.tsx` | Creato — console Diritti GDPR |
| `frontend/apps/admin/src/{api/hooks.ts,routing/routes.tsx,shell/Sidebar.tsx}` | Modificato — hook `/admin/gdpr/*`, route `gdpr[/tickets/:id|/exports/:id]`, voce nav |
| `frontend/apps/admin/e2e/gdpr.spec.ts` | Creato |
| `docs/compliance/manifests/platform.yaml` + `ropa.{it,en}.md` | Modificato/Rigenerato — voci `support_ticket.subject`, `support_ticket_message.body` |
| `docs/usecases/08-compliance-gdpr/0034-*.md`, `0035-*.md`, `09-.../0039-*.md`, `10-.../0049-*.md`, `_INDEX.md` | Modificato — punti aperti risolti/tracciati, indice 0034 → ✅ |
| `dev/docker-compose.yml` | Modificato — ElasticMQ pinnato a `1.6.14` (fix collaudo: il `latest` corrente non pubblica `linux/arm64` e rompeva lo stack su Apple Silicon/Colima) |

## Cosa è stato fatto

Console **"Diritti GDPR"** nella SPA admin (UC 0034): pannello unico **in aggregazione** — non un nuovo
store — su export (stato/progress/scadenza link 7gg), recessi per-app (derivati dalle attivazioni
soft-deleted di account vivi), eliminazioni account in grace e ticket privacy, con puntatori
all'accessorio (deep-link CloudWatch Logs Insights e oggetto S3 costruiti da config per-ambiente — in
locale assenti — più registri di prova purge/limitazioni). **Ticketing in-house completo** (#13 D21):
entità con thread tenant-scoped, pagina "Supporto" nel backoffice (ogni ruolo, esente dai gate F31),
vista admin con risposta/stato/priorità, notifiche email bidirezionali via Mailer (Mailpit in dev,
fail-soft), ticket privacy con scadenza legale a 1 mese **auto-creati su export FAILED** (idempotenti
per indice unico su `export_job_id`) e retention 24 mesi via sweeper orario. **Limitazione art. 18**
(#13 D19): applica/rimuovi la sospensione di account/utente con causale dedicata `gdpr_restriction`
(mai sovrascrive una sospensione amministrativa) e prova di evasione in `gdpr_restriction_audit`.

## Decisioni prese

- **Recessi per-app derivati, non registrati**: nessuna tabella nuova — un recesso = subscription
  soft-deleted di un account vivo (unico flusso che le produce); gli account offboardati compaiono già
  come eliminazioni. Coerente con "non è un nuovo store" (#13 L75).
- **Scritture admin/sistema via JDBC con tenant esplicito** (`TicketStore`, `GdprRestrictionService`):
  l'admin opera fuori dal tenant del ticket e il consumer gira senza JWT — stesso razionale di
  `GdprJobStore`; le letture cross-tenant sono l'eccezione documentata all'invariante #2, gated
  `platform-admin` (pattern `AdminResource`).
- **Nessun download admin dell'export**: il dettaglio mostra la **chiave** S3 e il link alla console
  (minimizzazione); il contenuto resta scaricabile solo dall'utente via presigned URL.
- **Email best-effort**: l'invio non fa mai fallire l'operazione sul ticket (try/catch + log warn).
- **Retention ticket = hard-delete** (minimizzazione): la prova di evasione sta nei registri audit,
  non nel ticket; risposta utente su ticket `resolved` lo riapre, su `closed` → 409.
- **Formato deep-link AWS best-effort**: encoding "a stella" documentato della console; validazione su
  ambiente reale tracciata (UC 0034 "Punti aperti").
- **Bug trovato in collaudo manuale (corretto)**: la notifica email di apertura ticket riportava
  "tenant null" — il riferimento era costruito dall'entità appena `persist()`, ma il discriminator
  `@TenantId` viene valorizzato da Hibernate solo all'insert (flush). Fix: lato utente il
  `TicketRef` si costruisce da **JWT + entità** (`CallerContext`, coerente con l'invariante #1);
  rimossa la factory `TicketRef.of(SupportTicket)` (scorciatoia insidiosa) e aggiunta asserzione di
  regressione in `TicketApiTest` (la mail contiene il tenant reale e nessun "null").

## Invarianti appgrove

- **tenant_id solo dal JWT**: `TicketResource` scrive via ORM col discriminator (mai da body/param);
  gli endpoint admin non accettano tenant come input se non per lettura cross-tenant gated.
- **Filtro row-level**: ticket/messaggi estendono `BaseTenantEntity` (filtro automatico → i ticket
  altrui sono 404, test dedicato); le query native cross-tenant sono la stessa eccezione documentata
  di `AdminResource`, ammessa solo perché `@RolesAllowed(platform-admin)`.
- **Logging strutturato**: ogni evento (`ticket.opened/user-reply/admin-reply/admin-update/auto-created`,
  `gdpr.restriction applied/removed`, `ticket.retention-sweep`) porta `tenant_id`/`user_id`/attore.
- Modulo Terraform: non toccato (nessuna nuova app; nessun nuovo servizio → niente app-start/Caddyfile).

## Gate privacy (UC 0031)

Scanner: **9 segnali**, tutti classificati (co-pilota step-03, conferma esplicita dello sviluppatore):
nuovo trattamento "gestione richieste di supporto e diritti" → voci manifesto `support_ticket.subject`
e `support_ticket_message.body` (finalità supporto/diritti; base contratto art. 6.1.b + obbligo legale
art. 12 per i ticket privacy; retention 24 mesi dalla chiusura, attuata dallo sweeper; categoria
ordinaria, niente allegati) + RoPA rigenerato (`npm run assemble`) + check `@PersonalData`↔manifesto
verde. `suspended_reason`/`gdpr_restriction_audit` = dati tecnici di stato/prova (come
`status`/`gdpr_purge_audit`), senza voce manifesto. **Classificazione: MINOR, piattaforma core**
(finalità interna al contratto, trattamento avviato dall'utente, nessun nuovo sub-responsabile — SES
in cloud è servizio AWS già in lista #13 H45; flag registrato per la lista pubblica di UC 0002).

## Note per il revisore

- **Contratto cross-area**: nuovi endpoint `/api/platform/v1/tickets*` (utente) e
  `/api/platform/v1/admin/gdpr/*` (platform-admin); OpenAPI e `api-client` rigenerati nello stesso
  commit. Nessun breaking (solo aggiunte).
- **Decisioni differite tracciate**: UC 0034 (relay SES cloud, validazione deep-link in cloud — i due
  punti aperti pre-esistenti art. 18 e auto-ticket sono risolti da questa change); UC 0035 (trigger
  cloud dello sweeper ticket, retention dei registri audit 12 mesi, scoping finestre console);
  UC 0039 (alimentare la console coi cambi consenso quando nascerà il consent log); UC 0049 (link al
  registro breach dalla console quando esisterà).
- **Fix fuori scope minimo (dev stack)**: durante il collaudo manuale `./app-start.sh` falliva su
  Apple Silicon perché il `latest` di `softwaremill/elasticmq-native` non pubblica più `linux/arm64`
  (e la cache locale conteneva immagini amd64 di una VM Colima precedente): ElasticMQ è ora **pinnato
  a `1.6.14`** (multi-arch) in `dev/docker-compose.yml`; le altre immagini stantie sono state
  ri-scaricate (nessuna modifica al repo oltre al pin). Verificato: stack su, schema `platform` a V7.
- Nit pre-esistente (change 0029, non toccato): `npm run typecheck` del backoffice segnala
  `e2e/privacy.spec.ts` per `Array.at()` (lib TS); le suite (`npm test`/Playwright) non ne risentono.
- In locale i deep-link AWS non compaiono (config assente per scelta); per vederli in cloud servono
  `appgrove.aws-console.region` e `appgrove.aws-console.log-group`.

## Test

- **Backend** (`services/core`, `mvn test` — **verde**, 151+ test): `TicketApiTest` (apertura con
  scadenza legale, thread, riapertura su resolved, 409 su chiuso, **isolamento tenant**, anonimo 401,
  notifica casella supporto via MockMailbox); `TicketAdminApiTest` (403 non-admin, lista cross-tenant
  con nome account, risposta → in_progress + email al richiedente, cambio stato → `closed_at` + email);
  `AdminGdprConsoleTest` (aggregazione 4 tipi con scadenze, filtro, dettaglio export con chiave ZIP,
  niente deep-link in locale, registro purge); `GdprRestrictionApiTest` (applica/rimuovi su utente e
  account con prova audit, 409 doppia applicazione e su sospensione amministrativa, 404, 403);
  `AutoTicketOnExportFailureTest` (un solo ticket per job fallito anche sotto redelivery, priorità alta
  + scadenza, notifica); `TicketRetentionSweeperTest` ("adesso" iniettabile: oltre 24 mesi eliminato,
  recenti/aperti intatti); `AwsConsoleLinksTest` (unit: config assente → nessun link; URL con
  regione/query/log-group codificati); aggiornati `PlatformGdprContractTest` (i ticket entrano in
  export/purge del contratto piattaforma) e `GdprExportApiTest` (4° step).
- **Frontend** (`npm test` — **verde**: admin 11, backoffice 82): `gdpr.test.tsx` (aggregazione +
  puntatori, limitazione con ConfirmDialog e registro, dettaglio ticket con risposta e cambio stato);
  `SupportPage.test.tsx` (apertura privacy con scadenza, thread con risposta, chiuso senza form).
- **E2E Playwright** (**verdi**: admin 2, backoffice 17): `gdpr.spec.ts` (aggregazione → deep-link →
  dettaglio export con puntatore S3 → ticket auto-creato → risposta → limitazione art. 18 con conferma
  e prova nel registro); `support.spec.ts` (apri ticket privacy → thread → risposta);
  `privacy.spec.ts` esteso (export FAILED → messaggio + link alla pagina Supporto). Nessuna baseline
  visiva toccata.
- **Compliance** (`tools/compliance`, `npm test` — **verde**, 17 test): parità lingue manifesti +
  freshness RoPA post-rigenerazione.
- Esecuzione canonica: `./run-tests.sh backend`, `./run-tests.sh frontend`, `./run-tests.sh compliance`
  — tutte verdi sul diff finale (infra non toccata).

## Stato criteri di accettazione

- [x] La console admin aggrega export/recessi/eliminazioni/ticket privacy con stato, timeline, scadenze
      e puntatori (S3, audit purge, Logs Insights quando configurato); accesso solo platform-admin.
- [x] Un export FAILED genera automaticamente un solo ticket privacy con scadenza a 1 mese e notifica
      email; l'utente apre/segue ticket dal backoffice e l'admin risponde dalla console (thread + email).
- [x] L'admin applica/rimuove la limitazione art. 18 con causale dedicata e prova in audit; i ticket
      oltre retention 24 mesi vengono eliminati dallo sweeper.
- [x] Suite verdi: `mvn test` (core), `npm test` + Playwright e2e (admin e backoffice), gate privacy
      UC 0031 eseguito con classificazione MINOR confermata.
