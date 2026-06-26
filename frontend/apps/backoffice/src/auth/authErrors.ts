import { ApiError } from '@appgrove/api-client'
import type { TFn } from './schemas'

/** Chiavi di messaggio per status HTTP (problem+json). Le pagine passano override per il proprio contesto. */
export type StatusMessages = Record<number, string>

/**
 * Mappa un errore (tipicamente {@link ApiError} da problem+json) in un messaggio localizzato.
 * `overrides` permette a ogni schermata di specializzare gli status che conosce (es. 401 = codice non
 * valido nel 2FA, 410 = invito scaduto), con fallback a un messaggio generico.
 */
export function authErrorMessage(err: unknown, t: TFn, overrides: StatusMessages = {}): string {
  if (err instanceof ApiError) {
    const key = overrides[err.status]
    if (key) return key
    switch (err.status) {
      case 409:
        return t('errors.emailTaken')
      case 403:
        return t('errors.emailNotVerified')
      case 401:
        return t('errors.invalidCredentials')
    }
  }
  return t('errors.generic')
}
