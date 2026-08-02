import { readFileSync, readdirSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'
import { render } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { EmptyIllustration } from './EmptyIllustration'
import { NotFoundIllustration } from './NotFoundIllustration'
import { ILLUSTRATION_VIEWBOX } from './Illustration'

const QUI = dirname(fileURLToPath(import.meta.url))

const FIGURE = [
  { nome: 'empty', Componente: EmptyIllustration },
  { nome: 'not-found', Componente: NotFoundIllustration },
]

describe('illustrazioni on-brand (UC 0087)', () => {
  it.each(FIGURE)('«$nome» è decorativa e riconoscibile', ({ nome, Componente }) => {
    const { container } = render(<Componente />)
    const svg = container.querySelector('svg')!
    expect(svg.getAttribute('data-illustration')).toBe(nome)
    expect(svg.getAttribute('viewBox')).toBe(ILLUSTRATION_VIEWBOX)
    // Decorativa: chi usa un lettore di schermo non deve sentirsi leggere la figura.
    expect(svg.getAttribute('aria-hidden')).toBe('true')
    expect(svg.getAttribute('role')).toBe('presentation')
    expect(svg.getAttribute('class')).toContain('ag-illustration')
  })

  it.each(FIGURE)('«$nome» accetta classi dal punto d uso senza perdere le proprie', ({ Componente }) => {
    const { container } = render(<Componente className="max-w-[200px]" />)
    const classe = container.querySelector('svg')!.getAttribute('class') ?? ''
    expect(classe).toContain('max-w-[200px]')
    expect(classe).toContain('ag-illustration')
  })

  it('nessuna figura cabla un colore: si dipinge solo con le classi-token', () => {
    // La regola n.2 della nota di stile. Un colore scritto per esteso congela la figura
    // sul tema chiaro e la rompe in silenzio su quello scuro.
    for (const file of readdirSync(QUI).filter((f) => f.endsWith('.tsx') && !f.includes('.test.'))) {
      const sorgente = readFileSync(join(QUI, file), 'utf8')
      expect(sorgente.match(/#[0-9a-fA-F]{3,8}\b/g), `${file} contiene un colore cablato`).toBe(null)
      expect(/\brgba?\(/.test(sorgente), `${file} contiene un colore cablato`).toBe(false)
    }
  })

  it('nessuna figura contiene testo: le lingue del prodotto sono cinque', () => {
    for (const { Componente } of FIGURE) {
      const { container } = render(<Componente />)
      expect(container.querySelectorAll('text')).toHaveLength(0)
      expect(container.textContent).toBe('')
    }
  })

  it("l'accento resta il punto focale, quindi è usato con parsimonia", () => {
    // Regola n.4: una figura in cui l'accento è ovunque non ha più un punto focale.
    for (const { Componente } of FIGURE) {
      const { container } = render(<Componente />)
      const forme = [...container.querySelectorAll('svg *')]
      const conAccento = forme.filter((e) => (e.getAttribute('class') ?? '').includes('accent'))
      expect(conAccento.length).toBeGreaterThan(0)
      expect(conAccento.length).toBeLessThanOrEqual(Math.ceil(forme.length / 2))
    }
  })
})
