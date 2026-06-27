import { createTypedClient, type ApiClientConfig } from '@appgrove/api-client'
import type { paths, components } from './schema'

/**
 * Client tipizzato dell'app **fatture** (UC 0052): stesso origin/auth del core, `paths` generati
 * dall'OpenAPI del servizio fatture (`gen:fatture`). Co-locato nel modulo per restare autocontenuto
 * (l'estrazione a microfrontend tocca solo la entry del registry, #01 dec.11).
 */
export function createFattureClient(config: ApiClientConfig) {
  return createTypedClient<paths>(config)
}

export type FattureClient = ReturnType<typeof createFattureClient>

// Alias di comodo verso i tipi generati (drift-guard: se lo spec cambia, `tsc` rompe la build, #10 G25).
export type InvoiceView = components['schemas']['InvoiceView']
export type LineView = components['schemas']['LineView']
export type CreateInvoice = components['schemas']['CreateInvoice']
export type UpdateInvoice = components['schemas']['UpdateInvoice']
export type QuotaStatusView = components['schemas']['QuotaStatusView']
