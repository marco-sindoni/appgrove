import { test, expect } from '@playwright/test'
import { tenant } from '../helpers/api'
import { loginIntoAccount, switchAccountTo } from '../helpers/browser'
import { dbRow, dbExec } from '../helpers/db'

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
  await loginIntoAccount(page, persona.email, persona.password, persona.displayName)
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
