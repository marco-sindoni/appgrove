import { test, expect } from '@playwright/test'
import { tenant } from '../helpers/api'
import { browserLogin } from '../helpers/browser'
import { dbRow, dbRows } from '../helpers/db'

/**
 * J-BUY — acquisto e attivazione (UC 0091).
 *
 * Tenant fresco → catalogo (/billing) → tier a pagamento (Mini-CRM Team) → overlay del fake
 * Paddle → il webhook (pipeline REALE: ingest → coda → consumer) materializza la subscription →
 * polling post-checkout "attivata" → la sidebar mostra l'app tra YOUR APPS → il modulo si monta
 * e risponde. Assert DB: subscription ed eventi webhook col tenant_id giusto, nessuna riga per
 * altri tenant (leak detector, #10 dec. 13).
 *
 * L'app comprabile è il Mini-CRM, attivato per la run dal global-setup (decisione 3, change 0070).
 */

test('[J-BUY] catalogo → tier → fake Paddle → webhook reale → attivata → modulo montato → DB coerente', async ({
  page,
}) => {
  const t = await tenant('jbuy')
  await browserLogin(page, t.email, t.password)

  // ── 1. catalogo: scelta dell'app e del tier a pagamento ─────────────────────
  await page.goto('/billing')
  await expect(page.getByRole('heading', { name: 'Get an app', level: 1 })).toBeVisible()
  const picker = page.getByRole('region', { name: 'Choose an app' })
  await picker
    .locator(':scope > *')
    .filter({ hasText: 'Mini-CRM' })
    .getByRole('button', { name: 'Subscribe' })
    .click()

  // Tier: il free (senza prezzo) ha il bottone disabilitato, il Team (a pagamento) no —
  // l'unico Subscribe abilitato della scelta tier è quindi quello del Team.
  await expect(page.getByRole('heading', { name: 'Mini-CRM Team' })).toBeVisible()
  await expect(page.getByText('14-day free trial')).toBeVisible()
  await page.getByRole('button', { name: 'Subscribe', disabled: false }).click()

  // ── 2-3. overlay stub → webhook sulla pipeline reale → polling → attivata ───
  await expect(page.getByText('Activating your subscription…')).toBeVisible()
  await expect(page.getByText('All set! Your subscription is active.')).toBeVisible({ timeout: 30_000 })

  // ── sidebar YOUR APPS: il modulo compare e si monta davvero ────────────────
  await page.getByRole('button', { name: 'Open app' }).click()
  await expect(page).toHaveURL(/\/app\/crm/)
  await expect(page.getByRole('heading', { name: 'Contatti', level: 1 })).toBeVisible()
  const nav = page.getByRole('navigation', { name: 'Platform' })
  await expect(nav.locator('a[href="/app/crm"]')).toBeVisible()

  // ── 4. assert DB (leak detector) ────────────────────────────────────────────
  const [status, tierKey] = dbRow(
    `select s.status, at.key
       from platform.subscription s
       join platform.app a on a.id = s.app_id
       join platform.app_tier at on at.id = s.app_tier_id
      where s.tenant_id = $1 and a.slug = 'crm' and s.deleted_at is null`,
    [t.tenantId],
  )
  expect(['trialing', 'active']).toContain(status)
  expect(tierKey).toBe('team')
  // Gli eventi webhook della pipeline sono registrati e processati per QUESTO tenant.
  const processed = dbRows(
    `select outcome from platform.webhook_event where tenant_id = $1`,
    [t.tenantId],
  )
  expect(processed.length).toBeGreaterThan(0)
  expect(processed.every(([o]) => o === 'processed')).toBeTruthy()
  // Nessuna riga per altri tenant: la subscription appena nata appartiene a UN solo tenant.
  expect(
    dbRows(`select tenant_id from platform.subscription where tenant_id = $1`, [t.tenantId]),
  ).toHaveLength(1)
  expect(dbRow(`select count(*) from platform.users where tenant_id = $1`, [t.tenantId])[0]).toBe('1')
})
