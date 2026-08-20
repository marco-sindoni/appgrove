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

const MIO = { accountId: 'tenant-mio', accountName: 'Il mio account' }
const AZIENDA = { accountId: 'tenant-azienda', accountName: 'Azienda Beta' }

/**
 * Backend minimo per gli inviti ricevuti (UC 0118), **con stato**: accettare crea davvero la seconda
 * appartenenza e sposta l'account attivo, e il token emesso dopo il ricaricamento lo segue.
 *
 * Lo stato serve perché accettare **ricarica** l'applicazione: senza di esso, dopo il ricaricamento
 * il finto server risponderebbe con lo stesso invito ancora in attesa e il collaudo non proverebbe
 * nulla — proverebbe solo che il pulsante si può premere.
 */
async function mockBackend(page: Page) {
  const state = {
    active: MIO.accountId,
    memberships: [MIO],
    invitations: [{ id: 'inv-1', accountId: AZIENDA.accountId, accountName: AZIENDA.accountName }],
  }

  await page.route('**/config.json', (route) =>
    route.fulfill({
      json: { env: 'test', authBaseUrl: ORIGIN, coreBaseUrl: ORIGIN, cognito: { userPoolId: '', clientId: '' } },
    }),
  )
  await page.route('**/api/auth/refresh', (route) =>
    route.fulfill({
      json: {
        // Il claim dell'account viene SEMPRE dal server: è lui a sapere qual è l'account attivo.
        access_token: jwt({ sub: 'user-1', tenant_id: state.active, roles: ['owner'], upn: 'u@x.io' }),
        id_token: jwt({ sub: 'user-1', email: 'u@x.io', name: 'Utente Uno' }),
        token_type: 'Bearer',
      },
    }),
  )
  await page.route('**/api/platform/v1/me/invitations', (route) =>
    route.fulfill({ json: { invitations: state.invitations } }),
  )
  await page.route('**/api/platform/v1/me/invitations/*/accept', (route) => {
    // Accettare crea l'appartenenza e la rende attiva: è esattamente ciò che fa il servizio.
    state.memberships = [MIO, AZIENDA]
    state.active = AZIENDA.accountId
    state.invitations = []
    return route.fulfill({ status: 204, body: '' })
  })
  await page.route('**/api/platform/v1/me/invitations/*/reject', (route) => {
    state.invitations = []
    return route.fulfill({ status: 204, body: '' })
  })
  await page.route('**/api/platform/v1/me/memberships', (route) =>
    route.fulfill({ json: { activeAccountId: state.active, memberships: state.memberships } }),
  )
  await page.route('**/api/platform/v1/me/entitlements', (route) =>
    route.fulfill({ json: { entitlements: [] } }),
  )
  await page.route('**/api/platform/v1/me/legal/status', (route) =>
    route.fulfill({ json: { pending: [], notices: [] } }),
  )
  await page.route('**/api/platform/v1/accounts/me', (route) =>
    route.fulfill({
      json: {
        id: state.active,
        name: state.memberships.find((m) => m.accountId === state.active)?.accountName,
        status: 'active',
      },
    }),
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

test('[L2-INVITE-EXISTING] invito ricevuto: sezione in testa al cruscotto, numero nel menu, accettazione con ricarica', async ({
  page,
}) => {
  await mockBackend(page)
  await page.goto('/')

  // La sezione è in TESTA al cruscotto, non un pulsantino nell'intestazione: un invito a collaborare
  // con un'altra azienda chiede una decisione consapevole.
  const invito = page.getByText('Azienda Beta invites you to work in their account.')
  await expect(invito).toBeVisible()
  // Chi paga il posto va detto qui, non scoperto in fattura.
  await expect(page.getByText('The seat is paid by the account that invites you, not by you.')).toBeVisible()

  // Il numero sulla voce «Dashboard» del menu: da un'altra schermata l'invito resterebbe invisibile.
  const sidebar = page.getByRole('navigation', { name: 'Platform' })
  await expect(sidebar.getByLabel('1 invitation waiting')).toBeVisible()

  // Con una sola appartenenza il selettore dell'account NON esiste (UC 0117): è il caso di partenza.
  await expect(page.getByLabel('Switch account')).toHaveCount(0)

  // Accettare RICARICA l'applicazione: il claim nuovo nasce dal ricaricamento, e attenderlo è parte
  // del comportamento — senza l'attesa l'asserzione guarderebbe ancora il documento vecchio.
  const reloaded = page.waitForEvent('load')
  await page.getByRole('button', { name: 'Accept' }).click()
  await reloaded

  // Dopo l'accettazione: due appartenenze → il selettore compare, e l'account attivo è quello nuovo.
  await expect(sidebar.getByTestId('active-account-name')).toHaveText('Azienda Beta')
  await expect(page.getByLabel('Switch account')).toBeVisible()
  // L'invito non è più in attesa: sezione e numero sparisco insieme.
  await expect(page.getByText('Azienda Beta invites you to work in their account.')).toHaveCount(0)
  await expect(sidebar.getByLabel('1 invitation waiting')).toHaveCount(0)
})

test('[L2-INVITE-EXISTING] rifiutare l’invito lo chiude senza entrare in nessun account', async ({ page }) => {
  await mockBackend(page)
  await page.goto('/')

  await expect(page.getByText('Azienda Beta invites you to work in their account.')).toBeVisible()
  await page.getByRole('button', { name: 'Decline' }).click()

  // La voce sparisce e nessuna appartenenza nasce: il selettore resta assente.
  await expect(page.getByText('Azienda Beta invites you to work in their account.')).toHaveCount(0)
  await expect(page.getByLabel('Switch account')).toHaveCount(0)
})
