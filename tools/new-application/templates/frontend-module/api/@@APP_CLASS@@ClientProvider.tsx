import { createContext, useContext, useMemo, type ReactNode } from 'react'
import { buildAuthClientConfig } from '../../../api/apiClient'
import { useConfig } from '../../../config'
import { create@@APP_CLASS@@Client, type @@APP_CLASS@@Client } from './client'

const @@APP_CLASS@@ClientContext = createContext<@@APP_CLASS@@Client | null>(null)

/**
 * Fornisce il client @@APP_ID@@ ai componenti del modulo. Costruito dalla config runtime + auth
 * condivisa della shell (host): sull'estrazione a microfrontend basterà passare un `client` esterno.
 */
export function @@APP_CLASS@@ClientProvider({
  children,
  client,
}: {
  children: ReactNode
  client?: @@APP_CLASS@@Client
}) {
  const config = useConfig()
  const value = useMemo(
    () => client ?? create@@APP_CLASS@@Client(buildAuthClientConfig(config)),
    [client, config],
  )
  return <@@APP_CLASS@@ClientContext.Provider value={value}>{children}</@@APP_CLASS@@ClientContext.Provider>
}

export function use@@APP_CLASS@@Client(): @@APP_CLASS@@Client {
  const client = useContext(@@APP_CLASS@@ClientContext)
  if (!client) {
    throw new Error('use@@APP_CLASS@@Client deve essere usato dentro <@@APP_CLASS@@ClientProvider>')
  }
  return client
}
