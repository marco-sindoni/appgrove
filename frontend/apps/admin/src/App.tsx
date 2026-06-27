import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from '@appgrove/design-system'
import { I18nextProvider } from '@appgrove/i18n'
import type { i18n as I18n } from 'i18next'
import { ConfigProvider, type RuntimeConfig } from './config'
import { ApiClientProvider } from './api/apiClient'
import { makeQueryClient } from './api/queryClient'
import { SessionGate } from './auth/SessionGate'
import { routes } from './routing/routes'

const router = createBrowserRouter(routes)
const queryClient = makeQueryClient()

/**
 * Composizione dei provider della console admin (ordine: config → tema → i18n → query → api → router).
 * `SessionGate` ripristina la sessione via cookie prima di montare il router.
 */
export function App({ config, i18n }: { config: RuntimeConfig; i18n: I18n }) {
  return (
    <ConfigProvider value={config}>
      <ThemeProvider>
        <I18nextProvider i18n={i18n}>
          <QueryClientProvider client={queryClient}>
            <ApiClientProvider>
              <SessionGate>
                <RouterProvider router={router} />
              </SessionGate>
            </ApiClientProvider>
          </QueryClientProvider>
        </I18nextProvider>
      </ThemeProvider>
    </ConfigProvider>
  )
}
