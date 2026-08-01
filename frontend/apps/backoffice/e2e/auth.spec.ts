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
const accessToken = jwt({ sub: 'user-1', tenant_id: 'tenant-1', roles: ['owner'], upn: 'u@x.io' })
const idToken = jwt({ sub: 'user-1', email: 'u@x.io', name: 'Utente Uno' })
const tokenBody = { access_token: accessToken, id_token: idToken, token_type: 'Bearer' }

/** Stato di partenza: anonimo (refresh-on-load fallisce) finché non si effettua il login/verifica. */
async function mockAnonymous(page: Page, authed: { value: boolean }) {
  await page.route('**/config.json', (route) =>
    route.fulfill({
      json: { env: 'local', authBaseUrl: ORIGIN, coreBaseUrl: ORIGIN, cognito: { userPoolId: '', clientId: '' } },
    }),
  )
  await page.route('**/api/auth/refresh', (route) =>
    authed.value
      ? route.fulfill({ json: tokenBody })
      : route.fulfill({ status: 401, json: { title: 'Unauthorized' } }),
  )
  await page.route('**/api/platform/v1/users/me', (route) =>
    route.fulfill({ json: { id: 'u1', email: 'u@x.io', displayName: 'U', role: 'owner', status: 'active', tenantId: 'tenant-1' } }),
  )
  await page.route('**/api/platform/v1/accounts/me', (route) =>
    route.fulfill({ json: { id: 'a1', name: 'Acme', status: 'active' } }),
  )
  // Entitlement del tenant: va simulato sempre, altrimenti la shell segnala (giustamente) che non
  // riesce a leggerli — UC 0077.
  await page.route('**/api/platform/v1/me/entitlements', (route) =>
    route.fulfill({ json: { entitlements: [] } }),
  )
}

test('[L2-AUTH] anonimo: aprire la radice porta al login (la shell non resta appesa)', async ({ page }) => {
  // Regressione della change 0065: con la sessione anonima la lettura degli entitlement non parte
  // nemmeno, quindi non sarà mai "conclusa". Trattarla come caricamento in corso lasciava la rotta
  // protetta montata a tempo indeterminato — senza mai redirigere al login — e la faceva ciclare
  // fino a "Maximum update depth exceeded". Nessuna prova la copriva: quelle di navigazione usano
  // lo stub degli entitlement, che è sempre "concluso".
  await mockAnonymous(page, { value: false })
  await page.goto('/')
  await expect(page).toHaveURL(/\/login/)
  await expect(page.getByLabel('Email')).toBeVisible()
})

test('[L2-AUTH] login con credenziali porta alla dashboard', async ({ page }) => {
  const authed = { value: false }
  await mockAnonymous(page, authed)
  await page.route('**/api/auth/login', (route) => {
    authed.value = true
    return route.fulfill({ json: tokenBody })
  })

  await page.goto('/login')
  await page.getByLabel('Email').fill('owner@acme.test')
  await page.getByLabel('Password').fill('Password1!')
  await page.getByRole('button', { name: 'Sign in' }).click()

  await expect(page.getByText('Platform')).toBeVisible()
  await expect(page.getByText('Your apps')).toBeVisible()
})

test('[L2-AUTH] signup wizard: account → verifica → workspace → done → dashboard', async ({ page }) => {
  const authed = { value: false }
  await mockAnonymous(page, authed)
  await page.route('**/api/auth/signup', (route) => route.fulfill({ status: 201, json: { status: 'verification_required' } }))
  await page.route('**/api/platform/v1/accounts/me', (route) => route.fulfill({ json: { id: 'a1', name: 'Acme', status: 'active' } }))
  await page.route('**/api/platform/v1/me/entitlements', (route) => route.fulfill({ json: { entitlements: [] } }))
  // Newsletter (UC 0039): l'iscrizione al signup è best-effort; non spuntiamo il consenso in questo flusso.
  await page.route('**/api/platform/v1/newsletter/**', (route) => route.fulfill({ status: 202 }))

  await page.goto('/signup')
  // UC 0039: il consenso newsletter non è mai pre-attivato (privacy by default).
  await expect(page.getByRole('checkbox')).not.toBeChecked()
  // exact: il testo del consenso newsletter contiene la parola "email", quindi il match per
  // sottostringa di getByLabel colpirebbe anche la checkbox — qui vogliamo solo il campo Email.
  await page.getByLabel('Email', { exact: true }).fill('new@x.io')
  await page.getByLabel('Password', { exact: true }).fill('Password1!')
  await page.getByRole('button', { name: 'Create account' }).click()

  // step Verifica: simula il click sul link email abilitando il refresh, poi "continua"
  await expect(page.getByText(/We sent a verification link/)).toBeVisible()
  authed.value = true
  await page.getByRole('button', { name: 'I’ve verified — continue' }).click()

  // step Workspace → Done → dashboard
  await page.getByRole('button', { name: 'Continue' }).click()
  await page.getByRole('button', { name: 'Go to dashboard' }).click()
  await expect(page.getByText('Platform')).toBeVisible()
})
