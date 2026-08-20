import { test, expect } from '@playwright/test'
import { tenant, login, uniqueEmail, authedFetch, pollUntil, jwtPayload } from '../helpers/api'
import { browserLogin, acceptLegalGateIfPresent } from '../helpers/browser'
import { waitForEmail, extractLink } from '../helpers/mailbox'
import { buyApp } from '../helpers/paddle'
import { dbRow } from '../helpers/db'

const BACKOFFICE_URL = process.env.PLATFORM_BACKOFFICE_URL ?? 'http://localhost:24173'
const MEMBER_PASSWORD = 'Password1!'

/**
 * J-MEMBERS — inviti e ruoli B2B (UC 0091), su Mini-CRM.
 *
 * Owner → invito → EMAIL DI INVITO REALMENTE RICEVUTA (Mailpit) → il membro apre il link in
 * una SECONDA SESSIONE browser, imposta la password ed entra col ruolo member → posti (seat)
 * del crm fino al tetto del tier free (2) → 429 reale + invito all'upgrade → acquisto del tier
 * Team (fake Paddle, webhook sulla pipeline reale) → l'assegnazione riesce di nuovo (è il ramo
 * «upgrade sblocca la quota» dello use case, sulla metrica a giacenza — decisione 4, change 0070)
 * → cambio ruolo e rimozione dall'owner; il membro rimosso perde l'accesso (assert dalla SUA
 * sessione) → protezione ultimo owner (assert UI).
 */

test('[J-MEMBERS] invito con email reale → seconda sessione → seat fino al 429 → upgrade sblocca → ruoli e revoca', async ({
  page,
  browser,
}) => {
  const owner = await tenant('jmembers-owner')
  const inviteeEmail = uniqueEmail('jmembers-invitee')
  await browserLogin(page, owner.email, owner.password)

  // ── 1. invito dall'owner → email reale → accettazione in seconda sessione ───
  await page.goto('/members')
  await expect(page.getByRole('heading', { name: 'Members', level: 1 })).toBeVisible()
  await page.getByLabel('Email', { exact: true }).fill(inviteeEmail)
  await page.getByRole('button', { name: 'Send invitation' }).click()
  await expect(page.getByText(`Invitation sent to ${inviteeEmail}.`)).toBeVisible()
  await expect(page.getByRole('cell', { name: inviteeEmail })).toBeVisible()

  const inviteMail = await waitForEmail(inviteeEmail)
  expect(inviteMail.Subject).toBe('You have been invited to appgrove')
  expect(inviteMail.From.Address).toBe('noreply@appgrove.app')
  const acceptLink = extractLink(inviteMail, '/accept')

  const memberContext = await browser.newContext()
  const memberPage = await memberContext.newPage()
  await memberPage.goto(acceptLink)
  await expect(memberPage.getByRole('heading', { name: 'Join the workspace' })).toBeVisible()
  await memberPage.getByLabel('Password').fill(MEMBER_PASSWORD)
  await memberPage.getByRole('button', { name: 'Accept invitation' }).click()
  // Auto-login del membro; primo ingresso nel workspace → gate legale come per ogni utente nuovo.
  await acceptLegalGateIfPresent(memberPage)
  await expect(memberPage.getByRole('navigation', { name: 'Platform' })).toBeVisible()

  // Il membro è entrato col ruolo member, nello STESSO tenant dell'owner (mai scelto dal client).
  const memberTokens = await login(inviteeEmail, MEMBER_PASSWORD)
  const memberSub = String(jwtPayload(memberTokens.access_token).sub)
  const [memberTenant, memberRole] = dbRow(
    `select m.tenant_id, m.role from platform.membership m
       join platform.identity i on i.id = m.identity_id
      where lower(i.email) = lower($1) and m.deleted_at is null and i.deleted_at is null`,
    [inviteeEmail],
  )
  expect(memberTenant).toBe(owner.tenantId)
  expect(memberRole).toBe('member')

  // ── 2. posti del crm: assegnazione fino al tetto free (2) → 429 → upgrade sblocca ──
  await page.goto('/app/crm/members')
  await expect(page.getByRole('heading', { name: 'Membri', level: 1 })).toBeVisible()
  await page.getByLabel('Identificativo utente').fill(owner.userSub)
  await page.getByRole('button', { name: 'Assegna posto' }).click()
  await expect(page.getByRole('cell', { name: owner.userSub })).toBeVisible()
  await page.getByLabel('Identificativo utente').fill(memberSub)
  await page.getByRole('button', { name: 'Assegna posto' }).click()
  await expect(page.getByRole('cell', { name: memberSub })).toBeVisible()
  await expect(page.getByText('2 / 2')).toBeVisible()

  // Posti esauriti: il terzo assegnamento riceve il 429 REALE + invito all'upgrade.
  const thirdSeatUser = `e2e-terzo-${Date.now().toString(36)}`
  await page.getByLabel('Identificativo utente').fill(thirdSeatUser)
  await page.getByRole('button', { name: 'Assegna posto' }).click()
  await expect(
    page.getByRole('alert').filter({ hasText: 'Posti esauriti: fai upgrade per assegnarne altri.' }),
  ).toBeVisible()
  await page.getByRole('button', { name: 'Passa a un piano superiore' }).click()
  // Billing è ora di sola fatturazione (UC 0096): l'invito all'upgrade porta dov'è la card
  // dell'abbonamento con il suo "Change plan", non su una griglia d'acquisto.
  await expect(page.getByRole('heading', { name: 'Billing', level: 1 })).toBeVisible()

  // Upgrade: acquisto del tier Team (checkout reale via API; l'attivazione passa dai webhook).
  await buyApp(owner.tokens, 'crm', 'team')
  // La nuova cap (10) arriva all'app via invalidazione della proiezione entitlement: attesa su condizione.
  await pollUntil(
    async () => {
      const res = await authedFetch(BACKOFFICE_URL, '/api/crm/v1/seats', owner.tokens)
      if (!res.ok) return false
      return ((await res.json()) as { limit?: number }).limit === 10
    },
    { message: 'cap posti non aggiornata a 10 dopo l’upgrade (proiezione entitlement non rinfrescata?)' },
  )
  await page.goto('/app/crm/members')
  await page.getByLabel('Identificativo utente').fill(thirdSeatUser)
  await page.getByRole('button', { name: 'Assegna posto' }).click()
  await expect(page.getByRole('cell', { name: thirdSeatUser })).toBeVisible()
  await expect(page.getByText('3 / 10')).toBeVisible()
  // La proiezione d'uso a giacenza si materializza nel core (coda app-usage): assert a polling.
  await pollUntil(
    () =>
      dbRow(
        `select coalesce(max(value), -1) from platform.app_usage_stock
          where app_slug = 'crm' and tenant_id = $1 and metric = 'seats'`,
        [owner.tenantId],
      )[0] === '3',
    { message: 'app_usage_stock non riflette i 3 posti occupati (coda app-usage non consumata?)' },
  )

  // ── 3. cambio ruolo e rimozione; il membro rimosso perde l'accesso ──────────
  await page.goto('/members')
  const roleSelect = page.getByLabel(`Change role: ${inviteeEmail}`)
  await roleSelect.selectOption('admin')
  await expect(roleSelect).toHaveValue('admin')
  expect(
    dbRow(
      `select m.role from platform.membership m
         join platform.identity i on i.id = m.identity_id
        where lower(i.email) = lower($1) and m.deleted_at is null`,
      [inviteeEmail],
    )[0],
  ).toBe('admin')

  const memberRow = page.getByRole('row').filter({ hasText: inviteeEmail })
  await memberRow.getByRole('button', { name: 'Remove' }).click()
  await page.getByRole('dialog').getByRole('button', { name: 'Remove' }).click()
  await expect(page.getByRole('cell', { name: inviteeEmail })).toHaveCount(0)

  // Dalla sessione del membro: al ricaricamento il ripristino sessione (refresh) fallisce → login.
  await memberPage.reload()
  await expect(memberPage.getByRole('button', { name: 'Sign in' })).toBeVisible()
  await memberContext.close()

  // ── 4. protezione ultimo owner (assert UI): niente cambio ruolo, rimozione disabilitata ──
  const ownerRow = page.getByRole('row').filter({ hasText: owner.email })
  await expect(ownerRow.getByRole('combobox')).toHaveCount(0)
  await expect(ownerRow.getByRole('button', { name: 'Remove' })).toBeDisabled()

  // ── leak detector: le appartenenze del conto sono owner + membro (uscito, soft-delete) ──
  expect(dbRow(`select count(*) from platform.membership where tenant_id = $1`, [owner.tenantId])[0]).toBe('2')
  expect(
    dbRow(`select count(*) from platform.membership where tenant_id = $1 and deleted_at is null`, [owner.tenantId])[0],
  ).toBe('1')
  expect(
    dbRow(`select status from platform.invitations where tenant_id = $1 and lower(email) = lower($2)`, [
      owner.tenantId,
      inviteeEmail,
    ])[0],
  ).toBe('accepted')
})
