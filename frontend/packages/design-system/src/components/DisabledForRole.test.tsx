import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { axe } from 'jest-axe'
import { Button } from './Button'
import { DisabledForRole } from './DisabledForRole'

const REASON = 'Serve il ruolo Editor: chiedi all’owner o a un amministratore dell’applicazione.'

describe('DisabledForRole', () => {
  it('quando il ruolo basta rende il comando così com’è, senza involucri', () => {
    const { container } = render(
      <DisabledForRole allowed reason={REASON}>
        <Button>Crea fattura</Button>
      </DisabledForRole>,
    )
    const button = screen.getByRole('button', { name: 'Crea fattura' })
    expect(button).toBeEnabled()
    // Nessun contenitore aggiunto: il bottone è il primo figlio, come se l'involucro non ci fosse.
    expect(container.firstElementChild).toBe(button)
  })

  // La regola della storia §6: PRESENTE ma disabilitato. Nascondere farebbe credere che la funzione non
  // esista, e chi la cerca non chiederebbe l'abilitazione a nessuno.
  it('quando il ruolo non basta il comando resta visibile e disabilitato', () => {
    render(
      <DisabledForRole allowed={false} reason={REASON}>
        <Button>Crea fattura</Button>
      </DisabledForRole>,
    )
    const button = screen.getByRole('button', { name: /Crea fattura/ })
    expect(button).toBeVisible()
    expect(button).toBeDisabled()
    expect(button).toHaveAttribute('aria-disabled', 'true')
    expect(button).toHaveAttribute('tabindex', '-1')
  })

  it('la spiegazione compare al passaggio del puntatore', () => {
    const { container } = render(
      <DisabledForRole allowed={false} reason={REASON}>
        <Button>Crea fattura</Button>
      </DisabledForRole>,
    )
    expect(container.querySelector('[title]')).toHaveAttribute('title', REASON)
  })

  // Il punto per cui l'involucro esiste: senza il testo collegato, un lettore di schermo annuncerebbe
  // «bottone, disabilitato» e niente altro — cioè nessun motivo e nessuna via d'uscita.
  it('la spiegazione è leggibile dagli strumenti di assistenza, collegata al comando', () => {
    render(
      <DisabledForRole allowed={false} reason={REASON}>
        <Button>Crea fattura</Button>
      </DisabledForRole>,
    )
    const button = screen.getByRole('button', { name: /Crea fattura/ })
    const describedBy = button.getAttribute('aria-describedby')
    expect(describedBy).toBeTruthy()
    expect(document.getElementById(describedBy!)).toHaveTextContent(REASON)
  })

  it('non ha violazioni a11y in nessuno dei due stati', async () => {
    const denied = render(
      <DisabledForRole allowed={false} reason={REASON}>
        <Button>Crea fattura</Button>
      </DisabledForRole>,
    )
    expect(await axe(denied.container)).toHaveNoViolations()

    const granted = render(
      <DisabledForRole allowed reason={REASON}>
        <Button>Crea fattura</Button>
      </DisabledForRole>,
    )
    expect(await axe(granted.container)).toHaveNoViolations()
  })
})
