import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { unwrap, type components } from '@appgrove/api-client'
import { useApiClient } from './apiClient'

// Tipi derivati dallo schema OpenAPI del core (admin endpoints, UC 0021). Derivati da `components`
// perché openapi-fetch, su risposte array, può inferire `never`: per questo le query passano il tipo
// esplicito a `unwrap<...>()`.
export type OverviewView = components['schemas']['OverviewView']
export type AdminAccountView = components['schemas']['AdminAccountView']
export type AccountDetailView = components['schemas']['AccountDetailView']
export type AdminUserView = components['schemas']['AdminUserView']
export type EntitlementCell = components['schemas']['EntitlementCell']
export type BillingRow = components['schemas']['BillingRow']
export type AppView = components['schemas']['AppView']

/** KPI di piattaforma (`GET /admin/overview`). */
export function useOverview() {
  const client = useApiClient()
  return useQuery({
    queryKey: ['admin', 'overview'],
    queryFn: () => unwrap<OverviewView>(client.GET('/api/platform/v1/admin/overview')),
  })
}

/** Elenco account (tenant) con conteggi (`GET /admin/accounts`). */
export function useAccounts() {
  const client = useApiClient()
  return useQuery({
    queryKey: ['admin', 'accounts'],
    queryFn: () => unwrap<AdminAccountView[]>(client.GET('/api/platform/v1/admin/accounts')),
  })
}

/** Dettaglio di un account (`GET /admin/accounts/{id}`): anagrafica + utenti + entitlement derivato. */
export function useAccountDetail(id: string) {
  const client = useApiClient()
  return useQuery({
    queryKey: ['admin', 'accounts', id],
    queryFn: () =>
      unwrap<AccountDetailView>(
        client.GET('/api/platform/v1/admin/accounts/{id}', { params: { path: { id } } }),
      ),
    enabled: !!id,
  })
}

/** Elenco utenti cross-tenant (`GET /admin/users`). */
export function useUsers() {
  const client = useApiClient()
  return useQuery({
    queryKey: ['admin', 'users'],
    queryFn: () => unwrap<AdminUserView[]>(client.GET('/api/platform/v1/admin/users')),
  })
}

/** Matrice entitlement tenant×app (`GET /admin/entitlements`). */
export function useEntitlements() {
  const client = useApiClient()
  return useQuery({
    queryKey: ['admin', 'entitlements'],
    queryFn: () => unwrap<EntitlementCell[]>(client.GET('/api/platform/v1/admin/entitlements')),
  })
}

/** Righe di fatturazione cross-tenant (`GET /admin/billing`). */
export function useBilling() {
  const client = useApiClient()
  return useQuery({
    queryKey: ['admin', 'billing'],
    queryFn: () => unwrap<BillingRow[]>(client.GET('/api/platform/v1/admin/billing')),
  })
}

/** Catalogo app di piattaforma (`GET /admin/apps`). */
export function useApps() {
  const client = useApiClient()
  return useQuery({
    queryKey: ['admin', 'apps'],
    queryFn: () => unwrap<AppView[]>(client.GET('/api/platform/v1/admin/apps')),
  })
}

/**
 * Abilita/disabilita un'app (`PATCH /admin/apps/{id}`, danger zone). Invalida il catalogo app e la
 * matrice entitlement (lo stato `appActive` cambia per ogni tenant).
 */
export function useSetAppStatus() {
  const client = useApiClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (vars: { id: string; status: 'active' | 'inactive' }) =>
      unwrap<AppView>(
        client.PATCH('/api/platform/v1/admin/apps/{id}', {
          params: { path: { id: vars.id } },
          body: { status: vars.status },
        }),
      ),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['admin', 'apps'] })
      void queryClient.invalidateQueries({ queryKey: ['admin', 'entitlements'] })
    },
  })
}
