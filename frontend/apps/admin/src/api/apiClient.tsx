import { createContext, useContext, useMemo, type ReactNode } from 'react'
import { createApiClient, type ApiClient } from '@appgrove/api-client'
import { useAuthStore, getAccessToken } from '../auth/authStore'
import { refreshSession } from '../auth/authApi'
import { useConfig, type RuntimeConfig } from '../config'

/**
 * Costruisce il client del core cablato sull'auth store:
 * - `getAccessToken` legge il token in memoria;
 * - `refresh` chiama `/api/auth/refresh` e aggiorna lo store (per l'interceptor 401→refresh→retry);
 * - `onAuthFailure` azzera la sessione (logout UX).
 */
export function buildApiClient(config: RuntimeConfig): ApiClient {
  return createApiClient({
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
  })
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
