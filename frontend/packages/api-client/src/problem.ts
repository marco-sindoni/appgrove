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

/** Costruisce un {@link ApiError} da una Response non-ok, leggendo il problem+json se presente. */
export async function toApiError(response: Response): Promise<ApiError> {
  let problem: ProblemDetail | null = null
  const contentType = response.headers.get('content-type') ?? ''
  if (contentType.includes('json')) {
    try {
      problem = (await response.clone().json()) as ProblemDetail
    } catch {
      problem = null
    }
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
  const { data, response } = await promise
  if (!response.ok) throw await toApiError(response)
  return data as T
}
