import { describe, expect, it } from 'vitest'
import {
  LOGO_COMPACT_BELOW,
  LOGO_PATHS,
  LOGO_VIEWBOX,
  logoLockupSvg,
  logoMarkSvg,
  logoPathsFor,
} from './logo.mjs'

describe('logo condiviso (UC 0086/0087)', () => {
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
    const svg = logoMarkSvg({ size: 32 })
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

  // ── Artwork definitivo (UC 0087) ───────────────────────────────────────────
  it('negli spazi piccoli cade il dettaglio fine, e nient altro', () => {
    const sempre = LOGO_PATHS.filter((p) => p.detail === 'always')
    const fine = LOGO_PATHS.filter((p) => p.detail === 'full')
    expect(sempre.length).toBeGreaterThan(0)
    expect(fine.length).toBeGreaterThan(0)

    // La soglia è quella dichiarata, non un numero sparso nel codice.
    expect(logoPathsFor(LOGO_COMPACT_BELOW)).toEqual(LOGO_PATHS)
    expect(logoPathsFor(LOGO_COMPACT_BELOW - 1)).toEqual(sempre)

    const piccolo = logoMarkSvg({ size: 16, accent: '#123456', contrast: '#654321' })
    for (const p of sempre) expect(piccolo).toContain(p.d)
    for (const p of fine) expect(piccolo).not.toContain(p.d)
  })

  it('il richiamo esplicito del disegno compatto vince sulla dimensione', () => {
    const fine = LOGO_PATHS.filter((p) => p.detail === 'full')
    expect(logoMarkSvg({ size: 512, compact: true })).not.toContain(fine[0]!.d)
    expect(logoMarkSvg({ size: 12, compact: false })).toContain(fine[0]!.d)
  })

  it('la variante monocromatica usa un colore solo e svuota la piastrella', () => {
    const svg = logoMarkSvg({ size: 64, accent: '#123456', contrast: '#654321', mono: '#abcdef' })
    const trovati = new Set(svg.match(/#[0-9a-f]{6}/gi) ?? [])
    expect([...trovati]).toEqual(['#abcdef'])
    // Piastrella a contorno: se restasse piena, la foglia dello stesso colore sparirebbe.
    expect(svg).toContain('fill="none"')
    expect(svg).toContain('stroke="#abcdef"')
  })

  it('anche il logo completo rispetta la monocromia, wordmark incluso', () => {
    const svg = logoLockupSvg({ height: 32, accent: '#123456', contrast: '#654321', text: '#111111', mono: '#abcdef' })
    const trovati = new Set(svg.match(/#[0-9a-f]{6}/gi) ?? [])
    expect([...trovati]).toEqual(['#abcdef'])
  })
})
