import type { ReactNode } from 'react'
import { useBootstrapSession } from './useBootstrapSession'
import { FullPageMessage } from '../shell/FullPageMessage'

/**
 * Avvia il refresh-on-load (ripristino sessione via cookie) al mount dell'app e **blocca** il render
 * del router finché la sessione non è nota (status ≠ `idle`). Così il router monta in uno stato auth
 * stabile, evitando lo swap idle→auth dentro l'albero delle route.
 */
export function SessionGate({ children }: { children: ReactNode }) {
  const { ready } = useBootstrapSession()
  if (!ready) return <FullPageMessage tone="status" messageKey="auth.restoring" />
  return <>{children}</>
}
