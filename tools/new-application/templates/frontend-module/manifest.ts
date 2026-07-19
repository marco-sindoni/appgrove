import { lazy } from 'react'
import type { ModuleManifest } from '../../registry/types'
import { t } from './strings'

/**
 * Manifest co-locato del modulo **@@APP_NAME@@**: id = chiave di entitlement/registry, sezioni
 * sidebar, e il componente React **lazy** montato dalla shell via contratto Context.
 *
 * L'id DEVE coincidere con `appgrove.app-id` del servizio e con lo `slug` del listino: è la stessa
 * chiave che lega diritti, rotte e fatturazione. Disallinearli fa sparire l'app dalla sidebar senza
 * alcun errore visibile.
 */
export const @@APP_CAMEL@@Manifest: ModuleManifest = {
  id: '@@APP_ID@@',
  name: t.appName,
  icon: '@@ICON@@',
  accentToken: '@@ACCENT@@',
  sections: [{ id: 'items', label: t.sectionItems, route: '', icon: '@@ICON@@' }],
  component: lazy(() => import('./@@APP_CLASS@@Module')),
}
