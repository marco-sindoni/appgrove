import { test, expect } from '@playwright/test'
import { tenant } from '../helpers/api'
import { browserLogin, adminSession } from '../helpers/browser'
import { dbRow, dbRows } from '../helpers/db'

/**
 * A-GDPR — console Diritti GDPR da un capo all'altro, fra DUE attori (UC 0092, su UC 0034).
 *
 * Il cliente esercita i propri diritti (ticket privacy + export dei dati); l'operatore di
 * piattaforma li ritrova **aggregati** nella propria console, risponde nel filo di conversazione e
 * applica la **limitazione del trattamento (art. 18)**; il cliente vede la risposta e subisce la
 * limitazione; l'operatore la **rimuove** e il cliente rientra.
 *
 * La limitazione si applica all'**utente** e non all'account: è l'unico bersaglio che produce un
 * effetto osservabile dalla sessione del cliente, perché la validità di una sessione dipende dallo
 * stato della riga utente (divergenza annotata nei punti aperti di UC 0034).
 *
 * Un secondo tenant "canarino" prova l'isolamento: né il ticket né l'export devono sfiorarlo.
 */

test('A-GDPR: ticket privacy + export → aggregazione in console → risposta visibile → limitazione art. 18 applicata e rimossa', async ({
  page,
  browser,
}) => {
  const run = Date.now().toString(36)
  const canary = await tenant(`agdpr-canary-${run}`)
  // Nome unico per esecuzione: la console aggrega TUTTI gli account e il database della suite non
  // viene azzerato fra una corsa e l'altra.
  const t = await tenant(`agdpr-${run}`)
  const subject = `Richiesta privacy ${run}`

  // ── 1. sessione A (cliente): ticket privacy dalla pagina Supporto ───────────
  await browserLogin(page, t.email, t.password)
  await page.goto('/support')
  await expect(page.getByRole('heading', { name: 'Support', level: 1 })).toBeVisible()
  await page.getByLabel('Type').selectOption('privacy')
  await page.getByLabel('Subject').fill(subject)
  await page.getByLabel('Message').fill('Vorrei sapere quali dati trattate sul mio conto.')
  await page.getByRole('button', { name: 'Open ticket' }).click()
  await expect(page.getByText('Vorrei sapere quali dati trattate sul mio conto.')).toBeVisible()

  // ── 1b. …ed export dei propri dati (job asincrono reale) ────────────────────
  await page.goto('/privacy')
  await expect(page.getByRole('heading', { name: 'My data', level: 1 })).toBeVisible()
  await page.getByRole('button', { name: /Start export/ }).click()
  await expect(page.getByText('Export ready.')).toBeVisible({ timeout: 60_000 })

  // ── 2. sessione B (operatore): la console aggrega richiesta ed export ───────
  const { context: adminContext, page: admin } = await adminSession(browser)
  await admin.getByRole('link', { name: 'GDPR rights' }).click()
  await expect(admin.getByRole('heading', { name: 'GDPR rights', exact: true })).toBeVisible()

  const ownRows = admin.getByRole('row').filter({ hasText: t.displayName })
  // Export con la prova di completamento (stato concluso + data di completamento).
  const exportRow = ownRows.filter({ hasText: 'Export' }).first()
  await expect(exportRow).toBeVisible()
  await expect(exportRow).toContainText('COMPLETED')
  const [zipKey] = dbRow(
    `select zip_key from platform.gdpr_export_job
      where tenant_id = $1 and status = 'COMPLETED' order by created_at desc limit 1`,
    [t.tenantId],
  )
  expect(zipKey).not.toBe('')
  // Ticket privacy: presente nell'aggregazione delle richieste…
  await expect(ownRows.filter({ hasText: 'Privacy ticket' }).first()).toBeVisible()
  // …e nell'elenco dei ticket, da cui si apre il dettaglio.
  await admin.getByRole('link', { name: subject }).click()
  await expect(admin.getByRole('heading', { name: new RegExp(subject) })).toBeVisible()

  // ── 3. l'operatore risponde nel filo; il cliente vede la risposta ───────────
  const reply = `Le rispondiamo nel merito — ${run}`
  await admin.getByLabel('Reply to the user').fill(reply)
  await admin.getByRole('button', { name: 'Send reply' }).click()
  await expect(admin.getByText(reply)).toBeVisible()

  await page.goto('/support')
  await page.getByRole('button', { name: subject }).click()
  await expect(page.getByText(reply)).toBeVisible()

  // ── 4. limitazione art. 18 sull'utente, con conferma ────────────────────────
  const [userId] = dbRow(
    `select id from platform.users where lower(email) = lower($1) and deleted_at is null`,
    [t.email],
  )
  await admin.getByRole('link', { name: '← GDPR rights' }).click()
  await admin.getByLabel('Target', { exact: true }).selectOption('user')
  await admin.getByLabel('Target ID (UUID)').fill(userId)
  await admin.getByLabel('Note (optional)').fill(`Limitazione richiesta dall'interessato — ${run}`)
  await admin.getByRole('button', { name: 'Apply restriction' }).click()
  await admin.getByRole('dialog').getByRole('button', { name: 'Apply restriction' }).click()
  const activeEntry = admin.getByRole('listitem').filter({ hasText: userId })
  await expect(activeEntry).toBeVisible()

  // ── 5. sessione A: la limitazione si sente davvero ──────────────────────────
  // La sessione non è più ripristinabile: si esce in modo pulito verso l'accesso…
  await page.goto('/')
  await expect(page.getByRole('button', { name: 'Sign in' })).toBeVisible()
  // …e un nuovo tentativo viene rifiutato (l'utenza è sospesa lato server).
  await page.getByLabel('Email').fill(t.email)
  await page.getByLabel('Password').fill(t.password)
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByRole('alert')).toBeVisible()
  await expect(page.getByRole('navigation', { name: 'Platform' })).toHaveCount(0)

  // ── 6. la limitazione è reversibile: rimossa, il cliente rientra ────────────
  await activeEntry.getByRole('button', { name: 'Remove' }).click()
  await admin.getByRole('dialog').getByRole('button', { name: 'Remove' }).click()
  await expect(admin.getByRole('listitem').filter({ hasText: userId })).toHaveCount(0)

  // Il cliente rientra da solo al primo ricaricamento: il cookie di sessione non era stato
  // cancellato, era il server a non onorarlo più — tolta la limitazione, torna a onorarlo. È la
  // prova più stringente della reversibilità: non serve nemmeno riaccedere.
  await page.goto('/')
  await expect(page.getByRole('navigation', { name: 'Platform' })).toBeVisible()

  // ── 7. prove nel registro + isolamento (rilevatore di travaso) ──────────────
  const [adminSub] = dbRow(
    `select cognito_sub from platform.users where lower(email) = lower($1) and deleted_at is null`,
    ['admin@appgrove.test'],
  )
  const trail = dbRows(
    `select action, actor, tenant_id from platform.gdpr_restriction_audit
      where target_kind = 'user' and target_id = $1 order by executed_at`,
    [userId],
  )
  expect(trail.map(([action]) => action)).toEqual(['applied', 'removed'])
  expect(trail.every(([, actor]) => actor === adminSub)).toBeTruthy()
  expect(trail.every(([, , tenantId]) => tenantId === t.tenantId)).toBeTruthy()
  expect(
    dbRow(`select status, suspended_reason from platform.users where id::text = $1`, [userId]),
  ).toEqual(['active', ''])

  // Ticket ed export appartengono al solo tenant giusto: il canarino resta pulito.
  expect(dbRow(`select count(*) from platform.support_ticket where tenant_id = $1`, [t.tenantId])[0]).toBe('1')
  expect(dbRow(`select count(*) from platform.support_ticket where tenant_id = $1`, [canary.tenantId])[0]).toBe('0')
  expect(dbRow(`select count(*) from platform.gdpr_export_job where tenant_id = $1`, [canary.tenantId])[0]).toBe('0')
  expect(
    dbRow(`select count(*) from platform.gdpr_restriction_audit where tenant_id = $1`, [canary.tenantId])[0],
  ).toBe('0')

  await adminContext.close()
})
