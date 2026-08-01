import { useCallback } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { unwrap, type components } from '@appgrove/api-client'
import type { Language } from '@appgrove/i18n'
import { useApiClient } from '../api/apiClient'
import { useAuthStore } from '../auth/authStore'

/** Una app della vetrina, così come la serve il read-model del catalogo (UC 0095). */
export type CatalogApp = components['schemas']['CatalogAppView']
/** Lo stato della card: i sei stati del dominio, nessuno inventato dal frontend. */
export type CatalogAppState = components['schemas']['CatalogAppState']

/** Chiave della lettura del catalogo: condivisa con chi la invalida (checkout). */
export const CATALOG_KEY = ['me', 'catalog'] as const

/** Quante card per pagina: sei, come il riferimento visivo approvato (change 0066). */
export const CATALOG_PAGE_SIZE = 6

/** Le sei tinte di categoria del sistema di design, nell'ordine usato per il ripiego deterministico. */
const CATEGORY_TINTS = ['green', 'amber', 'red', 'blue', 'violet', 'teal'] as const
export type CategoryTint = (typeof CATEGORY_TINTS)[number]

/**
 * Vetrina del catalogo per questo account da `GET /api/platform/v1/me/catalog`. Abilitata solo a
 * sessione autenticata (il token lo propaga il client).
 *
 * <p>A differenza degli entitlement, questa lettura **non** si rinfresca al ritorno sulla scheda: il
 * catalogo non governa l'accesso, quindi una sua istantanea leggermente vecchia non nega né concede
 * nulla. Ciò che deve essere fresco per forza — il menu laterale — ha già la sua regola (UC 0077).
 */
export function useAppCatalog() {
  const client = useApiClient()
  const status = useAuthStore((s) => s.status)
  return useQuery({
    queryKey: CATALOG_KEY,
    enabled: status === 'authenticated',
    queryFn: () => unwrap(client.GET('/api/platform/v1/me/catalog', {})),
  })
}

/** Rilegge il catalogo dopo un'attivazione: senza, la card resterebbe "available" fino al ricaricamento. */
export function useInvalidateCatalog() {
  const queryClient = useQueryClient()
  return useCallback(() => queryClient.invalidateQueries({ queryKey: CATALOG_KEY }), [queryClient])
}

/**
 * Descrizione breve nella lingua attiva, con ripiego sull'inglese e poi sulla prima disponibile. Il
 * backend serve tutte e cinque le lingue proprio perché questa scelta possa avvenire qui, senza
 * ricaricare il catalogo quando l'utente cambia lingua. Funzione pura → provabile senza React.
 */
export function describeApp(app: CatalogApp, language: Language): string {
  const byLanguage = app.descriptions ?? {}
  return byLanguage[language] ?? byLanguage.en ?? Object.values(byLanguage)[0] ?? ''
}

/**
 * Tinta di categoria della card: quella dichiarata a listino, oppure — se l'app non ne dichiara una —
 * una **derivata dallo slug**, stabile fra un caricamento e l'altro. Meglio una tinta arbitraria ma
 * costante che una card senza identità visiva: il colore qui aiuta a ritrovare l'app, non a dire
 * qualcosa di vero su di essa.
 */
export function tintFor(app: CatalogApp): CategoryTint {
  const declared = app.category
  if (declared && (CATEGORY_TINTS as readonly string[]).includes(declared)) {
    return declared as CategoryTint
  }
  const slug = app.appSlug ?? ''
  let sum = 0
  for (let i = 0; i < slug.length; i += 1) sum += slug.charCodeAt(i)
  return CATEGORY_TINTS[sum % CATEGORY_TINTS.length]
}

/**
 * Filtra la vetrina per nome e descrizione, senza distinzione fra maiuscole e minuscole. La ricerca
 * lavora sull'**intero** catalogo, non sulla pagina visibile: cercare qualcosa che sta a pagina tre e
 * non trovarlo sarebbe il difetto peggiore di una ricerca. Funzione pura.
 */
export function filterApps(apps: CatalogApp[], query: string, language: Language): CatalogApp[] {
  const needle = query.trim().toLowerCase()
  if (!needle) return apps
  return apps.filter((app) => {
    const haystack = `${app.name ?? ''} ${describeApp(app, language)}`.toLowerCase()
    return haystack.includes(needle)
  })
}

/**
 * Taglia l'elenco nella pagina richiesta e dice quante pagine ci sono in tutto. La pagina viene
 * **riportata dentro i limiti** invece di essere presa alla lettera: quando una ricerca riduce i
 * risultati, chiedere una pagina che non esiste più deve mostrare l'ultima, non il vuoto. Funzione pura.
 */
export function paginate<T>(
  items: T[],
  page: number,
  size = CATALOG_PAGE_SIZE,
): { items: T[]; page: number; pages: number } {
  const pages = Math.max(1, Math.ceil(items.length / size))
  const current = Math.min(Math.max(1, page), pages)
  return { items: items.slice((current - 1) * size, current * size), page: current, pages }
}
