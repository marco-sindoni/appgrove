import { describe, it, expect } from 'vitest'
import { screen } from '@testing-library/react'
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

  it('non ha violazioni a11y', async () => {
    const { container } = renderWithProviders(<Sidebar />, { entitled: ['demo'] })
    expect(await axe(container)).toHaveNoViolations()
  })
})
