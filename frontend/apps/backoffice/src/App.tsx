import { useMemo } from 'react'
import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from '@appgrove/design-system'
import { I18nextProvider } from '@appgrove/i18n'
import type { i18n as I18n } from 'i18next'
import { ConfigProvider, type RuntimeConfig } from './config'
import { ApiClientProvider } from './api/apiClient'
import { makeQueryClient } from './api/queryClient'
import { StubEntitlementsProvider } from './registry/entitlements'
import { SessionGate } from './auth/SessionGate'
import { routes } from './routing/routes'

const router = createBrowserRouter(routes)
const queryClient = makeQueryClient()

/**
 * Composizione dei provider (ordine: config → tema → i18n → query → api → entitlement → router).
 * `entitled` viene dal provider **stub** (il core non espone ancora gli entitlement — rinvio tracciato).
 */
export function App({ config, i18n }: { config: RuntimeConfig; i18n: I18n }) {
  // STUB entitlement: in attesa dell'endpoint core, il demo è abilitato in locale.
  const entitled = useMemo(() => (config.env === 'local' ? ['demo'] : []), [config.env])

  return (
    <ConfigProvider value={config}>
      <ThemeProvider>
        <I18nextProvider i18n={i18n}>
          <QueryClientProvider client={queryClient}>
            <ApiClientProvider>
              <StubEntitlementsProvider entitled={entitled}>
                <SessionGate>
                  <RouterProvider router={router} />
                </SessionGate>
              </StubEntitlementsProvider>
            </ApiClientProvider>
          </QueryClientProvider>
        </I18nextProvider>
      </ThemeProvider>
    </ConfigProvider>
  )
}
