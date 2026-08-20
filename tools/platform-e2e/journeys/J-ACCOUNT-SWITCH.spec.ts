import { test, expect, type Page } from '@playwright/test'
import { tenant } from '../helpers/api'
import { acceptLegalGateIfPresent } from '../helpers/browser'
import { dbRow, dbExec } from '../helpers/db'

/** La risposta che DECIDE il gate legale: prima che arrivi, la shell è navigabile per fail-open. */
const LEGAL_SETTLED = (r: { url(): string }) => r.url().includes('/me/legal/status')

/**
 * Attende di essere **davvero** dentro l'account atteso, attraversando il gate legale quando compare.
 *
 * Perché non basta `acceptLegalGateIfPresent` da solo: il gate è **fail-open mentre il suo stato
 * carica** — per un istante la shell è navigabile e solo dopo il gate la sostituisce. Chiedere «gate
 * oppure shell» una volta sola perde quella corsa, e il gate si apre un attimo dopo. Questo percorso
 * entra in DUE account, quindi incontra il gate più volte di ogni altro: è quello che la paga.
 *
 * L'attesa è sull'**esito** — il nome dell'account nuovo nella barra laterale — e si riprova finché
 * non c'è, non su un istante fisso.
 */
async function expectInsideAccount(page: Page, accountName: string): Promise<void> {
  await expect(async () => {
    await acceptLegalGateIfPresent(page)
    await expect(page.getByRole('navigation', { name: 'Platform' }).getByTestId('active-account-name'))
      .toHaveText(accountName, { timeout: 1_000 })
  }).toPass({ timeout: 20_000 })
}

/**
 * Accesso dal browser. Non usa la scorciatoia condivisa `browserLogin` di proposito: quel passo dà
 * per assodato che la shell sia visibile appena attraversato il gate, e con il fail-open descritto
 * sopra non è sempre vero. Il presidio condiviso resta valido per i percorsi che incontrano il gate
 * una volta sola; il rimando è tracciato in docs/_BACKLOG.md.
 */
async function login(page: Page, email: string, password: string, accountName: string): Promise<void> {
  await page.goto('/login')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill(password)
  const legalSettled = page.waitForResponse(LEGAL_SETTLED)
  await page.getByRole('button', { name: 'Sign in' }).click()
  await legalSettled
  await expectInsideAccount(page, accountName)
}

/**
 * Cambia account dal selettore e arriva nell'account nuovo. Il **ricaricamento è parte del
 * comportamento** (UC 0117: mezza applicazione con l'account nuovo e mezza col vecchio è il modo
 * peggiore di sbagliare), quindi si attende l'evento di caricamento e non un istante fisso —
 * altrimenti l'asserzione successiva guarderebbe ancora il documento vecchio e leggerebbe il nome
 * dell'account di prima.
 *
 * Nell'account in cui si entra per la prima volta il **gate legale è pendente**, perché
 * l'accettazione dei documenti è per ACCOUNT e non per persona (UC 0056): ogni account è un
 * contratto a sé. Si attraversa come farebbe l'utente — è parte del percorso reale, non un artificio
 * del collaudo.
 */
async function switchAccountTo(page: Page, accountName: string): Promise<void> {
  await page.getByLabel('Switch account').click()
  const reloaded = page.waitForEvent('load')
  const legalSettled = page.waitForResponse(LEGAL_SETTLED)
  await page.getByRole('button', { name: new RegExp(accountName) }).click()
  await reloaded
  await legalSettled
  await expectInsideAccount(page, accountName)
}

/**
 * J-ACCOUNT-SWITCH — account attivo della sessione e selettore (UC 0117).
 *
 * La **stessa persona** entra nel proprio account e vede tutti i menu di piattaforma; passa
 * all'account dell'altra azienda, dove è collaboratrice, e la voce di gestione delle persone
 * scompare; torna indietro. Un solo essere umano, due esperienze diverse: è il collaudo più utile
 * della storia, perché prova insieme il cambio del claim, il ricaricamento e il fatto che i permessi
 * seguono l'account e non la persona.
 *
 * <p>La seconda appartenenza si costruisce con una **leva d'ambiente** e non da un percorso di
 * prodotto: nessun percorso ne crea una finché non esiste UC 0118 (inviti e registrazione con
 * identità esistente). È il terzo uso sanzionato di {@code dbExec}, dichiarato nel suo elenco.
 *
 * <p>Ciò che NON si anticipa: la visibilità fine per ruolo (UC 0107). L'asserzione usa la differenza
 * già vera oggi — le voci Account/Billing/Members sono di owner e admin, non dei member.
 */
test('[J-ACCOUNT-SWITCH] due appartenenze → selettore, cambio con ricarica, menu che seguono l’account, ritorno', async ({
  page,
}) => {
  // ── 1. due account veri, creati dalle API pubbliche come farebbe un utente ──
  const persona = await tenant('jswitch-persona') // owner del proprio account
  const azienda = await tenant('jswitch-azienda') // account di un'altra azienda

  const [identityId, primaAppartenenza] = dbRow(
    `select i.id, m.id from platform.identity i
       join platform.membership m on m.identity_id = i.id and m.tenant_id = $2
      where lower(i.email) = lower($1) and i.deleted_at is null and m.deleted_at is null`,
    [persona.email, persona.tenantId],
  )

  // Leva d'ambiente: la persona diventa collaboratrice dell'altra azienda. L'account attivo resta
  // il proprio, così l'accesso atterra dove atterrava prima.
  dbExec(
    `insert into platform.membership
       (id, tenant_id, identity_id, role, status, created_at, updated_at, created_by)
     values (gen_random_uuid(), $1, $2, 'member', 'active', now(), now(), 'e2e')`,
    [azienda.tenantId, identityId],
  )
  dbExec('update platform.identity set active_membership_id = $1 where id = $2', [
    primaAppartenenza,
    identityId,
  ])

  // ── 2. nel proprio account: nome dell'account, selettore, menu completi ────
  await login(page, persona.email, persona.password, persona.displayName)
  const sidebar = page.getByRole('navigation', { name: 'Platform' })
  await expect(sidebar.getByRole('link', { name: 'Members' })).toBeVisible()
  // Il selettore esiste perché le appartenenze sono due (con una sola non sarebbe nel documento).
  await expect(page.getByLabel('Switch account')).toBeVisible()

  // ── 3. cambio verso l'account dell'azienda: il claim cambia, la pagina ricarica ──
  await switchAccountTo(page, azienda.displayName)

  // Collaboratrice, non owner: la gestione delle persone non le appartiene, e la voce non c'è.
  await expect(sidebar.getByRole('link', { name: 'Members' })).toHaveCount(0)
  // La scelta è conservata lato server: è da lì che il token la rileggerà al prossimo rinnovo.
  const [attivaOra] = dbRow(
    `select m.tenant_id from platform.identity i
       join platform.membership m on m.id = i.active_membership_id
      where i.id = $1`,
    [identityId],
  )
  expect(attivaOra).toBe(azienda.tenantId)

  // ── 4. ritorno nel proprio account: la stessa persona, l'altra esperienza ──
  await switchAccountTo(page, persona.displayName)

  await expect(sidebar.getByRole('link', { name: 'Members' })).toBeVisible()

  // ── 5. la traccia di controllo: due cambi, nell'ordine in cui sono avvenuti ──
  const [cambi] = dbRow('select count(*) from platform.active_account_audit where identity_id = $1', [
    identityId,
  ])
  expect(Number(cambi)).toBe(2)
  const [ultimo] = dbRow(
    `select to_tenant_id from platform.active_account_audit
      where identity_id = $1 order by executed_at desc, id desc limit 1`,
    [identityId],
  )
  expect(ultimo).toBe(persona.tenantId)
})
