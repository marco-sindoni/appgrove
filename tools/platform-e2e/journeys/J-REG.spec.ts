import { test, expect } from '@playwright/test'
import { waitForEmail, extractLink } from '../helpers/mailbox'
import { dbRow, dbRows } from '../helpers/db'
import { tenant, login, uniqueEmail, DEFAULT_PASSWORD, jwtPayload } from '../helpers/api'

const CORE_API = process.env.PLATFORM_CORE_API ?? 'http://localhost:20080'

/**
 * J-REG — journey "registrazione" (UC 0090, primo journey della suite di piattaforma).
 *
 * Prova l'INTERA catena su stack vero: signup dal browser → email di verifica REALMENTE
 * spedita (Mailpit) e cliccata → onboarding workspace → dashboard con sidebar "senza app
 * attive" → verifica a livello DB (leak detector, #10 dec. 13): account/utente creati col
 * tenant_id atteso, nessun dato fuori dal tenant.
 *
 * Nessuna rotta intercettata: il backend è quello vero, in profilo dev, sulle porte della
 * suite. Ogni test crea da zero il proprio tenant (email sintetica unica per run).
 */

test('[J-REG] signup → email verificata davvero → onboarding → dashboard → DB coerente', async ({
  page,
  baseURL,
}) => {
  const email = uniqueEmail('jreg')
  const workspaceName = `E2E Workspace ${Date.now().toString(36)}`

  // ── 1. wizard di registrazione (browser, backend vero) ──────────────────────
  await page.goto('/signup')
  await page.getByLabel('Your name', { exact: false }).fill('E2E Owner')
  // exact: il testo del consenso newsletter contiene "email" e colliderebbe col match per sottostringa.
  await page.getByLabel('Email', { exact: true }).fill(email)
  await page.getByLabel('Password', { exact: true }).fill(DEFAULT_PASSWORD)
  await page.getByRole('button', { name: 'Create account' }).click()
  await expect(page.getByText(/We sent a verification link/)).toBeVisible()

  // ── 2. email di verifica REALE in Mailpit: mittente, lingua, link ───────────
  const message = await waitForEmail(email)
  expect(message.From.Address).toBe('noreply@appgrove.app')
  // La UI gira in inglese (locale del browser di test): l'oggetto atteso è quello EN (UC 0018).
  expect(message.Subject).toBe('Confirm your email address')
  const link = extractLink(message, '/verify')
  // Il link punta al server SPA della suite (auth.app-base-url iniettato da run.sh).
  expect(link.startsWith(baseURL!)).toBeTruthy()

  // ── 3. click del link estratto dall'email → verifica + auto-login ───────────
  await page.goto(link)
  await expect(page).toHaveURL(/\/signup\?step=workspace/)

  // ── 4. onboarding workspace → done → dashboard ──────────────────────────────
  // Il campo è PREFILLATO dal server (accounts/me → nome dell'account = display name del
  // signup): attendere il prefill prima di scrivere, o il valore digitato verrebbe
  // sovrascritto quando la query si risolve (attesa su condizione, non sleep).
  await expect(page.getByLabel('Workspace name')).toHaveValue('E2E Owner')
  await page.getByLabel('Workspace name').fill(workspaceName)
  await page.getByRole('button', { name: 'Continue' }).click()
  await page.getByRole('button', { name: 'Go to dashboard' }).click()

  // Primo accesso al workspace: gate BLOCCANTE di accettazione dei legali correnti
  // (UC 0056) — un tenant nuovo non ha accettazioni registrate, quindi il modale è parte
  // integrante del percorso di registrazione reale. Si attraversa come farebbe l'utente.
  const legalGate = page.getByRole('dialog', { name: 'Updated legal documents' })
  await expect(legalGate).toBeVisible()
  // Un consenso per OGNI documento corrente (ToS/PP/cookie/…): si spuntano tutti, come l'utente.
  for (const box of await legalGate.getByRole('checkbox').all()) await box.check()
  await legalGate.getByRole('button', { name: 'Continue' }).click()
  await expect(legalGate).not.toBeVisible()

  await expect(page.getByText('Platform')).toBeVisible()

  // ── 4-bis. la Dashboard è una PANORAMICA OPERATIVA (UC 0097) ────────────────
  // Fino alla change 0078 questa pagina mostrava soltanto il codice tecnico del workspace. Ora
  // saluta, nomina il workspace e riassume ciò che c'è dentro: si asserta su stack vero, perché è
  // la prima cosa che un utente registrato vede.
  await expect(page.getByRole('heading', { name: /^Welcome back/, level: 1 })).toBeVisible()
  await expect(
    page.getByText(`Here’s what’s happening in the ${workspaceName} workspace.`),
  ).toBeVisible()
  const glance = page.getByRole('complementary', { name: 'Workspace at a glance' })
  await expect(glance.getByText('Members')).toBeVisible()
  await expect(glance.getByText('Pending invites')).toBeVisible()
  await expect(glance.getByText('Active apps')).toBeVisible()
  await expect(glance.getByRole('button', { name: 'Browse the catalog' })).toBeVisible()

  // Sidebar "Your apps": per un tenant NUOVO gli entitlement sono la sola baseline
  // freemium (UC 0027: le app con un tier senza prezzo — es. `fatture` dal catalogo seed).
  // Non si cabla il catalogo nel test: si chiede al core la verità per QUESTO tenant
  // (provider reale, UC 0077) e si asserta che il browser mostri esattamente quella —
  // link presente per ogni app entitled del registry, assente per le altre, stato
  // "senza app attive" solo se l'intersezione è vuota.
  const tokens = await login(email, DEFAULT_PASSWORD)
  const res = await fetch(`${CORE_API}/api/platform/v1/me/entitlements`, {
    headers: { authorization: `Bearer ${tokens.access_token}` },
  })
  expect(res.status).toBe(200)
  const { entitlements } = (await res.json()) as { entitlements: Array<{ appSlug?: string }> }
  const entitledSlugs = entitlements.map((e) => e.appSlug).filter((s): s is string => !!s)
  const nav = page.getByRole('navigation', { name: 'Platform' })
  // Moduli app del registry build-time delle SPA (frontend/apps/backoffice/src/registry).
  const registryModules = ['fatture', 'crm']
  for (const slug of registryModules) {
    const link = nav.locator(`a[href="/app/${slug}"]`)
    if (entitledSlugs.includes(slug)) {
      await expect(link).toBeVisible()
    } else {
      await expect(link).toHaveCount(0)
    }
  }
  if (!registryModules.some((s) => entitledSlugs.includes(s))) {
    await expect(page.getByText('No active apps yet')).toBeVisible()
    await expect(page.getByRole('link', { name: 'Browse the apps' })).toBeVisible()
  }

  // ── 5. leak detector a livello DB (#10 dec. 13) ─────────────────────────────
  // L'utente esiste, è owner attivo, e il suo tenant è l'account creato dal signup
  // (rinominato dall'onboarding): tenant_id coerente end-to-end.
  const [tenantId, role, status, accountName] = dbRow(
    `select m.tenant_id, m.role, m.status, a.name
       from platform.membership m
       join platform.identity i on i.id = m.identity_id
       join platform.accounts a on a.id::text = m.tenant_id
      where lower(i.email) = lower($1) and m.deleted_at is null and i.deleted_at is null`,
    [email],
  )
  expect(role).toBe('owner')
  expect(status).toBe('active')
  expect(accountName).toBe(workspaceName)
  // Nessuna fuga fuori dal tenant: dopo UC 0116 l'indirizzo vive in UNA sola IDENTITÀ (mai
  // duplicata) e chi si è appena registrato ha UNA sola appartenenza — quella al conto che ha
  // creato lui. Il conto appena nato contiene SOLO il suo owner.
  expect(dbRows(`select id from platform.identity where lower(email) = lower($1)`, [email])).toHaveLength(1)
  expect(
    dbRows(
      `select m.tenant_id from platform.membership m
         join platform.identity i on i.id = m.identity_id
        where lower(i.email) = lower($1) and m.deleted_at is null`,
      [email],
    ),
  ).toHaveLength(1)
  expect(dbRow(`select count(*) from platform.membership where tenant_id = $1`, [tenantId])[0]).toBe('1')

  // ── 6. l'identificativo del workspace è in Account, non in Dashboard (UC 0097) ──
  // Confronto con il valore VERO letto dal database: prova che il codice mostrato è quello del
  // tenant e che la Dashboard non lo espone più.
  await expect(page.getByText(tenantId)).toHaveCount(0)
  await page.getByRole('link', { name: 'Account' }).click()
  const content = page.getByRole('main')
  await expect(content.getByRole('heading', { name: 'Workspace' })).toBeVisible()
  await expect(content.getByText(tenantId)).toBeVisible()
  await expect(content.getByRole('button', { name: 'Copy workspace ID' })).toBeVisible()
})

test('[J-REG-API] tenant() programmatico — helper pronto per gli UC 0091/0092', async () => {
  // Collauda l'helper condiviso: stesso flusso (signup → email vera → verify) via API, senza
  // browser. È la precondizione "tenant fresco" dei journey futuri: qui ne dimostriamo la
  // coerenza col DB e col claim tenant_id del JWT emesso (mai fornito dal client).
  const t = await tenant('helper')
  expect(String(jwtPayload(t.tokens.access_token).tenant_id)).toBe(t.tenantId)
  const [dbTenant, role] = dbRow(
    `select m.tenant_id, m.role from platform.membership m
       join platform.identity i on i.id = m.identity_id
      where lower(i.email) = lower($1) and m.deleted_at is null and i.deleted_at is null`,
    [t.email],
  )
  expect(dbTenant).toBe(t.tenantId)
  expect(role).toBe('owner')
  // Isolamento: il tenant fresco contiene solo il proprio owner.
  expect(dbRow(`select count(*) from platform.membership where tenant_id = $1`, [t.tenantId])[0]).toBe('1')
})
