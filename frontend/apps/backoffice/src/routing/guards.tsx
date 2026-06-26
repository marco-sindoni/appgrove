import { Suspense } from 'react'
import { Navigate, Outlet, useParams } from 'react-router-dom'
import { useAuthStore, type AuthStatus } from '../auth/authStore'
import { useEntitlements } from '../registry/entitlements'
import { findModule } from '../registry/registry'
import { FullPageMessage } from '../shell/FullPageMessage'

export interface GuardContext {
  status: AuthStatus
  roles: string[]
  entitled: string[]
}

/** Una guard ritorna `true` (consenti) oppure il path verso cui reindirizzare. */
export type Guard = (ctx: GuardContext) => true | string

/** Richiede una sessione autenticata. */
export const requireAuth: Guard = (ctx) => (ctx.status === 'authenticated' ? true : '/login')

/** Richiede un ruolo specifico (es. `platform-admin`). */
export const requireRole =
  (role: string): Guard =>
  (ctx) =>
    ctx.status !== 'authenticated' ? '/login' : ctx.roles.includes(role) ? true : '/forbidden'

/** Richiede l'entitlement a un'app (sidebar lo nasconde; questa è la difesa in profondità sulla route). */
export const requireEntitlement =
  (appId: string): Guard =>
  (ctx) =>
    ctx.status !== 'authenticated'
      ? '/login'
      : ctx.entitled.includes(appId)
        ? true
        : '/forbidden'

function useGuardContext(): { ready: boolean; ctx: GuardContext } {
  const status = useAuthStore((s) => s.status)
  const roles = useAuthStore((s) => s.claims?.roles ?? [])
  const { entitled, isLoading } = useEntitlements()
  return { ready: status !== 'idle' && !isLoading, ctx: { status, roles, entitled } }
}

/**
 * Route protetta: applica una {@link Guard}. Difesa in profondità lato UX (#03 dec.8) — l'enforcement
 * vero resta nel backend. Attende che la sessione/entitlement siano noti prima di decidere.
 */
export function ProtectedRoute({ guard }: { guard: Guard }) {
  const { ready, ctx } = useGuardContext()
  if (!ready) return <FullPageMessage tone="status" messageKey="auth.restoring" />
  const result = guard(ctx)
  if (result !== true) return <Navigate to={result} replace />
  return <Outlet />
}

/**
 * Host del modulo app: `requireEntitlement(:appId)` + montaggio del componente **lazy** del registry.
 * Un modulo non entitled non viene montato (redirect a /forbidden) — copre il test "entitled vs non".
 */
export function AppModuleHost() {
  const { appId = '' } = useParams()
  const { ready, ctx } = useGuardContext()
  if (!ready) return <FullPageMessage tone="status" messageKey="auth.restoring" />
  const result = requireEntitlement(appId)(ctx)
  if (result !== true) return <Navigate to={result} replace />
  const module = findModule(appId)
  if (!module) return <Navigate to="/forbidden" replace />
  const Module = module.component
  return (
    <Suspense fallback={<FullPageMessage tone="status" messageKey="states.loading" />}>
      <Module />
    </Suspense>
  )
}
