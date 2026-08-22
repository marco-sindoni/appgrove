import { describe, expect, it } from 'vitest'
import { APP_ROLES, appRoleAtLeast, type AppRole } from './app-role'

// L'ordinamento dei ruoli è la cosa che tutto il resto assume e nessun altro posto ricontrolla: un
// `editor` che passasse per un comando da `admin` sarebbe un difetto silenzioso e generalizzato a ogni
// schermata. Gemello del collaudo Java `AppRoleTest` (UC 0099/0101).
describe('appRoleAtLeast', () => {
  it("l'ordine è viewer, editor, admin", () => {
    expect(APP_ROLES).toEqual(['viewer', 'editor', 'admin'])
  })

  it('ogni ruolo basta per sé stesso', () => {
    for (const role of APP_ROLES) {
      expect(appRoleAtLeast(role, role)).toBe(true)
    }
  })

  it('un ruolo più capace basta per una richiesta più bassa', () => {
    expect(appRoleAtLeast('editor', 'viewer')).toBe(true)
    expect(appRoleAtLeast('admin', 'viewer')).toBe(true)
    expect(appRoleAtLeast('admin', 'editor')).toBe(true)
  })

  it('un ruolo meno capace non basta mai per una richiesta più alta', () => {
    expect(appRoleAtLeast('viewer', 'editor')).toBe(false)
    expect(appRoleAtLeast('viewer', 'admin')).toBe(false)
    expect(appRoleAtLeast('editor', 'admin')).toBe(false)
  })

  // L'owner sta sopra tutti perché la fonte di verità gli attribuisce `admin`, non perché esista un
  // quarto valore: il confronto non deve conoscere il concetto di owner.
  it("l'owner dell'account è rappresentato come admin e passa ogni confronto", () => {
    const owner: AppRole = 'admin'
    for (const required of APP_ROLES) {
      expect(appRoleAtLeast(owner, required)).toBe(true)
    }
  })

  it('un ruolo assente, vuoto o ignoto non è mai un permesso', () => {
    expect(appRoleAtLeast(null, 'viewer')).toBe(false)
    expect(appRoleAtLeast(undefined, 'viewer')).toBe(false)
    expect(appRoleAtLeast('', 'viewer')).toBe(false)
    // `owner` è un ruolo di PIATTAFORMA: non è un ruolo di applicazione e non deve diventarlo per
    // somiglianza del nome.
    expect(appRoleAtLeast('owner', 'viewer')).toBe(false)
    expect(appRoleAtLeast('ADMIN', 'viewer')).toBe(false)
    expect(appRoleAtLeast('member', 'viewer')).toBe(false)
  })
})
