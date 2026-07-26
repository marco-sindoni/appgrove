import { useTranslation, type Language } from '@appgrove/i18n'
import { en, type @@APP_CLASS@@Messages } from './en'
import { it } from './it'
import { fr } from './fr'
import { es } from './es'
import { de } from './de'

/**
 * Bundle di traduzione per-modulo (UC 0060): la shell li registra nell'istanza i18n sotto lo
 * spazio-nomi = `id` del modulo (`@@APP_ID@@`) per le etichette di navigazione; le schermate li
 * leggono via `use@@APP_CLASS@@Messages`.
 */
export const @@APP_CAMEL@@Resources: Partial<Record<Language, @@APP_CLASS@@Messages>> = { en, it, fr, es, de }

/**
 * Stringhe del modulo nella lingua UI attiva, reattive al cambio lingua (l'hook si ri-renderizza
 * sull'evento `languageChanged`). Fallback su EN.
 */
export function use@@APP_CLASS@@Messages(): @@APP_CLASS@@Messages {
  const { i18n } = useTranslation()
  const key = i18n.language?.slice(0, 2) as Language | undefined
  return (key && @@APP_CAMEL@@Resources[key]) || en
}

export type { @@APP_CLASS@@Messages }
export { formatAmount, formatDate, localeTag } from './format'
