import { describe, it, expect } from 'vitest'
import { parseSocialLinks, SOCIAL_LINKS } from './social.ts'

describe('parseSocialLinks', () => {
  it('lista vuota / assente → nessun link', () => {
    expect(parseSocialLinks('')).toEqual([])
    expect(parseSocialLinks('links:')).toEqual([])
    expect(parseSocialLinks('links: []')).toEqual([])
  })

  it('legge le voci valide preservando ordine ed estrae label/href', () => {
    const raw = [
      'links:',
      '  - label: LinkedIn',
      '    href: https://www.linkedin.com/company/appgrove',
      '  - label: X',
      '    href: https://x.com/appgrove',
    ].join('\n')
    expect(parseSocialLinks(raw)).toEqual([
      { label: 'LinkedIn', href: 'https://www.linkedin.com/company/appgrove' },
      { label: 'X', href: 'https://x.com/appgrove' },
    ])
  })

  it('taglia gli spazi attorno a label e href', () => {
    const raw = 'links:\n  - label: "  LinkedIn  "\n    href: "  https://x.com/a  "'
    expect(parseSocialLinks(raw)).toEqual([{ label: 'LinkedIn', href: 'https://x.com/a' }])
  })

  it('lancia se una voce non ha label', () => {
    expect(() => parseSocialLinks('links:\n  - href: https://x.com/a')).toThrow(/label/)
  })

  it('lancia se href non è un URL https assoluto', () => {
    expect(() => parseSocialLinks('links:\n  - label: X\n    href: /x')).toThrow(/href/)
    expect(() =>
      parseSocialLinks('links:\n  - label: X\n    href: http://x.com/a'),
    ).toThrow(/href/)
  })

  it('lancia se `links` non è una lista', () => {
    expect(() => parseSocialLinks('links: nope')).toThrow(/lista/)
  })
})

describe('SOCIAL_LINKS (file consegnato)', () => {
  it('è un array', () => {
    expect(Array.isArray(SOCIAL_LINKS)).toBe(true)
  })

  it('ogni voce eventualmente presente rispetta il contratto', () => {
    for (const s of SOCIAL_LINKS) {
      expect(s.label.length).toBeGreaterThan(0)
      expect(s.href).toMatch(/^https:\/\/\S+/)
    }
  })
})
