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

/**
 * Codici di stato con cui il server **parla alla persona**: il loro `detail` è un messaggio scritto per
 * essere letto, già nella lingua di chi chiede, e va mostrato tale e quale.
 *
 * Tutto il resto — e in particolare i `5xx` — resta dietro il messaggio generico: là il `detail`, quando
 * c'è, descrive un guasto interno e non serve a chi guarda.
 */
const SPEAKING_STATUSES = new Set([400, 402, 403, 409, 422, 429])

/**
 * Messaggio da mostrare per un rifiuto, con ripiego sul generico (UC 0099, promosso qui da UC 0101).
 *
 * Serve perché l'autorizzazione per applicazione poggia interamente su rifiuti che si **distinguono a
 * parole**: «non hai accesso a questa applicazione, chiedi l'abilitazione» e «serve almeno `editor`, il tuo
 * ruolo è `viewer`» sono due `403` con lo stesso codice e due significati diversi, e la seconda frase è
 * l'unica che dice alla persona cosa può fare. Un `catch` che scrive «si è verificato un errore» butta via
 * esattamente l'informazione per cui quei messaggi sono stati scritti.
 *
 * <p>È nato dentro il modulo del Mini-CRM, quando era la sola applicazione col varco del ruolo, con scritto
 * nel suo commento che si sarebbe promosso qui appena l'avesse adottato anche `fatture`. UC 0101 lo fa
 * adottare a tutte: l'aiutante si sposta invece di essere duplicato, e chi scriverà l'applicazione numero
 * tre lo trova senza doverlo cercare in un modulo altrui.
 */
export function refusalMessage(err: unknown, fallback: string): string {
  if (!(err instanceof ApiError)) return fallback
  const detail = err.problem?.detail
  if (!detail || !SPEAKING_STATUSES.has(err.status)) return fallback
  return detail
}
