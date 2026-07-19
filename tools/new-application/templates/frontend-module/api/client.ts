import { createTypedClient, type ApiClientConfig } from '@appgrove/api-client'
import type { paths, components } from './schema'

/**
 * Client tipizzato dell'app **@@APP_NAME@@**: stesso origin/auth del core, `paths` generati
 * dall'OpenAPI del servizio (`npm run gen:@@APP_ID@@`). Co-locato nel modulo per restare
 * autocontenuto (l'estrazione a microfrontend tocca solo la entry del registry, #01 dec.11).
 */
export function create@@APP_CLASS@@Client(config: ApiClientConfig) {
  return createTypedClient<paths>(config)
}

export type @@APP_CLASS@@Client = ReturnType<typeof create@@APP_CLASS@@Client>

// Alias di comodo verso i tipi generati (drift-guard: se lo spec cambia, `tsc` rompe la build, #10 G25).
export type ItemView = components['schemas']['ItemView']
export type LineView = components['schemas']['LineView']
export type CreateItem = components['schemas']['CreateItem']
export type UpdateItem = components['schemas']['UpdateItem']
export type QuotaStatusView = components['schemas']['QuotaStatusView']
