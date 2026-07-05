import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { axe } from 'jest-axe'
import { Button } from './Button'

describe('Button', () => {
  it('renderizza il children e di default è type=button', () => {
    render(<Button>Salva</Button>)
    const btn = screen.getByRole('button', { name: 'Salva' })
    expect(btn).toBeInTheDocument()
    expect(btn).toHaveAttribute('type', 'button')
  })

  it('invoca onClick e rispetta disabled', async () => {
    const onClick = vi.fn()
    const { rerender } = render(<Button onClick={onClick}>Vai</Button>)
    await userEvent.click(screen.getByRole('button', { name: 'Vai' }))
    expect(onClick).toHaveBeenCalledTimes(1)

    rerender(
      <Button onClick={onClick} disabled>
        Vai
      </Button>,
    )
    await userEvent.click(screen.getByRole('button', { name: 'Vai' }))
    expect(onClick).toHaveBeenCalledTimes(1)
  })

  it('applica le classi della variante e della size', () => {
    render(
      <Button variant="danger" size="lg">
        Elimina
      </Button>,
    )
    const btn = screen.getByRole('button', { name: 'Elimina' })
    expect(btn.className).toContain('text-danger')
    expect(btn.className).toContain('h-12')
  })

  it('non ha violazioni a11y', async () => {
    const { container } = render(<Button>Accessibile</Button>)
    expect(await axe(container)).toHaveNoViolations()
  })
})
