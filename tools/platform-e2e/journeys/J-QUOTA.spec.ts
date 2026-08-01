import { test, expect } from '@playwright/test'
import { tenant, authedFetch } from '../helpers/api'
import { browserLogin } from '../helpers/browser'
import { dbRow } from '../helpers/db'

const BACKOFFICE_URL = process.env.PLATFORM_BACKOFFICE_URL ?? 'http://localhost:24173'

/**
 * J-QUOTA — uso dell'app e limite quota (UC 0091).
 *
 * Core-loop dell'app #1 (fatture, baseline freemium: tier free, 10 fatture/mese a consumo):
 * si crea fino al tetto, il banner consumo avanza, al superamento arriva il 429 REALE del
 * servizio con l'invito all'upgrade (schermata di creazione + banner globale) e la CTA porta
 * al catalogo. Assert DB: il conteggio reale delle fatture del mese riflette il consumo.
 *
 * Il ramo «upgrade di tier → la creazione riesce di nuovo» dello use case non è esercitabile
 * su fatture (il catalogo non ha un tier a pagamento) ed è coperto sulla metrica a giacenza
 * dei posti crm dentro J-MEMBERS (decisione 4, change 0070; rimando nei punti aperti di UC 0091).
 */

test('[J-QUOTA] core-loop fatture fino al tetto → banner → 429 reale con invito upgrade → DB coerente', async ({
  page,
}) => {
  const t = await tenant('jquota')

  // ── precondizione veloce: 9 fatture via API (le stesse rotte del modulo, via proxy SPA) ──
  for (let i = 1; i <= 9; i += 1) {
    const res = await authedFetch(BACKOFFICE_URL, '/api/fatture/v1/invoices', t.tokens, {
      body: { customerName: `Cliente ${i}` },
    })
    if (res.status !== 201) {
      throw new Error(`creazione fattura ${i} via API: HTTP ${res.status} — ${await res.text()}`)
    }
  }

  await browserLogin(page, t.email, t.password)

  // ── 1. banner consumo: 9/10, poi la decima dal browser → tetto raggiunto ────
  await page.goto('/app/fatture')
  await expect(page.getByRole('heading', { name: 'Invoices', level: 1 })).toBeVisible()
  const quota = page.getByRole('status').filter({ hasText: 'Invoices this month' })
  await expect(quota.getByText('9 / 10')).toBeVisible()

  await page.getByRole('button', { name: 'New invoice' }).click()
  await page.getByLabel('Customer name').fill('Cliente Decimo')
  await page.getByRole('button', { name: 'Create invoice' }).click()
  await expect(page.getByRole('cell', { name: 'Cliente Decimo' })).toBeVisible()
  await expect(quota.getByText('10 / 10')).toBeVisible()
  await expect(quota.getByText('You have reached your plan’s monthly limit.')).toBeVisible()
  await expect(quota.getByRole('button', { name: 'Upgrade your plan' })).toBeVisible()

  // ── 2. oltre il tetto: 429 REALE del servizio + invito all'upgrade ──────────
  await page.getByRole('button', { name: 'New invoice' }).click()
  await page.getByLabel('Customer name').fill('Cliente Undicesimo')
  await page.getByRole('button', { name: 'Create invoice' }).click()
  const moduleAlert = page
    .getByRole('alert')
    .filter({ hasText: 'Monthly limit reached: upgrade to create more invoices.' })
  await expect(moduleAlert).toBeVisible()
  // Il 429 alimenta anche il banner globale di enforcement (UC 0027).
  await expect(page.getByRole('alert').filter({ hasText: 'Plan limit reached' })).toBeVisible()
  // La CTA del modulo porta a Billing, dove vivono i cambi di piano (UC 0096: la pagina è ora di sola
  // fatturazione; `fatture` non ha tier a pagamento, quindi lì non c'è nulla da comprare — ciò che il
  // journey prova è che l'invito porti da qualche parte di sensato, non nel vuoto).
  await moduleAlert.getByRole('button', { name: 'Upgrade your plan' }).click()
  await expect(page.getByRole('heading', { name: 'Billing', level: 1 })).toBeVisible()

  // ── assert DB: il contatore quota è il consumo reale del mese, solo di questo tenant ──
  expect(
    dbRow(
      `select count(*) from app_fatture.invoice
        where tenant_id = $1 and deleted_at is null
          and created_at >= date_trunc('month', now() at time zone 'utc')`,
      [t.tenantId],
    )[0],
  ).toBe('10')
  // Anche l'API informativa di quota (fuori dal gate) riflette il consumo.
  const q = await authedFetch(BACKOFFICE_URL, '/api/fatture/v1/quota', t.tokens)
  expect(q.status).toBe(200)
  expect((await q.json()) as object).toMatchObject({ metric: 'fatture', used: 10, limit: 10, remaining: 0 })
})
