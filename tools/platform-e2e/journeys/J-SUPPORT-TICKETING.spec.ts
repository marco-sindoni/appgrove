import { test, expect } from '@playwright/test'
import { tenant } from '../helpers/api'
import { browserLogin, adminSession } from '../helpers/browser'
import { dbRow } from '../helpers/db'
import { waitForEmail } from '../helpers/mailbox'

/**
 * J-SUPPORT-TICKETING — assistenza nativa da un capo all'altro, fra DUE attori (UC 0075).
 *
 * Il cliente apre una richiesta dalla pagina Supporto; l'email di conferma arriva davvero (Mailpit)
 * e — per minimizzazione — <b>non</b> contiene il testo della conversazione; la piattaforma trova la
 * richiesta nella coda della sezione «Ticket», con la provenienza registrata; risponde, e la
 * richiesta passa in attesa dell'utente; il cliente vede la risposta e replica, rimettendo la palla
 * alla piattaforma; la piattaforma chiude, sotto conferma, e il cliente non può più rispondere.
 *
 * In coda: un'istanza privacy che tocca categorie particolari (art. 9) deve nascere a priorità alta
 * e contrassegnata «da rivedere», e portare la scadenza legale di un mese. È l'unica rete che
 * impedisce a una richiesta delicata di restare in fondo alla coda.
 *
 * Un secondo tenant "canarino" prova l'isolamento: nessuna richiesta deve sfiorarlo.
 */

test('[J-SUPPORT-TICKETING] apertura + email di conferma → coda della piattaforma → risposta → replica → chiusura, con escalation art. 9', async ({
  page,
  browser,
}) => {
  const run = Date.now().toString(36)
  const canary = await tenant(`jsupport-canary-${run}`)
  // Nome unico per esecuzione: la coda aggrega TUTTI i conti e il database della suite non viene
  // azzerato fra una corsa e l'altra.
  const t = await tenant(`jsupport-${run}`)
  const subject = `Non trovo lo storico pagamenti ${run}`

  // ── 1. sessione A (cliente): apre una richiesta di assistenza generica ──────
  await browserLogin(page, t.email, t.password)
  await page.goto('/support')
  await expect(page.getByRole('heading', { name: 'Support', level: 1 })).toBeVisible()
  // Gli allegati sono fuori da questa versione, e l'interfaccia lo dice prima che l'utente ci provi.
  await expect(page.getByText(/Attachments are not supported yet/)).toBeVisible()
  await page.getByLabel('Type').selectOption('support')
  await page.getByLabel('Subject').fill(subject)
  await page.getByLabel('Message').fill('Dalla pagina Billing non vedo le ricevute.')
  await page.getByRole('button', { name: 'Open ticket' }).click()
  await expect(page.getByText('Dalla pagina Billing non vedo le ricevute.')).toBeVisible()

  // ── 2. l'email di conferma parte davvero, e non porta con sé la conversazione ─
  // Si cerca per l'oggetto scritto dall'utente (unico per esecuzione): è indipendente dalla lingua
  // in cui la conferma viene resa, che dipende dalla preferenza dell'utente.
  const confirmation = await waitForEmail(t.email, { subjectContains: subject })
  expect(confirmation.Text).not.toContain('Dalla pagina Billing non vedo le ricevute.')
  expect(confirmation.Text).toContain('/support')

  // ── 3. sessione B (piattaforma): la richiesta è nella coda, con la provenienza ─
  const { context: adminContext, page: admin } = await adminSession(browser)
  // Il collegamento alla coda esiste in due punti — nel menu e come ritorno dal dettaglio: si passa
  // sempre da quello del menu, altrimenti dal dettaglio il selettore ne troverebbe due.
  const queueLink = admin
    .getByRole('navigation', { name: 'Platform admin' })
    .getByRole('link', { name: 'Support requests' })
  await queueLink.click()
  await expect(admin.getByRole('heading', { name: 'Support requests', level: 1 })).toBeVisible()
  const row = admin.getByRole('row').filter({ hasText: subject })
  await expect(row).toBeVisible()
  await expect(row).toContainText(t.displayName)
  await expect(row).toContainText('In-app form')

  // ── 4. la piattaforma risponde: la palla passa al cliente ───────────────────
  const reply = `Le ricevute sono nella sezione Payments — ${run}`
  await row.getByRole('link', { name: subject }).click()
  await admin.getByLabel('Reply to the user').fill(reply)
  await admin.getByRole('button', { name: 'Send reply' }).click()
  await expect(admin.getByText(reply)).toBeVisible()
  await expect(admin.getByText('Status: Waiting for the user', { exact: true })).toBeVisible()

  // ── 5. il cliente vede la risposta, capisce che tocca a lui, e replica ──────
  await page.goto('/support')
  await expect(page.getByText('Waiting for you')).toBeVisible()
  await page.getByRole('button', { name: subject }).click()
  await expect(page.getByText(reply)).toBeVisible()
  await expect(page.getByText(/the request is waiting for you/)).toBeVisible()
  await page.getByLabel('Reply').fill('Trovate, grazie.')
  await page.getByRole('button', { name: 'Send reply' }).click()
  await expect(page.getByText('Trovate, grazie.')).toBeVisible()
  // La replica rimette la palla alla piattaforma: lo stato torna aperto.
  await expect
    .poll(() => dbRow(`select status from platform.support_ticket where subject = $1`, [subject])[0])
    .toBe('open')

  // ── 6. escalation art. 9: l'istanza delicata nasce urgente e contrassegnata ──
  const sensitive = `Cancellate i miei dati sanitari ${run}`
  await page.goto('/support')
  await page.getByLabel('Type').selectOption('privacy')
  await page.getByLabel('Subject').fill(sensitive)
  await page.getByLabel('Message').fill('Vi ho mandato per errore il referto della mia malattia.')
  await page.getByRole('button', { name: 'Open ticket' }).click()
  // Il termine di legge è detto a parole al cliente, non lasciato a una data in una colonna.
  await expect(page.getByText(/by law we reply within one month/)).toBeVisible()

  await queueLink.click()
  const sensitiveRow = admin.getByRole('row').filter({ hasText: sensitive })
  await expect(sensitiveRow).toContainText('Needs review')
  await expect(sensitiveRow).toContainText('High')
  // …e il filtro per priorità la isola dal resto della coda.
  await admin.getByLabel('Priority').selectOption('high')
  await expect(admin.getByRole('row').filter({ hasText: subject })).toHaveCount(0)
  await expect(sensitiveRow).toBeVisible()

  // ── 7. la piattaforma chiude la prima richiesta, sotto conferma ─────────────
  await admin.getByLabel('Priority').selectOption('')
  await admin.getByRole('row').filter({ hasText: subject }).getByRole('link', { name: subject }).click()
  // Il nome accessibile «Status» esiste su DUE pagine — il filtro della coda e il modulo del
  // dettaglio — e il passaggio fra le due è asincrono (rotta dell'interfaccia + caricamento della
  // richiesta). Senza attendere il dettaglio DAVVERO caricato, `selectOption` cade sul filtro della
  // coda: il modulo resta sullo stato che aveva, «Update» non chiede conferma e il test fallisce
  // dicendo «finestra di conferma assente» invece di «ho parlato con la pagina sbagliata».
  await expect(admin.getByRole('heading', { name: `Request: ${subject}`, level: 1 })).toBeVisible()
  await admin.getByLabel('Status').selectOption('closed')
  await admin.getByRole('button', { name: 'Update' }).click()
  await expect(admin.getByRole('dialog')).toContainText('Close this request?')
  await admin.getByRole('dialog').getByRole('button', { name: 'Close the request' }).click()
  await expect(admin.getByText('This request is closed')).toBeVisible()

  // ── 8. il cliente non può più rispondere su una richiesta chiusa ────────────
  await page.goto('/support')
  await page.getByRole('button', { name: subject }).click()
  await expect(page.getByText(/This ticket is closed/)).toBeVisible()
  await expect(page.getByLabel('Reply')).toHaveCount(0)

  // ── 9. prove su database + isolamento (rilevatore di travaso) ───────────────
  expect(
    dbRow(
      `select source, status, closed_at is not null from platform.support_ticket where subject = $1`,
      [subject],
    ),
  ).toEqual(['form', 'closed', 't'])
  expect(
    dbRow(
      `select type, source, priority, flagged_for_review, due_at is not null
         from platform.support_ticket where subject = $1`,
      [sensitive],
    ),
  ).toEqual(['privacy', 'form', 'high', 't', 't'])
  expect(
    dbRow(`select count(*) from platform.support_ticket where tenant_id = $1`, [t.tenantId])[0],
  ).toBe('2')
  expect(
    dbRow(`select count(*) from platform.support_ticket where tenant_id = $1`, [canary.tenantId])[0],
  ).toBe('0')

  await adminContext.close()
})
