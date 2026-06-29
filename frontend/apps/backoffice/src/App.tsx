import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from '@appgrove/design-system'
import { I18nextProvider } from '@appgrove/i18n'
import type { i18n as I18n } from 'i18next'
import { ConfigProvider, type RuntimeConfig } from './config'
import { ApiClientProvider } from './api/apiClient'
import { makeQueryClient } from './api/queryClient'
import { EntitlementsProvider } from './registry/entitlements'
import { SessionGate } from './auth/SessionGate'
import { routes } from './routing/routes'

const router = createBrowserRouter(routes)
const queryClient = makeQueryClient()

/**
 * Composizione dei provider (ordine: config → tema → i18n → query → api → session → entitlement → router).
 * Gli entitlement sono **reali** (UC 0027): `EntitlementsProvider` legge `/me/entitlements` da core, ed è
 * montato **dentro** il SessionGate (sessione stabile → token disponibile). Il modulo `demo` (senza
 * backend) resta abilitato solo in locale.
 */
export function App({ config, i18n }: { config: RuntimeConfig; i18n: I18n }) {
  return (
    <ConfigProvider value={config}>
      <ThemeProvider>
        <I18nextProvider i18n={i18n}>
          <QueryClientProvider client={queryClient}>
            <ApiClientProvider>
              <SessionGate>
                <EntitlementsProvider demoInLocal={config.env === 'local'}>
                  <RouterProvider router={router} />
                </EntitlementsProvider>
              </SessionGate>
            </ApiClientProvider>
          </QueryClientProvider>
        </I18nextProvider>
      </ThemeProvider>
    </ConfigProvider>
  )
}
