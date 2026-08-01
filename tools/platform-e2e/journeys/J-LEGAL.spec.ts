import { test, expect } from '@playwright/test'
import { tenant } from '../helpers/api'
import { browserLogin } from '../helpers/browser'
import { dbRow, dbExec } from '../helpers/db'

/**
 * J-LEGAL — ri-accettazione legale a runtime (UC 0091 / UC 0056).
 *
 * Un utente in regola (ha già accettato i documenti correnti al primo ingresso) subisce la
 * PUBBLICAZIONE di una nuova versione major dei Termini: la leva è l'upsert su
 * platform.legal_version (dbExec — simula l'atto di deploy della CI, non un'azione utente;
 * decisione 8, change 0070). Al rientro la schermata è bloccante, la pagina "I miei dati"
 * resta raggiungibile (esenzione GDPR), la lettura+spunta+accettazione sblocca l'ingresso e
 * l'accettazione è registrata su DB (chi, cosa, quando).
 *
 * Questo journey gira nel progetto `legal-serial`, DOPO tutti gli altri: la versione legale è
 * globale a tutti i tenant. A fine journey la riga viene ripristinata (e in ogni caso la sync
 * legale allo startup dei servizi riallinea le versioni a ogni esecuzione della suite).
 */

test('[J-LEGAL] nuova major dei Termini → gate bloccante → esenzione "I miei dati" → accettazione registrata', async ({
  page,
}) => {
  const t = await tenant('jlegal')
  // Primo ingresso: attraversa il gate dei documenti correnti (l'utente è "in regola").
  await browserLogin(page, t.email, t.password)
  const gate = page.getByRole('dialog', { name: 'Updated legal documents' })
  await expect(gate).not.toBeVisible()

  const [origMajor, origVersion, origDate] = dbRow(
    `select major, version, effective_date from platform.legal_version where component = 'terms'`,
  )
  const bumpedMajor = String(Number(origMajor) + 1)
  const bumpedVersion = `${bumpedMajor}.0.0`
  try {
    // ── 1. leva d'ambiente: si pubblica una nuova versione MAJOR dei Termini ──
    dbExec(
      `update platform.legal_version
          set major = $1, version = $2, effective_date = current_date, updated_at = now(), updated_by = 'e2e'
        where component = 'terms'`,
      [bumpedMajor, bumpedVersion],
    )

    // Al rientro (rimontaggio della shell) il gate è BLOCCANTE, con il solo componente bumpato.
    await page.reload()
    await expect(gate).toBeVisible()
    await expect(gate.getByText(`Version ${bumpedVersion}`)).toBeVisible()
    await expect(gate.getByRole('checkbox')).toHaveCount(1)
    await expect(gate.getByRole('button', { name: 'Continue' })).toBeDisabled()
    // L'app dietro il gate non è raggiungibile.
    await expect(page.getByRole('navigation', { name: 'Platform' }).getByText('Your apps')).toHaveCount(0)

    // La pagina "I miei dati" resta raggiungibile (esenzione dei diritti GDPR dal blocco).
    await page.goto('/privacy')
    await expect(gate).not.toBeVisible()
    await expect(page.getByRole('heading', { name: 'My data', level: 1 })).toBeVisible()
    await page.goto('/')
    await expect(gate).toBeVisible()

    // ── 2. lettura del documento → spunta → accettazione → ingresso ───────────
    await gate.getByRole('button', { name: 'Read the document' }).click()
    await gate.getByRole('checkbox', { name: 'I accept' }).check()
    await gate.getByRole('button', { name: 'Continue' }).click()
    await expect(gate).not.toBeVisible()
    await expect(page.getByRole('navigation', { name: 'Platform' })).toBeVisible()

    // Assert DB: chi (tenant+utente dal JWT), cosa (componente+versione major), quando.
    const [component, version, major, actType, acceptedAt] = dbRow(
      `select component, version, major, act_type, accepted_at
         from platform.legal_acceptance
        where tenant_id = $1 and user_id = $2 and component = 'terms' and version = $3`,
      [t.tenantId, t.userSub, bumpedVersion],
    )
    expect(component).toBe('terms')
    expect(version).toBe(bumpedVersion)
    expect(major).toBe(bumpedMajor)
    expect(actType).toBe('accept')
    expect(acceptedAt.length).toBeGreaterThan(0)
  } finally {
    // Ripristino della versione pubblicata (la sync allo startup riallinea comunque ogni run).
    dbExec(
      `update platform.legal_version
          set major = $1, version = $2, effective_date = $3, updated_at = now(), updated_by = 'sync-legal'
        where component = 'terms'`,
      [origMajor, origVersion, origDate],
    )
  }
})
