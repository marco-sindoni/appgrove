import { describe, it, expect, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { axe } from 'jest-axe'
import { Sidebar } from './Sidebar'
import { renderWithProviders } from '../test/utils'

describe('Sidebar', () => {
  it('mostra la sezione PLATFORM e il modulo entitled in YOUR APPS', () => {
    renderWithProviders(<Sidebar />, { entitled: ['demo'] })
    expect(screen.getByText('Platform')).toBeInTheDocument()
    expect(screen.getByText('Your apps')).toBeInTheDocument()
    expect(screen.getByText('Demo app')).toBeInTheDocument()
  })

  it('non mostra i moduli senza entitlement', () => {
    renderWithProviders(<Sidebar />, { entitled: [] })
    expect(screen.queryByText('Demo app')).not.toBeInTheDocument()
  })

  it('mostra le etichette di navigazione del modulo nella lingua attiva', () => {
    // Modulo migrato all'i18n (fatture): nome e sezione seguono la lingua (UC 0060).
    const it = renderWithProviders(<Sidebar />, { entitled: ['fatture'], language: 'it' })
    expect(screen.getAllByText('Fatture').length).toBeGreaterThan(0)
    it.unmount()
    renderWithProviders(<Sidebar />, { entitled: ['fatture'], language: 'fr' })
    expect(screen.getAllByText('Factures').length).toBeGreaterThan(0)
    expect(screen.queryByText('Fatture')).not.toBeInTheDocument()
  })

  it('mostra lo stato di caricamento invece di dichiarare "nessuna app"', () => {
    renderWithProviders(<Sidebar />, { entitled: [], entitlementsLoading: true })
    expect(screen.getByRole('status')).toHaveTextContent('Loading')
    expect(screen.queryByText('No active apps yet')).not.toBeInTheDocument()
  })

  it('con entitlement non leggibili mostra un errore con riprova, non lo stato vuoto (UC 0077)', () => {
    // Il difetto chiuso: un guasto di rete diceva a un cliente pagante di non avere nessuna app.
    renderWithProviders(<Sidebar />, { entitled: [], entitlementsError: true })
    expect(screen.getByRole('alert')).toHaveTextContent('We couldn’t load your apps')
    expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument()
    expect(screen.queryByText('No active apps yet')).not.toBeInTheDocument()
  })

  it('senza app attive invita alla vetrina del catalogo', () => {
    // L'invito punta al catalogo (UC 0095), non più alla fatturazione: scoprire le app e pagarle sono
    // due cose diverse, e la prima ora ha una pagina sua.
    renderWithProviders(<Sidebar />, { entitled: [] })
    expect(screen.getByText('No active apps yet')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Browse the apps' })).toHaveAttribute('href', '/catalog')
  })

  it('la riprova richiama la rilettura degli entitlement', async () => {
    const retry = vi.fn()
    renderWithProviders(<Sidebar />, {
      entitled: [],
      entitlementsError: true,
      entitlementsRetry: retry,
    })
    await userEvent.click(screen.getByRole('button', { name: 'Retry' }))
    expect(retry).toHaveBeenCalledOnce()
  })

  it('non ha violazioni a11y', async () => {
    const { container } = renderWithProviders(<Sidebar />, { entitled: ['demo'] })
    expect(await axe(container)).toHaveNoViolations()
  })
})
