import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { unwrap, type InvitationView } from '@appgrove/api-client'
import { useApiClient } from './apiClient'

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

// ── Membri & inviti (UC 0059) — tutti gli endpoint sono owner/admin lato core ────────────────

/** Lista membri del tenant (`GET /users`). Una pagina ampia: la UI non pagina (fuori scope). */
export function useMembers() {
  const client = useApiClient()
  return useQuery({
    queryKey: ['users', 'list'],
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

/** Inviti in sospeso del tenant (`GET /invitations`). */
export function useInvitations() {
  const client = useApiClient()
  return useQuery({
    queryKey: ['invitations', 'list'],
    queryFn: () =>
      unwrap(client.GET('/api/platform/v1/invitations', { params: { query: { size: 100 } } })),
  })
}

/** Crea un invito (`POST /invitations`) → `InvitationView` col **token grezzo** (solo qui). */
export function useCreateInvitation() {
  const client = useApiClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (vars: { email: string; role: string }) =>
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
