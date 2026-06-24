import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { axe } from 'jest-axe'
import { Input } from './Input'

describe('Input', () => {
  it('accetta digitazione', async () => {
    render(<Input aria-label="Email" />)
    const input = screen.getByLabelText('Email')
    await userEvent.type(input, 'ciao@appgrove.app')
    expect(input).toHaveValue('ciao@appgrove.app')
  })

  it('riflette lo stato invalido in aria-invalid e nel bordo', () => {
    render(<Input aria-label="Email" invalid />)
    const input = screen.getByLabelText('Email')
    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(input.className).toContain('border-danger')
  })

  it('non ha violazioni a11y se etichettato', async () => {
    const { container } = render(
      <label>
        Nome
        <Input />
      </label>,
    )
    expect(await axe(container)).toHaveNoViolations()
  })
})
