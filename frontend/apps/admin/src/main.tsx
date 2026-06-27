import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { createI18n } from '@appgrove/i18n'
import { App } from './App'
import { loadConfig } from './config'
import './styles.css'

/** Bootstrap: carica `config.json` runtime PRIMA del render (#03/#12), poi monta l'app. */
async function bootstrap() {
  const rootEl = document.getElementById('root')
  if (!rootEl) throw new Error('#root non trovato')

  try {
    const config = await loadConfig()
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
