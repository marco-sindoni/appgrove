import { lazy } from 'react'
import type { ModuleManifest } from '../../registry/types'
import { t } from './strings'

/**
 * Manifest co-locato del modulo **Mini-CRM**: id = chiave di entitlement/registry, sezioni
 * sidebar, e il componente React **lazy** montato dalla shell via contratto Context.
 *
 * L'id DEVE coincidere con `appgrove.app-id` del servizio e con lo `slug` del listino: è la stessa
 * chiave che lega diritti, rotte e fatturazione. Disallinearli fa sparire l'app dalla sidebar senza
 * alcun errore visibile.
 */
export const crmManifest: ModuleManifest = {
  id: 'crm',
  name: t.appName,
  icon: 'contacts',
  accentToken: 'cat-teal',
  sections: [
    { id: 'contacts', label: t.sectionContacts, route: '', icon: 'contacts' },
    { id: 'members', label: t.sectionMembers, route: 'members', icon: 'group' },
  ],
  component: lazy(() => import('./CrmModule')),
}
