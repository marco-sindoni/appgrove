import { test, expect } from '@playwright/test'

const BASE = process.env.APPGROVE_L3_BASE_URL
const EMAIL = process.env.APPGROVE_L3_USER_EMAIL
const PASSWORD = process.env.APPGROVE_L3_USER_PASSWORD

/**
 * L3 — smoke checkout su Paddle SANDBOX reale (UC 0029, #09 D20 L3): UN percorso felice con vero
 * Paddle.js, carta di test e vero webhook, per validare il contratto reale che L1/L2 mockano.
 *
 * Auto-skip senza env sandbox: la suite è pre-release, si attiva quando esistono
 * l'account Paddle Sandbox (UC 0001), il loader Paddle.js reale (gated #14) e la pipeline di
 * release che la esegue nel gate di approvazione (UC 0005). I selettori dentro l'iframe Paddle
 * vanno finalizzati alla prima esecuzione reale (punto aperto tracciato in UC 0029).
 */
test.describe('L3 — smoke checkout su Paddle Sandbox', () => {
  test.skip(
    !BASE || !EMAIL || !PASSWORD,
    'env sandbox assente (APPGROVE_L3_BASE_URL / APPGROVE_L3_USER_EMAIL / APPGROVE_L3_USER_PASSWORD): ' +
      'suite pre-release, non per-PR — vedi e2e-l3/README.md',
  )

  test('acquisto sandbox: checkout reale → webhook reale → attivazione', async ({ page }) => {
    // login reale (auth locale, UC 0010) sull'ambiente deployato
    await page.goto('/login')
    await page.getByLabel(/email/i).fill(EMAIL!)
    await page.getByLabel(/password/i).fill(PASSWORD!)
    await page.getByRole('button', { name: /sign in|accedi/i }).click()

    // scelta tier e acquisto server-initiated (stessi passi della L2, ma senza mock)
    await page.goto('/billing')
    await page.getByRole('button', { name: /subscribe/i }).first().click()

    // overlay Paddle Sandbox reale: carta di test 4242 4242 4242 4242, scadenza futura, CVC 100.
    // I selettori dell'iframe si finalizzano alla prima esecuzione reale (UC 0029, punti aperti).
    const paddleFrame = page.frameLocator('iframe[name*="paddle" i]')
    await paddleFrame.locator('#cardNumber').fill('4242424242424242')

    // attivazione: il VERO webhook sandbox aggiorna la subscription → l'UX di polling conferma.
    await expect(page.getByText(/all set!.*active/i)).toBeVisible({ timeout: 120_000 })
  })
})
