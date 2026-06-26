import type { ComponentType, LazyExoticComponent } from 'react'
import type { Accent, Theme } from '@appgrove/design-system'
import type { Language } from '@appgrove/i18n'

/** Voce di menu (sezione) di un modulo app nella sidebar. */
export interface ModuleSection {
  id: string
  /** Label già localizzata dal modulo (le stringhe app sono per-modulo, #03 dec.6). */
  label: string
  /** Route relativa alla base del modulo → montata sotto `/app/<id>`. */
  route: string
  icon?: string
}

/**
 * Manifest co-locato di un modulo app (#01 dec.10/11, #03 dec.6): identità, sezioni sidebar,
 * metadata, e il componente React **lazy** che la shell monta via contratto Context.
 */
export interface ModuleManifest {
  /** `app_id`: chiave di entitlement e del registry. */
  id: string
  /** Nome display (`app_name`). */
  name: string
  icon?: string
  /** Token colore-categoria del design-system (es. `cat-violet`). */
  accentToken?: string
  sections: ModuleSection[]
  component: LazyExoticComponent<ComponentType>
}

/** API di navigazione/preferenze che la shell espone ai moduli (il modulo non tocca il router). */
export interface ShellNavApi {
  navigate: (to: string) => void
  setAccent: (accent: Accent) => void
  setLanguage: (language: Language) => void
}

/**
 * Contratto shell↔modulo (#01 dec.11): il modulo riceve token getter, `tenant_id`, `user_id`, ruoli,
 * theme e nav API. Il modulo **non** gestisce auth e **non** legge `tenant_id` fuori da qui.
 */
export interface ShellContextValue {
  getToken: () => string | null
  tenantId: string
  userId: string
  roles: string[]
  theme: { theme: Theme; accent: Accent }
  nav: ShellNavApi
}
