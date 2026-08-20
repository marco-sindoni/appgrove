import { test, expect } from '@playwright/test'
import { execFileSync } from 'node:child_process'
import { mkdtempSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { tenant, authedFetch, pollUntil } from '../helpers/api'
import { browserLogin } from '../helpers/browser'
import { buyApp } from '../helpers/paddle'
import { dbRow } from '../helpers/db'

const BACKOFFICE_URL = process.env.PLATFORM_BACKOFFICE_URL ?? 'http://localhost:24173'
const CORE_API = process.env.PLATFORM_CORE_API ?? 'http://localhost:20080'

/**
 * J-PRIVACY — diritti sui dati (UC 0091 / UC 0033).
 *
 * Rettifica del nome (art. 16, visibile in UI e su DB) → export account con JOB ASINCRONO
 * REALE (code per-servizio + aggregatore, archivio ZIP su MinIO, link presigned con scadenza,
 * DOWNLOAD EFFETTIVO e validazione del contenuto: i dati del tenant, SOLO del tenant) →
 * recesso per-app (esporta → conferma → purge reale sulla coda, con audit su DB) →
 * eliminazione account (grace con scadenza, stato pending_deletion, annullamento).
 *
 * Un secondo tenant "canarino" prova l'isolamento: i suoi dati non devono comparire nell'export.
 */

test('[J-PRIVACY] rettifica → export reale con download validato → recesso per-app → eliminazione con grace', async ({
  page,
}) => {
  const canary = await tenant('jprivacy-canary')
  const t = await tenant('jprivacy')
  // App attivata (subscription vera): è la condizione dei diritti per-app (export/recesso).
  await buyApp(t.tokens, 'crm', 'team')
  expect(
    [200, 201].includes(
      (await authedFetch(BACKOFFICE_URL, '/api/crm/v1/seats', t.tokens, { body: { userId: t.userSub } })).status,
    ),
  ).toBeTruthy()
  const contactName = `Contatto Riservato ${Date.now().toString(36)}`
  expect(
    (await authedFetch(BACKOFFICE_URL, '/api/crm/v1/contacts', t.tokens, { body: { displayName: contactName } }))
      .status,
  ).toBe(201)

  await browserLogin(page, t.email, t.password)
  await page.goto('/privacy')
  await expect(page.getByRole('heading', { name: 'My data', level: 1 })).toBeVisible()

  // ── 1. rettifica del nome (art. 16): visibile e persistita ──────────────────
  await page.getByLabel('Display name').fill('Nome Rettificato')
  await page.getByRole('button', { name: 'Save' }).click()
  await expect(page.getByText('Saved')).toBeVisible()
  expect(
    dbRow(`select display_name from platform.identity where lower(email) = lower($1) and deleted_at is null`, [
      t.email,
    ])[0],
  ).toBe('Nome Rettificato')
  await page.reload()
  await expect(page.getByLabel('Display name')).toHaveValue('Nome Rettificato')

  // ── 2. export account: job asincrono reale → pronto → download validato ─────
  await page.getByRole('button', { name: /Start export/ }).click()
  await expect(page.getByText('Export ready.')).toBeVisible({ timeout: 60_000 })
  // Il link presigned (con scadenza) si apre in una nuova scheda; la scadenza compare in UI.
  const popupPromise = page.waitForEvent('popup')
  await page.getByRole('button', { name: 'Download archive' }).click()
  await popupPromise
  await expect(page.getByText(/Link valid until/)).toBeVisible()

  // Download EFFETTIVO e validazione del contenuto via API (stesso link presigned).
  const [jobId, zipKey] = dbRow(
    `select id, zip_key from platform.gdpr_export_job
      where tenant_id = $1 and status = 'COMPLETED' order by created_at desc limit 1`,
    [t.tenantId],
  )
  expect(zipKey).toContain(jobId)
  const dl = await authedFetch(CORE_API, `/api/platform/v1/gdpr/exports/${jobId}/download`, t.tokens)
  expect(dl.status).toBe(200)
  const { url, expiresAt } = (await dl.json()) as { url: string; expiresAt: string }
  expect(new Date(expiresAt).getTime()).toBeGreaterThan(Date.now())
  const zipRes = await fetch(url)
  expect(zipRes.status).toBe(200)
  const zipBytes = Buffer.from(await zipRes.arrayBuffer())
  expect(zipBytes.subarray(0, 2).toString('latin1')).toBe('PK')
  const dir = mkdtempSync(join(tmpdir(), 'jprivacy-'))
  const zipPath = join(dir, 'export.zip')
  writeFileSync(zipPath, zipBytes)
  const entries = execFileSync('unzip', ['-Z1', zipPath], { encoding: 'utf8' }).trim().split('\n')
  expect(entries).toContain('platform.json')
  expect(entries).toContain('crm.json')
  const content = execFileSync('unzip', ['-p', zipPath], { encoding: 'utf8' })
  // I dati del tenant... (profilo rettificato e dato applicativo) — e SOLO del tenant.
  expect(content).toContain(t.email)
  expect(content).toContain('Nome Rettificato')
  expect(content).toContain(contactName)
  expect(content).not.toContain(canary.email)

  // ── 3. recesso per-app: esporta → conferma → purge reale con audit ──────────
  await expect(page.getByRole('heading', { name: 'Withdraw from an app' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Confirm withdrawal' })).toBeDisabled()
  await page.getByRole('button', { name: 'Export app data' }).click()
  await expect(page.getByRole('button', { name: 'Confirm withdrawal' })).toBeEnabled({ timeout: 60_000 })
  await page.getByRole('button', { name: 'Confirm withdrawal' }).click()
  await page.getByRole('dialog').getByRole('button', { name: 'Confirm withdrawal' }).click()
  // Esito osservabile del recesso: contro il backend VERO il messaggio di conferma vive dentro la
  // riga dell'app e, appena la lista delle app attivate si rinfresca (la subscription è stata
  // dismessa), quella riga viene smontata — il messaggio può quindi non essere mai visibile.
  // Si attende perciò la prima delle due evidenze, entrambe di successo: il messaggio, oppure
  // l'elenco che non contiene più l'app. (Difetto UX minore annotato nei punti aperti di UC 0033.)
  await expect(
    page.getByText(/Withdrawal completed/).or(page.getByText('No apps activated on this account.')),
  ).toBeVisible({ timeout: 30_000 })
  // La purge è asincrona (coda tenant-purge-crm): i dati dell'app spariscono FISICAMENTE,
  // resta la prova nell'audit di purge; la subscription è dismessa (soft-delete).
  await pollUntil(
    () => dbRow(`select count(*) from app_crm.contact where tenant_id = $1`, [t.tenantId])[0] === '0',
    { message: 'dati crm non purgati dopo il recesso (coda tenant-purge-crm non consumata?)' },
  )
  const [purgeReason] = dbRow(
    `select reason from app_crm.gdpr_purge_audit where tenant_id = $1 order by executed_at desc limit 1`,
    [t.tenantId],
  )
  expect(purgeReason).toBe('app-withdrawal')
  expect(
    dbRow(`select count(*) from platform.subscription where tenant_id = $1 and deleted_at is null`, [t.tenantId])[0],
  ).toBe('0')

  // ── 4. eliminazione account: grace con scadenza → annulla ───────────────────
  await page.getByRole('button', { name: 'Delete this account' }).click()
  await page.getByRole('dialog').getByRole('button', { name: 'Delete this account' }).click()
  await expect(page.getByText('Deletion requested: the account is deactivated.')).toBeVisible()
  await expect(page.getByText(/Permanent deletion on/)).toBeVisible()
  expect(dbRow(`select status from platform.accounts where id::text = $1`, [t.tenantId])[0]).toBe('pending_deletion')
  await page.getByRole('button', { name: 'Cancel deletion' }).click()
  await expect(page.getByText('Deletion canceled: the account is active again.')).toBeVisible()
  expect(dbRow(`select status from platform.accounts where id::text = $1`, [t.tenantId])[0]).toBe('active')

  // ── leak detector: il canarino è rimasto intatto e isolato ──────────────────
  expect(dbRow(`select count(*) from platform.membership where tenant_id = $1`, [canary.tenantId])[0]).toBe('1')
  expect(dbRow(`select count(*) from platform.membership where tenant_id = $1 and deleted_at is null`, [t.tenantId])[0]).toBe(
    '1',
  )
})
