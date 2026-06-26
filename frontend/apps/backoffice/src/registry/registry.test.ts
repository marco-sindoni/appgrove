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
})
