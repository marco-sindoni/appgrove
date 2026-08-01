import { lazy } from 'react'
import type { ModuleManifest } from '../../registry/types'
import { FATTURE_NS, fattureResources } from './i18n'

/**
 * Manifest co-locato del modulo **fatture** (app #1, UC 0052): id = chiave di entitlement/registry,
 * sezioni sidebar, e il componente React **lazy** montato dalla shell via contratto Context.
 *
 * `name`/`label` sono chiavi i18n (spazio-nomi = id del modulo) risolte dalla shell con `t()` (UC 0060);
 * `resources` sono i bundle di traduzione che la shell registra nell'istanza i18n.
 */
export const fattureManifest: ModuleManifest = {
  id: 'fatture',
  name: `${FATTURE_NS}:appName`,
  icon: 'receipt_long',
  accentToken: 'cat-blue',
  sections: [
    { id: 'invoices', label: `${FATTURE_NS}:sectionInvoices`, route: '', icon: 'receipt_long' },
  ],
  resources: fattureResources,
  // Quota principale dell'app, per la barra di consumo della Dashboard (UC 0097): l'uso corrente lo
  // conosce solo il servizio dell'app, quindi la Dashboard va a leggerlo là.
  quota: { path: '/api/fatture/v1/quota', unitLabel: `${FATTURE_NS}:quotaUnit` },
  component: lazy(() => import('./FattureModule')),
}
