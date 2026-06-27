import { createContext, useContext } from 'react'

/**
 * Configurazione runtime caricata da `public/config.json` all'avvio (#03/#12):
 * **un solo build** promosso local→test→prod, endpoint parametrizzati per ambiente.
 * Il file NON va messo in cache (rivalidato a ogni load).
 */
export interface RuntimeConfig {
  env: string
  /** Base URL del provider auth (`/api/auth/*`). In locale: auth-local via Caddy. */
  authBaseUrl: string
  /** Base URL dell'API core (`/api/platform/v1/*`). */
  coreBaseUrl: string
  /** Parametri Cognito cloud (placeholder in locale: si usa auth-local). */
  cognito: { userPoolId: string; clientId: string }
}

/** Carica e valida la config runtime prima del render dell'app. */
export async function loadConfig(): Promise<RuntimeConfig> {
  const res = await fetch('/config.json', { cache: 'no-store' })
  if (!res.ok) throw new Error(`config.json non caricato: HTTP ${res.status}`)
  const cfg = (await res.json()) as Partial<RuntimeConfig>
  if (!cfg.authBaseUrl || !cfg.coreBaseUrl) {
    throw new Error('config.json incompleto: authBaseUrl/coreBaseUrl mancanti')
  }
  return {
    env: cfg.env ?? 'unknown',
    authBaseUrl: cfg.authBaseUrl,
    coreBaseUrl: cfg.coreBaseUrl,
    cognito: cfg.cognito ?? { userPoolId: '', clientId: '' },
  }
}

const ConfigContext = createContext<RuntimeConfig | null>(null)
export const ConfigProvider = ConfigContext.Provider

export function useConfig(): RuntimeConfig {
  const cfg = useContext(ConfigContext)
  if (!cfg) throw new Error('useConfig deve essere usato dentro <ConfigProvider>')
  return cfg
}
