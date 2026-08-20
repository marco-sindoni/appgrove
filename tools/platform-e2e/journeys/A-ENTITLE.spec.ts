import { test, expect, type Browser, type Page } from '@playwright/test'
import { tenant, authedFetch, type Tenant } from '../helpers/api'
import { browserLogin, adminSession } from '../helpers/browser'
import { buyApp } from '../helpers/paddle'
import { dbRow } from '../helpers/db'

const CORE_API = process.env.PLATFORM_CORE_API ?? 'http://localhost:20080'

/**
 * A-ENTITLE — coerenza della matrice dei diritti d'accesso (UC 0092, su UC 0077).
 *
 * UC 0077 ha unificato in una sola regola la risposta alla domanda «questo account ha accesso a
 * quest'app?», prima duplicata fra il read-model del cliente e la matrice della console — e
 * divergente: la matrice non vedeva le app abilitate dalla **fascia gratuita di base** e ignorava
 * l'account in eliminazione. Chi apriva la console per sapere cosa vede un cliente poteva leggere
 * una risposta falsa.
 *
 * Qui la coerenza smette di essere un ragionamento e diventa un'osservazione: tre tenant portati
 * in tre stati diversi per vie reali, e per ciascuno il confronto fra **quello che la console
 * dice** e **quello che il cliente vede nel proprio menu laterale**. Devono coincidere esattamente.
 *
 * Journey parallelo: agisce solo dentro i propri tre tenant e legge la matrice filtrandoli.
 */

/** Nomi (chiave `href`) delle app visibili nel menu laterale del cliente. */
async function sidebarApps(page: Page): Promise<string[]> {
  const nav = page.getByRole('navigation', { name: 'Platform' })
  await expect(nav).toBeVisible()
  const hrefs = await nav.locator('a[href^="/app/"]').evaluateAll((links) =>
    links.map((l) => (l.getAttribute('href') ?? '').split('/')[2]),
  )
  return [...new Set(hrefs)].sort()
}

/** Apre una sessione browser per il tenant e ne legge le app del menu laterale. */
async function appsSeenBy(browser: Browser, t: Tenant): Promise<string[]> {
  const context = await browser.newContext()
  try {
    const page = await context.newPage()
    await browserLogin(page, t.email, t.password)
    return await sidebarApps(page)
  } finally {
    await context.close()
  }
}

test('[A-ENTITLE] acquisto attivo · sola fascia gratuita · account in eliminazione → matrice ≡ menu laterale', async ({
  browser,
}) => {
  const run = Date.now().toString(36)

  // ── 1. tre tenant, tre stati, tutti raggiunti per vie reali ─────────────────
  // I nomi sono unici per esecuzione: la matrice della console elenca l'account per nome e il
  // database della suite non viene azzerato fra una corsa e l'altra.
  const paid = await tenant(`aentitle-paid-${run}`)
  const free = await tenant(`aentitle-free-${run}`)
  const deleting = await tenant(`aentitle-deleting-${run}`)

  await buyApp(paid.tokens, 'crm', 'team')

  // Account in eliminazione: la richiesta che l'utente stesso può fare dai propri dati. La
  // disattivazione è immediata (UC 0033) e vale zero diritti — nessuna leva artificiosa serve.
  const deletionRequest = await authedFetch(CORE_API, '/api/platform/v1/accounts/me/deletion', deleting.tokens, {
    method: 'POST',
  })
  expect(deletionRequest.status).toBe(202)
  expect(dbRow(`select status from platform.accounts where id::text = $1`, [deleting.tenantId])[0]).toBe(
    'pending_deletion',
  )

  // ── 2. quello che ciascun cliente vede nel proprio menu laterale ────────────
  // Attesi: chi paga e chi non paga vedono entrambe le app (Fatture e Mini-CRM offrono una fascia
  // gratuita di base); chi è in eliminazione non vede nulla.
  const seenPaid = await appsSeenBy(browser, paid)
  const seenFree = await appsSeenBy(browser, free)
  const seenDeleting = await appsSeenBy(browser, deleting)

  expect(seenPaid).toContain('crm')
  expect(seenFree).toContain('crm')
  expect(seenDeleting).toEqual([])

  // ── 3. quello che la console dice degli stessi tre account ──────────────────
  const { context: adminContext, page: admin } = await adminSession(browser)
  await admin.getByRole('link', { name: 'Entitlements' }).click()
  await expect(admin.getByRole('heading', { name: 'Entitlements' })).toBeVisible()

  // La matrice è cross-account: si attende che i tre tenant appena creati vi siano comparsi
  // prima di leggerla (attesa su condizione, mai a tempo).
  for (const t of [paid, free, deleting]) {
    await expect(admin.getByRole('row').filter({ hasText: t.displayName }).first()).toBeVisible()
  }

  /** App con diritto d'accesso «Yes» per l'account, lette dalla matrice della console. */
  const matrixApps = async (accountName: string): Promise<string[]> => {
    const rows = admin.getByRole('row').filter({ hasText: accountName })
    const cells = await rows.evaluateAll((trs) =>
      trs.map((tr) => Array.from(tr.querySelectorAll('td')).map((td) => td.textContent?.trim() ?? '')),
    )
    return cells
      .filter((c) => c[3] === 'Yes')
      .map((c) => c[1])
      .sort()
  }

  // La matrice mostra il NOME dell'app (Fatture, Mini-CRM), il menu laterale il suo
  // identificativo di rotta: si confronta l'insieme delle app, tradotto in identificativi.
  //
  // Il confronto si restringe alle app che hanno un MODULO nel frontend, perché il menu laterale
  // è «registro dei moduli ∩ diritti d'accesso» (#03 dec. 6): in profilo di sviluppo il catalogo
  // porta anche app-fixture (`notes`, `teams`) che nessun modulo serve — comparirle nella matrice
  // è corretto, e pretenderle nel menu sarebbe un errore del collaudo, non del prodotto.
  const NAME_TO_ID: Record<string, string> = { Fatture: 'fatture', 'Mini-CRM': 'crm' }
  const MODULE_IDS = ['crm', 'fatture']
  const asModuleIds = (names: string[]) =>
    [...new Set(names.map((n) => NAME_TO_ID[n] ?? n))].filter((id) => MODULE_IDS.includes(id)).sort()

  expect(asModuleIds(await matrixApps(paid.displayName))).toEqual(seenPaid)
  expect(asModuleIds(await matrixApps(free.displayName))).toEqual(seenFree)
  expect(asModuleIds(await matrixApps(deleting.displayName))).toEqual(seenDeleting)

  // ── 4. la differenza fra i tre stati è visibile anche in console ────────────
  // L'account che paga porta lo stato della sottoscrizione; quello gratuito no (nessuna
  // sottoscrizione: è la baseline che prima della regola unica restava invisibile qui).
  const paidRow = admin.getByRole('row').filter({ hasText: paid.displayName }).filter({ hasText: 'Mini-CRM' })
  await expect(paidRow).toContainText(/active|trialing/)
  const freeRow = admin.getByRole('row').filter({ hasText: free.displayName }).filter({ hasText: 'Mini-CRM' })
  await expect(freeRow).toContainText('—')

  // ── rilevatore di travaso: i tre tenant restano distinti e separati ─────────
  for (const t of [paid, free, deleting]) {
    expect(dbRow(`select count(*) from platform.membership where tenant_id = $1`, [t.tenantId])[0]).toBe('1')
  }
  expect(dbRow(`select status from platform.accounts where id::text = $1`, [paid.tenantId])[0]).toBe('active')
  expect(dbRow(`select status from platform.accounts where id::text = $1`, [free.tenantId])[0]).toBe('active')

  await adminContext.close()
})
