import { describe, it, expect } from 'vitest'
import { createElement } from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import NewsletterForm, { type NewsletterLabels } from './NewsletterForm.tsx'

// Rende il componente a markup statico (nessun DOM del browser richiesto: gira in
// node come gli altri test del sito) e verifica gli invarianti di conformità del
// form newsletter (UC 0039): consenso NON pre-spuntato e presenza del campo esca.

const labels: NewsletterLabels = {
  ariaLabel: 'Newsletter',
  emailPlaceholder: 'tu@email.com',
  cta: 'Avvisami',
  consentLabel: 'Acconsento a ricevere la newsletter.',
  success: 'Controlla la tua email.',
  error: 'Qualcosa è andato storto.',
}

function render() {
  return renderToStaticMarkup(
    createElement(NewsletterForm, { apiUrl: 'http://localhost:8080', lang: 'it', labels }),
  )
}

describe('NewsletterForm (UC 0039)', () => {
  it('la checkbox di consenso NON è pre-spuntata', () => {
    const html = render()
    expect(html).toContain('type="checkbox"')
    // React non emette l'attributo `checked` quando lo stato è false.
    expect(html).not.toContain('checked')
  })

  it('espone il campo esca "website" nascosto e fuori dall\'albero di accessibilità', () => {
    const html = render()
    expect(html).toMatch(/name="website"/)
    expect(html).toContain('aria-hidden="true"')
    expect(html).toContain('tabindex="-1"')
  })

  it('il pulsante di invio parte disabilitato finché manca il consenso', () => {
    const html = render()
    expect(html).toMatch(/<button[^>]*disabled/)
  })
})
