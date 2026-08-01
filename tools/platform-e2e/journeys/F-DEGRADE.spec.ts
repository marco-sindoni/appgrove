import { test, expect } from '@playwright/test'
import { tenant } from '../helpers/api'
import { browserLogin } from '../helpers/browser'
import { dbExec, dbRow } from '../helpers/db'
import { CORE_SERVICE, ensureServiceUp, isServiceUp, startService, stopService } from '../helpers/services'

/**
 * F-DEGRADE — guasti di piattaforma visti dall'utente (UC 0092, su UC 0077).
 *
 * Qui il guasto è **vero**: il processo che risponde per i diritti d'accesso viene fermato, non se
 * ne simula la risposta. È l'unico modo di collaudare la regola più delicata della piattaforma —
 * *un guasto di lettura non deve mai presentarsi come mancanza di diritto* — perché è proprio
 * nell'insieme di stati reali che le due cose si confondono: la lettura fallisce, l'elenco dei
 * diritti resta vuoto, e un cliente pagante si sente dire che non ha comprato nulla.
 *
 * Menu laterale in errore con "riprova" (mai «nessuna app») → rotta del modulo in **errore**, mai
 * accesso negato → servizio riavviato → "riprova" → shell di nuovo normale **senza ricaricare la
 * pagina** → variante sessione: con la sessione invalidata dal lato server si esce puliti verso
 * l'accesso, una volta sola e senza restare appesi.
 *
 * Journey SERIALE (progetto `degrade-serial`): ferma un servizio condiviso da tutta la suite.
 *
 * **L'app osservata è `fatture`, non il Mini-CRM**, e la ragione non è casuale: riavviare il core
 * rilancia la sincronizzazione del listino, che riallinea lo stato delle app allo YAML — e nello
 * YAML il Mini-CRM è disabilitato di proposito (change 0042), quindi dopo il riavvio sparirebbe per
 * ragioni che con il guasto non c'entrano nulla. `fatture` è attiva nel listino e ha una fascia
 * gratuita di base: nessun acquisto serve, e il riavvio non ne cambia lo stato.
 */

const APPS_ERROR = 'We couldn’t load your apps'
const APP_ID = 'fatture'

test.afterEach(() => {
  // Ripristino incondizionato: la batteria non può finire con un servizio giù, nemmeno quando il
  // journey è morto a metà. La rete di sicurezza successiva è nel global-teardown.
  const problem = ensureServiceUp(CORE_SERVICE)
  if (problem) throw new Error(problem)
})

test('F-DEGRADE: servizio fermato davvero → errore con riprova, mai diniego → riavvio → rientro senza ricaricare', async ({
  page,
}) => {
  // ── 0. cliente autenticato con l'app funzionante ────────────────────────────
  const t = await tenant('fdegrade')
  await browserLogin(page, t.email, t.password)
  const nav = page.getByRole('navigation', { name: 'Platform' })
  await expect(nav.locator(`a[href="/app/${APP_ID}"]`)).toBeVisible()
  await page.goto(`/app/${APP_ID}`)
  await expect(page.getByRole('heading', { name: 'Invoices', level: 1 })).toBeVisible()

  // ── 1. il servizio dei diritti d'accesso viene FERMATO ──────────────────────
  stopService(CORE_SERVICE)
  expect(isServiceUp(CORE_SERVICE)).toBeFalsy()

  // ── 2. la shell ricaricata col servizio giù, guardata da due punti ──────────
  // Una sola navigazione, sulla rotta del modulo: si osservano insieme il menu laterale e
  // l'area contenuti, che sono i due posti dove il guasto rischia di travestirsi da diniego.
  await page.goto(`/app/${APP_ID}`)

  // Menu laterale: errore con "riprova", mai «nessuna app». Il margine di attesa è largo perché
  // la lettura fallisce e viene ritentata una volta prima di dichiararsi in errore.
  await expect(nav.getByRole('alert')).toContainText(APPS_ERROR, { timeout: 30_000 })
  await expect(nav.getByRole('button', { name: 'Retry' })).toBeVisible()
  // La bugia da cui nasce la regola: un guasto raccontato come assenza di diritti.
  await expect(nav.getByText('No active apps yet')).toHaveCount(0)

  // Area contenuti: ERRORE, non accesso negato — e la rotta resta quella chiesta.
  await expect(page.getByRole('main').getByRole('alert')).toContainText(APPS_ERROR)
  await expect(page).toHaveURL(new RegExp(`/app/${APP_ID}$`))
  await expect(page).not.toHaveURL(/\/forbidden/)

  // ── 3. servizio riavviato → "riprova" → shell normale SENZA ricaricare ──────
  startService(CORE_SERVICE)
  expect(isServiceUp(CORE_SERVICE)).toBeTruthy()
  await nav.getByRole('button', { name: 'Retry' }).click()
  // Stessa pagina, nessun ricaricamento: il modulo si monta al posto dell'errore.
  await expect(nav.locator(`a[href="/app/${APP_ID}"]`)).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Invoices', level: 1 })).toBeVisible()
  await expect(page).toHaveURL(new RegExp(`/app/${APP_ID}$`))

  // ── 4. variante sessione: sessione invalidata dal lato server ───────────────
  // Leva d'ambiente (secondo uso sanzionato di dbExec, vedi helpers/db.ts): non esiste un'azione
  // utente né un endpoint di sviluppo che invalidi una sessione, e le vie di prodotto equivalenti
  // appartengono ad altri journey. Agisce solo sull'utente usa-e-getta di QUESTO journey.
  dbExec(`update platform.users set status = 'suspended' where cognito_sub = $1`, [t.userSub])
  expect(dbRow(`select status from platform.users where cognito_sub = $1`, [t.userSub])[0]).toBe('suspended')

  const navigazioni: string[] = []
  page.on('framenavigated', (frame) => {
    if (frame === page.mainFrame()) navigazioni.push(new URL(frame.url()).pathname)
  })
  await page.goto('/')
  await expect(page.getByRole('button', { name: 'Sign in' })).toBeVisible()
  // Uscita PULITA: nessuno stato appeso al ripristino sessione…
  await expect(page.getByText('Restoring your session…')).toHaveCount(0)
  // …e nessun rimbalzo: si arriva all'accesso una volta sola, non in ciclo.
  expect(navigazioni.filter((p) => p === '/login')).toHaveLength(1)
})
