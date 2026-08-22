import { describe, it, expect } from 'vitest'
import { buildRoster } from './roster'

const owner = {
  id: 'u-owner',
  email: 'owner@acme.test',
  displayName: 'Owner',
  role: 'owner',
  status: 'active',
  joinedAt: '2024-01-01T00:00:00Z',
  apps: [{ appId: 'a1', app: 'crm', implicit: true }],
}
const zoe = {
  id: 'u-zoe',
  email: 'zoe@acme.test',
  displayName: 'Zoe',
  role: 'member',
  status: 'active',
  joinedAt: '2025-05-05T00:00:00Z',
  apps: [{ appId: 'a1', app: 'crm', role: 'admin', implicit: false }],
}
const ada = {
  id: 'u-ada',
  email: 'ada@acme.test',
  displayName: 'Ada',
  role: 'member',
  status: 'suspended',
  joinedAt: '2025-02-02T00:00:00Z',
}
const invito = {
  id: 'inv-1',
  email: 'nuova@acme.test',
  status: 'pending',
  expiresAt: '2026-09-01T00:00:00Z',
}

describe('buildRoster (UC 0100)', () => {
  it('fonde persone e inviti in un solo elenco, con l’owner in testa', () => {
    const rows = buildRoster({ members: [zoe, ada, owner], invitations: [invito] })

    expect(rows.map((r) => r.email)).toEqual([
      'owner@acme.test',
      'ada@acme.test',
      'zoe@acme.test',
      'nuova@acme.test',
    ])
    expect(rows.map((r) => r.kind)).toEqual(['person', 'person', 'person', 'invitation'])
  })

  it('traduce gli stati nei tre valori che il modello sa produrre', () => {
    const rows = buildRoster({ members: [owner, ada], invitations: [invito] })
    expect(rows.find((r) => r.email === 'owner@acme.test')?.status).toBe('active')
    expect(rows.find((r) => r.email === 'ada@acme.test')?.status).toBe('suspended')
    expect(rows.find((r) => r.email === 'nuova@acme.test')?.status).toBe('invited')
  })

  it('porta le applicazioni di ciascuno, e l’elenco vuoto per chi non ne ha', () => {
    const rows = buildRoster({ members: [owner, zoe, ada] })
    expect(rows.find((r) => r.email === 'zoe@acme.test')?.apps).toEqual([
      { appId: 'a1', app: 'crm', role: 'admin', implicit: false },
    ])
    // Chi non è abilitato a nulla: elenco vuoto, non `undefined` — la schermata non deve difendersi.
    expect(rows.find((r) => r.email === 'ada@acme.test')?.apps).toEqual([])
    // Un invito non ha applicazioni: non è ancora entrato.
    expect(buildRoster({ invitations: [invito] })[0].apps).toEqual([])
  })

  it('un invito non ha data di ingresso ma ha una scadenza', () => {
    const riga = buildRoster({ invitations: [invito] })[0]
    expect(riga.joinedAt).toBeUndefined()
    expect(riga.expiresAt).toBe('2026-09-01T00:00:00Z')
  })

  /**
   * Le righe intoccabili: la propria e quella dell'**ultimo** owner. Il rifiuto vero arriva dal
   * servizio (409); questa è la cortesia che evita di provarci.
   */
  it('blocca la propria riga e quella dell’ultimo owner', () => {
    const rows = buildRoster({ members: [owner, zoe], meId: 'u-zoe' })
    expect(rows.find((r) => r.email === 'owner@acme.test')?.locked).toBe(true)
    expect(rows.find((r) => r.email === 'zoe@acme.test')?.locked).toBe(true)
    expect(rows.find((r) => r.email === 'zoe@acme.test')?.isSelf).toBe(true)
  })

  it('con due owner nessuno dei due è l’ultimo, e le azioni si riaprono', () => {
    const secondo = { ...zoe, role: 'owner' }
    const rows = buildRoster({ members: [owner, secondo] })
    expect(rows.every((r) => r.locked)).toBe(false)
    expect(rows.map((r) => r.isOwner)).toEqual([true, true])
  })

  it('non esplode senza dati (primo caricamento)', () => {
    expect(buildRoster({})).toEqual([])
  })
})
