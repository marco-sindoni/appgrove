import type { ComponentType, LazyExoticComponent } from 'react'
import type { Accent, Theme } from '@appgrove/design-system'
import type { Language } from '@appgrove/i18n'

/** Voce di menu (sezione) di un modulo app nella sidebar. */
export interface ModuleSection {
  id: string
  /**
   * Etichetta della voce: chiave i18n risolta dalla shell con `t()` (UC 0060) — tipicamente con lo
   * spazio-nomi del modulo, es. `fatture:sectionInvoices`. I moduli non ancora migrati all'i18n
   * possono passare una stringa già localizzata: `t()` la restituisce invariata (fallback sulla chiave).
   */
  label: string
  /** Route relativa alla base del modulo → montata sotto `/app/<id>`. */
  route: string
  icon?: string
}

/**
 * Descrittore **facoltativo** della quota principale di un'app (UC 0097): dove leggerla e come si
 * chiama ciò che si consuma. Serve alla Dashboard per la barra "usato / limite" della card dell'app.
 *
 * <p>Vive nel manifest e non in un read-model di piattaforma perché **l'uso corrente lo conosce solo il
 * servizio dell'app**: il core non lo sa e non deve interpellare le app per saperlo. Un modulo che non
 * dichiara il descrittore semplicemente non mostra la barra — nessun errore, nessuna barra inventata.
 */
export interface ModuleQuota {
  /** Percorso della lettura di quota esposta dal servizio dell'app, es. `/api/fatture/v1/quota`. */
  path: string
  /**
   * Nome di ciò che si consuma ("invoices", "seats", …), mostrato accanto ai numeri. Vale la stessa
   * convenzione di `ModuleSection.label`: chiave i18n del modulo, oppure stringa già localizzata per i
   * moduli non ancora migrati all'i18n (`t()` restituisce invariata una chiave che non conosce).
   */
  unitLabel: string
}

/**
 * Manifest co-locato di un modulo app (#01 dec.10/11, #03 dec.6): identità, sezioni sidebar,
 * metadata, e il componente React **lazy** che la shell monta via contratto Context.
 */
export interface ModuleManifest {
  /** `app_id`: chiave di entitlement e del registry. */
  id: string
  /**
   * Nome display: chiave i18n risolta dalla shell con `t()` (UC 0060), es. `fatture:appName`.
   * Vale la stessa retro-compatibilità di `ModuleSection.label` per i moduli non ancora migrati.
   */
  name: string
  icon?: string
  /** Token colore-categoria del design-system (es. `cat-violet`). */
  accentToken?: string
  sections: ModuleSection[]
  /**
   * Bundle di traduzione per-modulo (UC 0060): per ogni lingua l'oggetto di stringhe che la shell
   * registra nell'istanza i18n sotto lo spazio-nomi = `id` del modulo. Assente per i moduli non
   * ancora migrati all'i18n (restano con stringhe italiane cablate).
   */
  resources?: Partial<Record<Language, Record<string, unknown>>>
  /** Quota principale dell'app, per la barra di consumo della Dashboard (UC 0097). Facoltativa. */
  quota?: ModuleQuota
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
