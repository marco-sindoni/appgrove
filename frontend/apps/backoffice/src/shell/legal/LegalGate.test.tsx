import { describe, it, expect, beforeAll, afterAll, afterEach, beforeEach } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { axe } from 'jest-axe'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { renderWithProviders } from '../../test/utils'
import { LegalGate } from './LegalGate'

const API = 'http://localhost/api/platform/v1'

type ComponentStatus = { component: string; version: string; effectiveDate: string; act: string }
let status: { pending: ComponentStatus[]; notices: ComponentStatus[] }

const server = setupServer(
  http.get(`${API}/me/legal/status`, () => HttpResponse.json(status)),
  http.get(`${API}/legal/:component`, ({ params }) =>
    HttpResponse.json({
      component: params.component,
      lang: 'en',
      version: '2.0.0',
      effectiveDate: '2026-01-01',
      markdown: '# Termini\nTesto del documento',
    }),
  ),
  http.post(`${API}/me/legal/acceptance`, () => {
    // L'accettazione azzera i pendenti: la successiva rilettura dello stato non blocca più.
    status = { pending: [], notices: [] }
    return HttpResponse.json(status)
  }),
)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

beforeEach(() => {
  status = {
    pending: [{ component: 'terms', version: '2.0.0', effectiveDate: '2026-01-01', act: 'accept' }],
    notices: [],
  }
})

/** Sentinella: contenuto della shell che il gate deve nascondere quando blocca. */
function guarded() {
  return (
    <LegalGate>
      <div>app-content</div>
    </LegalGate>
  )
}

describe('LegalGate (UC 0056 — ri-accettazione legale runtime)', () => {
  it('con documenti pendenti mostra la schermata bloccante e nasconde il contenuto', async () => {
    renderWithProviders(guarded(), { route: '/', language: 'en' })

    expect(await screen.findByText('Updated legal documents')).toBeInTheDocument()
    expect(screen.queryByText('app-content')).not.toBeInTheDocument()
  })

  it('senza documenti pendenti rende il contenuto normale della shell', async () => {
    status.pending = []
    renderWithProviders(guarded(), { route: '/', language: 'en' })

    expect(await screen.findByText('app-content')).toBeInTheDocument()
    expect(screen.queryByText('Updated legal documents')).not.toBeInTheDocument()
  })

  it('aprendo il documento ne carica e mostra il contenuto markdown', async () => {
    const user = userEvent.setup()
    renderWithProviders(guarded(), { route: '/', language: 'en' })

    await screen.findByText('Updated legal documents')
    await user.click(screen.getByRole('button', { name: 'Read the document' }))

    expect(await screen.findByText('Termini')).toBeInTheDocument()
    expect(screen.getByText('Testo del documento')).toBeInTheDocument()
  })

  it('abilita "Continue" solo quando tutte le caselle sono spuntate, poi accetta', async () => {
    const user = userEvent.setup()
    renderWithProviders(guarded(), { route: '/', language: 'en' })

    await screen.findByText('Updated legal documents')
    const continueBtn = screen.getByRole('button', { name: 'Continue' })
    expect(continueBtn).toBeDisabled()

    await user.click(screen.getByRole('checkbox', { name: 'I accept' }))
    expect(continueBtn).toBeEnabled()

    // Dopo l'accettazione lo stato torna vuoto → il contenuto normale compare.
    await user.click(continueBtn)
    expect(await screen.findByText('app-content')).toBeInTheDocument()
  })

  it('sulla route /privacy non blocca anche con documenti pendenti (diritti GDPR sempre accessibili)', async () => {
    renderWithProviders(guarded(), { route: '/privacy', language: 'en' })

    expect(await screen.findByText('app-content')).toBeInTheDocument()
    expect(screen.queryByText('Updated legal documents')).not.toBeInTheDocument()
  })

  it('nessuna violazione di accessibilità sulla schermata bloccante', async () => {
    const { container } = renderWithProviders(guarded(), { route: '/', language: 'en' })
    await screen.findByText('Updated legal documents')
    expect(await axe(container)).toHaveNoViolations()
  })
})
