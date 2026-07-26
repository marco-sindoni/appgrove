import type { ModuleManifest } from './types'
import { demoManifest } from '../modules/demo/manifest'
import { fattureManifest } from '../modules/fatture/manifest'
import { crmManifest } from '../modules/crm/manifest'
import { useEntitlements } from './entitlements'

/**
 * Mappa **build-time** dei moduli esistenti (#01 dec.10): il modulo **demo** (banco di prova dell'App
 * Registry, UC 0020) e la prima app reale **fatture** (UC 0052). La sidebar mostra ciò che è entitled.
 */
export const MODULES: ModuleManifest[] = [demoManifest, fattureManifest, crmManifest]

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

/** Istanza i18n vista come registratore di bundle (evita di dipendere dal tipo di i18next qui). */
interface ResourceRegistrar {
  addResourceBundle(lng: string, ns: string, resources: object, deep?: boolean, overwrite?: boolean): unknown
}

/**
 * Registra nell'istanza i18n i bundle di traduzione per-modulo (UC 0060), sotto lo spazio-nomi = `id`
 * del modulo, così che la sidebar mostri le etichette localizzate anche prima di aprire il modulo.
 * Va chiamata subito dopo aver creato l'istanza (bootstrap dell'app e utility di test).
 */
export function registerModuleResources(i18n: ResourceRegistrar, modules: ModuleManifest[] = MODULES): void {
  for (const mod of modules) {
    if (!mod.resources) continue
    for (const [lng, bundle] of Object.entries(mod.resources)) {
      if (bundle) i18n.addResourceBundle(lng, mod.id, bundle, true, true)
    }
  }
}
