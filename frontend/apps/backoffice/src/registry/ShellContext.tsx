import { createContext, useContext, useMemo, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTheme } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { getAccessToken, useAuthStore } from '../auth/authStore'
import type { ShellContextValue } from './types'

const ShellContext = createContext<ShellContextValue | null>(null)

/** Espone il contratto shell↔modulo (token getter, tenant/user/ruoli, theme, nav API). */
export function ShellProvider({ children }: { children: ReactNode }) {
  const claims = useAuthStore((s) => s.claims)
  const { theme, accent, setAccent } = useTheme()
  const navigate = useNavigate()
  const { i18n } = useTranslation()

  const value = useMemo<ShellContextValue>(
    () => ({
      getToken: getAccessToken,
      tenantId: claims?.tenantId ?? '',
      userId: claims?.userId ?? '',
      roles: claims?.roles ?? [],
      theme: { theme, accent },
      nav: {
        navigate: (to) => navigate(to),
        setAccent,
        setLanguage: (language) => void i18n.changeLanguage(language),
      },
    }),
    [claims, theme, accent, setAccent, navigate, i18n],
  )

  return <ShellContext.Provider value={value}>{children}</ShellContext.Provider>
}

/** Hook usato dai moduli app per accedere al contesto della shell. */
export function useShellContext(): ShellContextValue {
  const ctx = useContext(ShellContext)
  if (!ctx) throw new Error('useShellContext deve essere usato dentro <ShellProvider>')
  return ctx
}
