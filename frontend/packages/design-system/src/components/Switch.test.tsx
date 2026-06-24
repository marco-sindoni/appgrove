import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { axe } from 'jest-axe'
import { Switch } from './Switch'

describe('Switch', () => {
  it('espone role switch ed è commutabile', async () => {
    const onCheckedChange = vi.fn()
    render(<Switch aria-label="Notifiche" onCheckedChange={onCheckedChange} />)
    const sw = screen.getByRole('switch', { name: 'Notifiche' })
    expect(sw).toHaveAttribute('aria-checked', 'false')
    await userEvent.click(sw)
    expect(onCheckedChange).toHaveBeenCalledWith(true)
  })

  it('rispetta disabled', async () => {
    const onCheckedChange = vi.fn()
    render(<Switch aria-label="Notifiche" disabled onCheckedChange={onCheckedChange} />)
    await userEvent.click(screen.getByRole('switch', { name: 'Notifiche' }))
    expect(onCheckedChange).not.toHaveBeenCalled()
  })

  it('non ha violazioni a11y', async () => {
    const { container } = render(<Switch aria-label="Tema scuro" />)
    expect(await axe(container)).toHaveNoViolations()
  })
})
