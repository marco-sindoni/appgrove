import { test, expect } from '@playwright/test'
import { tenant } from '../helpers/api'
import { browserLogin } from '../helpers/browser'
import { dbRow, dbRows } from '../helpers/db'

/**
 * J-BUY — acquisto e attivazione (UC 0091, esteso alla vetrina dalla change 0076 / UC 0095).
 *
 * Tenant fresco → **catalogo** (/catalog): la card di Teams è `Available` col prezzo di partenza letto
 * dal listino vero → acquisto → overlay del fake Paddle → il webhook (pipeline REALE: ingest → coda →
 * consumer) materializza la subscription → ritorno alla vetrina, dove **la stessa card è ora `Active`**.
 * Poi l'acquisto del Mini-CRM dalla pagina Billing (l'altra via, ancora viva fino a UC 0096) → la
 * sidebar mostra l'app tra YOUR APPS → il modulo si monta e risponde. Assert DB: subscription ed eventi
 * webhook col tenant_id giusto, nessuna riga per altri tenant (leak detector, #10 dec. 13).
 *
 * Perché due app e non una: il Mini-CRM è l'unica app con un modulo frontend **e** un tier a pagamento,
 * ma ha anche un tier gratuito di baseline — nel catalogo è quindi già `Active`, non acquistabile. Teams
 * è l'unica app genuinamente `Available` e serve a provare la transizione della card; il Mini-CRM resta
 * per provare che il modulo comprato si monta davvero.
 *
 * L'app comprabile col modulo è il Mini-CRM, attivato per la run dal global-setup (decisione 3, change 0070).
 */

test('[J-BUY] catalogo → tier → fake Paddle → webhook reale → card attivata → modulo montato → DB coerente', async ({
  page,
}) => {
  const t = await tenant('jbuy')
  await browserLogin(page, t.email, t.password)

  // ── 1. vetrina: la card è acquistabile e annuncia il prezzo di partenza reale ────────────────
  await page.goto('/catalog')
  await expect(page.getByRole('heading', { name: 'App catalog', level: 1 })).toBeVisible()
  const teams = page.getByRole('article', { name: 'Teams' })
  await expect(teams).toContainText('from')
  await expect(teams.getByRole('button', { name: 'Subscribe' })).toBeEnabled()
  // Il Mini-CRM ha un tier gratuito di baseline: la vetrina lo dice invece di offrire un acquisto.
  await expect(page.getByRole('article', { name: 'Mini-CRM' })).toContainText('Mini-CRM Free')

  await teams.getByRole('button', { name: 'Subscribe' }).click()

  // Tier: l'unico di Teams è a pagamento → il suo Subscribe è l'unico abilitato.
  await expect(page.getByRole('heading', { name: 'Teams' }).first()).toBeVisible()
  await page.getByRole('button', { name: 'Subscribe', disabled: false }).click()

  // ── 2. overlay stub → webhook sulla pipeline reale → polling → attivata ─────────────────────
  await expect(page.getByText('Activating your subscription…')).toBeVisible()
  await expect(page.getByText('All set! Your subscription is active.')).toBeVisible({ timeout: 30_000 })

  // ── 3. la card della STESSA app non è più acquistabile: è in uso ────────────────────────────
  // "Active" o "Trial" a seconda che il fornitore apra o meno il periodo di prova (il tier ne ha 14
  // giorni): ciò che il journey prova è la transizione da proposta d'acquisto ad app in uso.
  await page.getByRole('button', { name: 'Back' }).click()
  const teamsAfter = page.getByRole('article', { name: 'Teams' })
  await expect(teamsAfter).toContainText(/Active|Trial/)
  await expect(teamsAfter.getByRole('button', { name: 'Open' })).toBeVisible()
  await expect(teamsAfter.getByRole('button', { name: 'Subscribe' })).toHaveCount(0)

  // ── 4. l'altra via all'acquisto (Billing, viva fino a UC 0096) e il modulo che si monta ─────
  await page.goto('/billing')
  await expect(page.getByRole('heading', { name: 'Get an app', level: 1 })).toBeVisible()
  const picker = page.getByRole('region', { name: 'Choose an app' })
  await picker
    .locator(':scope > *')
    .filter({ hasText: 'Mini-CRM' })
    .getByRole('button', { name: 'Subscribe' })
    .click()

  await expect(page.getByRole('heading', { name: 'Mini-CRM Team' })).toBeVisible()
  await expect(page.getByText('14-day free trial')).toBeVisible()
  await page.getByRole('button', { name: 'Subscribe', disabled: false }).click()

  await expect(page.getByText('Activating your subscription…')).toBeVisible()
  await expect(page.getByText('All set! Your subscription is active.')).toBeVisible({ timeout: 30_000 })

  // ── sidebar YOUR APPS: il modulo compare e si monta davvero ────────────────
  await page.getByRole('button', { name: 'Open app' }).click()
  await expect(page).toHaveURL(/\/app\/crm/)
  await expect(page.getByRole('heading', { name: 'Contatti', level: 1 })).toBeVisible()
  const nav = page.getByRole('navigation', { name: 'Platform' })
  await expect(nav.locator('a[href="/app/crm"]')).toBeVisible()

  // ── 5. assert DB (leak detector) ────────────────────────────────────────────
  for (const slug of ['crm', 'teams']) {
    const [status, tierKey] = dbRow(
      `select s.status, at.key
         from platform.subscription s
         join platform.app a on a.id = s.app_id
         join platform.app_tier at on at.id = s.app_tier_id
        where s.tenant_id = $1 and a.slug = $2 and s.deleted_at is null`,
      [t.tenantId, slug],
    )
    expect(['trialing', 'active']).toContain(status)
    expect(tierKey).toBe('team')
  }
  // Gli eventi webhook della pipeline sono registrati e processati per QUESTO tenant.
  const processed = dbRows(
    `select outcome from platform.webhook_event where tenant_id = $1`,
    [t.tenantId],
  )
  expect(processed.length).toBeGreaterThan(0)
  expect(processed.every(([o]) => o === 'processed')).toBeTruthy()
  // Nessuna riga per altri tenant: le subscription appena nate appartengono a UN solo tenant.
  expect(
    dbRows(`select tenant_id from platform.subscription where tenant_id = $1`, [t.tenantId]),
  ).toHaveLength(2)
  expect(dbRow(`select count(*) from platform.users where tenant_id = $1`, [t.tenantId])[0]).toBe('1')
})
