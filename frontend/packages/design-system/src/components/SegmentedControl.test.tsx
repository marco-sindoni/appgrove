import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { axe } from 'jest-axe'
import { SegmentedControl } from './SegmentedControl'

const options = [
  { value: 'monthly', label: 'Mensile' },
  { value: 'annual', label: 'Annuale' },
]

describe('SegmentedControl', () => {
  it('seleziona di default la prima opzione e notifica i cambi', async () => {
    const onValueChange = vi.fn()
    render(
      <SegmentedControl options={options} aria-label="Ciclo" onValueChange={onValueChange} />,
    )
    expect(screen.getByRole('radio', { name: 'Mensile' })).toHaveAttribute('data-state', 'on')
    await userEvent.click(screen.getByRole('radio', { name: 'Annuale' }))
    expect(onValueChange).toHaveBeenCalledWith('annual')
  })

  it('non ha violazioni a11y', async () => {
    const { container } = render(<SegmentedControl options={options} aria-label="Ciclo" />)
    expect(await axe(container)).toHaveNoViolations()
  })
})
