// @vitest-environment node
// Il lettore dei token è un modulo Node (legge il file sorgente dal disco): va provato
// nell'ambiente in cui gira davvero, non nel browser simulato usato dai componenti.
import { describe, expect, it } from 'vitest'
// @ts-expect-error — modulo JavaScript puro condiviso con Node (nessuna dichiarazione di tipo:
// serve solo ai test, i consumatori TypeScript usano i componenti, non il lettore).
import { readTokens, hex, tripletToHex, colorHexes, allBrandHexes, THEMES, ACCENTS } from './tokens.mjs'
import { ACCENT_COLORS } from '../theme/theme'

describe('lettura programmatica dei token (UC 0086)', () => {
  it('legge i token base dal tema chiaro', () => {
    const tokens = readTokens()
    expect(tokens.bg).toBe('244 244 241')
    expect(tokens.surface).toBe('255 255 255')
    expect(tokens.text).toBe('38 36 32')
    expect(tokens['radius-lg']).toBe('18px')
  })

  it('il tema scuro sovrascrive i neutri e lascia intatto il resto', () => {
    const light = readTokens({ theme: 'light' })
    const dark = readTokens({ theme: 'dark' })
    expect(dark.bg).toBe('22 21 18')
    expect(dark.text).toBe('241 239 233')
    // i raggi non dipendono dal tema: devono restare identici
    expect(dark['radius-lg']).toBe(light['radius-lg'])
  })

  it('l accento scelto sovrascrive il colore d accento', () => {
    expect(hex('accent', { accent: 'coral' })).toBe('#ec5a72')
    expect(hex('accent', { accent: 'violet' })).toBe('#7b6ef0')
    expect(hex('accent', { accent: 'teal' })).toBe('#16b6a4')
    expect(hex('accent', { accent: 'blue' })).toBe('#4f86e0')
  })

  it('converte le terne RGB in esadecimale, con lo zero iniziale dove serve', () => {
    expect(tripletToHex('236 90 114')).toBe('#ec5a72')
    expect(tripletToHex('0 0 0')).toBe('#000000')
    expect(tripletToHex('255 255 255')).toBe('#ffffff')
    expect(() => tripletToHex('18px')).toThrow(/terna RGB/)
  })

  it('espone i colori-categoria per-app usati dalle landing', () => {
    const colors = colorHexes()
    expect(colors['cat-blue']).toBe('#5b8def')
    expect(colors['cat-violet']).toBe('#8a76f0')
    expect(colors['cat-green']).toBe('#3aae73')
    expect(colors['cat-amber']).toBe('#dd9b34')
    expect(colors['cat-red']).toBe('#e3654f')
    expect(colors['cat-teal']).toBe('#1fb6a6')
  })

  it('rifiuta temi, accenti e token sconosciuti invece di restituire valori muti', () => {
    expect(() => readTokens({ theme: 'sepia' })).toThrow(/tema sconosciuto/)
    expect(() => readTokens({ accent: 'fucsia' })).toThrow(/accento sconosciuto/)
    expect(() => hex('non-esiste')).toThrow(/token sconosciuto/)
  })

  it('l insieme dei colori del brand copre ogni tema e ogni accento', () => {
    const all = allBrandHexes()
    // i neutri di entrambi i temi
    expect(all.has('#f4f4f1')).toBe(true) // sfondo chiaro
    expect(all.has('#161512')).toBe(true) // sfondo scuro
    // tutti e quattro gli accenti
    for (const accent of ACCENTS) expect(all.has(hex('accent', { accent }))).toBe(true)
    expect(THEMES).toEqual(['light', 'dark'])
  })
})

describe('anti-drift dei duplicati necessari', () => {
  // `ACCENT_COLORS` vive in theme.ts perché i pallini del selettore colore girano nel
  // browser, che non può leggere il filesystem: è una copia INEVITABILE. Questo test la
  // sorveglia, così non può divergere in silenzio dalla sorgente (§5 dello use case 0086).
  it('gli esadecimali degli accenti in theme.ts coincidono con i token', () => {
    for (const [accent, color] of Object.entries(ACCENT_COLORS)) {
      expect(color).toBe(hex('accent', { accent }))
    }
  })
})
