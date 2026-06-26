// Catalogo EN — fonte del design (UC 0020 §i18n). L'IT ne è la traduzione (it.ts).
export const en = {
  app: { name: 'appgrove' },
  nav: {
    platform: 'Platform',
    yourApps: 'Your apps',
    dashboard: 'Dashboard',
    settings: 'Settings',
    account: 'Account',
    billing: 'Billing',
  },
  topbar: {
    notifications: 'Notifications',
    toggleTheme: 'Toggle theme',
    language: 'Language',
    accent: 'Accent',
    userMenu: 'User menu',
    logout: 'Log out',
  },
  states: {
    loading: 'Loading…',
    empty: 'Nothing here yet',
    error: 'Something went wrong',
    retry: 'Retry',
  },
  auth: {
    restoring: 'Restoring your session…',
    loginRequired: 'Please sign in to continue',
    notEntitled: 'You don’t have access to this app',
    signIn: 'Sign in',
  },
  settings: {
    title: 'Settings',
    displayName: 'Display name',
    save: 'Save',
    saved: 'Saved',
  },
  validation: {
    required: 'This field is required',
    tooShort: 'Too short (min {{min}})',
    tooLong: 'Too long (max {{max}})',
  },
}

/** Forma del catalogo (chiavi tipizzate, valori string): IT ne è un'implementazione (it.ts). */
export type Resources = typeof en
