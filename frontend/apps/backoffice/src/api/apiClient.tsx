import { createContext, useContext, useMemo, type ReactNode } from 'react'
import { createApiClient, type ApiClient, type ApiClientConfig } from '@appgrove/api-client'
import { useAuthStore, getAccessToken } from '../auth/authStore'
import { refreshSession } from '../auth/authApi'
import { useConfig, type RuntimeConfig } from '../config'

/**
 * Config auth condivisa (Bearer + 401→refresh→retry + logout su fallimento) cablata sull'auth store:
 * - `getAccessToken` legge il token in memoria;
 * - `refresh` chiama `/api/auth/refresh` e aggiorna lo store;
 * - `onAuthFailure` azzera la sessione (logout UX).
 *
 * Riusata sia dal client del **core** sia dai client **per-app** (es. fatture, UC 0052) via
 * `createTypedClient`: stesso origin, stessa meccanica auth, solo `paths` diversi.
 */
export function buildAuthClientConfig(config: RuntimeConfig): ApiClientConfig {
  return {
    baseUrl: config.coreBaseUrl,
    getAccessToken,
    refresh: async () => {
      const tokens = await refreshSession(config.authBaseUrl)
      if (!tokens) {
        useAuthStore.getState().clear()
        return false
      }
      useAuthStore.getState().setSession(tokens)
      return true
    },
    onAuthFailure: () => useAuthStore.getState().clear(),
  }
}

/** Costruisce il client tipizzato del core a partire dalla config auth condivisa. */
export function buildApiClient(config: RuntimeConfig): ApiClient {
  return createApiClient(buildAuthClientConfig(config))
}

const ApiClientContext = createContext<ApiClient | null>(null)

export function ApiClientProvider({
  children,
  client,
}: {
  children: ReactNode
  client?: ApiClient
}) {
  const config = useConfig()
  const value = useMemo(() => client ?? buildApiClient(config), [client, config])
  return <ApiClientContext.Provider value={value}>{children}</ApiClientContext.Provider>
}

export function useApiClient(): ApiClient {
  const client = useContext(ApiClientContext)
  if (!client) throw new Error('useApiClient deve essere usato dentro <ApiClientProvider>')
  return client
}
