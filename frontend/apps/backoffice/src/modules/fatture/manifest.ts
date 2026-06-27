import { lazy } from 'react'
import type { ModuleManifest } from '../../registry/types'
import { t } from './strings'

/**
 * Manifest co-locato del modulo **fatture** (app #1, UC 0052): id = chiave di entitlement/registry,
 * sezioni sidebar, e il componente React **lazy** montato dalla shell via contratto Context.
 */
export const fattureManifest: ModuleManifest = {
  id: 'fatture',
  name: t.appName,
  icon: 'receipt_long',
  accentToken: 'cat-blue',
  sections: [{ id: 'invoices', label: t.sectionInvoices, route: '', icon: 'receipt_long' }],
  component: lazy(() => import('./FattureModule')),
}
