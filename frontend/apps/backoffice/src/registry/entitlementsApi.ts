import { useQuery } from '@tanstack/react-query'
import { unwrap } from '@appgrove/api-client'
import { useApiClient } from '../api/apiClient'
import { useAuthStore } from '../auth/authStore'

/** Forma minima del read-model `/me/entitlements` consumata dal registry (slug = chiave di entitlement). */
interface MeEntitlements {
  entitlements?: Array<{ appSlug?: string }>
}

/**
 * Deriva l'insieme degli `app_id`/slug entitled dal read-model di core (UC 0027), opzionalmente
 * unendo il modulo **demo** in locale. `demo` non ha catalogo né backend (è dimostrativo): non compare
 * mai in `/me/entitlements`, quindi resta abilitato **solo** nello stub locale (regola "Avvio locale").
 * Funzione pura → testabile senza React.
 */
export function computeEntitled(view: MeEntitlements | undefined, demoInLocal: boolean): string[] {
  const slugs = (view?.entitlements ?? [])
    .map((e) => e.appSlug)
    .filter((s): s is string => typeof s === 'string' && s.length > 0)
  return demoInLocal ? Array.from(new Set(['demo', ...slugs])) : slugs
}

/**
 * Entitlement reali del tenant da `GET /api/platform/v1/me/entitlements`. Abilitata solo a sessione
 * autenticata (il token è propagato dal client). Sostituisce lo stub hardcoded: un'app appena
 * acquistata compare in sidebar appena il read-model la include (invalidando questa query).
 */
export function useMyEntitlements() {
  const client = useApiClient()
  const status = useAuthStore((s) => s.status)
  return useQuery({
    queryKey: ['me', 'entitlements'],
    enabled: status === 'authenticated',
    queryFn: () => unwrap(client.GET('/api/platform/v1/me/entitlements', {})),
  })
}
