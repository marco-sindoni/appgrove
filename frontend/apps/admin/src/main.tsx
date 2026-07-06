import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { installErrorReporter } from '@appgrove/error-reporter'
import { createI18n } from '@appgrove/i18n'
import { App } from './App'
import { useAuthStore } from './auth/authStore'
import { loadConfig } from './config'
import './styles.css'

/** Bootstrap: carica `config.json` runtime PRIMA del render (#03/#12), poi monta l'app. */
async function bootstrap() {
  const rootEl = document.getElementById('root')
  if (!rootEl) throw new Error('#root non trovato')

  try {
    const config = await loadConfig()
    // Reporter globale errori (#08/23, UC 0006): registrato subito dopo la config (l'endpoint
    // arriva da lì) e prima del render, così cattura anche gli errori di mount. Senza endpoint
    // (locale/e2e) è un NO-OP. Gli id utente/tenant sono opachi, letti dallo store di sessione.
    installErrorReporter({
      appId: 'admin',
      endpoint: config.errorIngestUrl,
      buildSha: import.meta.env.VITE_BUILD_SHA ?? 'dev',
      getContext: () => {
        const claims = useAuthStore.getState().claims
        return claims ? { userId: claims.userId, tenantId: claims.tenantId } : {}
      },
    })
    const i18n = createI18n()
    createRoot(rootEl).render(
      <StrictMode>
        <App config={config} i18n={i18n} />
      </StrictMode>,
    )
  } catch (err) {
    rootEl.textContent = `Configurazione non caricata: ${(err as Error).message}`
  }
}

void bootstrap()
