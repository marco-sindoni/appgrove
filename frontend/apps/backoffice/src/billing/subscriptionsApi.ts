import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { unwrap } from '@appgrove/api-client'
import { useApiClient } from '../api/apiClient'
import { useAuthStore } from '../auth/authStore'

const KEY = ['me', 'subscriptions']

/**
 * Read-model dedicato del portale cliente (UC 0028): `GET /me/subscriptions` — tutte le subscription del
 * tenant, anche non-attive (distinto da `/me/entitlements`). Abilitato solo a sessione autenticata.
 */
export function useMySubscriptions() {
  const client = useApiClient()
  const status = useAuthStore((s) => s.status)
  return useQuery({
    queryKey: KEY,
    enabled: status === 'authenticated',
    queryFn: () => unwrap(client.GET('/api/platform/v1/me/subscriptions', {})),
  })
}

/** Cambio piano (upgrade immediato / downgrade schedulato — deciso server-side). OWNER-only. */
export function useChangeTier(appSlug: string) {
  const client = useApiClient()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { targetTierKey: string; billingCycle: string }) =>
      unwrap(
        client.POST('/api/platform/v1/me/subscriptions/{appSlug}/change-tier', {
          params: { path: { appSlug } },
          body: vars,
        }),
      ),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  })
}

/** Disdetta a fine periodo. OWNER-only. */
export function useCancelSubscription(appSlug: string) {
  const client = useApiClient()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () =>
      unwrap(
        client.POST('/api/platform/v1/me/subscriptions/{appSlug}/cancel', {
          params: { path: { appSlug } },
        }),
      ),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  })
}

/** Annulla una disdetta programmata. OWNER-only. */
export function useResumeSubscription(appSlug: string) {
  const client = useApiClient()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () =>
      unwrap(
        client.POST('/api/platform/v1/me/subscriptions/{appSlug}/resume', {
          params: { path: { appSlug } },
        }),
      ),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  })
}

/**
 * Genera la sessione Customer Portal Paddle server-side (`POST /me/portal-session`) → `{ url }`. OWNER-only.
 * L'apertura dell'URL è del componente (nuova scheda). Delega metodo di pagamento (PCI) + fatture (MoR).
 */
export function usePortalSession() {
  const client = useApiClient()
  return useMutation({
    mutationFn: () => unwrap(client.POST('/api/platform/v1/me/portal-session', {})),
  })
}
