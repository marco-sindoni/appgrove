import { describe, expect, it } from 'vitest'
import { LOGO_PATHS, LOGO_VIEWBOX, logoMarkSvg, logoLockupSvg } from './logo.mjs'

describe('logo condiviso (UC 0086)', () => {
  it('il mark è un SVG autoconsistente con i colori passati dal chiamante', () => {
    const svg = logoMarkSvg({ size: 48, accent: '#ec5a72', contrast: '#ffffff' })
    expect(svg.startsWith('<svg')).toBe(true)
    expect(svg.endsWith('</svg>')).toBe(true)
    expect(svg).toContain('xmlns="http://www.w3.org/2000/svg"')
    expect(svg).toContain('width="48" height="48"')
    expect(svg).toContain(`viewBox="${LOGO_VIEWBOX}"`)
    expect(svg).toContain('#ec5a72')
    expect(svg).toContain('#ffffff')
  })

  it('disegna tutti i tracciati definiti, nessuno escluso', () => {
    const svg = logoMarkSvg()
    for (const path of LOGO_PATHS) expect(svg).toContain(path.d)
  })

  it('la variante scura cambia i colori senza cambiare il disegno', () => {
    const chiaro = logoMarkSvg({ accent: '#ec5a72', contrast: '#ffffff' })
    const scuro = logoMarkSvg({ accent: '#ec5a72', contrast: '#161512' })
    expect(scuro).not.toBe(chiaro)
    for (const path of LOGO_PATHS) expect(scuro).toContain(path.d)
  })

  it('il logo completo affianca il wordmark al mark ed è più largo che alto', () => {
    const svg = logoLockupSvg({ height: 40, text: '#262420' })
    expect(svg).toContain('>appgrove</text>')
    expect(svg).toContain('#262420')
    const [, width, height] = svg.match(/width="([\d.]+)" height="([\d.]+)"/) ?? []
    expect(Number(width)).toBeGreaterThan(Number(height))
    expect(Number(height)).toBe(40)
  })

  it('non contiene colori cablati oltre ai valori di ripiego dichiarati', () => {
    // I colori arrivano SEMPRE dal chiamante: passandone di arbitrari, nell'SVG non deve
    // comparire nessun altro esadecimale (nessun colore dimenticato nel disegno).
    const svg = logoMarkSvg({ accent: '#123456', contrast: '#654321' })
    const trovati = new Set(svg.match(/#[0-9a-f]{6}/gi) ?? [])
    expect([...trovati].sort()).toEqual(['#123456', '#654321'])
  })
})
