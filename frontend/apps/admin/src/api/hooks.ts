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
// Vocabolario del ticketing (UC 0075): tipo, provenienza, stato e priorità vengono dallo schema,
// così un valore nuovo lato servizio rompe la compilazione qui invece di passare inosservato.
export type TicketType = components['schemas']['TicketType']
export type TicketSource = components['schemas']['TicketSource']
export type TicketStatus = components['schemas']['TicketStatus']
export type TicketPriority = components['schemas']['TicketPriority']
export type ApplyRestriction = components['schemas']['ApplyRestriction']
export type AdminAccountView = components['schemas']['AdminAccountView']
export type AccountDetailView = components['schemas']['AccountDetailView']
export type AdminUserView = components['schemas']['AdminUserView']
export type EntitlementCell = components['schemas']['EntitlementCell']
export type BillingRow = components['schemas']['BillingRow']
export type AppView = components['schemas']['AppView']
export type AppStatusAuditView = components['schemas']['AppStatusAuditView']
export type ReconciliationView = components['schemas']['ReconciliationView']
export type ReconciliationPeriod = components['schemas']['ReconciliationPeriod']
export type ReconciliationTotals = components['schemas']['ReconciliationTotals']
export type PayoutView = components['schemas']['PayoutView']

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

/**
 * Riconciliazione fra ricavo lordo e denaro accreditato (`GET /admin/reconciliation`, UC 0071):
 * totali, righe per mese e accrediti con la loro quadratura.
 */
export function useReconciliation() {
  const client = useApiClient()
  return useQuery({
    queryKey: ['admin', 'reconciliation'],
    queryFn: () =>
      unwrap<ReconciliationView>(client.GET('/api/platform/v1/admin/reconciliation')),
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

/**
 * Coda dei ticket cross-account (`GET /admin/tickets`) — UC 0075. Filtri opzionali per tipo, stato
 * e priorità; l'ordinamento (scadenze più vicine per prime) arriva già fatto dal servizio: la coda
 * dev'essere giusta anche letta dall'API, non solo a schermo.
 */
export function useAdminTickets(filters?: {
  type?: TicketType
  status?: TicketStatus
  priority?: TicketPriority
}) {
  const client = useApiClient()
  return useQuery({
    queryKey: [
      'admin',
      'tickets',
      filters?.type ?? 'all',
      filters?.status ?? 'all',
      filters?.priority ?? 'all',
    ],
    queryFn: () =>
      unwrap<AdminTicketView[]>(
        client.GET('/api/platform/v1/admin/tickets', {
          params: {
            query: {
              ...(filters?.type ? { type: filters.type } : {}),
              ...(filters?.status ? { status: filters.status } : {}),
              ...(filters?.priority ? { priority: filters.priority } : {}),
            },
          },
        }),
      ),
  })
}

/** Dettaglio ticket con filo di conversazione (`GET /admin/tickets/{id}`). */
export function useAdminTicket(id: string) {
  const client = useApiClient()
  return useQuery({
    queryKey: ['admin', 'tickets', 'detail', id],
    enabled: !!id,
    queryFn: () =>
      unwrap<AdminTicketDetailView>(
        client.GET('/api/platform/v1/admin/tickets/{id}', { params: { path: { id } } }),
      ),
  })
}

/** Risposta di chi assiste (`POST /admin/tickets/{id}/messages`): porta in attesa dell'utente. */
export function useAdminReplyTicket() {
  const client = useApiClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (vars: { id: string; body: string }) =>
      unwrap<AdminMessageView>(
        client.POST('/api/platform/v1/admin/tickets/{id}/messages', {
          params: { path: { id: vars.id } },
          body: { body: vars.body },
        }),
      ),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['admin', 'tickets'] })
      void queryClient.invalidateQueries({ queryKey: ['admin', 'gdpr'] })
    },
  })
}

/** Cambio stato/priorità (`PATCH /admin/tickets/{id}` — ops sicure, mai il contenuto). */
export function useUpdateAdminTicket() {
  const client = useApiClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (vars: { id: string } & UpdateTicket) =>
      unwrap<AdminTicketView>(
        client.PATCH('/api/platform/v1/admin/tickets/{id}', {
          params: { path: { id: vars.id } },
          body: { status: vars.status, priority: vars.priority },
        }),
      ),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['admin', 'tickets'] })
      void queryClient.invalidateQueries({ queryKey: ['admin', 'gdpr'] })
    },
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
 * Disabilita/riabilita un'app (`PATCH /admin/apps/{id}`, danger zone — UC 0076). La `reason` è la
 * motivazione facoltativa dell'operatore, che finisce nel registro. Invalida il catalogo app, il
 * registro e la matrice entitlement (lo stato `appActive` cambia per ogni tenant).
 */
export function useSetAppStatus() {
  const client = useApiClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (vars: { id: string; status: 'active' | 'inactive'; reason?: string }) =>
      unwrap<AppView>(
        client.PATCH('/api/platform/v1/admin/apps/{id}', {
          params: { path: { id: vars.id } },
          body: { status: vars.status, ...(vars.reason ? { reason: vars.reason } : {}) },
        }),
      ),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['admin', 'apps'] })
      void queryClient.invalidateQueries({ queryKey: ['admin', 'apps-audit'] })
      void queryClient.invalidateQueries({ queryKey: ['admin', 'entitlements'] })
    },
  })
}

/** Registro delle disabilitazioni/riabilitazioni (`GET /admin/apps/audit`, UC 0076), più recenti prima. */
export function useAppStatusAudit() {
  const client = useApiClient()
  return useQuery({
    queryKey: ['admin', 'apps-audit'],
    queryFn: () =>
      unwrap<AppStatusAuditView[]>(client.GET('/api/platform/v1/admin/apps/audit')),
  })
}
