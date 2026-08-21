import { ApiError } from '@appgrove/api-client'

/**
 * Codici di stato con cui il server **parla alla persona**: il loro `detail` è un messaggio scritto per
 * essere letto, già nella lingua di chi chiede, e va mostrato tale e quale.
 *
 * Tutto il resto — e in particolare i `5xx` — resta dietro il messaggio generico: là il `detail`, quando
 * c'è, descrive un guasto interno e non serve a chi guarda.
 */
const SPEAKING = new Set([400, 402, 403, 409, 422, 429])

/**
 * Messaggio da mostrare per un rifiuto, con ripiego sul generico (UC 0099).
 *
 * Serve perché la storia dell'autorizzazione per-applicazione poggia interamente su rifiuti che si
 * **distinguono a parole**: «non hai accesso a questa applicazione, chiedi l'abilitazione» e «serve almeno
 * `editor`, il tuo ruolo è `viewer`» sono due `403` con lo stesso codice e due significati diversi, e la
 * seconda frase è l'unica che dice alla persona cosa può fare. Un `catch` che scrive «si è verificato un
 * errore» butta via esattamente l'informazione per cui quei messaggi sono stati scritti — ed è ciò che
 * accadeva in tutte le schermate del Mini-CRM.
 *
 * Vive nel modulo perché il Mini-CRM è, oggi, la sola applicazione con il varco del ruolo per-applicazione.
 * Quando lo adotterà anche `fatture`, questo aiutante si promuove a `@appgrove/api-client`, accanto a
 * {@link ApiError} — non si duplica.
 */
export function refusalMessage(err: unknown, fallback: string): string {
  if (!(err instanceof ApiError)) return fallback
  const detail = err.problem?.detail
  if (!detail || !SPEAKING.has(err.status)) return fallback
  return detail
}
