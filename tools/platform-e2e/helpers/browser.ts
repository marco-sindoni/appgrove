/**
 * browser — passi UI condivisi dai journey (UC 0091/0092): accesso dal browser e attraversamento
 * del gate legale bloccante (UC 0056), che per ogni tenant nuovo è parte del percorso reale del
 * primo ingresso nel workspace (decisione 13 della change 0069), più la **seconda sessione
 * amministratore** sulla console admin usata dai journey che osservano un effetto fra due attori
 * (UC 0092: l'azione dell'amministratore vista dal cliente).
 */
import { expect, type Browser, type BrowserContext, type Page } from '@playwright/test'
import { ADMIN_EMAIL, ADMIN_PASSWORD } from './api'

/**
 * Se il gate di accettazione legali è visibile, lo attraversa come farebbe l'utente
 * (spunta OGNI consenso corrente e conferma); altrimenti non fa nulla. L'attesa è sulla
 * prima delle due condizioni (gate o shell navigabile), mai a tempo fisso.
 */
export async function acceptLegalGateIfPresent(page: Page): Promise<void> {
  const gate = page.getByRole('dialog', { name: 'Updated legal documents' })
  const nav = page.getByRole('navigation', { name: 'Platform' })
  await expect(gate.or(nav).first()).toBeVisible()
  if (await gate.isVisible()) {
    for (const box of await gate.getByRole('checkbox').all()) await box.check()
    await gate.getByRole('button', { name: 'Continue' }).click()
    await expect(gate).not.toBeVisible()
  }
}

/**
 * Login dal browser (pagina /login) e ingresso nella shell, gate legale incluso.
 * Precondizione dei journey che non collaudano l'autenticazione stessa.
 */
export async function browserLogin(page: Page, email: string, password: string): Promise<void> {
  await page.goto('/login')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill(password)
  await page.getByRole('button', { name: 'Sign in' }).click()
  await acceptLegalGateIfPresent(page)
  await expect(page.getByRole('navigation', { name: 'Platform' })).toBeVisible()
}

/** Console admin servita dalla suite (server statico dedicato, porta distinta dal backoffice). */
export const ADMIN_URL = process.env.PLATFORM_ADMIN_URL ?? 'http://localhost:24174'

/**
 * Seconda sessione browser sulla **console admin**: contesto isolato (cookie e memoria separati
 * da quelli del cliente), accesso col platform-admin del seed, attesa della shell admin.
 *
 * Contesto proprio e non semplice seconda scheda: è il punto dei journey fra due attori — le due
 * sessioni non devono poter condividere nulla, o l'osservazione non proverebbe niente. Chi chiama
 * chiude il contesto (o lascia che lo faccia la chiusura del browser di fine test).
 */
export async function adminSession(browser: Browser): Promise<{ context: BrowserContext; page: Page }> {
  const context = await browser.newContext({ baseURL: ADMIN_URL })
  const page = await context.newPage()
  await page.goto('/login')
  await page.getByLabel('Email').fill(ADMIN_EMAIL)
  await page.getByLabel('Password').fill(ADMIN_PASSWORD)
  await page.getByRole('button', { name: 'Sign in' }).click()
  // La console è riservata al ruolo platform-admin: se il token non lo portasse, qui si finirebbe
  // su /forbidden invece che nella shell — l'attesa sulla navigazione admin è anche il gate di ruolo.
  await expect(page.getByRole('navigation', { name: 'Platform admin' })).toBeVisible()
  return { context, page }
}
