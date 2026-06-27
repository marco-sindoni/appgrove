import { describe, it, expect } from 'vitest'
import { intersectModules, MODULES, findModule } from './registry'

describe('App Registry — intersezione moduli ∩ entitlement', () => {
  it('mostra solo i moduli a cui il tenant è entitled', () => {
    const visible = intersectModules(MODULES, ['demo'])
    expect(visible.map((m) => m.id)).toEqual(['demo'])
  })

  it('nasconde i moduli senza entitlement', () => {
    expect(intersectModules(MODULES, [])).toEqual([])
    expect(intersectModules(MODULES, ['altra-app'])).toEqual([])
  })

  it('findModule risolve per app_id', () => {
    expect(findModule('demo')?.name).toBe('Demo app')
    expect(findModule('inesistente')).toBeUndefined()
  })

  it('il modulo fatture (UC 0052) è registrato e visibile solo se entitled', () => {
    expect(findModule('fatture')?.id).toBe('fatture')
    // regression guard: senza entitlement "fatture" l'app non compare
    expect(intersectModules(MODULES, ['demo']).map((m) => m.id)).not.toContain('fatture')
    expect(intersectModules(MODULES, ['fatture']).map((m) => m.id)).toEqual(['fatture'])
  })
})
