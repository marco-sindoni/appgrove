import { createContext, useContext, useMemo, type ReactNode } from 'react'
import { buildAuthClientConfig } from '../../../api/apiClient'
import { useConfig } from '../../../config'
import { createCrmClient, type CrmClient } from './client'

const CrmClientContext = createContext<CrmClient | null>(null)

/**
 * Fornisce il client crm ai componenti del modulo. Costruito dalla config runtime + auth
 * condivisa della shell (host): sull'estrazione a microfrontend basterà passare un `client` esterno.
 */
export function CrmClientProvider({
  children,
  client,
}: {
  children: ReactNode
  client?: CrmClient
}) {
  const config = useConfig()
  const value = useMemo(
    () => client ?? createCrmClient(buildAuthClientConfig(config)),
    [client, config],
  )
  return <CrmClientContext.Provider value={value}>{children}</CrmClientContext.Provider>
}

export function useCrmClient(): CrmClient {
  const client = useContext(CrmClientContext)
  if (!client) {
    throw new Error('useCrmClient deve essere usato dentro <CrmClientProvider>')
  }
  return client
}
