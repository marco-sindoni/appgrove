// Mapping degli errori HTTP del backend in errori tipizzati lato client.
// Il backend appgrove emette errori come `application/problem+json` (RFC 9457, #03 dec.5).

/** Corpo di un errore RFC 9457 (`application/problem+json`). */
export interface ProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  /** Campi di estensione specifici (es. `errors` di validazione). */
  [key: string]: unknown
}

/** Errore tipizzato sollevato dal data layer quando la richiesta fallisce. */
export class ApiError extends Error {
  readonly status: number
  readonly problem: ProblemDetail | null

  constructor(status: number, problem: ProblemDetail | null, message?: string) {
    super(message ?? problem?.detail ?? problem?.title ?? `HTTP ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }
}

/**
 * Costruisce un {@link ApiError} da una Response non-ok, leggendo il problem+json se presente.
 *
 * `parsedBody` è il corpo che il chiamante ha **già** letto, quando l'ha letto: `openapi-fetch`
 * consuma il corpo della risposta d'errore prima di restituirla, e dopo di lui `response.clone()`
 * non è più possibile. Senza questo parametro il campo `problem` risultava `null` proprio nelle
 * chiamate passate dal client generato — cioè quasi tutte — e ogni informazione dell'errore oltre al
 * codice di stato andava perduta in silenzio.
 */
export async function toApiError(response: Response, parsedBody?: unknown): Promise<ApiError> {
  let problem: ProblemDetail | null = null
  const contentType = response.headers.get('content-type') ?? ''
  if (contentType.includes('json')) {
    try {
      problem = (await response.clone().json()) as ProblemDetail
    } catch {
      problem = null
    }
  }
  if (problem === null && parsedBody !== null && typeof parsedBody === 'object') {
    problem = parsedBody as ProblemDetail
  }
  return new ApiError(response.status, problem)
}

/** Risultato di una chiamata `openapi-fetch`. */
export interface FetchResult<T> {
  data?: T
  error?: unknown
  response: Response
}

/**
 * Estrae `data` da un risultato `openapi-fetch`, sollevando {@link ApiError} su risposta non-ok.
 * Pensato per essere usato dentro le query/mutation di TanStack Query.
 */
export async function unwrap<T>(promise: Promise<FetchResult<T>>): Promise<T> {
  const { data, error, response } = await promise
  // `error` è il corpo che openapi-fetch ha già letto: passarlo è l'unico modo perché `problem`
  // arrivi valorizzato a chi deve distinguere DUE rifiuti con lo stesso codice di stato.
  if (!response.ok) throw await toApiError(response, error)
  return data as T
}
