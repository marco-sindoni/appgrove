import type { Language } from '@appgrove/i18n'

/** Locale BCP-47 per la formattazione, derivato dalla lingua UI attiva (UC 0060). */
const LOCALE_TAG: Record<Language, string> = {
  en: 'en-GB',
  it: 'it-IT',
  fr: 'fr-FR',
  es: 'es-ES',
  de: 'de-DE',
}

/** Tag di locale dalla lingua i18n attiva (`i18n.language`, es. `fr` → `fr-FR`); default en-GB. */
export function localeTag(language: string | undefined): string {
  const key = language?.slice(0, 2) as Language | undefined
  return (key && LOCALE_TAG[key]) || 'en-GB'
}

/** Formatta un importo come valuta (default EUR) secondo la lingua attiva, con fallback robusto. */
export function formatAmount(amount: number | undefined, currency = 'EUR', language?: string): string {
  if (amount == null) return '—'
  try {
    return new Intl.NumberFormat(localeTag(language), { style: 'currency', currency }).format(amount)
  } catch {
    return `${amount.toFixed(2)} ${currency}`
  }
}

/** Formatta una data ISO secondo la lingua attiva, con fallback su trattino. */
export function formatDate(iso: string | undefined, language?: string): string {
  if (!iso) return '—'
  const d = new Date(iso)
  return Number.isNaN(d.getTime()) ? '—' : d.toLocaleDateString(localeTag(language))
}
