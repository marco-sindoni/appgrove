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

const ACME = { accountId: 'tenant-acme', accountName: 'Acme Corp' }
const BETA = { accountId: 'tenant-beta', accountName: 'Beta Srl' }

/**
 * Backend minimo per il selettore dell'account (UC 0117), **con stato**: l'account attivo cambia
 * davvero quando la scrittura arriva, e il token emesso dal rinnovo lo segue. Serve perché il cambio
 * di account **ricarica** l'applicazione: senza stato, dopo il ricaricamento il finto server
 * risponderebbe ancora con l'account di prima e il collaudo non proverebbe nulla.
 *
 * `roles` cambia con l'account, come nella realtà: owner nel proprio, member nell'altro. È la
 * differenza già vera oggi (le voci Account/Billing/Members sono di owner e admin) — la visibilità
 * per ruolo più fine è di UC 0107 e questo collaudo non la anticipa.
 */
async function mockBackend(page: Page, memberships: { accountId: string; accountName: string }[]) {
  const state = { active: memberships[0].accountId }
  const roleFor = (accountId: string) => (accountId === ACME.accountId ? 'owner' : 'member')

  await page.route('**/config.json', (route) =>
    route.fulfill({
      json: { env: 'test', authBaseUrl: ORIGIN, coreBaseUrl: ORIGIN, cognito: { userPoolId: '', clientId: '' } },
    }),
  )
  await page.route('**/api/auth/refresh', (route) =>
    route.fulfill({
      json: {
        // Il claim dell'account viene SEMPRE dal server: è lui a sapere qual è l'account attivo.
        access_token: jwt({
          sub: 'user-1',
          tenant_id: state.active,
          roles: [roleFor(state.active)],
          upn: 'u@x.io',
        }),
        id_token: jwt({ sub: 'user-1', email: 'u@x.io', name: 'Utente Uno' }),
        token_type: 'Bearer',
      },
    }),
  )
  await page.route('**/api/platform/v1/me/memberships', (route) =>
    route.fulfill({ json: { activeAccountId: state.active, memberships } }),
  )
  await page.route('**/api/platform/v1/me/active-account', async (route) => {
    const body = route.request().postDataJSON() as { accountId: string }
    if (!memberships.some((m) => m.accountId === body.accountId)) {
      // 404 e non 403: non si rivela l'esistenza di un account che non è tuo.
      return route.fulfill({ status: 404, json: { title: 'Not Found' } })
    }
    state.active = body.accountId
    return route.fulfill({ status: 204, body: '' })
  })
  await page.route('**/api/platform/v1/me/entitlements', (route) =>
    route.fulfill({ json: { entitlements: [] } }),
  )
  await page.route('**/api/platform/v1/me/legal/status', (route) =>
    route.fulfill({ json: { pending: [], notices: [] } }),
  )
}

test('[L2-ACCOUNT-SWITCH] con una sola appartenenza il nome c’è e il selettore NON esiste', async ({
  page,
}) => {
  // Il caso di tutti gli utenti di oggi: nessun comando in più, non «un comando disabilitato».
  await mockBackend(page, [ACME])
  await page.goto('/')

  const sidebar = page.getByRole('navigation', { name: 'Platform' })
  await expect(sidebar.getByTestId('active-account-name')).toHaveText('Acme Corp')
  await expect(page.getByLabel('Switch account')).toHaveCount(0)
})

test('[L2-ACCOUNT-SWITCH] due appartenenze: si cambia account, l’applicazione ricarica e i menu seguono il ruolo', async ({
  page,
}) => {
  await mockBackend(page, [ACME, BETA])
  await page.goto('/')

  const sidebar = page.getByRole('navigation', { name: 'Platform' })
  await expect(sidebar.getByTestId('active-account-name')).toHaveText('Acme Corp')
  // Owner nel proprio account: la voce di gestione delle persone c'è.
  await expect(sidebar.getByRole('link', { name: 'Members' })).toBeVisible()

  await page.getByLabel('Switch account').click()
  await page.getByRole('button', { name: /Beta Srl/ }).click()

  // Ricaricamento completo: il nome dell'account cambia e il token nuovo porta l'altro ruolo.
  await expect(sidebar.getByTestId('active-account-name')).toHaveText('Beta Srl')
  await expect(sidebar.getByRole('link', { name: 'Members' })).toHaveCount(0)

  // …e si torna indietro: la stessa persona, due esperienze diverse.
  await page.getByLabel('Switch account').click()
  await page.getByRole('button', { name: /Acme Corp/ }).click()
  await expect(sidebar.getByTestId('active-account-name')).toHaveText('Acme Corp')
  await expect(sidebar.getByRole('link', { name: 'Members' })).toBeVisible()
})

test('[L2-ACCOUNT-SWITCH] l’account cambiato in un’altra scheda si vede e invita a ricaricare', async ({
  page,
}) => {
  // Non è un varco (il token vecchio vale per un account a cui la persona appartiene davvero) ma è
  // confusione su chi si sta guardando, e la confusione è essa stessa un rischio.
  await mockBackend(page, [ACME, BETA])
  await page.route('**/api/platform/v1/me/memberships', (route) =>
    route.fulfill({ json: { activeAccountId: BETA.accountId, memberships: [ACME, BETA] } }),
  )
  await page.goto('/')

  await expect(page.getByRole('alert')).toContainText('The active account changed in another tab.')
  await expect(page.getByRole('button', { name: 'Reload' })).toBeVisible()
})
