import { test, expect } from '@playwright/test'
import { tenant, loginRaw, loginMfa, jwtPayload } from '../helpers/api'
import { browserLogin, acceptLegalGateIfPresent } from '../helpers/browser'
import { waitForEmail, extractLink } from '../helpers/mailbox'
import { totp } from '../helpers/totp'
import { dbRow } from '../helpers/db'

const NEW_PASSWORD = 'NuovaPassword2!'

/**
 * J-PWD — credenziali e secondo fattore (UC 0091).
 *
 * Reset password: richiesta → risposta neutra → EMAIL DI RESET REALMENTE RICEVUTA (Mailpit) →
 * link → nuova password → accesso (e la vecchia password non vale più). Attivazione 2FA:
 * enroll → secret → codice a tempo dell'helper totp() → conferma → login con la challenge
 * REALE del secondo fattore (il bypass dev è disattivato per l'auth della suite — run.sh).
 *
 * L'assert dello use case «il vecchio refresh non vale più dopo il reset» è escluso: il
 * provider locale usa refresh JWT stateless senza revoca — divario di prodotto tracciato nei
 * punti aperti di UC 0058 (decisione 7, change 0070).
 */

test('[J-PWD] reset con email reale → nuova password → 2FA enroll + login a due passi', async ({
  page,
  browser,
}) => {
  const t = await tenant('jpwd')

  // ── 1. reset password: richiesta → risposta neutra → email reale → nuova password ──
  await page.goto('/login')
  await page.getByRole('link', { name: 'Forgot your password?' }).click()
  await expect(page.getByRole('heading', { name: 'Reset your password' })).toBeVisible()
  await page.getByLabel('Email').fill(t.email)
  await page.getByRole('button', { name: 'Send reset link' }).click()
  await expect(page.getByText('If an account exists for that email, a reset link is on its way.')).toBeVisible()

  const resetMail = await waitForEmail(t.email, { subjectContains: 'Reset your password' })
  expect(resetMail.From.Address).toBe('noreply@appgrove.app')
  const resetLink = extractLink(resetMail, '/reset')
  await page.goto(resetLink)
  await expect(page.getByRole('heading', { name: 'Choose a new password' })).toBeVisible()
  await page.getByLabel('New password').fill(NEW_PASSWORD)
  await page.getByRole('button', { name: 'Update password' }).click()
  await expect(page.getByText('Your password has been updated.')).toBeVisible()

  // La vecchia password non vale più; la nuova sì (accesso completo dal browser).
  await expect(loginRaw(t.email, t.password)).rejects.toThrow(/401/)
  await browserLogin(page, t.email, NEW_PASSWORD)

  // ── 2. attivazione 2FA: enroll → secret → codice a tempo → conferma ─────────
  await page.goto('/security')
  await page.getByRole('button', { name: 'Enable 2FA' }).click()
  await expect(page.getByText('Scan this QR code with your authenticator app', { exact: false })).toBeVisible()
  const secret = (await page.locator('code').innerText()).trim()
  expect(secret.length).toBeGreaterThan(15)
  await page.getByLabel('Enter the 6-digit code to confirm').fill(totp(secret))
  await page.getByRole('button', { name: 'Confirm' }).click()
  await expect(page.getByText('Two-factor authentication is enabled.')).toBeVisible()
  // Il secret vive nello schema dev-only dell'auth locale, abilitato solo dopo la conferma.
  const [totpEnabled] = dbRow(
    `select c.totp_enabled from auth_local.credentials c
       join platform.identity u on u.cognito_sub = c.cognito_sub
      where lower(u.email) = lower($1)`,
    [t.email],
  )
  expect(totpEnabled).toBe('t')

  // ── login con secondo fattore: challenge reale, dal browser e via API ───────
  const mfaContext = await browser.newContext()
  const mfaPage = await mfaContext.newPage()
  await mfaPage.goto('/login')
  await mfaPage.getByLabel('Email').fill(t.email)
  await mfaPage.getByLabel('Password').fill(NEW_PASSWORD)
  await mfaPage.getByRole('button', { name: 'Sign in' }).click()
  await expect(mfaPage.getByRole('heading', { name: 'Two-factor authentication' })).toBeVisible()
  await expect(mfaPage.getByText('Enter the 6-digit code from your authenticator app')).toBeVisible()
  await mfaPage.getByLabel('Code', { exact: true }).fill(totp(secret))
  await mfaPage.getByRole('button', { name: 'Verify' }).click()
  await acceptLegalGateIfPresent(mfaPage)
  await expect(mfaPage.getByRole('navigation', { name: 'Platform' })).toBeVisible()
  await mfaContext.close()

  // Via API: login → challenge (mai i token al primo passo); codice giusto → token del TENANT giusto.
  const challenge = await loginRaw(t.email, NEW_PASSWORD)
  if (!('mfa_required' in challenge)) throw new Error('atteso mfa_required=true dal login con 2FA attivo')
  await expect(loginMfa(challenge.challenge_token, '000000')).rejects.toThrow(/401/)
  const tokens = await loginMfa(challenge.challenge_token, totp(secret))
  expect(String(jwtPayload(tokens.access_token).tenant_id)).toBe(t.tenantId)
})
