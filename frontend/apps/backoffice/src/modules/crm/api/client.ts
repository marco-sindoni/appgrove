import { createTypedClient, type ApiClientConfig } from '@appgrove/api-client'
import type { paths, components } from './schema'

/**
 * Client tipizzato dell'app **Mini-CRM**: stesso origin/auth del core, `paths` generati
 * dall'OpenAPI del servizio (`npm run gen:crm`). Co-locato nel modulo per restare
 * autocontenuto (l'estrazione a microfrontend tocca solo la entry del registry, #01 dec.11).
 */
export function createCrmClient(config: ApiClientConfig) {
  return createTypedClient<paths>(config)
}

export type CrmClient = ReturnType<typeof createCrmClient>

// Alias di comodo verso i tipi generati (drift-guard: se lo spec cambia, `tsc` rompe la build, #10 G25).
export type ContactView = components['schemas']['ContactView']
export type CreateContact = components['schemas']['CreateContact']
export type UpdateContact = components['schemas']['UpdateContact']
export type InteractionView = components['schemas']['InteractionView']
export type CreateInteraction = components['schemas']['CreateInteraction']
export type SeatView = components['schemas']['SeatView']
export type SeatSummary = components['schemas']['SeatSummary']
export type QuotaStatusView = components['schemas']['QuotaStatusView']
