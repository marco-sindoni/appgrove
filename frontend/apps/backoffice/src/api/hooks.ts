import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { unwrap } from '@appgrove/api-client'
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
