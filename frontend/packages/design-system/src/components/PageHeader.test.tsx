import { render, screen } from '@testing-library/react'
import { axe } from 'jest-axe'
import { describe, expect, it } from 'vitest'
import { PageHeader } from './PageHeader'

describe('PageHeader', () => {
  it('rende titolo h1, sottotitolo e azioni', () => {
    render(
      <PageHeader
        title="Fatture"
        subtitle="Le fatture del tuo account"
        actions={<button type="button">Nuova fattura</button>}
      />,
    )
    expect(screen.getByRole('heading', { level: 1, name: 'Fatture' })).toBeInTheDocument()
    expect(screen.getByText('Le fatture del tuo account')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Nuova fattura' })).toBeInTheDocument()
  })

  it('senza icona usa il titolo grande 27px, con icona il riquadro tinto e titolo 24px', () => {
    const { rerender } = render(<PageHeader title="Impostazioni" />)
    expect(screen.getByRole('heading', { level: 1 }).className).toContain('text-[27px]')

    rerender(<PageHeader title="Fatture" icon="receipt_long" iconClassName="bg-cat-blue/15 text-cat-blue" />)
    expect(screen.getByRole('heading', { level: 1 }).className).toContain('text-2xl')
  })

  it('non ha violazioni a11y', async () => {
    const { container } = render(<PageHeader title="Fatture" subtitle="Sottotitolo" />)
    expect(await axe(container)).toHaveNoViolations()
  })
})
