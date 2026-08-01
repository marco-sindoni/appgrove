import { test, expect } from '@playwright/test'
import { tenant, authedFetch, pollUntil } from '../helpers/api'
import { browserLogin } from '../helpers/browser'
import { buyApp, sendPaddleWebhook, subscriptionEvent } from '../helpers/paddle'
import { dbRow } from '../helpers/db'

const BACKOFFICE_URL = process.env.PLATFORM_BACKOFFICE_URL ?? 'http://localhost:24173'
const CORE_API = process.env.PLATFORM_CORE_API ?? 'http://localhost:20080'

interface SubView {
  appSlug: string
  phase: string
  scheduledTierKey?: string
  cancelAt?: string
}

async function mySubscription(t: { tokens: Parameters<typeof authedFetch>[2] }): Promise<SubView | undefined> {
  const res = await authedFetch(CORE_API, '/api/platform/v1/me/subscriptions', t.tokens)
  if (!res.ok) throw new Error(`me/subscriptions: HTTP ${res.status}`)
  const { subscriptions } = (await res.json()) as { subscriptions: SubView[] }
  return subscriptions.find((s) => s.appSlug === 'crm')
}

/**
 * J-SUB — ciclo di vita abbonamento (UC 0091), su Mini-CRM Team.
 *
 * Downgrade programmato a fine periodo (self-service, stato mostrato) → disdetta (stato
 * mostrato) → fine periodo SIMULATA con un webhook sintetico firmato `subscription.canceled`
 * sulla pipeline reale (leva raccomandata dallo use case §5) → la UI mostra "scaduta" con le
 * azioni riattiva / esporta-cancella, il modulo non è più raggiungibile e le API rispondono
 * 402 — ma i dati NON sono cancellati (assert DB) → riattivazione via checkout → accesso e
 * dati ripristinati.
 */

test('[J-SUB] downgrade programmato → disdetta → scadenza via webhook → 402 e dati intatti → riattivazione', async ({
  page,
}) => {
  const t = await tenant('jsub')
  await buyApp(t.tokens, 'crm', 'team')
  // Posto per l'owner + un dato applicativo che deve SOPRAVVIVERE alla scadenza.
  const seat = await authedFetch(BACKOFFICE_URL, '/api/crm/v1/seats', t.tokens, { body: { userId: t.userSub } })
  expect([200, 201]).toContain(seat.status)
  const contact = await authedFetch(BACKOFFICE_URL, '/api/crm/v1/contacts', t.tokens, {
    body: { displayName: 'Cliente Duraturo' },
  })
  expect(contact.status).toBe(201)

  await browserLogin(page, t.email, t.password)
  // I cambi self-service usano window.confirm: si accetta come farebbe l'utente.
  page.on('dialog', (d) => void d.accept())

  // ── 1. downgrade programmato a fine periodo ─────────────────────────────────
  await page.goto('/billing')
  const panel = page.getByRole('region', { name: 'Your subscriptions' })
  await expect(panel.getByText('Plan: Mini-CRM Team')).toBeVisible()
  await panel.getByRole('button', { name: 'Change plan' }).click()
  await panel.getByRole('button', { name: 'Mini-CRM Free' }).click()
  // Il comando passa dal provider → webhook → read-model (asincrono): attesa su condizione API.
  await pollUntil(async () => (await mySubscription(t))?.scheduledTierKey === 'free', {
    message: 'downgrade programmato non riflesso dal read-model (webhook non applicato?)',
  })
  await page.reload()
  await expect(panel.getByText(/Downgrade scheduled to/)).toBeVisible()

  // ── 2. disdetta self-service, poi fine periodo simulata via webhook firmato ─
  await panel.getByRole('button', { name: 'Cancel', exact: true }).click()
  await pollUntil(async () => !!(await mySubscription(t))?.cancelAt, {
    message: 'disdetta programmata non riflessa dal read-model',
  })
  await page.reload()
  await expect(panel.getByText(/Cancellation scheduled: access until/)).toBeVisible()

  const [appId, paddleSubId] = dbRow(
    `select s.app_id, s.paddle_subscription_id
       from platform.subscription s join platform.app a on a.id = s.app_id
      where s.tenant_id = $1 and a.slug = 'crm' and s.deleted_at is null`,
    [t.tenantId],
  )
  await sendPaddleWebhook(
    subscriptionEvent({
      eventType: 'subscription.canceled',
      status: 'canceled',
      tenantId: t.tenantId,
      appId,
      paddleSubscriptionId: paddleSubId,
      occurredAt: new Date(Date.now() + 1_000),
    }),
  )
  await pollUntil(async () => (await mySubscription(t))?.phase === 'ENDED', {
    message: 'fine periodo non riflessa dal read-model (webhook subscription.canceled non applicato?)',
  })

  // ── UI "scaduta": azioni riattiva / esporta-cancella; modulo non raggiungibile ──
  await page.reload()
  await expect(panel.getByText('Subscription expired. Reactivate or exercise your data rights.')).toBeVisible()
  await expect(panel.getByRole('button', { name: 'Reactivate' })).toBeVisible()
  await expect(panel.getByRole('button', { name: 'Export / delete your data' })).toBeVisible()
  await page.goto('/app/crm')
  await expect(page.getByText('You don’t have access to this app')).toBeVisible()
  // Le API dell'app rispondono 402 (semantica UC 0027) — e i dati NON sono stati toccati.
  await pollUntil(
    async () =>
      (await authedFetch(BACKOFFICE_URL, '/api/crm/v1/contacts', t.tokens, { body: { displayName: 'X' } }))
        .status === 402,
    { message: 'atteso 402 dalle API crm a subscription scaduta (proiezione entitlement non invalidata?)' },
  )
  expect(dbRow(`select status from platform.subscription where tenant_id = $1`, [t.tenantId])[0]).toBe('canceled')
  expect(
    dbRow(`select count(*) from app_crm.contact where tenant_id = $1 and deleted_at is null`, [t.tenantId])[0],
  ).toBe('1')

  // ── 3. riattivazione: dal pannello si riapre il checkout → accesso e dati intatti ──
  await page.goto('/billing')
  await panel.getByRole('button', { name: 'Reactivate' }).click()
  await expect(page.getByRole('heading', { name: 'Mini-CRM Team' })).toBeVisible()
  await page.getByRole('button', { name: 'Subscribe', disabled: false }).click()
  await expect(page.getByText('All set! Your subscription is active.')).toBeVisible({ timeout: 30_000 })
  // L'accesso torna e il dato creato prima della scadenza è ancora lì.
  await pollUntil(
    async () => (await authedFetch(BACKOFFICE_URL, '/api/crm/v1/contacts?page=0&size=20', t.tokens)).status === 200,
    { message: 'accesso crm non ripristinato dopo la riattivazione' },
  )
  await page.goto('/app/crm')
  await expect(page.getByRole('heading', { name: 'Contatti', level: 1 })).toBeVisible()
  await expect(page.getByRole('cell', { name: 'Cliente Duraturo' })).toBeVisible()
})
