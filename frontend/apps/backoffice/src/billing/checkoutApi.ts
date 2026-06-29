import { useMutation, useQuery } from '@tanstack/react-query'
import { unwrap } from '@appgrove/api-client'
import { useApiClient } from '../api/apiClient'
import { POLL_INTERVAL_MS } from './checkoutMachine'

/** Catalogo lato cliente: tier + prezzi per ciclo di un'app (`GET /checkout/apps/{slug}/tiers`). */
export function useAppTiers(appSlug: string | null) {
  const client = useApiClient()
  return useQuery({
    queryKey: ['checkout', 'tiers', appSlug],
    enabled: !!appSlug,
    queryFn: () =>
      unwrap(
        client.GET('/api/platform/v1/checkout/apps/{appSlug}/tiers', {
          params: { path: { appSlug: appSlug! } },
        }),
      ),
  })
}

/** Avvia il checkout server-initiated (`POST /checkout/apps/{slug}`) → `{ checkoutToken }`. OWNER-only. */
export function useStartCheckout(appSlug: string) {
  const client = useApiClient()
  return useMutation({
    mutationFn: (vars: { tierKey: string; billingCycle: string }) =>
      unwrap(
        client.POST('/api/platform/v1/checkout/apps/{appSlug}', {
          params: { path: { appSlug } },
          body: vars,
        }),
      ),
  })
}

/**
 * Polling dello stato subscription (`GET /checkout/apps/{slug}/subscription`) — abilitato solo durante
 * l'attivazione; si ferma da sé quando `active` (#09 C17).
 */
export function useAppSubscriptionStatus(appSlug: string, enabled: boolean) {
  const client = useApiClient()
  return useQuery({
    queryKey: ['checkout', 'subscription', appSlug],
    enabled,
    refetchInterval: (query) => (query.state.data?.active ? false : POLL_INTERVAL_MS),
    queryFn: () =>
      unwrap(
        client.GET('/api/platform/v1/checkout/apps/{appSlug}/subscription', {
          params: { path: { appSlug } },
        }),
      ),
  })
}
