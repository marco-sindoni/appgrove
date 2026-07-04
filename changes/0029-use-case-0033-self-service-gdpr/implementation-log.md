# Implementation Log — Change 0029: Self-service GDPR (UC 0033)

**Branch**: `change/0029-use-case-0033-self-service-gdpr`
**Aree**: `services/core`, `frontend` (backoffice, api-client, i18n)
**Completata**: 2026-07-04

## File modificati

| File | Azione |
|---|---|
| `services/core/.../db/migration/V6__account_deletion_grace.sql` | Creato — colonna `deletion_requested_at` su `accounts` |
| `services/core/.../platform/AccountStatus.java`, `Account.java`, `AccountDtos.java` | Modificati — stato `pending_deletion`, campo/scadenza grace (`DELETION_GRACE` 14gg), `AccountView` con `deletionRequestedAt`/`deletionEffectiveAt` |
| `services/core/.../gdpr/AccountDeletionResource.java` | Creato — `POST/DELETE /accounts/me/deletion` (OWNER, 409 su doppia richiesta/annullo a vuoto) |
| `services/core/.../gdpr/AccountDeletionSweeper.java` | Creato — job schedulato (1h): a grace scaduta invoca `TenantOffboarding.offboard` e soft-marca l'account (JDBC, fuori richiesta) |
| `services/core/.../billing/EntitlementReadModel.java` | Modificato — account `pending_deletion` → zero entitlement (disattivazione immediata: registry + gate 402) |
| `services/core/.../platform/UserDtos.java`, `UserResource.java` | Modificati — `UpdateMe` + `PATCH /users/me` (rettifica nome, ogni ruolo, art. 16) |
| `services/core/.../gdpr/ProfileExportResource.java` | Creato — `GET /users/me/export`: JSON sincrono (profilo, account, inviti alla propria email), allegato |
| `services/core/.../gdpr/GdprDtos.java`, `GdprWithdrawalResource.java` | Creato/Modificato — `POST /gdpr/apps/{slug}/withdrawal` (OWNER/ADMIN): verifica export per-app COMPLETED, soft-delete subscription, purge su `tenant-purge-<app>` |
| `services/core/.../gdpr/GdprResource.java` | Modificato — `start` tipizzato (`@ResponseStatus(202)` + `JobView`) per l'OpenAPI/client |
| `services/core/.../META-INF/openapi/openapi.{yaml,json}` | Rigenerati — endpoint nuovi + risposte 202 tipizzate |
| `frontend/packages/api-client/src/schema.ts` | Rigenerato (`npm run gen`) |
| `frontend/packages/i18n/src/resources/{en,it}.ts` | Modificati — blocco `privacy.*` + `nav.privacy` (parità EN/IT) |
| `frontend/apps/backoffice/src/pages/privacy/PrivacyPage.tsx`, `privacyApi.ts` | Creati — pagina "I miei dati" + hook TanStack Query (polling job, download blob) |
| `frontend/apps/backoffice/src/routing/routes.tsx`, `shell/Sidebar.tsx` | Modificati — route `/privacy` (non gateata, F31) + voce menu |
| `frontend/apps/backoffice/src/billing/SubscriptionsPanel.tsx` | Modificato — CTA "Esporta / elimina i tuoi dati" (fase ENDED) → `/privacy` (prima segnaposto `/account`) |
| Test: `core/.../gdpr/{AccountDeletionApiTest,GdprWithdrawalApiTest,ProfileSelfServiceTest}.java`, `GdprGateExemptionTest.java` (esteso), `PrivacyPage.test.tsx`, `e2e/privacy.spec.ts` | Creati/Modificato — vedi sezione Test |
| `docs/usecases/*` (rimandi 0017/0025/0033/0034/0035/0039), `docs/usecases/_INDEX.md`, `requirements.md` | Rimandi tracciati + indice 0033 → ✅ |

## Cosa è stato fatto

Resi esercitabili in-app i diritti GDPR di UC 0033 sopra il framework della change 0028: pagina
"I miei dati" nel backoffice (profilo con rettifica del nome, export del profilo utente in JSON
sincrono dal core, interfaccia dell'export account/per-app con polling e link firmato con scadenza,
recesso per-app con flusso esporta → conferma → cancellazione immediata, eliminazione account con
grazia di 14 giorni annullabile, dichiarazioni artt. 18/21/22 con canale `privacy@`). La macchina
della grazia (anticipata da UC 0035): richiesta → `pending_deletion` + disattivazione immediata
(zero entitlement dal read-model → registry e gate 402 chiusi, API di piattaforma e diritti sempre
fruibili), annullo entro 14gg, sweeper orario che a scadenza invoca l'offboarding della change 0028.

## Decisioni prese

1. **Disattivazione immediata = zero entitlement dal read-model** (`EntitlementReadModel`): un solo
   punto centrale chiude registry frontend e gate 402 delle app; niente meccanismi per-servizio.
2. **Sweeper con "adesso" iniettabile e fixture retrodatate** nei test (niente attese reali); gira
   su scheduler applicativo Quarkus in locale — il trigger cloud è di UC 0035 (tracciato).
3. **Recesso per-app**: prova del passo "esporta" = id di un export job per-app COMPLETED della
   stessa app (tenant-scoped: job altrui → 404; kind/stato sbagliati → 409). L'attivazione si
   rimuove con soft-delete delle subscription (nessun setter su `Subscription`: la scrittura di
   billing resta della pipeline webhook); `provider.cancelSubscription` chiamato best-effort —
   semantica con Paddle reale tracciata su UC 0025.
4. **Export del profilo sincrono** (`GET /users/me/export`, JSON con Content-Disposition): i dati
   dell'utente-persona vivono nel core, nessun job/fan-out necessario (decisione UC 0032).
5. **Risposte 202 tipizzate** (`@ResponseStatus` + DTO al posto di `Response`) su start export /
   richiesta eliminazione / recesso: l'OpenAPI espone gli schema e il client generato è tipato.
6. **Percorso del recesso** sotto il namespace dei diritti (`/gdpr/apps/{slug}/withdrawal`) anziché
   il `/account/apps/...` bozzato nei requirements: coerente con l'esenzione F31 e la guardia
   statica sul package gdpr.
7. **Titoli delle card come `h2` espliciti** (non `CardTitle`=h3): ordine dei heading valido
   (verifica axe nel test componente), stesso pattern di MembersPage.

## Invarianti appgrove

- **Tenant ID solo dal JWT**: tutti i nuovi endpoint leggono tenant/utente da `CallerContext`
  (`tenant_id`/`sub`); lo sweeper (fuori richiesta) usa il tenant persistito nella riga account;
  nessun identificativo dal body oltre `appId`/`exportJobId`, validati contro dati tenant-scoped.
- **Filtro row-level**: letture/scritture via repository tenant-scoped (discriminator) o per
  `id = tenant_id`; l'export job altrui è un 404 (testato); l'export profilo filtra per `sub` +
  email nel tenant corrente.
- **Modulo `microsaas_app`**: nessuna infra nuova; scheduling cloud tracciato su UC 0035.
- **Logging strutturato**: `user.rectify`, `gdpr.profile-export`, `gdpr.account-deletion.request/
  cancel/purge`, `gdpr.withdrawal` con `tenant_id`, `app_id` (dove pertinente), `user_id`.

## Note per il revisore

- **Contratto cross-area**: nuovi endpoint core → OpenAPI rigenerata → `api-client` rigenerato
  (`npm run gen`) e ricompilato insieme a `i18n` (i tipi vivono nei `dist` dei package).
- **Gate privacy (UC 0031)**: scanner → 1 segnale (`Account.deletionRequestedAt`) = marcatore
  tecnico di stato che attua #13 E25: niente `@PersonalData` né manifesto/RoPA (analogo a
  status/audit, change 0028). Endpoint nuovi = diritti già dichiarati; nessun sub-processor.
  **Classificazione confermata dallo sviluppatore: MINOR, piattaforma core** (2026-07-04).
- **Decisioni differite tracciate** (regola CLAUDE.md): UC 0039 (unsubscribe + centro preferenze
  consensi, con requisito in-app annotato), UC 0017 (cambio email con verifica; + UC 0058/☁0015-16),
  UC 0034 (gestione operativa limitazione art. 18; ticket privacy su export FAILED), UC 0035
  (macchina grace anticipata qui: restano inattività 24 mesi, retention per-categoria, archivio
  audit, trigger cloud), UC 0025 (semantica recesso con Paddle reale), UC 0033 (ripresa job export
  dopo reload / elenco export).
- **Dati B2B** verso il tenant-titolare: fuori scope come da use case (tooling UC 0034).
- `run-tests.sh` invariato: nessun modulo aggiunto/rimosso; i test nuovi girano dentro `mvn test`,
  `npm test` e `npm run e2e` esistenti. Nessuna baseline visiva aggiornata.

## Test

- **core** (`mvn test`, 129 verdi): `AccountDeletionApiTest` (richiesta → 202 + `pending_deletion` +
  zero entitlement; doppia richiesta/annullo a vuoto → 409; annullo → riattivo; OWNER-only → 403;
  F31 con subscription canceled; sweeper: solo grace scaduta, fan-out piattaforma+app, idempotente),
  `GdprWithdrawalApiTest` (conferma prima del completamento → 409; flusso completo → 202 + purge in
  coda + attivazione rimossa + nuovo export per-app 404; kind sbagliato → 409; job di altro tenant →
  404; member → 403), `ProfileSelfServiceTest` (rettifica + persistenza + 400 su vuoto; export con
  soli dati propri — inviti altrui esclusi; 404 senza profilo), `GdprGateExemptionTest` esteso alle
  4 resource GDPR (guardia statica F31).
- **frontend** (`npm test`: 79 verdi; `npm run e2e`): `PrivacyPage.test.tsx` (6 test: sezioni + axe,
  gating per ruolo member, rettifica, export account con link/scadenza, recesso con dialog e job
  id corretto, eliminazione con grace e annullo), `e2e/privacy.spec.ts` (5 scenari L2: rettifica,
  download JSON profilo, export account pronto, recesso completo, elimina+annulla).
- **compliance** (`./run-tests.sh compliance`): parità lingue manifesti + freshness RoPA + test
  scanner verdi (nessuna modifica ai manifesti).

## Stato criteri di accettazione

- [x] Pagina "I miei dati": profilo visibile e modificabile (nome), export profilo scaricato,
      export account avviato/monitorato/scaricato con scadenza visibile (vitest + Playwright).
- [x] Eliminazione account: conferma → disattivazione immediata (zero entitlement); annullo entro la
      grace → riattivazione; a grace scaduta lo sweeper invoca l'offboarding (fixture retrodatate).
- [x] Recesso per-app: export completato → conferma → attivazione rimossa + messaggio in coda purge;
      rifiutato (409) se l'export per-app non è completato.
- [x] Sicurezza: endpoint sui soli dati del chiamante (tenant dal JWT, `/me` dal `sub`; job altrui
      404); diritti fruibili senza subscription attiva (`GdprGateExemptionTest` esteso + test F31).
