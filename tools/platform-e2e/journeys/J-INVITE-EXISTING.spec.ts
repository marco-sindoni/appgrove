import { test, expect } from '@playwright/test'
import { authedFetch, tenant } from '../helpers/api'
import {
  expectInsideAccount,
  loginIntoAccount,
  switchAccountTo,
  waitForLegalDecision,
} from '../helpers/browser'
import { dbRow } from '../helpers/db'

const CORE_API = process.env.PLATFORM_CORE_API ?? 'http://localhost:20080'

/**
 * J-INVITE-EXISTING — un'azienda invita una persona che **ha già** un proprio account (UC 0118).
 *
 * È il percorso che prima di questa storia non esisteva: l'invito partiva e il rifiuto arrivava
 * all'accettazione, come violazione del vincolo «una persona → un solo account». Ora la persona
 * accetta **dalla propria sessione** — nessuna seconda registrazione, nessuna parola d'accesso nuova
 * — e si trova con due appartenenze e una sola identità.
 *
 * <p>Quattro cose si provano insieme, e nessuna delle quattro si può provare senza le altre:
 * <ol>
 *   <li>l'invito a un indirizzo che ha già un'identità <b>si crea</b> come qualunque altro, e la
 *       risposta a chi invita <b>non contiene</b> traccia dell'identità collegata;</li>
 *   <li>l'invito compare come consenso da dare <b>in testa al cruscotto</b> della persona, col nome
 *       dell'azienda che invita, e col numero sulla voce del menu;</li>
 *   <li>accettando nasce una <b>appartenenza</b> in più e l'account nuovo diventa quello attivo;</li>
 *   <li>da lì la persona passa fra i due account col selettore (UC 0117), che prima non aveva perché
 *       aveva un solo account.</li>
 * </ol>
 *
 * <p>Il collegamento all'identità esistente si verifica <b>in banca dati</b> e non nella risposta: è
 * esattamente la distinzione che la storia impone — il valore esiste, serve, e non esce mai verso
 * l'account che ha invitato (UC 0118 §5).
 */
test('[J-INVITE-EXISTING] invito a chi ha già un account → accettazione dal cruscotto → seconda appartenenza → passaggio fra i due account', async ({
  page,
}) => {
  // ── 1. due account veri, creati dalle API pubbliche come farebbe un utente ──
  const persona = await tenant('jinvite-persona') // ha già un proprio account, ne è owner
  const azienda = await tenant('jinvite-azienda') // l'azienda che invita

  // ── 2. l'azienda invita l'indirizzo della persona ──────────────────────────
  const res = await authedFetch(CORE_API, '/api/platform/v1/invitations', azienda.tokens, {
    body: { email: persona.email, role: 'member' },
  })
  expect(res.status, "l'invito a chi esiste già si crea come qualunque altro").toBe(201)
  const created = (await res.json()) as Record<string, unknown>
  // Nessun campo che riveli l'identità collegata: sapere che quella persona ha già un rapporto con
  // la piattaforma non è un'informazione dell'account che la invita.
  expect(Object.keys(created)).not.toContain('identityId')
  expect(JSON.stringify(created)).not.toContain('identity_id')

  // Il collegamento però ESISTE, valorizzato lato server: è ciò che permetterà di far entrare la
  // persona senza coniarne una seconda identità.
  const [identityIdCollegata] = dbRow(
    'select identity_id from platform.invitations where id = $1',
    [String(created.id)],
  )
  const [identityId] = dbRow(
    'select id from platform.identity where lower(email) = lower($1) and deleted_at is null',
    [persona.email],
  )
  expect(identityIdCollegata).toBe(identityId)

  // ── 3. la persona entra nel proprio account e trova l'invito nel cruscotto ──
  await loginIntoAccount(page, persona.email, persona.password, persona.displayName)
  const sidebar = page.getByRole('navigation', { name: 'Platform' })
  // Con una sola appartenenza il selettore non esiste ancora (UC 0117): è lo stato di partenza.
  await expect(page.getByLabel('Switch account')).toHaveCount(0)
  // Il numero sulla voce del menu: da un'altra schermata l'invito resterebbe invisibile.
  await expect(sidebar.getByLabel('1 invitation waiting')).toBeVisible()
  await expect(
    page.getByText(`${azienda.displayName} invites you to work in their account.`),
  ).toBeVisible()

  // ── 4. accetta: nasce l'appartenenza e l'account nuovo diventa quello attivo ──
  const reloaded = page.waitForEvent('load')
  // La decisione sul gate legale si attende registrandola PRIMA del clic. Senza questa attesa il
  // journey guarda la shell nella finestra di fail-open, la trova, prosegue — e un istante dopo il
  // gate prende il posto della shell, facendo cadere l'asserzione sul selettore su una pagina che
  // mostra i documenti da accettare. È la stessa cura che `switchAccountTo` si fa in casa.
  const legalSettled = waitForLegalDecision(page)
  await page.getByRole('button', { name: 'Accept' }).click()
  await reloaded
  await legalSettled
  // Primo ingresso nell'account dell'azienda: il gate legale è pendente, perché l'accettazione dei
  // documenti è per ACCOUNT e non per persona (UC 0056). Non si usa `switchAccountTo`: non c'è nessun
  // selettore da aprire — il cambio di account l'ha fatto l'accettazione stessa.
  await expectInsideAccount(page, azienda.displayName)

  // Il selettore c'è perché le appartenenze sono due (UC 0117). Va verificato PRIMA dell'assenza di
  // «Members»: un'attesa su un elemento che deve ESSERCI dice anche che la barra laterale è popolata,
  // mentre `toHaveCount(0)` è vero anche su una barra ancora vuota — passerebbe per il motivo
  // sbagliato, e coprirebbe proprio il difetto che stiamo escludendo.
  await expect(page.getByLabel('Switch account')).toBeVisible()
  // Collaboratrice, non owner: la gestione delle persone non le appartiene, e la voce non c'è.
  await expect(sidebar.getByRole('link', { name: 'Members' })).toHaveCount(0)

  // Una sola identità, due appartenenze: è il cuore della storia.
  const [identita] = dbRow(
    'select count(*) from platform.identity where lower(email) = lower($1) and deleted_at is null',
    [persona.email],
  )
  expect(Number(identita)).toBe(1)
  const appartenenze = dbRow(
    `select count(*) from platform.membership where identity_id = $1 and deleted_at is null`,
    [identityId],
  )
  expect(Number(appartenenze[0])).toBe(2)
  const [statoInvito] = dbRow('select status from platform.invitations where id = $1', [
    String(created.id),
  ])
  expect(statoInvito).toBe('accepted')

  // ── 5. e da qui si passa fra i due account, come una persona sola con due lavori ──
  await switchAccountTo(page, persona.displayName)
  await expect(sidebar.getByRole('link', { name: 'Members' })).toBeVisible()
})
