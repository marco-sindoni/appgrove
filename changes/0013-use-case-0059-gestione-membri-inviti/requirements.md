# Change 0013: Gestione membri & inviti (UI backoffice)

**Branch**: `change/0013-use-case-0059-gestione-membri-inviti`
**Aree**: frontend (`apps/backoffice`; +chiavi `packages/i18n`)
**Data**: 2026-06-26
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/06-frontend/0059-gestione-membri-inviti.md](../../docs/usecases/06-frontend/0059-gestione-membri-inviti.md)
**Tocca dati personali?**: Sì — la UI **espone** email/nome dei membri, trattamento **già dichiarato** in UC 0013. Nessun nuovo trattamento (la SPA non persiste nulla); checkpoint privacy di step-03 = no-op documentato.

## Problema / Obiettivo

Owner/admin di un tenant B2B non hanno UI per gestire il team: oggi inviti e membri esistono solo come API core (UC 0013).
Questa change aggiunge la sezione **"Membri"** del backoffice per **vedere** i membri, **invitare** nuovi utenti (email +
ruolo) con invio email, **revocare** inviti pendenti, **cambiare ruolo** e **sospendere/riattivare/rimuovere** membri —
tutto contro endpoint core esistenti. Completa il lato owner/admin di UC 0017 (che copre solo l'accept lato invitato).

## Scope

**Solo `frontend/apps/backoffice`** (nessuna modifica ai servizi: gli endpoint esistono già — UC 0013/0058).

- **Hook API** (TanStack Query, pattern di `api/hooks.ts`): `useMembers` (`GET /users` paged), `useUpdateMember`
  (`PATCH /users/{id}` — role/status), `useRemoveMember` (`DELETE /users/{id}`), `useInvitations` (`GET /invitations`
  paged), `useCreateInvitation` (`POST /invitations` → `InvitationView` col token), `useRevokeInvitation`
  (`DELETE /invitations/{id}`). Invalidazione cache coerente dopo le mutazioni.
- **Invio invito (Opzione A)**: dopo `POST /invitations`, la SPA chiama `${authBaseUrl}/api/auth/invitations/send`
  `{email, token, role}` (email su Mailpit in locale) **e** mostra l'`/accept?token=…` con copia-negli-appunti come
  fallback. Errori dei due passi gestiti distintamente (invito creato anche se l'email fallisce → mostra il link).
- **Pagina `/members`** (route protetta, nuova voce sidebar `PLATFORM`): tabella **membri** (email, nome, ruolo, stato +
  azioni) e tabella **inviti pendenti** (email, ruolo, scadenza, revoca); form **invito** (email + ruolo admin/member).
  Stati loading/empty/error/success; conferme per azioni distruttive (sospendi/rimuovi/revoca). EN/IT, a11y (axe).
- **Gating ruolo**: nuovo guard riusabile `requireAnyRole(['owner','admin'])` sulla route + voce sidebar visibile solo a
  owner/admin (difesa in profondità; enforcement vero nel core via `@RolesAllowed`).
- **Azioni membro**: cambia ruolo (admin↔member), sospendi/riattiva (`status`), rimuovi (soft-delete `DELETE`).
- **Protezioni UX**: disabilito sospendi/rimuovi/declassa **su me stesso** e sull'**ultimo owner**; mappo gli errori
  problem+json (RFC 9457) in messaggi localizzati.
- **Schemi Zod** per il form invito allineati a `CreateInvitation` (`@Email`, `@Size(max=320)`, role ∈ {admin, member}).

## Fuori scope

- **Modifiche ai servizi** (core/auth-local): nessuna.
- **Trasferimento ownership** (promozione/retrocessione a `owner`): non in questa change; ruoli assegnabili = admin/member.
- **Guard backend "ultimo owner"**: assente nel core (verificato) → **tracciato come gap di UC 0013**, qui solo difesa UX.
- **Seat limits / billing** (#09), **SSO**, **accept invito** (UC 0017), **bulk actions**, **ricerca/filtri avanzati**.
- **Orchestrazione cloud dell'invio** (send via auth BFF invece di auth-local) → tracciato come confine cloud (UC 0015/0017).

## Criteri di accettazione

- [ ] `/members` (gated owner/admin) mostra tabella membri + inviti pendenti con stati loading/empty/error.
- [ ] Invito: `POST /invitations` + `POST /api/auth/invitations/send`; l'invito compare tra i pendenti; link `/accept`
      copiabile; email visibile su Mailpit in locale.
- [ ] Revoca invito (`DELETE /invitations/{id}`), cambio ruolo + sospendi/riattiva (`PATCH`), rimozione (`DELETE`) membro,
      con conferma sulle azioni distruttive e cache invalidata.
- [ ] Protezioni UX (self / ultimo owner) attive; errori problem+json mappati EN/IT.
- [ ] Test component (Vitest+RTL+MSW) + E2E (Playwright: invita → pendente → revoca) + a11y axe verdi; typecheck pulito.

## Invarianti appgrove toccati

- **tenant_id solo dal JWT**: la SPA non invia mai `tenant_id`; lista/azioni sono tenant-scoped dal core (claim JWT +
  filtro row-level automatico). La UI legge i ruoli solo dai claim del token (per il gating UX).
- **Filtro row-level**: N/A lato client (enforcement core); la UI consuma dati già filtrati.
- **Modulo `microsaas_app` / logging strutturato**: N/A (SPA statica, nessuna infra/log server-side).

## Requisiti di test

- **Component**: lista membri/inviti (render + stati query), invito (POST core + send auth → riga pendente), revoca
  (DELETE), cambio ruolo e sospendi (PATCH), protezioni UX (azioni disabilitate su self/ultimo owner), mappatura errori
  problem+json, a11y axe. Backend mockato via MSW.
- **E2E** (Playwright): invita → compare tra i pendenti → revoca (backend mockato via `page.route`).
- **Gating**: route `/members` redirige a `/forbidden` per ruolo `member` (guard).

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | Sì (frontend → core `/users`,`/invitations`; frontend → auth-local `/invitations/send`) — **consumo** di contratti esistenti, nessuna modifica |
| Version bump | minor (nuova sezione UI) |
