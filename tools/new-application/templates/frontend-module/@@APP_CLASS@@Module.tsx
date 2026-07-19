import { useEffect } from 'react'
import { Route, Routes } from 'react-router-dom'
import { installErrorReporter } from '@appgrove/error-reporter'
import { useConfig } from '../../config'
import { useShellContext } from '../../registry/ShellContext'
import { @@APP_CLASS@@ClientProvider } from './api/@@APP_CLASS@@ClientProvider'
import { ItemListScreen } from './screens/ItemListScreen'
import { ItemCreateScreen } from './screens/ItemCreateScreen'
import { ItemDetailScreen } from './screens/ItemDetailScreen'

/**
 * Modulo **@@APP_NAME@@** montato dalla shell sotto `/app/@@APP_ID@@/*`. Autocontenuto: client
 * tipizzato co-locato, route interne, UI col design system. Legge `tenant_id`/`user_id` solo dal
 * contesto shell; l'enforcement quota è backend (il banner è solo esperienza d'uso).
 */
export default function @@APP_CLASS@@Module() {
  const config = useConfig()
  const shell = useShellContext()

  // Reporter errori del modulo (#08 23, UC 0006): registrato con l'app_id di QUESTA app, così un
  // errore del modulo non viene attribuito alla shell. Senza endpoint configurato (locale, test,
  // e2e) è inerte e non registra alcun handler. Si disinstalla allo smontaggio: il modulo è lazy e
  // può essere montato e smontato più volte nella stessa sessione.
  useEffect(
    () =>
      installErrorReporter({
        appId: '@@APP_ID@@',
        endpoint: config.errorIngestUrl,
        buildSha: import.meta.env.VITE_BUILD_SHA ?? 'dev',
        getContext: () => ({ userId: shell.userId, tenantId: shell.tenantId }),
      }),
    [config.errorIngestUrl, shell.userId, shell.tenantId],
  )

  return (
    <@@APP_CLASS@@ClientProvider>
      <div data-testid="@@APP_ID@@-module">
        <Routes>
          <Route index element={<ItemListScreen />} />
          <Route path="new" element={<ItemCreateScreen />} />
          <Route path=":id" element={<ItemDetailScreen />} />
        </Routes>
      </div>
    </@@APP_CLASS@@ClientProvider>
  )
}
