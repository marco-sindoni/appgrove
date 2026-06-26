import type { Middleware } from 'openapi-fetch'

/** Header tecnico che marca una richiesta come già ritentata, per evitare loop di refresh. */
export const RETRY_HEADER = 'x-ag-retried'

export interface AuthMiddlewareConfig {
  /** Ritorna l'access token corrente (in memoria nello store auth), o null se assente. */
  getAccessToken: () => string | null
  /**
   * Esegue `POST /api/auth/refresh` (cookie HttpOnly) e aggiorna l'access token nello store.
   * Ritorna true se la sessione è stata ripristinata, false altrimenti.
   */
  refresh: () => Promise<boolean>
  /** Invocata quando il refresh fallisce: la sessione va terminata (logout UX). */
  onAuthFailure?: () => void
  /** fetch sottostante (override per i test). Default: il `fetch` globale. */
  fetch?: typeof fetch
}

/**
 * Middleware `openapi-fetch` che implementa il contratto auth della shell (#03 dec.5/8):
 * 1. inietta `Authorization: Bearer <access>` su ogni richiesta;
 * 2. su **401** chiama una volta `/api/auth/refresh` e **ritenta** la richiesta originale;
 * 3. se il refresh fallisce, invoca `onAuthFailure` e propaga la 401.
 *
 * La richiesta viene clonata in `onRequest` così il corpo è ancora leggibile al retry.
 */
export function authMiddleware(config: AuthMiddlewareConfig): Middleware {
  const doFetch = config.fetch ?? fetch
  const pending = new Map<string, Request>()

  return {
    onRequest({ request, id }) {
      const token = config.getAccessToken()
      if (token) request.headers.set('Authorization', `Bearer ${token}`)
      // clona prima dell'invio: il corpo dell'originale verrà consumato dalla fetch
      pending.set(id, request.clone())
      return request
    },

    async onResponse({ request, response, id }) {
      const original = pending.get(id)
      pending.delete(id)

      if (response.status !== 401) return response
      // già ritentata → non insistere: la sessione è compromessa
      if (request.headers.get(RETRY_HEADER) || !original) {
        config.onAuthFailure?.()
        return response
      }

      const restored = await config.refresh()
      if (!restored) {
        config.onAuthFailure?.()
        return response
      }

      const retry = new Request(original, {
        headers: new Headers(original.headers),
      })
      retry.headers.set(RETRY_HEADER, '1')
      const token = config.getAccessToken()
      if (token) retry.headers.set('Authorization', `Bearer ${token}`)
      return doFetch(retry)
    },
  }
}
