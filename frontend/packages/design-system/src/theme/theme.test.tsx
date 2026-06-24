import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { applyTheme } from './theme'
import { ThemeProvider, useTheme } from './ThemeProvider'

describe('applyTheme', () => {
  it('scrive data-theme e data-accent sul nodo', () => {
    const node = document.createElement('div')
    applyTheme({ theme: 'dark', accent: 'violet' }, node)
    expect(node.getAttribute('data-theme')).toBe('dark')
    expect(node.getAttribute('data-accent')).toBe('violet')
  })
})

function Probe() {
  const { theme, accent, toggleTheme, setAccent } = useTheme()
  return (
    <div>
      <span data-testid="state">{`${theme}/${accent}`}</span>
      <button onClick={toggleTheme}>toggle</button>
      <button onClick={() => setAccent('teal')}>teal</button>
    </div>
  )
}

describe('ThemeProvider', () => {
  it('riflette tema/accent sul documento e reagisce ai cambi', async () => {
    render(
      <ThemeProvider defaultTheme="light" defaultAccent="coral">
        <Probe />
      </ThemeProvider>,
    )
    expect(document.documentElement.getAttribute('data-theme')).toBe('light')
    expect(document.documentElement.getAttribute('data-accent')).toBe('coral')

    await userEvent.click(screen.getByRole('button', { name: 'toggle' }))
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')

    await userEvent.click(screen.getByRole('button', { name: 'teal' }))
    expect(document.documentElement.getAttribute('data-accent')).toBe('teal')
    expect(screen.getByTestId('state')).toHaveTextContent('dark/teal')
  })
})
