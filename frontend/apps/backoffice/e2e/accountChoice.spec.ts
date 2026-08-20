import { test, expect, type Page } from '@playwright/test'

const ORIGIN = 'http://localhost:4173'

function base64url(obj: unknown): string {
  return Buffer.from(JSON.stringify(obj))
    .toString('base64')
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')
}
const jwt = (payload: Record<string, unknown>) =>
  `${base64url({ alg: 'none' })}.${base64url(payload)}.sig`

const ACME = { account_id: 'tenant-acme', account_name: 'Acme Corp' }
const BETA = { account_id: 'tenant-beta', account_name: 'Beta Srl' }

/**
 * La schermata di **scelta dell'account** all'accesso (UC 0118), che chiude il rimando di UC 0117:
 * l'esito «appartieni a più account e nessuno è attivo» esisteva già lato server, ma non c'era dove
 * rispondere. Ora è una sfida, additiva esattamente come quella del secondo fattore.
 *
 * Lo stato di partenza è **anonimo**: il rinnovo al caricamento fallisce, quindi si arriva
 * all'accesso. È lì che la scelta si fa — in un posto solo.
 */
async function mockBackend(page: Page) {
  const state = { authed: false, chosen: '' }

  await page.route('**/config.json', (route) =>
    route.fulfill({
      json: { env: 'test', authBaseUrl: ORIGIN, coreBaseUrl: ORIGIN, cognito: { userPoolId: '', clientId: '' } },
    }),
  )
  await page.route('**/api/auth/refresh', (route) =>
    state.authed
      ? route.fulfill({
          json: {
            access_token: jwt({ sub: 'user-1', tenant_id: state.chosen, roles: ['owner'], upn: 'u@x.io' }),
            id_token: jwt({ sub: 'user-1', email: 'u@x.io', name: 'Utente Uno' }),
            token_type: 'Bearer',
          },
        })
      : route.fulfill({ status: 401, json: { title: 'Unauthorized' } }),
  )
  // L'accesso NON emette token: la persona appartiene a due account e nessuno è attivo.
  await page.route('**/api/auth/login', (route) =>
    route.fulfill({
      json: { account_selection_required: true, choice_token: 'ch-1', accounts: [ACME, BETA] },
    }),
  )
  await page.route('**/api/auth/login/account', async (route) => {
    const body = route.request().postDataJSON() as { choice_token: string; account_id: string }
    if (body.choice_token !== 'ch-1' || ![ACME, BETA].some((a) => a.account_id === body.account_id)) {
      // 404 e non 403: l'esistenza di un account non è un'informazione di chi chiede.
      return route.fulfill({ status: 404, json: { title: 'Not Found' } })
    }
    state.authed = true
    state.chosen = body.account_id
    return route.fulfill({
      json: {
        access_token: jwt({ sub: 'user-1', tenant_id: body.account_id, roles: ['owner'], upn: 'u@x.io' }),
        id_token: jwt({ sub: 'user-1', email: 'u@x.io', name: 'Utente Uno' }),
        token_type: 'Bearer',
      },
    })
  })
  await page.route('**/api/platform/v1/me/memberships', (route) =>
    route.fulfill({
      json: {
        activeAccountId: state.chosen,
        memberships: [
          { accountId: ACME.account_id, accountName: ACME.account_name },
          { accountId: BETA.account_id, accountName: BETA.account_name },
        ],
      },
    }),
  )
  await page.route('**/api/platform/v1/me/invitations', (route) => route.fulfill({ json: { invitations: [] } }))
  await page.route('**/api/platform/v1/me/entitlements', (route) =>
    route.fulfill({ json: { entitlements: [] } }),
  )
  await page.route('**/api/platform/v1/me/legal/status', (route) =>
    route.fulfill({ json: { pending: [], notices: [] } }),
  )
  await page.route('**/api/platform/v1/accounts/me', (route) =>
    route.fulfill({ json: { id: state.chosen, name: 'Beta Srl', status: 'active' } }),
  )
  await page.route('**/api/platform/v1/apps/catalog**', (route) => route.fulfill({ json: { apps: [] } }))
  await page.route('**/api/platform/v1/me/subscriptions', (route) =>
    route.fulfill({ json: { subscriptions: [] } }),
  )
  await page.route('**/api/platform/v1/users**', (route) =>
    route.fulfill({ json: { content: [], page: 0, size: 100, totalElements: 0 } }),
  )
  await page.route('**/api/platform/v1/invitations**', (route) =>
    route.fulfill({ json: { content: [], page: 0, size: 100, totalElements: 0 } }),
  )
  await page.route('**/api/auth/2fa/status', (route) => route.fulfill({ json: { enabled: true } }))
}

test('[L2-ACCOUNT-CHOICE] più account e nessuna scelta: si scegli all’accesso e si entra in quello scelto', async ({
  page,
}) => {
  await mockBackend(page)
  await page.goto('/login')

  await page.getByLabel('Email').fill('persona@x.io')
  await page.getByLabel('Password').fill('Password1!')
  await page.getByRole('button', { name: 'Sign in' }).click()

  // La schermata di scelta, non un messaggio d'errore: la persona è già provata, e l'elenco dei suoi
  // account lo vede solo lei.
  await expect(page.getByText('Choose an account')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Acme Corp' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Beta Srl' })).toBeVisible()

  await page.getByRole('button', { name: 'Beta Srl' }).click()

  // Dentro l'account scelto, con il selettore presente (due appartenenze).
  const sidebar = page.getByRole('navigation', { name: 'Platform' })
  await expect(sidebar.getByTestId('active-account-name')).toHaveText('Beta Srl')
  await expect(page.getByLabel('Switch account')).toBeVisible()
})
