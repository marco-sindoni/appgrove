// @appgrove/i18n — setup react-i18next + cataloghi delle 5 lingue del sito (UC 0020, UC 0060).
import i18next, { type i18n } from 'i18next'
import { initReactI18next } from 'react-i18next'
import { en, type Resources } from './resources/en'
import { it } from './resources/it'
import { fr } from './resources/fr'
import { es } from './resources/es'
import { de } from './resources/de'

// Le 5 lingue del sito vetrina (UC 0037): la UI delle app è allineata alla stessa parità (UC 0060).
export const LANGUAGES = ['en', 'it', 'fr', 'es', 'de'] as const
export type Language = (typeof LANGUAGES)[number]
export const DEFAULT_LANGUAGE: Language = 'en'

/** Nomi delle lingue nella lingua stessa (endonimi), per il selettore in topbar. */
export const LANGUAGE_LABELS: Record<Language, string> = {
  en: 'English',
  it: 'Italiano',
  fr: 'Français',
  es: 'Español',
  de: 'Deutsch',
}

export const resources = {
  en: { translation: en },
  it: { translation: it },
  fr: { translation: fr },
  es: { translation: es },
  de: { translation: de },
} as const

/** Chiave di persistenza della lingua scelta manualmente (localStorage). */
export const LANGUAGE_STORAGE_KEY = 'appgrove.lang'

function isSupported(value: string | null | undefined): value is Language {
  return !!value && (LANGUAGES as readonly string[]).includes(value)
}

/**
 * Lingua attiva all'avvio, da una fonte controllabile anche dalla cattura screenshot (UC 0060):
 * preferenza salvata → lingua del browser (`navigator.language`) → default. Sempre ristretta alle
 * lingue supportate. Robusta fuori dal browser (test/SSR): torna al default.
 */
export function detectLanguage(): Language {
  if (typeof window === 'undefined') return DEFAULT_LANGUAGE
  try {
    const saved = window.localStorage?.getItem(LANGUAGE_STORAGE_KEY)
    if (isSupported(saved)) return saved
  } catch {
    // localStorage non accessibile (modalità privata / policy): si prosegue col browser.
  }
  const browser = window.navigator?.language?.slice(0, 2)
  return isSupported(browser) ? browser : DEFAULT_LANGUAGE
}

/** Ricorda la lingua scelta manualmente, così da ritrovarla al ricaricamento (UC 0060). NO-OP fuori dal browser. */
export function persistLanguage(language: Language): void {
  if (typeof window === 'undefined') return
  try {
    window.localStorage?.setItem(LANGUAGE_STORAGE_KEY, language)
  } catch {
    // Persistenza best-effort: se localStorage non è disponibile, la scelta resta valida per la sessione.
  }
}

/**
 * Crea un'istanza i18next isolata (una per app/per test) già legata a react-i18next.
 * La lingua si cambia a runtime con `instance.changeLanguage('it')` (topbar); l'avvio parte dalla
 * lingua rilevata quando l'app passa `detectLanguage()` (UC 0060).
 */
export function createI18n(language: Language = DEFAULT_LANGUAGE): i18n {
  const instance = i18next.createInstance()
  void instance.use(initReactI18next).init({
    resources,
    lng: language,
    fallbackLng: DEFAULT_LANGUAGE,
    interpolation: { escapeValue: false },
    returnNull: false,
  })
  return instance
}

export type { Resources }
export { useTranslation, Trans, I18nextProvider } from 'react-i18next'

// Tipizzazione delle chiavi: `t('nav.platform')` è type-safe per i consumer (autocompletamento + drift catch).
declare module 'i18next' {
  interface CustomTypeOptions {
    defaultNS: 'translation'
    resources: { translation: Resources }
  }
}
