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

/** Richiede **almeno uno** dei ruoli indicati (es. `['owner','admin']` per la gestione membri). */
export const requireAnyRole =
  (roles: string[]): Guard =>
  (ctx) =>
    ctx.status !== 'authenticated'
      ? '/login'
      : roles.some((r) => ctx.roles.includes(r))
        ? true
        : '/forbidden'

/** Richiede l'entitlement a un'app (sidebar lo nasconde; questa è la difesa in profondità sulla route). */
export const requireEntitlement =
  (appId: string): Guard =>
  (ctx) =>
    ctx.status !== 'authenticated'
      ? '/login'
      : ctx.entitled.includes(appId)
        ? true
        : '/forbidden'

/** Riferimento stabile per l'assenza di ruoli: vedi la nota sul selettore qui sotto. */
const NO_ROLES: string[] = []

function useGuardContext(): { ready: boolean; entitlementsFailed: boolean; ctx: GuardContext } {
  const status = useAuthStore((s) => s.status)
  // Il selettore deve restituire un riferimento STABILE: `s.claims?.roles ?? []` creava un array
  // nuovo a ogni lettura dello store e faceva ciclare il render all'infinito (stessa trappola
  // documentata in Sidebar.tsx). Finché la rotta si smontava subito — utente anonimo → redirect al
  // login — il difetto restava nascosto; basta che il componente resti montato un istante perché
  // l'applicazione esploda con "Maximum update depth exceeded".
  const roles = useAuthStore((s) => s.claims?.roles) ?? NO_ROLES
  const { entitled, isLoading, isError } = useEntitlements()
  return {
    ready: status !== 'idle' && !isLoading,
    entitlementsFailed: isError,
    ctx: { status, roles, entitled },
  }
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
  const { ready, entitlementsFailed, ctx } = useGuardContext()
  if (!ready) return <FullPageMessage tone="status" messageKey="auth.restoring" />
  // Entitlement non leggibili: è un **errore**, non un diniego (UC 0077). Dire "non hai accesso a
  // questa app" a chi l'ha pagata, perché una chiamata è fallita, è il difetto che questa change
  // chiude. Il ritentativo è a portata di mano nella sidebar, che resta montata accanto.
  if (ctx.status === 'authenticated' && entitlementsFailed) {
    return <FullPageMessage tone="error" messageKey="apps.error" />
  }
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
