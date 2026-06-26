import { createContext, useContext, type ReactNode } from 'react'

/** Stato degli entitlement del tenant (app_id a cui è abbonato). */
export interface EntitlementsState {
  entitled: string[]
  isLoading: boolean
  isError: boolean
}

const EntitlementsContext = createContext<EntitlementsState | null>(null)

/**
 * Provider entitlement **STUB** (UC 0020).
 *
 * Il core NON espone ancora un endpoint che serva gli entitlement (derivati da `platform.subscription`,
 * #01 dec.10/#09 dec.12). Finché non esiste, la sorgente è un set statico iniettato (config/fixture).
 * Rinvio tracciato: vedi "Punti aperti / decisioni differite" di UC 0020 + _BACKLOG.md (contratto
 * frontend↔core). La sostituzione con una query reale tocca **solo** questo provider.
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
