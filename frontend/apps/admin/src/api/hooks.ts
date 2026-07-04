import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { unwrap, type components } from '@appgrove/api-client'
import { useApiClient } from './apiClient'

// Tipi derivati dallo schema OpenAPI del core (admin endpoints, UC 0021). Derivati da `components`
// perché openapi-fetch, su risposte array, può inferire `never`: per questo le query passano il tipo
// esplicito a `unwrap<...>()`.
export type OverviewView = components['schemas']['OverviewView']
export type GdprRequestView = components['schemas']['RequestView']
export type GdprExportDetailView = components['schemas']['ExportDetailView']
export type AdminTicketView = components['schemas']['AdminTicketView']
export type AdminTicketDetailView = components['schemas']['AdminTicketDetailView']
export type AdminMessageView = components['schemas']['AdminMessageView']
export type RestrictionsView = components['schemas']['RestrictionsView']
export type PurgeAuditView = components['schemas']['PurgeAuditView']
export type UpdateTicket = components['schemas']['UpdateTicket']
export type ApplyRestriction = components['schemas']['ApplyRestriction']
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

// ── Console "Diritti GDPR" (UC 0034, #13 L75) ────────────────────────────────

/** Tabella aggregata delle richieste diritti (`GET /admin/gdpr/requests`), filtro tipo opzionale. */
export function useGdprRequests(type?: string) {
  const client = useApiClient()
  return useQuery({
    queryKey: ['admin', 'gdpr', 'requests', type ?? 'all'],
    queryFn: () =>
      unwrap<GdprRequestView[]>(
        client.GET('/api/platform/v1/admin/gdpr/requests', {
          params: { query: type ? { type } : {} },
        }),
      ),
  })
}

/** Dettaglio export: item per-servizio + puntatore S3 (`GET /admin/gdpr/exports/{id}`). */
export function useGdprExportDetail(id: string) {
  const client = useApiClient()
  return useQuery({
    queryKey: ['admin', 'gdpr', 'exports', id],
    enabled: !!id,
    queryFn: () =>
      unwrap<GdprExportDetailView>(
        client.GET('/api/platform/v1/admin/gdpr/exports/{id}', { params: { path: { id } } }),
      ),
  })
}

/** Ticket cross-tenant (`GET /admin/gdpr/tickets`), filtri opzionali tipo/stato. */
export function useAdminTickets(filters?: {
  type?: 'support' | 'privacy'
  status?: 'open' | 'in_progress' | 'resolved' | 'closed'
}) {
  const client = useApiClient()
  return useQuery({
    queryKey: ['admin', 'gdpr', 'tickets', filters?.type ?? 'all', filters?.status ?? 'all'],
    queryFn: () =>
      unwrap<AdminTicketView[]>(
        client.GET('/api/platform/v1/admin/gdpr/tickets', {
          params: {
            query: {
              ...(filters?.type ? { type: filters.type } : {}),
              ...(filters?.status ? { status: filters.status } : {}),
            },
          },
        }),
      ),
  })
}

/** Dettaglio ticket con thread (`GET /admin/gdpr/tickets/{id}`). */
export function useAdminTicket(id: string) {
  const client = useApiClient()
  return useQuery({
    queryKey: ['admin', 'gdpr', 'tickets', 'detail', id],
    enabled: !!id,
    queryFn: () =>
      unwrap<AdminTicketDetailView>(
        client.GET('/api/platform/v1/admin/gdpr/tickets/{id}', { params: { path: { id } } }),
      ),
  })
}

/** Risposta dell'admin nel thread (`POST /admin/gdpr/tickets/{id}/messages`). */
export function useAdminReplyTicket() {
  const client = useApiClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (vars: { id: string; body: string }) =>
      unwrap<AdminMessageView>(
        client.POST('/api/platform/v1/admin/gdpr/tickets/{id}/messages', {
          params: { path: { id: vars.id } },
          body: { body: vars.body },
        }),
      ),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['admin', 'gdpr'] }),
  })
}

/** Cambio stato/priorità del ticket (`PATCH /admin/gdpr/tickets/{id}` — ops sicure, mai il contenuto). */
export function useUpdateAdminTicket() {
  const client = useApiClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (vars: { id: string } & UpdateTicket) =>
      unwrap<AdminTicketView>(
        client.PATCH('/api/platform/v1/admin/gdpr/tickets/{id}', {
          params: { path: { id: vars.id } },
          body: { status: vars.status, priority: vars.priority },
        }),
      ),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['admin', 'gdpr'] }),
  })
}

/** Limitazioni art. 18 attive + registro prove (`GET /admin/gdpr/restrictions`). */
export function useRestrictions() {
  const client = useApiClient()
  return useQuery({
    queryKey: ['admin', 'gdpr', 'restrictions'],
    queryFn: () =>
      unwrap<RestrictionsView>(client.GET('/api/platform/v1/admin/gdpr/restrictions')),
  })
}

/** Applica la limitazione art. 18 (`POST /admin/gdpr/restrictions`). */
export function useApplyRestriction() {
  const client = useApiClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (vars: ApplyRestriction) =>
      unwrap(client.POST('/api/platform/v1/admin/gdpr/restrictions', { body: vars })),
    onSuccess: () =>
      void queryClient.invalidateQueries({ queryKey: ['admin', 'gdpr', 'restrictions'] }),
  })
}

/** Rimuove la limitazione art. 18 (`DELETE /admin/gdpr/restrictions/{kind}/{id}`). */
export function useRemoveRestriction() {
  const client = useApiClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (vars: { targetKind: string; targetId: string }) =>
      unwrap(
        client.DELETE('/api/platform/v1/admin/gdpr/restrictions/{targetKind}/{targetId}', {
          params: { path: { targetKind: vars.targetKind, targetId: vars.targetId } },
        }),
      ),
    onSuccess: () =>
      void queryClient.invalidateQueries({ queryKey: ['admin', 'gdpr', 'restrictions'] }),
  })
}

/** Registro prove di erasure (`GET /admin/gdpr/purge-audit`). */
export function usePurgeAudit() {
  const client = useApiClient()
  return useQuery({
    queryKey: ['admin', 'gdpr', 'purge-audit'],
    queryFn: () =>
      unwrap<PurgeAuditView[]>(client.GET('/api/platform/v1/admin/gdpr/purge-audit')),
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
