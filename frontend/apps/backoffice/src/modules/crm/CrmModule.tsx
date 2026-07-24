import { useEffect } from 'react'
import { Route, Routes } from 'react-router-dom'
import { installErrorReporter } from '@appgrove/error-reporter'
import { useConfig } from '../../config'
import { useShellContext } from '../../registry/ShellContext'
import { CrmClientProvider } from './api/CrmClientProvider'
import { ContactListScreen } from './screens/ContactListScreen'
import { ContactCreateScreen } from './screens/ContactCreateScreen'
import { ContactDetailScreen } from './screens/ContactDetailScreen'
import { MembersScreen } from './screens/MembersScreen'

/**
 * Modulo **Mini-CRM** montato dalla shell sotto `/app/crm/*`. Autocontenuto: client
 * tipizzato co-locato, route interne, UI col design system. Legge `tenant_id`/`user_id` solo dal
 * contesto shell; l'enforcement quota è backend (il banner è solo esperienza d'uso).
 */
export default function CrmModule() {
  const config = useConfig()
  const shell = useShellContext()

  // Reporter errori del modulo (#08 23, UC 0006): registrato con l'app_id di QUESTA app, così un
  // errore del modulo non viene attribuito alla shell. Senza endpoint configurato (locale, test,
  // e2e) è inerte e non registra alcun handler. Si disinstalla allo smontaggio: il modulo è lazy e
  // può essere montato e smontato più volte nella stessa sessione.
  useEffect(
    () =>
      installErrorReporter({
        appId: 'crm',
        endpoint: config.errorIngestUrl,
        buildSha: import.meta.env.VITE_BUILD_SHA ?? 'dev',
        getContext: () => ({ userId: shell.userId, tenantId: shell.tenantId }),
      }),
    [config.errorIngestUrl, shell.userId, shell.tenantId],
  )

  return (
    <CrmClientProvider>
      <div data-testid="crm-module">
        <Routes>
          <Route index element={<ContactListScreen />} />
          <Route path="new" element={<ContactCreateScreen />} />
          <Route path="members" element={<MembersScreen />} />
          <Route path=":id" element={<ContactDetailScreen />} />
        </Routes>
      </div>
    </CrmClientProvider>
  )
}
