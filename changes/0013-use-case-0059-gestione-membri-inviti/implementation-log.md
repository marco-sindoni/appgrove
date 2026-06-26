# Implementation Log — Change 0013: Gestione membri & inviti (UC 0059)

**Branch**: `change/0013-use-case-0059-gestione-membri-inviti`
**Aree**: frontend (`apps/backoffice`; +`packages/i18n`, +`packages/api-client`)
**Completata**: 2026-06-26

## File modificati

| File | Azione |
|---|---|
| `frontend/packages/i18n/src/resources/en.ts`, `it.ts` | Modificato — namespace `members` + `nav.members` (EN+IT, parità) |
| `frontend/packages/api-client/src/contract.ts`, `index.ts` | Modificato — alias drift-guard `InvitationView` (da `components` schema) + export |
| `frontend/apps/backoffice/src/api/hooks.ts` | Modificato — `useMembers/useUpdateMember/useRemoveMember/useInvitations/useCreateInvitation/useRevokeInvitation` |
| `frontend/apps/backoffice/src/auth/authApi.ts` | Modificato — `sendInvitation` (`POST /api/auth/invitations/send`) |
| `frontend/apps/backoffice/src/auth/schemas.ts` | Modificato — `inviteSchema` (email + role admin/member) |
| `frontend/apps/backoffice/src/routing/guards.tsx` | Modificato — guard `requireAnyRole(['owner','admin'])` |
| `frontend/apps/backoffice/src/routing/routes.tsx` | Modificato — route protetta `/members` (gated owner/admin) |
| `frontend/apps/backoffice/src/shell/Sidebar.tsx` | Modificato — voce nav "Membri" (visibile solo owner/admin) |
| `frontend/apps/backoffice/src/pages/members/MembersPage.tsx` (+test) | Creato — pagina membri/inviti |
| `frontend/apps/backoffice/src/pages/members/ConfirmDialog.tsx` | Creato — dialog conferma accessibile (azioni distruttive) |
| `frontend/apps/backoffice/src/routing/navigation.test.tsx` | Modificato — test gating ruolo `member` → `/forbidden` |
| `frontend/apps/backoffice/e2e/members.spec.ts` | Creato — E2E invita→pendente→revoca |
| `docs/usecases/04-platform-core/0013-…md` | Modificato — "Punti aperti" (guard ultimo-owner, OpenAPI `POST /invitations`) |
| `docs/usecases/06-frontend/0059-…md`, `_INDEX.md` | Modificato — stato 🟢, "Punti aperti" aggiornati, _INDEX → ✅ |

## Cosa è stato fatto

Sezione **"Membri"** del backoffice (UC 0059, lato owner/admin) sopra la shell (UC 0020), cablata su endpoint **già
esistenti** del core (UC 0013) e di auth-local (UC 0058). Route protetta `/members` + voce sidebar visibili solo a
owner/admin (guard `requireAnyRole`; enforcement vero nel core via `@RolesAllowed`). La pagina mostra **tabella membri**
(email, nome, ruolo, stato) e **tabella inviti pendenti** (email, ruolo, scadenza), con:
- **Invito** (Opzione A): `POST /api/platform/v1/invitations` (token grezzo) → `POST /api/auth/invitations/send` (email su
  Mailpit in locale; BFF in cloud) + link `/accept?token=…` **copiabile** come fallback; se l'email fallisce, l'invito
  resta valido e si mostra il link.
- **Revoca** invito (`DELETE /invitations/{id}`), **cambio ruolo** admin↔member (`PATCH /users/{id}`), **sospendi/riattiva**
  (`PATCH status`), **rimuovi** (`DELETE /users/{id}`), con `ConfirmDialog` accessibile sulle azioni distruttive.
- **Protezioni UX**: azioni distruttive/declassamento disabilitate su **se stessi** e sull'**ultimo owner**; niente
  trasferimento di ownership (ruoli assegnabili admin/member). Errori problem+json mappati (409 invito → messaggio dedicato).

## Decisioni prese

- **Opzione A** (gate domanda 1): invio email reale via auth-local + link copiabile.
- **Collocazione** (gate domanda 2): route dedicata `/members` + voce sidebar gated (non tab in Settings); nuovo guard
  `requireAnyRole`.
- **Azioni** (gate domanda 3): cambia ruolo + sospendi/riattiva + rimuovi; protezioni UX su self/ultimo-owner.
- **`InvitationView`** tipizzato dal componente schema (non dal path): l'OpenAPI del `POST /invitations` non documenta il
  body → cast lato client; gap backend tracciato in UC 0013.

## Invarianti appgrove

- **tenant_id/user_id solo dal JWT** — la SPA non invia mai `tenant_id`; lista/azioni sono tenant-scoped dal core (claim
  JWT + filtro row-level automatico). Il gating UX legge i ruoli **solo** dai claim del token.
- **Filtro row-level** — N/A lato client (enforcement core); la UI consuma dati già filtrati.
- **Modulo `microsaas_app` / logging strutturato** — N/A (SPA statica).

## Note per il revisore

- **Nessuna modifica ai servizi**: solo consumo di contratti esistenti (core UC 0013, auth-local UC 0058).
- **Decisioni differite tracciate** (regola costituzione): guard "ultimo owner" + OpenAPI `POST /invitations` → **UC 0013**;
  invio email cloud via BFF → **UC 0015/0017** (in UC 0059); paginazione → UC 0059. Protezione ultimo-owner qui è **solo UX**.
- **E2E visual baseline (#10 F)**: E2E con asserzioni `getByRole`/`getByLabel`, nessuno snapshot visivo → nessun baseline.
- **Privacy/RoPA**: nessun nuovo trattamento — la UI espone email/nome membri già dichiarati in UC 0013; nessuna persistenza client.

## Test

Tutte le suite delle aree toccate **verdi**.

- **`@appgrove/i18n`** (3): parità chiavi EN↔IT (incl. namespace `members`).
- **`@appgrove/backoffice`** (43, +7 vs change 0012): MembersPage (lista, invito+send, revoca con conferma, cambio ruolo,
  protezioni UX su self/ultimo-owner, a11y axe) + gating ruolo `member`→`/forbidden`, oltre alle suite esistenti.
- **E2E Playwright** (chromium, 5, **passati**): shell (2) + auth (2) + **membri (1): invita→pendente→revoca** (backend mockato).
- **Typecheck** `tsc` verde su tutti i workspace toccati (api-client/i18n/backoffice).

Esito `npm test` (root): api-client ✓ · design-system ✓ · i18n 3 ✓ · backoffice 43 ✓. Playwright 5 ✓.

## Stato criteri di accettazione

- [x] `/members` (gated owner/admin) con tabella membri + inviti pendenti e stati loading/empty/error.
- [x] Invito: `POST /invitations` + `POST /api/auth/invitations/send`; compare tra i pendenti; link `/accept` copiabile.
- [x] Revoca invito, cambio ruolo, sospendi/riattiva, rimozione membro, con conferma sulle distruttive e cache invalidata.
- [x] Protezioni UX (self/ultimo owner); errori problem+json mappati EN/IT.
- [x] Component + E2E + a11y + typecheck verdi.
- [x] Rinvii tracciati (UC 0013/0015/0017/0059); UC 0059 → ✅ in _INDEX.
