import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { unwrap, type InvitationView } from '@appgrove/api-client'
import { useAuthStore } from '../auth/authStore'
import { useApiClient } from './apiClient'

/**
 * Vero se chi è in sessione può leggere le persone dell'account e i suoi inviti — da UC 0100 il
 * **solo owner**, come il core.
 *
 * Serve a **non chiedere** ciò che sappiamo verrebbe rifiutato: la sezione «Members» è già riservata
 * all'owner, ma il cruscotto (UC 0097) mostra gli stessi due numeri a chiunque apra il backoffice, e
 * mostrare una riga rotta a un collaboratore è peggio che non mostrarla.
 */
export function useCanReadMembers(): boolean {
  return useAuthStore((s) => !!s.claims?.roles?.includes('owner'))
}

/** Profilo dell'utente corrente (`GET /users/me`). Dati personali già dichiarati in UC 0013. */
export function useCurrentUser() {
  const client = useApiClient()
  return useQuery({
    queryKey: ['users', 'me'],
    queryFn: () => unwrap(client.GET('/api/platform/v1/users/me')),
  })
}

/** Account (tenant) corrente (`GET /accounts/me`). */
export function useCurrentAccount() {
  const client = useApiClient()
  return useQuery({
    queryKey: ['accounts', 'me'],
    queryFn: () => unwrap(client.GET('/api/platform/v1/accounts/me')),
  })
}

/** Aggiorna il nome dell'account (`PATCH /accounts/me`). */
export function useUpdateAccountName() {
  const client = useApiClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (name: string) =>
      unwrap(client.PATCH('/api/platform/v1/accounts/me', { body: { name } })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['accounts', 'me'] }),
  })
}

// ── Newsletter (UC 0039) — preferenza dell'utente autenticato, user-scoped (tenant_id dal JWT) ──

/** Stato dell'iscrizione newsletter dell'utente corrente (`GET /newsletter/preference`). */
export function useNewsletterPreference() {
  const client = useApiClient()
  return useQuery({
    queryKey: ['newsletter', 'preference'],
    queryFn: () => unwrap(client.GET('/api/platform/v1/newsletter/preference')),
  })
}

/** Attiva/disattiva l'iscrizione (`PUT /newsletter/preference`): grant/revoke canale account. */
export function useSetNewsletterPreference() {
  const client = useApiClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (subscribed: boolean) =>
      unwrap(client.PUT('/api/platform/v1/newsletter/preference', { body: { subscribed } })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['newsletter', 'preference'] }),
  })
}

// ── Persone & inviti dell'account (UC 0059, elenco unico UC 0100) — endpoint del SOLO owner ──

/**
 * Le persone dell'account (`GET /users`), ciascuna con le applicazioni su cui è abilitata e la data di
 * ingresso (UC 0100). Una pagina ampia: la schermata non pagina (la paginazione dell'elenco è rimandata
 * alla ricerca globale del backoffice, UC 0088).
 */
export function useMembers() {
  const client = useApiClient()
  const canRead = useCanReadMembers()
  return useQuery({
    queryKey: ['users', 'list'],
    enabled: canRead,
    queryFn: () => unwrap(client.GET('/api/platform/v1/users', { params: { query: { size: 100 } } })),
  })
}

/** Aggiorna ruolo/stato di un membro (`PATCH /users/{id}`). */
export function useUpdateMember() {
  const client = useApiClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (vars: { id: string; role?: string; status?: string }) =>
      unwrap(
        client.PATCH('/api/platform/v1/users/{id}', {
          params: { path: { id: vars.id } },
          body: { role: vars.role, status: vars.status },
        }),
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['users', 'list'] }),
  })
}

/** Rimuove (soft-delete) un membro (`DELETE /users/{id}`). */
export function useRemoveMember() {
  const client = useApiClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) =>
      unwrap(client.DELETE('/api/platform/v1/users/{id}', { params: { path: { id } } })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['users', 'list'] }),
  })
}

/** Inviti in attesa dell'account (`GET /invitations`): righe dello stesso elenco delle persone (UC 0100). */
export function useInvitations() {
  const client = useApiClient()
  const canRead = useCanReadMembers()
  return useQuery({
    queryKey: ['invitations', 'list'],
    enabled: canRead,
    queryFn: () =>
      unwrap(client.GET('/api/platform/v1/invitations', { params: { query: { size: 100 } } })),
  })
}

/**
 * Crea un invito (`POST /invitations`) → `InvitationView` col **token grezzo** (solo qui).
 *
 * Il corpo è **solo l'indirizzo** (UC 0100): il ruolo non si sceglie più, perché non era una scelta —
 * chi entra entra come persona dell'account, e i poteri si concedono dopo, una applicazione alla volta.
 */
export function useCreateInvitation() {
  const client = useApiClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (vars: { email: string }) =>
      unwrap<InvitationView>(client.POST('/api/platform/v1/invitations', { body: vars })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['invitations', 'list'] }),
  })
}

/** Revoca un invito in sospeso (`DELETE /invitations/{id}`). */
export function useRevokeInvitation() {
  const client = useApiClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) =>
      unwrap(client.DELETE('/api/platform/v1/invitations/{id}', { params: { path: { id } } })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['invitations', 'list'] }),
  })
}

// ── Ri-accettazione legale runtime (UC 0056) — stato/versionamento dei documenti legali ──────────

/**
 * Stato legale dell'utente corrente (`GET /me/legal/status`): `pending` = documenti vincolanti da
 * ri-accettare (blocca la shell), `notices` = aggiornamenti minori (banner non bloccante). `act` vale
 * `accept` per i termini (accettazione), `acknowledge` per privacy/cookie (presa visione).
 */
export function useLegalStatus() {
  const client = useApiClient()
  return useQuery({
    queryKey: ['legal', 'status'],
    queryFn: () => unwrap(client.GET('/api/platform/v1/me/legal/status')),
  })
}

/** Registra l'accettazione dei componenti (`POST /me/legal/acceptance`) e ricarica lo stato. */
export function useAcceptLegal() {
  const client = useApiClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (vars: { components: string[] }) =>
      unwrap(client.POST('/api/platform/v1/me/legal/acceptance', { body: vars })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['legal', 'status'] }),
  })
}

/**
 * Documento legale localizzato (`GET /legal/{component}?lang`): il `markdown` ha i token già risolti
 * lato server. Abilitato solo quando `component` è definito (caricamento on-demand alla richiesta).
 */
export function useLegalDoc(component: string | undefined, lang: string) {
  const client = useApiClient()
  return useQuery({
    queryKey: ['legal', 'doc', component, lang],
    queryFn: () =>
      unwrap(
        client.GET('/api/platform/v1/legal/{component}', {
          params: { path: { component: component as string }, query: { lang } },
        }),
      ),
    enabled: component != null,
  })
}

// ── Appartenenze e account attivo (UC 0117) ───────────────────────────────────

/** Chiave della lettura delle appartenenze: condivisa con chi la invalida dopo un cambio. */
export const MEMBERSHIPS_KEY = ['me', 'memberships'] as const

/**
 * Gli account a cui la persona in sessione appartiene, e quale è **attivo**
 * (`GET /me/memberships`).
 *
 * <p>Il campo `activeAccountId` è la verità corrente lato server, non il valore grezzo conservato:
 * serve al selettore per marcare l'account su cui si sta lavorando e — confrontato con l'account del
 * token che la scheda ha in mano — per accorgersi che l'account attivo è **cambiato in un'altra
 * scheda**. Per questo la lettura si rinfresca al ritorno sulla scheda, come già fa quella degli
 * entitlement: è l'unico modo perché una scheda dimenticata aperta si accorga del cambio.
 */
export function useMyMemberships() {
  const client = useApiClient()
  const status = useAuthStore((s) => s.status)
  return useQuery({
    queryKey: MEMBERSHIPS_KEY,
    enabled: status === 'authenticated',
    refetchOnWindowFocus: true,
    refetchOnReconnect: true,
    queryFn: () => unwrap(client.GET('/api/platform/v1/me/memberships')),
  })
}

/**
 * Cambia l'account attivo (`POST /me/active-account`). **Non** restituisce token: il rinnovo passa
 * dal percorso di rinnovo esistente, così l'account si stabilisce in un posto solo. Chi chiama deve
 * **ricaricare** l'applicazione — è il ricaricamento a far nascere il token nuovo. Vedi
 * `AccountSwitcher` nella shell.
 */
export function useSetActiveAccount() {
  const client = useApiClient()
  return useMutation({
    mutationFn: (accountId: string) =>
      unwrap(client.POST('/api/platform/v1/me/active-account', { body: { accountId } })),
  })
}

// ── Inviti ricevuti dalla persona in sessione (UC 0118) ─────────────────────

export const MY_INVITATIONS_KEY = ['me', 'invitations'] as const

/**
 * Gli inviti in attesa indirizzati alla persona in sessione (`GET /me/invitations`).
 *
 * <p>Diversa da {@link useInvitations}, che è la lettura dell'**account** («chi ho invitato io», solo
 * owner): questa ha per soggetto la **persona** e attraversa gli account per costruzione. Non è
 * riservata a nessun ruolo — un invito lo può ricevere chiunque.
 *
 * <p>Si rinfresca al ritorno sulla scheda, come le appartenenze: un invito che arriva mentre la
 * scheda è aperta deve poter comparire senza ricaricare.
 */
export function useMyInvitations() {
  const client = useApiClient()
  const status = useAuthStore((s) => s.status)
  return useQuery({
    queryKey: MY_INVITATIONS_KEY,
    enabled: status === 'authenticated',
    refetchOnWindowFocus: true,
    queryFn: () => unwrap(client.GET('/api/platform/v1/me/invitations')),
  })
}

/**
 * Accetta un invito ricevuto (`POST /me/invitations/{id}/accept`). Nasce una **appartenenza** in più,
 * non una seconda identità, e l'account accettato diventa quello attivo: per questo chi chiama
 * **ricarica** l'applicazione, come fa il selettore dell'account (UC 0117) — è il ricaricamento a far
 * nascere il token con il claim nuovo.
 */
export function useAcceptMyInvitation() {
  const client = useApiClient()
  return useMutation({
    mutationFn: (id: string) =>
      unwrap(
        client.POST('/api/platform/v1/me/invitations/{id}/accept', { params: { path: { id } } }),
      ),
  })
}

/** Rifiuta un invito ricevuto (`POST /me/invitations/{id}/reject`): si chiude e il posto si libera. */
export function useRejectMyInvitation() {
  const client = useApiClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) =>
      unwrap(
        client.POST('/api/platform/v1/me/invitations/{id}/reject', { params: { path: { id } } }),
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: MY_INVITATIONS_KEY }),
  })
}

/**
 * Apre un **proprio** account (`POST /me/accounts`, UC 0118 percorso B): serve solo il nome — chi
 * chiama è già una persona conosciuta, e chiedergli di nuovo indirizzo e parola d'accesso è il modo
 * in cui si finisce per crearsi una seconda identità con un altro indirizzo.
 *
 * <p>Il nuovo account diventa quello attivo: chi chiama **ricarica**, come per il cambio di account.
 */
export function useCreateOwnAccount() {
  const client = useApiClient()
  return useMutation({
    mutationFn: (name: string) =>
      unwrap(client.POST('/api/platform/v1/me/accounts', { body: { name } })),
  })
}
