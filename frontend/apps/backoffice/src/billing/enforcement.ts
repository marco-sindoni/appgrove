import { ApiError } from '@appgrove/api-client'

/**
 * Logica **pura** dei banner di enforcement (UC 0028, chiude il punto aperto di UC 0027). Traduce l'esito
 * dei gate backend in un tipo azionabile: **402** (entitlement/stato scaduto → riattiva o esporta dati) e
 * **429** (quota esaurita → upgrade). Il problem+json non porta un `type` distintivo (sempre `about:blank`),
 * quindi si discrimina per **status**. Niente React/rete → testabile a tavolino.
 */
export type EnforcementKind = 'entitlement' | 'quota'

/** Ricava il tipo di enforcement da un errore del data layer, o `null` se non è un gate 402/429. */
export function enforcementFromError(error: unknown): EnforcementKind | null {
  if (!(error instanceof ApiError)) return null
  if (error.status === 402) return 'entitlement'
  if (error.status === 429) return 'quota'
  return null
}
