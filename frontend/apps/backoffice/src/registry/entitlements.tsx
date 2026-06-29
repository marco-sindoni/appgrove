import { createContext, useContext, useMemo, type ReactNode } from 'react'
import { computeEntitled, useMyEntitlements } from './entitlementsApi'

/** Stato degli entitlement del tenant (app_id a cui è abbonato). */
export interface EntitlementsState {
  entitled: string[]
  isLoading: boolean
  isError: boolean
}

const EntitlementsContext = createContext<EntitlementsState | null>(null)

/**
 * Provider entitlement **reale** (UC 0027): legge `GET /api/platform/v1/me/entitlements` da core e ne
 * deriva l'insieme entitled (slug). Sostituisce lo stub: un'app appena acquistata compare in sidebar
 * appena il read-model la include. Il modulo `demo` (senza backend) è abilitato solo in locale
 * (`demoInLocal`). Va montato **dentro** il SessionGate (sessione stabile → token disponibile).
 */
export function EntitlementsProvider({
  demoInLocal = false,
  children,
}: {
  demoInLocal?: boolean
  children: ReactNode
}) {
  const query = useMyEntitlements()
  const value = useMemo<EntitlementsState>(
    () => ({
      entitled: computeEntitled(query.data, demoInLocal),
      isLoading: query.isLoading,
      isError: query.isError,
    }),
    [query.data, query.isLoading, query.isError, demoInLocal],
  )
  return <EntitlementsContext.Provider value={value}>{children}</EntitlementsContext.Provider>
}

/**
 * Provider entitlement **STUB** (UC 0020): set statico iniettato. Mantenuto per i test e per scenari
 * che non vogliono colpire la rete; in app la sorgente reale è {@link EntitlementsProvider}.
 */
export function StubEntitlementsProvider({
  entitled,
  children,
}: {
  entitled: string[]
  children: ReactNode
}) {
  return (
    <EntitlementsContext.Provider value={{ entitled, isLoading: false, isError: false }}>
      {children}
    </EntitlementsContext.Provider>
  )
}

export function useEntitlements(): EntitlementsState {
  const ctx = useContext(EntitlementsContext)
  if (!ctx) throw new Error('useEntitlements deve essere usato dentro un EntitlementsProvider')
  return ctx
}
