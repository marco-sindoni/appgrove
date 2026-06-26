import { lazy } from 'react'
import type { ModuleManifest } from '../../registry/types'

/**
 * Manifest del **modulo demo** (UC 0020): unico modulo del registry finché non arrivano le app reali
 * (UC 0052/0054). Serve a esercitare App Registry, intersezione entitlement e contratto Context.
 */
export const demoManifest: ModuleManifest = {
  id: 'demo',
  name: 'Demo app',
  icon: 'widgets',
  accentToken: 'cat-violet',
  sections: [
    { id: 'overview', label: 'Overview', route: '', icon: 'dashboard' },
    { id: 'items', label: 'Items', route: 'items', icon: 'list' },
  ],
  component: lazy(() => import('./DemoModule')),
}
