import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { axe } from 'jest-axe'
import { Icon } from './Icon'

describe('Icon', () => {
  it('è decorativa (aria-hidden) senza aria-label', () => {
    const { container } = render(<Icon name="eco" />)
    const span = container.querySelector('span')
    expect(span).toHaveAttribute('aria-hidden', 'true')
    expect(span).toHaveTextContent('eco')
  })

  it('diventa role=img con aria-label', () => {
    render(<Icon name="settings" aria-label="Impostazioni" />)
    expect(screen.getByRole('img', { name: 'Impostazioni' })).toBeInTheDocument()
  })

  it('applica FILL quando filled', () => {
    const { container } = render(<Icon name="eco" filled />)
    const span = container.querySelector('span')
    expect(span?.getAttribute('style')).toContain("'FILL' 1")
  })

  it('non ha violazioni a11y (decorativa)', async () => {
    const { container } = render(<Icon name="eco" />)
    expect(await axe(container)).toHaveNoViolations()
  })
})
