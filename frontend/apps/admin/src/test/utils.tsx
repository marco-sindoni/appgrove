import type { ReactElement, ReactNode } from 'react'
import { render, type RenderResult } from '@testing-library/react'
import { MemoryRouter, RouterProvider, createMemoryRouter } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from '@appgrove/design-system'
import { I18nextProvider, createI18n } from '@appgrove/i18n'
import type { ApiClient } from '@appgrove/api-client'
import { ConfigProvider, type RuntimeConfig } from '../config'
import { ApiClientProvider } from '../api/apiClient'
import { makeQueryClient } from '../api/queryClient'
import { routes } from '../routing/routes'

export const testConfig: RuntimeConfig = {
  env: 'test',
  authBaseUrl: 'http://localhost',
  coreBaseUrl: 'http://localhost',
  cognito: { userPoolId: '', clientId: '' },
  errorIngestUrl: '', // vuoto nei test: il reporter errori è inerte
}

/** Codifica base64url (browser-safe, senza padding) per i payload dei JWT finti. */
function base64url(obj: unknown): string {
  return btoa(JSON.stringify(obj)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

/** JWT finto (header.payload.sig) per i test: la firma non viene verificata lato client. */
export function fakeJwt(payload: Record<string, unknown>): string {
  return `${base64url({ alg: 'none' })}.${base64url(payload)}.sig`
}

export const fakeAccessToken = (overrides: Record<string, unknown> = {}) =>
  fakeJwt({ sub: 'admin-1', tenant_id: 'tenant-1', roles: ['platform-admin'], upn: 'admin@x.io', ...overrides })

export const fakeIdToken = (overrides: Record<string, unknown> = {}) =>
  fakeJwt({ sub: 'admin-1', email: 'admin@x.io', name: 'Admin Uno', ...overrides })

interface ProviderOptions {
  apiClient?: ApiClient
  route?: string
}

function Providers({ children, apiClient }: ProviderOptions & { children: ReactNode }) {
  return (
    <ConfigProvider value={testConfig}>
      <ThemeProvider>
        <I18nextProvider i18n={createI18n()}>
          <QueryClientProvider client={makeQueryClient()}>
            <ApiClientProvider client={apiClient}>{children}</ApiClientProvider>
          </QueryClientProvider>
        </I18nextProvider>
      </ThemeProvider>
    </ConfigProvider>
  )
}

/** Render di un componente con tutti i provider + un MemoryRouter (per i test di componente). */
export function renderWithProviders(
  ui: ReactElement,
  options: ProviderOptions = {},
): RenderResult {
  return render(
    <Providers {...options}>
      <MemoryRouter initialEntries={[options.route ?? '/']}>{ui}</MemoryRouter>
    </Providers>,
  )
}

/** Render dell'intera app (route reali) su un MemoryRouter — per test di navigazione/guard. */
export function renderApp(options: ProviderOptions = {}): RenderResult {
  const router = createMemoryRouter(routes, { initialEntries: [options.route ?? '/'] })
  return render(
    <Providers {...options}>
      <RouterProvider router={router} />
    </Providers>,
  )
}
