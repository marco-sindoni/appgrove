import { createContext, useContext, useMemo, type ReactNode } from 'react'
import { buildAuthClientConfig } from '../../../api/apiClient'
import { useConfig } from '../../../config'
import { createFattureClient, type FattureClient } from './client'

const FattureClientContext = createContext<FattureClient | null>(null)

/**
 * Fornisce il client fatture ai componenti del modulo. Costruito dalla config runtime + auth condivisa
 * della shell (host): sull'estrazione a microfrontend basterà passare un `client` esterno.
 */
export function FattureClientProvider({
  children,
  client,
}: {
  children: ReactNode
  client?: FattureClient
}) {
  const config = useConfig()
  const value = useMemo(
    () => client ?? createFattureClient(buildAuthClientConfig(config)),
    [client, config],
  )
  return <FattureClientContext.Provider value={value}>{children}</FattureClientContext.Provider>
}

export function useFattureClient(): FattureClient {
  const client = useContext(FattureClientContext)
  if (!client) throw new Error('useFattureClient deve essere usato dentro <FattureClientProvider>')
  return client
}
