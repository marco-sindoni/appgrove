import type { ModuleManifest } from './types'
import { demoManifest } from '../modules/demo/manifest'
import { fattureManifest } from '../modules/fatture/manifest'
import { useEntitlements } from './entitlements'

/**
 * Mappa **build-time** dei moduli esistenti (#01 dec.10): il modulo **demo** (banco di prova dell'App
 * Registry, UC 0020) e la prima app reale **fatture** (UC 0052). La sidebar mostra ciò che è entitled.
 */
export const MODULES: ModuleManifest[] = [demoManifest, fattureManifest]

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
