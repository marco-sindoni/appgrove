import createClient, { type Client } from 'openapi-fetch'
import type { paths } from './schema'
import { authMiddleware, type AuthMiddlewareConfig } from './auth-middleware'

export interface ApiClientConfig extends AuthMiddlewareConfig {
  /** Base URL dell'API core (es. `https://api.local.appgrove.app`), da `config.json` runtime. */
  baseUrl: string
}

export type ApiClient = Client<paths>

/**
 * Crea un client `openapi-fetch` tipizzato su uno spec **arbitrario** (`P` = `paths` generati
 * dall'OpenAPI di un servizio) con lo stesso middleware auth (Bearer + 401→refresh→retry). È il
 * building block riusato da ogni app del marketplace per parlare col **proprio** backend (es. modulo
 * fatture, UC 0052), tenendo i tipi co-locati nel modulo senza accoppiare questo pacchetto alle app.
 */
export function createTypedClient<P extends {}>(config: ApiClientConfig): Client<P> {
  const client = createClient<P>({
    baseUrl: config.baseUrl,
    fetch: config.fetch,
  })
  client.use(authMiddleware(config))
  return client
}

/**
 * Crea il client tipizzato del **core** con il middleware auth innestato.
 * Consumer-agnostic: base URL, token getter e refresh fn sono **iniettati** dall'app (#12 config runtime).
 */
export function createApiClient(config: ApiClientConfig): ApiClient {
  return createTypedClient<paths>(config)
}
