import { test, expect } from '@playwright/test'
import { tenant, authedFetch } from '../helpers/api'
import { browserLogin } from '../helpers/browser'
import { dbRow } from '../helpers/db'

const BACKOFFICE_URL = process.env.PLATFORM_BACKOFFICE_URL ?? 'http://localhost:24173'

/**
 * J-@@APP_UPPER@@ — core-loop dell'app @@APP_NAME@@ sulla suite di piattaforma (UC 0090/0094).
 *
 * Generato da `tools/new-application` insieme all'app: ogni app nasce col suo percorso end-to-end e
 * con la sua voce nel registro di copertura (docs/testing/copertura-e2e.yaml), invece di aspettare
 * che qualcuno se ne ricordi. È il gemello, sul dominio segnaposto, del journey J-QUOTA dell'app #1
 * `fatture`: stack vero (Postgres + servizi), browser vero, quota applicata davvero dal servizio.
 *
 * Che cosa dimostra, in ordine:
 *   1. il tenant nuovo ha il diritto d'uso di baseline e il modulo si monta nel backoffice;
 *   2. si arriva al tetto del piano gratuito (@@QUOTA_CAP_NOTE@@) e il banner consumo lo mostra;
 *   3. oltre il tetto il SERVIZIO rifiuta davvero (429), non l'interfaccia;
 *   4. il database contiene esattamente i record creati, e solo quelli di questo tenant.
 *
 * ── Due scelte deliberate, da conoscere prima di modificarlo ────────────────────────────────────
 *
 * **Non dipende dai testi dell'interfaccia.** Le asserzioni a schermo usano l'identificativo di
 * prova del modulo, il ruolo del banner e il codice del record (dato, non traduzione). Il primo
 * lavoro dopo lo scaffolding è riscrivere il dominio e le stringhe dell'app: un journey agganciato
 * a «New record» diventerebbe rosso il giorno dopo, per un motivo che non c'entra col prodotto.
 *
 * **Si salta finché l'app non è vendibile.** Il listino generato nasce `status: inactive` (i livelli
 * a pagamento li decide una persona), e senza app `active` non esiste alcun diritto d'uso: il
 * percorso non sarebbe rosso per un difetto, sarebbe rosso per una precondizione mancante. Perciò si
 * salta con un motivo esplicito e **si accende da solo** — nessun intervento, nessun promemoria —
 * quando il listino passa ad `active`.
 *
 * Da estendere man mano che l'app cresce: è uno scheletro onesto, non il collaudo finale.
 */

test('[J-@@APP_UPPER@@] core-loop @@APP_ID@@ — modulo montato, tetto del piano raggiunto, rifiuto reale oltre il tetto, DB coerente', async ({
  page,
}) => {
  const t = await tenant('@@APP_ID@@')

  // ── precondizione: l'app dev'essere concessa al tenant (listino `active` + tier gratuito) ──
  const entitlements = await authedFetch(BACKOFFICE_URL, '/api/platform/v1/me/entitlements', t.tokens)
  expect(entitlements.status).toBe(200)
  const { entitlements: granted } = (await entitlements.json()) as { entitlements: Array<{ appSlug: string }> }
  test.skip(
    !granted.some((e) => e.appSlug === '@@APP_ID@@'),
    "l'app @@APP_ID@@ non è ancora concessa: il listino services/core/src/main/resources/pricing/@@APP_ID@@.yaml "
      + 'è `status: inactive` (bozza dello scaffolding). Il percorso si accende da solo quando passerà ad `active`.',
  )

  // ── 1. fino al tetto del piano gratuito, dalle stesse rotte che usa il modulo ──
  const codici: string[] = []
  for (let i = 1; i <= @@FREE_CAP@@; i += 1) {
    const res = await authedFetch(BACKOFFICE_URL, '/api/@@APP_ID@@/v1/items', t.tokens, {
      body: { contactName: `Contatto ${i}` },
    })
    if (res.status !== 201) {
      throw new Error(`creazione record ${i} via API: HTTP ${res.status} — ${await res.text()}`)
    }
    codici.push(((await res.json()) as { code: string }).code)
  }

  // ── 2. dal browser: modulo montato, elenco popolato, banner consumo al tetto ──
  await browserLogin(page, t.email, t.password)
  await page.goto('/app/@@APP_ID@@')
  await expect(page.getByTestId('@@APP_ID@@-module')).toBeVisible()
  await expect(page.getByText(codici[0], { exact: true })).toBeVisible()
  await expect(page.getByRole('status').getByText('@@FREE_CAP@@ / @@FREE_CAP@@')).toBeVisible()

  // ── 3. oltre il tetto: il rifiuto è del SERVIZIO (429), non dell'interfaccia ──
  const oltre = await authedFetch(BACKOFFICE_URL, '/api/@@APP_ID@@/v1/items', t.tokens, {
    body: { contactName: 'Contatto oltre il tetto' },
  })
  expect(oltre.status).toBe(429)

  // ── 4. assert DB: i record esistono davvero, e sono solo quelli di questo tenant ──
  expect(
    dbRow('select count(*) from @@SCHEMA@@.item where tenant_id = $1 and deleted_at is null', [t.tenantId])[0],
  ).toBe('@@FREE_CAP@@')
})
