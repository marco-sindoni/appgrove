import { render, screen } from '@testing-library/react'
import { axe } from 'jest-axe'
import { describe, expect, it } from 'vitest'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeadCell,
  TableRow,
} from './Table'

function renderTable() {
  return render(
    <Table aria-label="Fatture">
      <TableHead>
        <TableRow>
          <TableHeadCell>Numero</TableHeadCell>
          <TableHeadCell>Stato</TableHeadCell>
        </TableRow>
      </TableHead>
      <TableBody>
        <TableRow interactive data-testid="riga">
          <TableCell>2026-0001</TableCell>
          <TableCell>Bozza</TableCell>
        </TableRow>
      </TableBody>
    </Table>,
  )
}

describe('Table', () => {
  it('rende una tabella semantica con header e celle', () => {
    renderTable()
    expect(screen.getByRole('table', { name: 'Fatture' })).toBeInTheDocument()
    expect(screen.getByRole('columnheader', { name: 'Numero' })).toBeInTheDocument()
    expect(screen.getByRole('cell', { name: '2026-0001' })).toBeInTheDocument()
  })

  it('header colonne in maiuscolo attenuato, riga interattiva con hover', () => {
    renderTable()
    expect(screen.getByRole('columnheader', { name: 'Stato' }).className).toContain('uppercase')
    expect(screen.getByTestId('riga').className).toContain('hover:bg-surface-2')
  })

  it('non ha violazioni a11y', async () => {
    const { container } = renderTable()
    expect(await axe(container)).toHaveNoViolations()
  })
})
