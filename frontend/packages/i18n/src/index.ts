// @appgrove/i18n — setup react-i18next + cataloghi EN/IT (UC 0020).
import i18next, { type i18n } from 'i18next'
import { initReactI18next } from 'react-i18next'
import { en, type Resources } from './resources/en'
import { it } from './resources/it'

export const LANGUAGES = ['en', 'it'] as const
export type Language = (typeof LANGUAGES)[number]
export const DEFAULT_LANGUAGE: Language = 'en'

export const resources = {
  en: { translation: en },
  it: { translation: it },
} as const

/**
 * Crea un'istanza i18next isolata (una per app/per test) già legata a react-i18next.
 * La lingua si cambia a runtime con `instance.changeLanguage('it')` (topbar, #03 dec.10).
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
