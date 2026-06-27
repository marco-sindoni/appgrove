import { ApiError } from '@appgrove/api-client'

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type TFn = (key: any, opts?: Record<string, unknown>) => string

/** Chiavi di messaggio per status HTTP (problem+json). Le pagine passano override per il proprio contesto. */
export type StatusMessages = Record<number, string>

/**
 * Mappa un errore (tipicamente {@link ApiError} da problem+json) in un messaggio localizzato.
 * `overrides` permette a ogni schermata di specializzare gli status che conosce (es. 401 = codice 2FA
 * non valido), con fallback a un messaggio generico.
 */
export function authErrorMessage(err: unknown, t: TFn, overrides: StatusMessages = {}): string {
  if (err instanceof ApiError) {
    const key = overrides[err.status]
    if (key) return key
    switch (err.status) {
      case 403:
        return t('admin.errors.emailNotVerified')
      case 401:
        return t('admin.errors.invalidCredentials')
    }
  }
  return t('admin.errors.generic')
}
