import { Navigate, Outlet } from 'react-router-dom'
import { useAuthStore, type AuthStatus } from '../auth/authStore'
import { FullPageMessage } from '../shell/FullPageMessage'

export interface GuardContext {
  status: AuthStatus
  roles: string[]
}

/** Una guard ritorna `true` (consenti) oppure il path verso cui reindirizzare. */
export type Guard = (ctx: GuardContext) => true | string

/** Richiede una sessione autenticata. */
export const requireAuth: Guard = (ctx) => (ctx.status === 'authenticated' ? true : '/login')

/** Richiede un ruolo specifico (qui sempre `platform-admin`). → `/forbidden` se manca, `/login` se anonimo. */
export const requireRole =
  (role: string): Guard =>
  (ctx) =>
    ctx.status !== 'authenticated' ? '/login' : ctx.roles.includes(role) ? true : '/forbidden'

function useGuardContext(): { ready: boolean; ctx: GuardContext } {
  const status = useAuthStore((s) => s.status)
  const roles = useAuthStore((s) => s.claims?.roles ?? [])
  return { ready: status !== 'idle', ctx: { status, roles } }
}

/**
 * Route protetta: applica una {@link Guard}. Difesa in profondità lato UX (#03 dec.8) — l'enforcement
 * vero resta nel backend. Attende che la sessione sia nota prima di decidere.
 */
export function ProtectedRoute({ guard }: { guard: Guard }) {
  const { ready, ctx } = useGuardContext()
  if (!ready) return <FullPageMessage tone="status" messageKey="auth.restoring" />
  const result = guard(ctx)
  if (result !== true) return <Navigate to={result} replace />
  return <Outlet />
}
