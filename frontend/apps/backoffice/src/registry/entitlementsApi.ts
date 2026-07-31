import { useCallback } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { unwrap } from '@appgrove/api-client'
import { useApiClient } from '../api/apiClient'
import { useAuthStore } from '../auth/authStore'

/** Chiave della lettura entitlement: condivisa con chi la invalida (billing, checkout). */
export const ENTITLEMENTS_KEY = ['me', 'entitlements'] as const

/** Forma minima del read-model `/me/entitlements` consumata dal registry (slug = chiave di entitlement). */
interface MeEntitlements {
  entitlements?: Array<{ appSlug?: string }>
}

/**
 * Deriva l'insieme degli `app_id`/slug entitled dal read-model di core (UC 0027), opzionalmente
 * unendo il modulo **demo** in locale. `demo` non ha catalogo né backend (è dimostrativo): non compare
 * mai in `/me/entitlements`, quindi resta abilitato **solo** nello stub locale (regola "Avvio locale").
 * Funzione pura → testabile senza React.
 */
export function computeEntitled(view: MeEntitlements | undefined, demoInLocal: boolean): string[] {
  const slugs = (view?.entitlements ?? [])
    .map((e) => e.appSlug)
    .filter((s): s is string => typeof s === 'string' && s.length > 0)
  return demoInLocal ? Array.from(new Set(['demo', ...slugs])) : slugs
}

/**
 * Entitlement reali del tenant da `GET /api/platform/v1/me/entitlements`. Abilitata solo a sessione
 * autenticata (il token è propagato dal client). Sostituisce lo stub hardcoded: un'app appena
 * acquistata compare in sidebar appena il read-model la include (invalidando questa query).
 *
 * **Freschezza** (UC 0077): oltre all'invalidazione esplicita dopo le mutazioni dell'utente
 * ({@link useInvalidateEntitlements}), questa lettura si rinfresca al **ritorno sulla scheda** e alla
 * **riconnessione di rete**. Serve per i cambiamenti che non nascono dall'utente — un'app disabilitata
 * dalla piattaforma, un abbonamento scaduto a fine periodo — che altrimenti resterebbero invisibili
 * fino a un ricaricamento della pagina. La shell disattiva quel comportamento in generale
 * (`api/queryClient.ts`): la riattivazione vale **solo** qui, dove il menu ne dipende, e costa zero
 * chiamate mentre l'utente non guarda. Una notifica dal server in tempo reale resta fuori scope
 * (rimando in `docs/_BACKLOG.md`).
 */
export function useMyEntitlements() {
  const client = useApiClient()
  const status = useAuthStore((s) => s.status)
  return useQuery({
    queryKey: ENTITLEMENTS_KEY,
    enabled: status === 'authenticated',
    refetchOnWindowFocus: true,
    refetchOnReconnect: true,
    queryFn: () => unwrap(client.GET('/api/platform/v1/me/entitlements', {})),
  })
}

/**
 * Stato della lettura come lo vede la shell, **non** come lo espone la libreria delle richieste.
 *
 * La differenza conta (UC 0077). Quando una lettura fallita viene ritentata e non ha ancora alcun dato,
 * la libreria la riporta a "in attesa" azzerando l'errore: chi guardasse i suoi stati grezzi vedrebbe
 * la shell tornare alla schermata di caricamento a ogni ritentativo — smontando la pagina aperta e
 * perdendo quello che l'utente stava facendo — e, nell'istante fra i due, un elenco vuoto scambiato
 * per "non hai diritto a nulla". Qui invece:
 *
 * - **caricamento** = la lettura è davvero in corso **e** non c'è ancora stato alcun esito, cioè solo
 *   il primo avvio. Il secondo pezzo della condizione copre il ritentativo; il primo copre il caso in
 *   cui la lettura non è nemmeno partita perché la sessione non è autenticata — lì non si sta
 *   caricando nulla e non lo si sarà mai, e chi attende "che il caricamento finisca" resterebbe
 *   appeso per sempre;
 * - **errore** = c'è già stato un esito e continuiamo a non avere dati — resta tale anche mentre si
 *   ritenta, finché un esito riuscito non lo smentisce.
 *
 * Funzione pura → provabile senza React.
 */
export function deriveEntitlementsStatus(
  fetchInProgress: boolean,
  settledAtLeastOnce: boolean,
  hasData: boolean,
): { isLoading: boolean; isError: boolean } {
  return {
    isLoading: fetchInProgress && !settledAtLeastOnce,
    isError: settledAtLeastOnce && !hasData,
  }
}

/**
 * Rilegge gli entitlement dopo un'azione che li cambia (acquisto attivato, cambio piano, disdetta,
 * ripresa): senza, il menu resta quello di prima fino al ricaricamento della pagina.
 */
export function useInvalidateEntitlements() {
  const queryClient = useQueryClient()
  return useCallback(
    () => queryClient.invalidateQueries({ queryKey: ENTITLEMENTS_KEY }),
    [queryClient],
  )
}
