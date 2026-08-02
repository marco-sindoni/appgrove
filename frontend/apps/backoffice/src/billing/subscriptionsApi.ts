import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { unwrap } from '@appgrove/api-client'
import { useApiClient } from '../api/apiClient'
import { useAuthStore } from '../auth/authStore'
import { useInvalidateEntitlements } from '../registry/entitlementsApi'
import { PENDING_POLL_MS } from './subscriptionsView'

const KEY = ['me', 'subscriptions']

/**
 * Read-model dedicato del portale cliente (UC 0028): `GET /me/subscriptions` — tutte le subscription del
 * tenant, anche non-attive (distinto da `/me/entitlements`). Abilitato solo a sessione autenticata.
 *
 * `polling` serve alla riconciliazione (UC 0067): dopo un comando la riga non è ancora aggiornata — la
 * scrive il webhook — quindi si rilegge a intervalli brevi finché il cambiamento non compare, e si smette
 * subito dopo. Fuori da quella finestra la lista non fa alcun polling.
 */
export function useMySubscriptions(polling = false) {
  const client = useApiClient()
  const status = useAuthStore((s) => s.status)
  return useQuery({
    queryKey: KEY,
    enabled: status === 'authenticated',
    refetchInterval: polling ? PENDING_POLL_MS : false,
    queryFn: () => unwrap(client.GET('/api/platform/v1/me/subscriptions', {})),
  })
}

/** Vero se il ruolo in sessione può agire sulla fatturazione (solo il titolare dell'account). */
export function useCanManageBilling() {
  return useAuthStore((s) => !!s.claims?.roles?.some((r) => r === 'owner'))
}

/** Cambio piano (upgrade immediato / downgrade schedulato — deciso server-side). OWNER-only. */
export function useChangeTier(appSlug: string) {
  const client = useApiClient()
  const qc = useQueryClient()
  // Cambio piano/disdetta/ripresa cambiano ciò a cui il tenant ha diritto: senza rileggere gli
  // entitlement il menu resterebbe quello di prima fino a un ricaricamento della pagina (UC 0077).
  const invalidateEntitlements = useInvalidateEntitlements()
  return useMutation({
    mutationFn: (vars: { targetTierKey: string; billingCycle: string }) =>
      unwrap(
        client.POST('/api/platform/v1/me/subscriptions/{appSlug}/change-tier', {
          params: { path: { appSlug } },
          body: vars,
        }),
      ),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: KEY })
      void invalidateEntitlements()
    },
  })
}

/** Disdetta a fine periodo. OWNER-only. */
export function useCancelSubscription(appSlug: string) {
  const client = useApiClient()
  const qc = useQueryClient()
  const invalidateEntitlements = useInvalidateEntitlements()
  return useMutation({
    mutationFn: () =>
      unwrap(
        client.POST('/api/platform/v1/me/subscriptions/{appSlug}/cancel', {
          params: { path: { appSlug } },
        }),
      ),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: KEY })
      void invalidateEntitlements()
    },
  })
}

/** Annulla una disdetta programmata. OWNER-only. */
export function useResumeSubscription(appSlug: string) {
  const client = useApiClient()
  const qc = useQueryClient()
  const invalidateEntitlements = useInvalidateEntitlements()
  return useMutation({
    mutationFn: () =>
      unwrap(
        client.POST('/api/platform/v1/me/subscriptions/{appSlug}/resume', {
          params: { path: { appSlug } },
        }),
      ),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: KEY })
      void invalidateEntitlements()
    },
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
