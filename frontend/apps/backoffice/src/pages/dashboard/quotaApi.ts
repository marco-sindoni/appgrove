import { useQuery } from '@tanstack/react-query'
import { toApiError } from '@appgrove/api-client'
import { buildAuthClientConfig } from '../../api/apiClient'
import { useAuthStore } from '../../auth/authStore'
import { useConfig } from '../../config'
import type { ModuleQuota } from '../../registry/types'

/** Stato di quota così come lo servono i servizi delle app (UC 0027): `limit`/`remaining` nulli = illimitato. */
export interface AppQuotaStatus {
  metric?: string
  used?: number
  limit?: number | null
  remaining?: number | null
}

/**
 * Consumo di quota di **una** app, letto dal servizio dell'app stessa (`GET /api/<app>/v1/quota`).
 *
 * <p>Non passa dal client tipizzato del core perché il percorso è dichiarato dal manifest del modulo e
 * varia da app ad app: qui serve una richiesta generica. Riusa però la stessa configurazione di
 * autenticazione del resto della shell — token in memoria e, su 401, un solo rinnovo della sessione e
 * un solo nuovo tentativo — così questa lettura non è un secondo modo di parlare col backend.
 *
 * <p>Ogni card ha la **sua** lettura: un'app che risponde male non deve poter spegnere la barra delle
 * altre, e questo è precisamente ciò che UC 0097 §5 chiede.
 */
export function useAppQuota(appId: string, quota: ModuleQuota | undefined) {
  const config = useConfig()
  const authenticated = useAuthStore((s) => s.status === 'authenticated')

  return useQuery({
    queryKey: ['app-quota', appId, quota?.path],
    enabled: authenticated && !!quota,
    // Un diniego per ruolo (il servizio dell'app può riservare la lettura a owner/admin) non è un
    // guasto passeggero: insistere non cambierebbe la risposta e riempirebbe la rete di rumore.
    retry: false,
    queryFn: async (): Promise<AppQuotaStatus> => {
      const clientConfig = buildAuthClientConfig(config)
      const call = () => {
        const token = clientConfig.getAccessToken()
        return fetch(`${config.coreBaseUrl}${quota!.path}`, {
          headers: token ? { Authorization: `Bearer ${token}` } : undefined,
        })
      }
      let res = await call()
      if (res.status === 401 && (await clientConfig.refresh())) res = await call()
      if (!res.ok) throw await toApiError(res)
      return (await res.json()) as AppQuotaStatus
    },
  })
}
