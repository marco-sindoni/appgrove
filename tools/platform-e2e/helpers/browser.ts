/**
 * browser — passi UI condivisi dai journey (UC 0091): accesso dal browser e attraversamento del
 * gate legale bloccante (UC 0056), che per ogni tenant nuovo è parte del percorso reale del
 * primo ingresso nel workspace (decisione 13 della change 0069).
 */
import { expect, type Page } from '@playwright/test'

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
