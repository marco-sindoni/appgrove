import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import {
  ACCENTS,
  DEFAULT_ACCENT,
  DEFAULT_THEME,
  THEMES,
  applyTheme,
  type Accent,
  type Theme,
} from './theme'

interface ThemeContextValue {
  theme: Theme
  accent: Accent
  setTheme: (t: Theme) => void
  setAccent: (a: Accent) => void
  toggleTheme: () => void
}

const ThemeContext = createContext<ThemeContextValue | null>(null)

export interface ThemeProviderProps {
  children: ReactNode
  defaultTheme?: Theme
  defaultAccent?: Accent
  /** Nodo su cui scrivere data-theme/data-accent. Default: document.documentElement. */
  target?: HTMLElement
}

/**
 * Provider che espone tema/accent come stato React e li riflette sugli attributi del nodo target
 * (CSS custom properties → cambio istantaneo). È il contratto shell→componenti per il theming (#03 dec.11).
 */
export function ThemeProvider({
  children,
  defaultTheme = DEFAULT_THEME,
  defaultAccent = DEFAULT_ACCENT,
  target,
}: ThemeProviderProps) {
  const [theme, setTheme] = useState<Theme>(defaultTheme)
  const [accent, setAccent] = useState<Accent>(defaultAccent)

  useEffect(() => {
    const node = target ?? (typeof document !== 'undefined' ? document.documentElement : undefined)
    if (node) applyTheme({ theme, accent }, node)
  }, [theme, accent, target])

  const value = useMemo<ThemeContextValue>(
    () => ({
      theme,
      accent,
      setTheme,
      setAccent,
      toggleTheme: () => setTheme((t) => (t === 'light' ? 'dark' : 'light')),
    }),
    [theme, accent],
  )

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}

export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext)
  if (!ctx) throw new Error('useTheme deve essere usato dentro <ThemeProvider>')
  return ctx
}

export { THEMES, ACCENTS }
