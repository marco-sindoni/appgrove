import { useTranslation, type Language } from '@appgrove/i18n'
import { en, type FattureMessages } from './en'
import { it } from './it'
import { fr } from './fr'
import { es } from './es'
import { de } from './de'

/** Spazio-nomi i18n del modulo: DEVE combaciare con l'`id` del manifest (`fatture`). */
export const FATTURE_NS = 'fatture'

/**
 * Bundle di traduzione per-modulo (UC 0060): la shell li registra nell'istanza i18n sotto lo
 * spazio-nomi `fatture` (per le etichette di navigazione), le schermate li leggono via `useFattureMessages`.
 */
export const fattureResources: Partial<Record<Language, FattureMessages>> = { en, it, fr, es, de }

/**
 * Stringhe del modulo nella lingua UI attiva, reattive al cambio lingua (l'hook si ri-renderizza
 * sull'evento `languageChanged`). Fallback su EN. Sostituisce il vecchio `strings.ts` cablato.
 */
export function useFattureMessages(): FattureMessages {
  const { i18n } = useTranslation()
  const key = i18n.language?.slice(0, 2) as Language | undefined
  return (key && fattureResources[key]) || en
}

export type { FattureMessages }
export { formatAmount, formatDate, localeTag } from './format'
