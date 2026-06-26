import type { ModuleManifest } from './types'
import { demoManifest } from '../modules/demo/manifest'
import { useEntitlements } from './entitlements'

/**
 * Mappa **build-time** dei moduli esistenti (#01 dec.10). Oggi solo il modulo **demo** (le app reali
 * sono UC 0052/0054, fuori scope): è il banco di prova dell'App Registry, da rimuovere all'arrivo
 * della prima app reale (rinvio tracciato in UC 0020).
 */
export const MODULES: ModuleManifest[] = [demoManifest]

/** Intersezione moduli ∩ entitlement → ciò che la sidebar "YOUR APPS" mostra (#03 dec.6). Pura, testabile. */
export function intersectModules(
  modules: ModuleManifest[],
  entitled: Iterable<string>,
): ModuleManifest[] {
  const set = new Set(entitled)
  return modules.filter((m) => set.has(m.id))
}

/** Moduli visibili al tenant corrente = registry ∩ entitlement. */
export function useVisibleModules(): ModuleManifest[] {
  const { entitled } = useEntitlements()
  return intersectModules(MODULES, entitled)
}

/** Lookup di un modulo per app_id (per le route guard `requireEntitlement`). */
export function findModule(appId: string): ModuleManifest | undefined {
  return MODULES.find((m) => m.id === appId)
}
