import type { Meta, StoryObj } from '@storybook/react-vite'
import { Badge } from './Badge'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeadCell,
  TableRow,
} from './Table'

const meta: Meta<typeof Table> = {
  title: 'Components/Table',
  component: Table,
  parameters: { layout: 'padded' },
}
export default meta

type Story = StoryObj<typeof Table>

export const Fatture: Story = {
  render: () => (
    <Table aria-label="Fatture">
      <TableHead>
        <TableRow>
          <TableHeadCell>Numero</TableHeadCell>
          <TableHeadCell>Cliente</TableHeadCell>
          <TableHeadCell>Stato</TableHeadCell>
          <TableHeadCell className="text-right">Totale</TableHeadCell>
        </TableRow>
      </TableHead>
      <TableBody>
        <TableRow interactive>
          <TableCell className="font-mono font-semibold text-fg-muted">2026-0001</TableCell>
          <TableCell className="font-bold">Northwind Ltd</TableCell>
          <TableCell>
            <Badge tone="success">Pagata</Badge>
          </TableCell>
          <TableCell className="text-right font-mono font-bold">€1.240,00</TableCell>
        </TableRow>
        <TableRow interactive>
          <TableCell className="font-mono font-semibold text-fg-muted">2026-0002</TableCell>
          <TableCell className="font-bold">Acme S.r.l.</TableCell>
          <TableCell>
            <Badge tone="danger">Scaduta</Badge>
          </TableCell>
          <TableCell className="text-right font-mono font-bold">€3.480,00</TableCell>
        </TableRow>
        <TableRow interactive>
          <TableCell className="font-mono font-semibold text-fg-muted">2026-0003</TableCell>
          <TableCell className="font-bold">Luce Design</TableCell>
          <TableCell>
            <Badge tone="neutral">Bozza</Badge>
          </TableCell>
          <TableCell className="text-right font-mono font-bold">€780,00</TableCell>
        </TableRow>
      </TableBody>
    </Table>
  ),
}
