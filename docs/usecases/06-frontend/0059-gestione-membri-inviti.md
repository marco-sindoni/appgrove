# UC 0059 — Gestione membri & inviti (UI backoffice)

**Area**: 06-frontend · **Fase**: 2 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0020](0020-shell-spa-backoffice.md) (shell), UC [0013](../04-platform-core/0013-account-utenti-inviti-api.md) (API core users/invitations), UC [0017](../05-auth/0017-flussi-auth.md) (accept-invite, lato invitato)
**Fonte decisioni**: #03, #02 · **Ultimo aggiornamento**: 2026-06-26

> Scorporato dalla change `0012-use-case-0017-…` (UC 0017): l'**invio** inviti / gestione membri è **gestione team**,
> non un flusso di auth. UC 0017 implementa solo l'**accept-invite** (lato invitato); questo UC possiede il lato
> **owner/admin** (lista membri, invita, revoca, cambia ruolo). Spec da rifinire prima dell'implementazione.

## 1. Obiettivo / Scope
Schermata "Membri" nelle impostazioni del backoffice (B2B multi-utente): owner/admin **vedono** i membri del tenant,
**invitano** nuovi utenti (email + ruolo), **revocano** inviti pendenti e **cambiano ruolo**/disattivano membri.
**Incluso**: lista membri + inviti pendenti, invito, revoca, cambio ruolo. **Escluso**: l'accettazione invito (UC 0017),
billing/seat-limits (#09), SSO.

## 2. Attori & ruoli
- **Owner/Admin** del tenant: gestiscono i membri. **Member**: sola lettura (o nessun accesso alla sezione).

## 3. Precondizioni
- Sessione attiva (shell UC 0020); core API `users`/`invitations` raggiungibili (UC 0013); ruolo owner/admin.

## 4. Flusso principale
1. Apri **Impostazioni → Membri**: `GET /api/platform/v1/users` (lista) + `GET /invitations` (pendenti).
2. **Invita**: form email + ruolo → `POST /api/platform/v1/invitations` (crea riga + token) → invio email invito
   (in locale `POST /api/auth/invitations/send`; in cloud lato auth BFF). L'invitato completa con UC 0017.
3. **Revoca** invito pendente: `DELETE /invitations/{id}`.
4. **Cambia ruolo / disattiva** membro: `PATCH /api/platform/v1/users/{id}` (`UpdateUser{role,status}`).

## 5. Flussi alternativi / edge / errori
- Email già membro/invitata → errore problem+json. Invito scaduto → ri-invio. 1 utente→1 tenant (vincolo #02).
- Non puoi rimuovere/declassare l'ultimo owner. Errori tipizzati (problem+json) con stati loading/empty/error.

## 6. Schermate & stati _(UI)_
Tabella membri (email, ruolo, stato) + tabella inviti pendenti (email, ruolo, scadenza, revoca). Form invito (modale/inline).
Stati loading/empty/error/success; conferme per azioni distruttive (revoca/disattiva). EN/IT, a11y.

## 7. Dati toccati
Legge/scrive `users`/`invitations` del tenant via core (filtrati per `tenant_id`). **Dati personali**: email/nome membri —
trattamento **già dichiarato** in UC 0013 (nessun nuovo trattamento; la UI espone dati esistenti).

## 8. Permessi & gate
`requireRole('owner'|'admin')` lato UX (difesa in profondità); enforcement vero nel core (`@RolesAllowed`). `tenant_id`
solo dal JWT; filtro row-level lato core.

## 9. Requisiti di test
- **Component** (Vitest+RTL+MSW): lista, invito (POST), revoca (DELETE), cambio ruolo (PATCH), stati query, a11y.
- **E2E** (Playwright): invita → compare tra i pendenti → revoca. **Security**: azioni gated per ruolo.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #03 (backoffice/IA), #02 (ruoli/inviti).
- **DoD**:
  1. Lista membri + inviti pendenti; invito/revoca/cambio ruolo funzionanti contro il core.
  2. Gating per ruolo (UX) coerente con l'enforcement core; problem+json mappato.
  3. Component + E2E + a11y verdi; EN/IT.

## Punti aperti / decisioni differite
- **Invio email invito — locale FATTO, cloud da fare** (change `0013-…`): in locale la SPA chiama
  `POST /api/auth/invitations/send` (auth-local) col token del core (Opzione A). In **cloud** lo stesso path deve essere
  servito dall'**auth BFF** (stessa `authBaseUrl`): l'endpoint `/api/auth/invitations/send` va implementato lì
  (allineare con UC 0015/0017). La SPA non cambia.
- **Paginazione membri/inviti**: la UI carica una pagina ampia (`size=100`) senza controlli di paginazione (fuori scope).
  Aggiungere paginazione/ricerca se un tenant supera ~100 membri o inviti.
- **Lacune backend (tracciate in UC 0013)**: guard "ultimo owner" assente lato core (qui solo protezione UX); OpenAPI di
  `POST /invitations` senza body di risposta (client costretto a cast manuale). Vedi UC 0013 "Punti aperti".
- **Seat limits / billing**: eventuali limiti al numero di membri per tier sono di #09 (non qui).
- **🏛️ Modello utenti tenant-level vs per-app (B2B/B2C)** *(richiesto 2026-07-01, dopo UC 0028)* — questione di piattaforma
  trasversale: la gestione membri qui è **tenant-level** (assume B2B), ma un'app può essere **B2C** (solo owner) o **B2B**
  (utenti invitati). Direzione preferita: invito **per-app** con limiti/pricing posti per-app + funzione "invita utenti"
  che legge gli utenti da **altre app B2B** del tenant (directory di comodo), **senza** offerta posti centralizzata. Dettaglio
  e opzioni in [_BACKLOG](../../_BACKLOG.md) → "Modello di gestione utenti — tenant-level vs per-app". Da approfondire in
  sessione dedicata.
