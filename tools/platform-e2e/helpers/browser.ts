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

/** La risposta che DECIDE il gate legale: prima che arrivi, la shell è navigabile per fail-open. */
const LEGAL_SETTLED = (r: { url(): string }) => r.url().includes('/me/legal/status')

/**
 * Attende di essere <b>davvero</b> dentro l'account atteso, attraversando il gate legale quando
 * compare, e insistendo sull'<b>esito</b> — il nome dell'account nella barra laterale.
 *
 * Perché non basta {@link acceptLegalGateIfPresent} da solo: il gate è **fail-open mentre il suo
 * stato carica** — per un istante la shell è navigabile e solo dopo il gate la sostituisce. Chiedere
 * «gate oppure shell» una volta sola perde quella corsa, e il gate si apre un attimo dopo. Serve ai
 * journey che entrano in PIÙ di un account: sono quelli che incontrano il gate più volte, e quindi
 * quelli che la pagano. Entrando per la prima volta in un account nuovo il gate è pendente perché
 * l'accettazione dei documenti è per ACCOUNT e non per persona (UC 0056): ogni account è un
 * contratto a sé, e attraversarlo è parte del percorso reale.
 */
export async function expectInsideAccount(page: Page, accountName: string): Promise<void> {
  await expect(async () => {
    await acceptLegalGateIfPresent(page)
    await expect(page.getByRole('navigation', { name: 'Platform' }).getByTestId('active-account-name'))
      .toHaveText(accountName, { timeout: 1_000 })
  }).toPass({ timeout: 20_000 })
}

/**
 * Accesso dal browser con attesa dell'account in cui si atterra. Non usa {@link browserLogin} di
 * proposito: quel passo dà per assodato che la shell sia visibile appena attraversato il gate, e con
 * il fail-open descritto sopra non è sempre vero. Il presidio condiviso resta valido per i journey
 * che incontrano il gate una volta sola.
 */
export async function loginIntoAccount(
  page: Page,
  email: string,
  password: string,
  accountName: string,
): Promise<void> {
  await page.goto('/login')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill(password)
  const legalSettled = page.waitForResponse(LEGAL_SETTLED)
  await page.getByRole('button', { name: 'Sign in' }).click()
  await legalSettled
  await expectInsideAccount(page, accountName)
}

/**
 * Cambia account dal selettore e arriva nell'account nuovo. Il <b>ricaricamento è parte del
 * comportamento</b> (UC 0117: mezza applicazione con l'account nuovo e mezza col vecchio è il modo
 * peggiore di sbagliare), quindi si attende l'evento di caricamento e non un istante fisso —
 * altrimenti l'asserzione successiva guarderebbe ancora il documento vecchio e leggerebbe il nome
 * dell'account di prima.
 */
export async function switchAccountTo(page: Page, accountName: string): Promise<void> {
  await page.getByLabel('Switch account').click()
  const reloaded = page.waitForEvent('load')
  const legalSettled = page.waitForResponse(LEGAL_SETTLED)
  await page.getByRole('button', { name: new RegExp(accountName) }).click()
  await reloaded
  await legalSettled
  await expectInsideAccount(page, accountName)
}
