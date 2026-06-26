import createClient, { type Client } from 'openapi-fetch'
import type { paths } from './schema'
import { authMiddleware, type AuthMiddlewareConfig } from './auth-middleware'

export interface ApiClientConfig extends AuthMiddlewareConfig {
  /** Base URL dell'API core (es. `https://api.local.appgrove.app`), da `config.json` runtime. */
  baseUrl: string
}

export type ApiClient = Client<paths>

/**
 * Crea il client tipizzato del core con il middleware auth innestato.
 * Consumer-agnostic: base URL, token getter e refresh fn sono **iniettati** dall'app (#12 config runtime).
 */
export function createApiClient(config: ApiClientConfig): ApiClient {
  const client = createClient<paths>({
    baseUrl: config.baseUrl,
    fetch: config.fetch,
  })
  client.use(authMiddleware(config))
  return client
}
