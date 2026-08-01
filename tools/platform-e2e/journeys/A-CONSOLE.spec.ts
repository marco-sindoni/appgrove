import { test, expect } from '@playwright/test'
import { tenant, authedFetch } from '../helpers/api'
import { browserLogin, adminSession } from '../helpers/browser'
import { buyApp } from '../helpers/paddle'
import { dbRow, dbRows } from '../helpers/db'

const BACKOFFICE_URL = process.env.PLATFORM_BACKOFFICE_URL ?? 'http://localhost:24173'

/**
 * A-CONSOLE — console di piattaforma e disabilitazione applicazione, fra DUE attori (UC 0092).
 *
 * Due sessioni browser isolate nello stesso journey: il **cliente** sul backoffice, l'**operatore
 * di piattaforma** sulla console admin. Il valore sta tutto nel legame fra le due: l'azione
 * dell'amministratore non si osserva dove viene compiuta, ma dall'altra parte — ed è proprio
 * quel legame che nessun collaudo con backend simulato può verificare.
 *
 * Cliente con Mini-CRM attivo e un contatto suo dentro → l'operatore vede gli indicatori, apre il
 * catalogo e **disabilita** l'app con motivazione → il cliente perde l'app dal menu e dalla rotta,
 * ma **non** perde i dati (assert sul database) → l'operatore **riabilita** → il cliente ritrova
 * app e contatto → il registro delle transizioni porta entrambe le azioni, con operatore e
 * motivazione, sia sul database sia nella tabella della console.
 *
 * Journey SERIALE (progetto `admin-serial`): lo stato dell'app di catalogo è globale a tutti i
 * tenant, quindi non può convivere con i journey paralleli.
 */

test('A-CONSOLE: cliente con app attiva → admin disabilita → app negata ma dati intatti → riabilita → tutto torna', async ({
  page,
  browser,
}) => {
  // ── 1. sessione A (cliente): app attivata, funzionante, con un dato suo dentro ──
  const t = await tenant('aconsole')
  await buyApp(t.tokens, 'crm', 'team')
  expect(
    [200, 201].includes(
      (await authedFetch(BACKOFFICE_URL, '/api/crm/v1/seats', t.tokens, { body: { userId: t.userSub } })).status,
    ),
  ).toBeTruthy()
  const contactName = `Contatto A-CONSOLE ${Date.now().toString(36)}`
  expect(
    (await authedFetch(BACKOFFICE_URL, '/api/crm/v1/contacts', t.tokens, { body: { displayName: contactName } }))
      .status,
  ).toBe(201)

  await browserLogin(page, t.email, t.password)
  const nav = page.getByRole('navigation', { name: 'Platform' })
  await expect(nav.locator('a[href="/app/crm"]')).toBeVisible()
  await page.goto('/app/crm')
  await expect(page.getByRole('heading', { name: 'Contatti', level: 1 })).toBeVisible()
  await expect(page.getByRole('cell', { name: contactName })).toBeVisible()

  // ── 2. sessione B (operatore): contesto isolato, console admin, gate di ruolo ──
  const { context: adminContext, page: admin } = await adminSession(browser)
  await expect(admin.getByRole('heading', { name: 'Overview' })).toBeVisible()
  // Gli indicatori di sintesi della pagina iniziale: ci sono tutti e quattro.
  for (const kpi of ['Accounts', 'Users', 'Active subscriptions', 'Disabled apps']) {
    await expect(admin.getByRole('heading', { name: kpi, exact: true })).toBeVisible()
  }

  await admin.getByRole('link', { name: 'Apps' }).click()
  await expect(admin.getByRole('heading', { name: 'Apps', exact: true })).toBeVisible()
  const appsTable = admin.getByRole('table').first()
  const crmRow = appsTable.getByRole('row').filter({ hasText: 'Mini-CRM' })
  // Lo stato si legge sull'ETICHETTA TRADOTTA, non sul valore grezzo del campo (UC 0076):
  // è quello che l'operatore vede davvero.
  await expect(crmRow.getByText('Active', { exact: true })).toBeVisible()

  // Motivazione unica per esecuzione: il registro delle transizioni non viene azzerato fra una
  // corsa e l'altra, e una motivazione fissa vi si accumulerebbe rendendo ambigua la riga cercata.
  const reason = `Manutenzione straordinaria A-CONSOLE ${Date.now().toString(36)}`
  await crmRow.getByRole('button', { name: 'Disable' }).click()
  const dialog = admin.getByRole('dialog')
  await dialog.getByLabel('Reason (optional)').fill(reason)
  await dialog.getByRole('button', { name: 'Disable' }).click()
  await expect(crmRow.getByText('Disabled', { exact: true })).toBeVisible()

  // ── 3. sessione A: l'app sparisce e la rotta nega — ma i dati restano ───────
  // Freschezza secondo UC 0077: qui si osserva la via del ricaricamento della pagina,
  // deterministica in un browser senza finestre; la via del "ritorno sulla scheda" è coperta dai
  // test del frontend. Si rientra dalla dashboard perché /app/crm ora rimbalza fuori dalla shell.
  await page.goto('/')
  await expect(nav).toBeVisible()
  await expect(nav.locator('a[href="/app/crm"]')).toHaveCount(0)
  // Qui il diniego è CORRETTO: il diritto è realmente venuto meno (il contrario del guasto,
  // che F-DEGRADE verifica non debba mai presentarsi come diniego).
  await page.goto('/app/crm')
  await expect(page).toHaveURL(/\/forbidden$/)

  // I dati non sono cancellati: la disabilitazione è una pausa, non una dismissione.
  expect(dbRow(`select count(*) from app_crm.contact where tenant_id = $1`, [t.tenantId])[0]).toBe('1')
  expect(
    dbRow(`select count(*) from platform.subscription where tenant_id = $1 and deleted_at is null`, [t.tenantId])[0],
  ).toBe('1')

  // ── 4. l'operatore riabilita: il cliente ritrova app e dato ─────────────────
  await crmRow.getByRole('button', { name: 'Re-enable' }).click()
  await admin.getByRole('dialog').getByRole('button', { name: 'Re-enable' }).click()
  await expect(crmRow.getByText('Active', { exact: true })).toBeVisible()

  await page.goto('/app/crm')
  await expect(page.getByRole('heading', { name: 'Contatti', level: 1 })).toBeVisible()
  await expect(page.getByRole('cell', { name: contactName })).toBeVisible()
  await expect(nav.locator('a[href="/app/crm"]')).toBeVisible()

  // ── 5. registro delle transizioni: database e console dicono la stessa cosa ──
  const [adminSub] = dbRow(
    `select cognito_sub from platform.users where lower(email) = lower($1) and deleted_at is null`,
    ['admin@appgrove.test'],
  )
  const trail = dbRows(
    `select a.from_status, a.to_status, a.actor, coalesce(a.reason, '')
       from platform.app_status_audit a
       join platform.app app on app.id = a.app_id
      where app.slug = 'crm'
      order by a.executed_at desc
      limit 2`,
  )
  expect(trail).toHaveLength(2)
  const [riabilitazione, disabilitazione] = trail
  expect(riabilitazione.slice(0, 3)).toEqual(['inactive', 'active', adminSub])
  expect(disabilitazione.slice(0, 3)).toEqual(['active', 'inactive', adminSub])
  expect(disabilitazione[3]).toBe(reason)

  const register = admin.getByRole('table').nth(1)
  await expect(register.getByRole('row').filter({ hasText: reason })).toBeVisible()
  await expect(register.getByRole('row').filter({ hasText: adminSub }).first()).toBeVisible()

  // ── rilevatore di travaso: l'azione dell'operatore non ha creato nulla nel tenant ──
  expect(dbRow(`select count(*) from platform.users where tenant_id = $1`, [t.tenantId])[0]).toBe('1')
  await adminContext.close()
})
