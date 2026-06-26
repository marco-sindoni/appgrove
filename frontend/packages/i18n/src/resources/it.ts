import type { Resources } from './en'

// Catalogo IT — traduzione del design EN (UC 0020 §i18n).
export const it: Resources = {
  app: { name: 'appgrove' },
  nav: {
    platform: 'Piattaforma',
    yourApps: 'Le tue app',
    dashboard: 'Dashboard',
    settings: 'Impostazioni',
    account: 'Account',
    billing: 'Fatturazione',
  },
  topbar: {
    notifications: 'Notifiche',
    toggleTheme: 'Cambia tema',
    language: 'Lingua',
    accent: 'Accento',
    userMenu: 'Menu utente',
    logout: 'Esci',
  },
  states: {
    loading: 'Caricamento…',
    empty: 'Ancora niente qui',
    error: 'Qualcosa è andato storto',
    retry: 'Riprova',
  },
  auth: {
    restoring: 'Ripristino della sessione…',
    loginRequired: 'Accedi per continuare',
    notEntitled: 'Non hai accesso a questa app',
    signIn: 'Accedi',
  },
  settings: {
    title: 'Impostazioni',
    displayName: 'Nome visualizzato',
    save: 'Salva',
    saved: 'Salvato',
  },
  validation: {
    required: 'Campo obbligatorio',
    tooShort: 'Troppo corto (min {{min}})',
    tooLong: 'Troppo lungo (max {{max}})',
  },
}
