import { createContext, useContext, useMemo, type ReactNode } from 'react'
import { computeEntitled, deriveEntitlementsStatus, useMyEntitlements } from './entitlementsApi'

/**
 * Stato degli entitlement del tenant (app_id a cui è abbonato).
 *
 * `isError` non è decorativo (UC 0077): senza distinguerlo, un guasto di rete diventa indistinguibile
 * da "non hai comprato nulla" — `entitled` resta vuoto e l'utente pagante si vede negare l'accesso a
 * ciò che ha pagato. Chi consuma questo stato deve trattare l'errore come errore, mai come assenza di
 * diritti. `retry` permette di ritentare la lettura senza ricaricare la pagina.
 */
export interface EntitlementsState {
  entitled: string[]
  isLoading: boolean
  isError: boolean
  retry: () => void
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
  const { refetch, data, isFetched, isLoading: fetchInProgress } = query
  const value = useMemo<EntitlementsState>(() => {
    // Stati come li vede la shell, non come li espone la libreria: vedi deriveEntitlementsStatus.
    const { isLoading, isError } = deriveEntitlementsStatus(
      fetchInProgress,
      isFetched,
      data !== undefined,
    )
    return {
      // In errore non si finge un elenco: `entitled` resta vuoto ma `isError` lo qualifica, e i
      // consumatori mostrano l'errore invece dello stato vuoto.
      entitled: computeEntitled(data, demoInLocal),
      isLoading,
      isError,
      retry: () => void refetch(),
    }
  }, [data, isFetched, fetchInProgress, refetch, demoInLocal])
  return <EntitlementsContext.Provider value={value}>{children}</EntitlementsContext.Provider>
}

/**
 * Provider entitlement **STUB** (UC 0020): set statico iniettato. Mantenuto per i test e per lo sviluppo
 * locale; in app la sorgente reale è {@link EntitlementsProvider}. Non è un ripiego in caso di errore:
 * nessun percorso dell'applicazione vi ricade quando la lettura reale fallisce (UC 0077).
 */
export function StubEntitlementsProvider({
  entitled,
  isLoading = false,
  isError = false,
  retry = () => {},
  children,
}: {
  entitled: string[]
  isLoading?: boolean
  isError?: boolean
  retry?: () => void
  children: ReactNode
}) {
  const value = useMemo<EntitlementsState>(
    () => ({ entitled, isLoading, isError, retry }),
    [entitled, isLoading, isError, retry],
  )
  return <EntitlementsContext.Provider value={value}>{children}</EntitlementsContext.Provider>
}

export function useEntitlements(): EntitlementsState {
  const ctx = useContext(EntitlementsContext)
  if (!ctx) throw new Error('useEntitlements deve essere usato dentro un EntitlementsProvider')
  return ctx
}
