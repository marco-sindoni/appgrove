import { ApiError } from '@appgrove/api-client'

/**
 * Logica **pura** dei banner di enforcement (UC 0028, chiude il punto aperto di UC 0027). Traduce l'esito
 * dei gate backend in un tipo azionabile: **402** (entitlement/stato scaduto → riattiva o esporta dati) e
 * **429** (quota esaurita → upgrade). Il problem+json non porta un `type` distintivo (sempre `about:blank`),
 * quindi si discrimina per **status**. Niente React/rete → testabile a tavolino.
 */
export type EnforcementKind = 'entitlement' | 'quota'

/**
 * Rifiuti **402 che non sono gate di entitlement** e non devono alzare il banner globale.
 *
 * Ce n'è uno, da UC 0103: l'addebito del posto rifiutato al momento dell'invito. Il codice 402 è quello
 * giusto — manca un pagamento, non un permesso — ma il banner sarebbe una bugia utile a nessuno:
 * direbbe «il tuo abbonamento è scaduto, riattivalo o esporta i dati» a un account il cui abbonamento è
 * perfettamente attivo, e nasconderebbe la cosa vera da fare, che è controllare il metodo di pagamento. Il
 * rifiuto si legge **dove è avvenuto**, sul modulo dell'invito, col motivo del fornitore accanto.
 *
 * Si discrimina per `type`, che qui c'è: il servizio lo restituisce come corpo di un rifiuto lecito, non
 * come eccezione (le eccezioni vengono riscritte ad `about:blank` dal mappatore, ed è la ragione per cui
 * l'osservazione «il type è sempre about:blank» valeva quando questa funzione è stata scritta).
 */
const NON_ENFORCEMENT_402 = new Set(['urn:appgrove:seats:charge-declined'])

/** Ricava il tipo di enforcement da un errore del data layer, o `null` se non è un gate 402/429. */
export function enforcementFromError(error: unknown): EnforcementKind | null {
  if (!(error instanceof ApiError)) return null
  if (error.status === 402) {
    return error.problem?.type && NON_ENFORCEMENT_402.has(error.problem.type) ? null : 'entitlement'
  }
  if (error.status === 429) return 'quota'
  return null
}
