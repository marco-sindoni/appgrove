import { useNavigate } from 'react-router-dom'
import {
  Button,
  Icon,
  PageHeader,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeadCell,
  TableRow,
} from '@appgrove/design-system'
import { QueryState } from '../../../shell/QueryState'
import { useInvoices } from '../api/hooks'
import { QuotaBanner } from '../components/QuotaBanner'
import { CustomerAvatar } from '../components/CustomerAvatar'
import { StatusBadge } from '../components/StatusBadge'
import { formatAmount, t } from '../strings'

/** Schermata lista fatture (mockup Invoices): header con riquadro icona app, banner quota, tabella card. */
export function InvoiceListScreen() {
  const navigate = useNavigate()
  const invoices = useInvoices()
  const rows = invoices.data?.content ?? []

  return (
    <div className="space-y-[22px]">
      <PageHeader
        title={t.title}
        subtitle={t.subtitle}
        icon="receipt_long"
        iconClassName="bg-cat-blue/15 text-cat-blue"
        actions={
          <Button
            className="bg-cat-blue shadow-[0_6px_16px_-6px_rgb(var(--ag-cat-blue))]"
            onClick={() => navigate('new')}
          >
            <Icon name="add" size={19} />
            {t.newInvoice}
          </Button>
        }
      />

      <QuotaBanner />

      <QueryState
        isLoading={invoices.isLoading}
        isError={invoices.isError}
        onRetry={() => void invoices.refetch()}
      >
        {rows.length === 0 ? (
          <div className="rounded-lg border border-line bg-surface px-6 py-12 text-center shadow-sm">
            <Icon name="receipt_long" size={42} className="text-fg-faint" />
            <p className="mt-3 text-[15px] font-bold text-fg">{t.empty}</p>
          </div>
        ) : (
          <Table>
            <TableHead>
              <TableRow>
                <TableHeadCell>{t.colNumber}</TableHeadCell>
                <TableHeadCell>{t.colCustomer}</TableHeadCell>
                <TableHeadCell>{t.colIssueDate}</TableHeadCell>
                <TableHeadCell>{t.colStatus}</TableHeadCell>
                <TableHeadCell className="text-right">{t.colTotal}</TableHeadCell>
                <TableHeadCell className="text-right">{t.colActions}</TableHeadCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((inv) => (
                <TableRow key={inv.id} interactive onClick={() => navigate(String(inv.id))}>
                  <TableCell className="font-mono font-semibold text-fg-muted">
                    {inv.number}
                  </TableCell>
                  <TableCell>
                    <span className="flex items-center gap-3">
                      <CustomerAvatar name={inv.customerName} />
                      <span className="font-semibold">{inv.customerName}</span>
                    </span>
                  </TableCell>
                  <TableCell className="text-fg-muted">
                    {inv.issueDate ? new Date(inv.issueDate).toLocaleDateString('it-IT') : '—'}
                  </TableCell>
                  <TableCell>
                    <StatusBadge status={inv.status} />
                  </TableCell>
                  <TableCell className="text-right font-mono font-bold">
                    {formatAmount(inv.totalAmount, inv.currency)}
                  </TableCell>
                  <TableCell className="text-right">
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={(e) => {
                        e.stopPropagation()
                        navigate(String(inv.id))
                      }}
                    >
                      {t.detailTitle}
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </QueryState>
    </div>
  )
}
