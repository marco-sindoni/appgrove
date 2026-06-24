import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { axe } from 'jest-axe'
import { Badge } from './Badge'

describe('Badge', () => {
  it('renderizza il testo e la tinta della tone', () => {
    render(<Badge tone="success">Attivo</Badge>)
    const badge = screen.getByText('Attivo')
    expect(badge).toBeInTheDocument()
    expect(badge.className).toContain('text-success')
  })

  it('non ha violazioni a11y', async () => {
    const { container } = render(<Badge tone="warning">In pausa</Badge>)
    expect(await axe(container)).toHaveNoViolations()
  })
})
