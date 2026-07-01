import { MutationCache, QueryCache, QueryClient } from '@tanstack/react-query'
import { enforcementFromError } from '../billing/enforcement'
import { useEnforcementStore } from '../billing/enforcementStore'

/** Intercetta i gate 402/429 (UC 0028) e alza il banner globale di enforcement. */
function reportEnforcement(error: unknown): void {
  const kind = enforcementFromError(error)
  if (kind) useEnforcementStore.getState().notify(kind)
}

/**
 * QueryClient dell'app: errori 4xx (es. 401 dopo refresh fallito, 402 entitlement) non si ritentano. Gli
 * esiti di enforcement (402/429) alimentano il banner azionabile globale via cache `onError` (UC 0028).
 */
export function makeQueryClient(): QueryClient {
  return new QueryClient({
    queryCache: new QueryCache({ onError: reportEnforcement }),
    mutationCache: new MutationCache({ onError: reportEnforcement }),
    defaultOptions: {
      queries: { retry: 1, refetchOnWindowFocus: false },
      mutations: { retry: 0 },
    },
  })
}
