import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { axe } from 'jest-axe'
import { Logo } from './Logo'

describe('Logo', () => {
  it('espone il nome accessibile appgrove', () => {
    render(<Logo />)
    expect(screen.getByRole('img', { name: 'appgrove' })).toBeInTheDocument()
  })

  it('può nascondere il wordmark', () => {
    render(<Logo showWordmark={false} />)
    expect(screen.queryByText('appgrove')).not.toBeInTheDocument()
  })

  it('non ha violazioni a11y', async () => {
    const { container } = render(<Logo />)
    expect(await axe(container)).toHaveNoViolations()
  })
})
