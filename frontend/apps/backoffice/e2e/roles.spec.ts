import { test, expect, type Page } from '@playwright/test'

// I due rifiuti dell'autorizzazione per applicazione, letti a parole sullo schermo (UC 0099, classificati
// da UC 0101). Sono due `403` con lo stesso codice e due significati diversi:
//
//   • «non hai accesso a questa applicazione» → chiedi l'abilitazione a chi governa l'applicazione;
//   • «serve almeno editor, il tuo ruolo è viewer» → hai accesso, ma non a QUESTA operazione.
//
// La differenza è tutta nella frase, e la frase la scrive il server. Un `catch` che mostra «si è
// verificato un errore» la butta via — è il difetto che il collaudo manuale del 2026-08-21 trovò nel
// Mini-CRM, e che qui non deve poter tornare nell'app #1, che è attiva e in mano a persone vere.
//
// Livello 2: backend simulato con `page.route`, perché ciò che è in prova è come la SCHERMATA tratta la
// risposta, non come il servizio la produce (quello è provato da AppRoleGateTest in `fatture` e `crm`).

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
const accessToken = jwt({ sub: 'u1', tenant_id: 'tenant-1', roles: ['member'], upn: 'collega@acme.test' })
const idToken = jwt({ sub: 'u1', email: 'collega@acme.test', name: 'Collega' })
const tokenBody = { access_token: accessToken, id_token: idToken, token_type: 'Bearer' }

const NO_ACCESS = {
  type: 'urn:appgrove:app-role:no-access',
  title: 'Accesso all’applicazione non concesso',
  status: 403,
  detail:
    'Non hai accesso all’applicazione fatture: chiedi l’abilitazione all’owner o a un amministratore dell’applicazione.',
}
const INSUFFICIENT = {
  type: 'urn:appgrove:app-role:insufficient',
  title: 'Ruolo insufficiente',
  status: 403,
  detail: 'Per questa operazione serve almeno il ruolo editor sull’applicazione fatture: il tuo ruolo è viewer.',
  requiredRole: 'editor',
  role: 'viewer',
}

/** SPA autenticata come collaboratore dell'account, con `fatture` fra i diritti dell'account. */
async function mockAuthed(page: Page): Promise<void> {
  await page.route('**/config.json', (route) =>
    route.fulfill({
      json: { env: 'local', authBaseUrl: ORIGIN, coreBaseUrl: ORIGIN, cognito: { userPoolId: '', clientId: '' } },
    }),
  )
  await page.route('**/api/auth/refresh', (route) => route.fulfill({ json: tokenBody }))
  await page.route('**/api/platform/v1/users/me', (route) =>
    route.fulfill({
      json: {
        id: 'u1',
        email: 'collega@acme.test',
        displayName: 'Collega',
        role: 'member',
        status: 'active',
        tenantId: 'tenant-1',
      },
    }),
  )
  await page.route('**/api/platform/v1/accounts/me', (route) =>
    route.fulfill({ json: { id: 'a1', name: 'Acme', status: 'active' } }),
  )
  // Il diritto dell'ACCOUNT c'è: è il varco più esterno e passa. Quello che manca — o non basta — è il
  // ruolo della PERSONA, che è il varco successivo e l'oggetto di questi due collaudi.
  await page.route('**/api/platform/v1/me/entitlements', (route) =>
    route.fulfill({
      json: {
        entitlements: [
          { appSlug: 'fatture', tierKey: 'free', limits: { fatture: { cap: 10, nature: 'flow', window: 'month' } } },
        ],
      },
    }),
  )
  await page.route('**/api/fatture/v1/quota', (route) =>
    route.fulfill({ json: { metric: 'fatture', used: 1, limit: 10, remaining: 9 } }),
  )
}

test('[J-APP-ROLE-REFUSALS] senza accesso all’applicazione la schermata dice CHI abilita, e non offre di riprovare', async ({
  page,
}) => {
  await mockAuthed(page)
  await page.route('**/api/fatture/v1/invoices?*', (route) =>
    route.fulfill({ status: 403, contentType: 'application/problem+json', json: NO_ACCESS }),
  )

  await page.goto('/app/fatture')

  await expect(page.getByText(NO_ACCESS.detail)).toBeVisible()
  // Un rifiuto non è un guasto: invitare a ripetere una richiesta che fallirà sempre manda una persona
  // a sbattere contro lo stesso muro finché non si arrende.
  await expect(page.getByRole('button', { name: /Retry|Riprova/ })).toHaveCount(0)
})

test('[J-APP-ROLE-REFUSALS] col ruolo insufficiente si legge quale ruolo serve, non «si è verificato un errore»', async ({
  page,
}) => {
  await mockAuthed(page)
  const invoices = [
    {
      id: 'inv-1',
      number: '2026-0001',
      customerName: 'Mario Rossi',
      status: 'issued',
      currency: 'EUR',
      totalAmount: 120,
    },
  ]
  // Un `viewer` LEGGE: l'elenco arriva. È la metà del contratto che si dimentica sempre di collaudare.
  await page.route('**/api/fatture/v1/invoices?*', (route) =>
    route.fulfill({ json: { content: invoices, page: 0, size: 20, totalElements: 1, totalPages: 1 } }),
  )
  // …e non SCRIVE: la creazione è dispositiva e chiede almeno `editor`.
  await page.route('**/api/fatture/v1/invoices', (route) =>
    route.fulfill({ status: 403, contentType: 'application/problem+json', json: INSUFFICIENT }),
  )

  await page.goto('/app/fatture')
  await expect(page.getByRole('cell', { name: 'Mario Rossi' })).toBeVisible()

  await page.getByRole('button', { name: 'New invoice' }).click()
  await page.getByLabel('Customer name').fill('Tentativo di un viewer')
  await page.getByRole('button', { name: 'Create invoice' }).click()

  const alert = page.getByRole('alert')
  await expect(alert).toContainText(INSUFFICIENT.detail)
  // La prova che la frase del server non è stata sostituita da un generico.
  await expect(alert).not.toContainText('Something went wrong')
})
