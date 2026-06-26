import { useEffect } from 'react'
import { useAuthStore } from './authStore'
import { refreshSession } from './authApi'
import { useConfig } from '../config'

/**
 * Al load della SPA tenta `POST /api/auth/refresh` (cookie) per ripristinare la sessione (#03 dec.8).
 * Sposta lo status da `idle` ad `authenticated`/`anonymous`: le route guard attendono che non sia `idle`.
 */
export function useBootstrapSession(): AuthStatusGate {
  const config = useConfig()
  const status = useAuthStore((s) => s.status)
  const setSession = useAuthStore((s) => s.setSession)
  const clear = useAuthStore((s) => s.clear)

  useEffect(() => {
    let active = true
    void (async () => {
      const tokens = await refreshSession(config.authBaseUrl)
      if (!active) return
      if (tokens) setSession(tokens)
      else clear()
    })()
    return () => {
      active = false
    }
  }, [config.authBaseUrl, setSession, clear])

  return { ready: status !== 'idle' }
}

export interface AuthStatusGate {
  /** true quando il refresh-on-load ha risposto (sessione nota). */
  ready: boolean
}
