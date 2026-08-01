import { lazy } from 'react'
import type { ModuleManifest } from '../../registry/types'
import { @@APP_CAMEL@@Resources } from './i18n'

/**
 * Manifest co-locato del modulo **@@APP_NAME@@**: id = chiave di entitlement/registry, sezioni
 * sidebar, e il componente React **lazy** montato dalla shell via contratto Context.
 *
 * L'id DEVE coincidere con `appgrove.app-id` del servizio e con lo `slug` del listino: è la stessa
 * chiave che lega diritti, rotte e fatturazione. Disallinearli fa sparire l'app dalla sidebar senza
 * alcun errore visibile.
 *
 * `name`/`label` sono chiavi i18n (spazio-nomi = id del modulo) risolte dalla shell con `t()` (UC 0060);
 * `resources` sono i bundle di traduzione che la shell registra nell'istanza i18n.
 */
export const @@APP_CAMEL@@Manifest: ModuleManifest = {
  id: '@@APP_ID@@',
  name: '@@APP_ID@@:appName',
  icon: '@@ICON@@',
  accentToken: '@@ACCENT@@',
  sections: [{ id: 'items', label: '@@APP_ID@@:sectionItems', route: '', icon: '@@ICON@@' }],
  resources: @@APP_CAMEL@@Resources,
  // Quota principale dell'app, per la barra di consumo della Dashboard (UC 0097): l'uso corrente lo
  // conosce solo il servizio dell'app, quindi la Dashboard va a leggerlo là.
  quota: { path: '/api/@@APP_ID@@/v1/quota', unitLabel: '@@APP_ID@@:quotaUnit' },
  component: lazy(() => import('./@@APP_CLASS@@Module')),
}
