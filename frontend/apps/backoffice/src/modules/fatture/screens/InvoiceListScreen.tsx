import { useNavigate } from 'react-router-dom'
import { ApiError, refusalMessage } from '@appgrove/api-client'
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
import { useTranslation } from '@appgrove/i18n'
import { QueryState } from '../../../shell/QueryState'
import { useInvoices } from '../api/hooks'
import { QuotaBanner } from '../components/QuotaBanner'
import { CustomerAvatar } from '../components/CustomerAvatar'
import { StatusBadge } from '../components/StatusBadge'
import { formatAmount, formatDate, useFattureMessages } from '../i18n'

/** Schermata lista fatture (mockup Invoices): header con riquadro icona app, banner quota, tabella card. */
export function InvoiceListScreen() {
  const navigate = useNavigate()
  const invoices = useInvoices()
  const rows = invoices.data?.content ?? []
  const m = useFattureMessages()
  const { i18n } = useTranslation()
  // Un 403 qui non è un guasto e non si ritenta: è un varco che ha detto no. I varchi parlano di cose
  // diverse — il diritto dell'account all'applicazione e il RUOLO della persona su di essa
  // (UC 0099/0101) — e solo il server sa quale ha risposto. Si mostra quindi la SUA frase, che nomina
  // il varco e la via d'uscita; un riquadro «riprova» inviterebbe a ripetere una richiesta che
  // fallirà sempre.
  const refused = (invoices.error as ApiError | null)?.status === 403
  const refusalText = refused ? refusalMessage(invoices.error, m.errorGeneric) : null

  return (
    <div className="space-y-[22px]">
      <PageHeader
        title={m.title}
        subtitle={m.subtitle}
        icon="receipt_long"
        iconClassName="bg-cat-blue/15 text-cat-blue"
        actions={
          <Button
            className="bg-cat-blue shadow-[0_6px_16px_-6px_rgb(var(--ag-cat-blue))]"
            onClick={() => navigate('new')}
          >
            <Icon name="add" size={19} />
            {m.newInvoice}
          </Button>
        }
      />

      <QuotaBanner />

      {refused ? (
        <div
          role="status"
          className="rounded-lg border border-line bg-surface px-6 py-12 text-center shadow-sm"
        >
          <Icon name="lock" size={42} className="text-fg-faint" />
          <p className="mx-auto mt-3 max-w-md text-[15px] font-bold text-fg">{refusalText}</p>
        </div>
      ) : (
      <QueryState
        isLoading={invoices.isLoading}
        isError={invoices.isError}
        onRetry={() => void invoices.refetch()}
      >
        {rows.length === 0 ? (
          <div className="rounded-lg border border-line bg-surface px-6 py-12 text-center shadow-sm">
            <Icon name="receipt_long" size={42} className="text-fg-faint" />
            <p className="mt-3 text-[15px] font-bold text-fg">{m.empty}</p>
          </div>
        ) : (
          <Table>
            <TableHead>
              <TableRow>
                <TableHeadCell>{m.colNumber}</TableHeadCell>
                <TableHeadCell>{m.colCustomer}</TableHeadCell>
                <TableHeadCell>{m.colIssueDate}</TableHeadCell>
                <TableHeadCell>{m.colStatus}</TableHeadCell>
                <TableHeadCell className="text-right">{m.colTotal}</TableHeadCell>
                <TableHeadCell className="text-right">{m.colActions}</TableHeadCell>
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
                    {formatDate(inv.issueDate, i18n.language)}
                  </TableCell>
                  <TableCell>
                    <StatusBadge status={inv.status} />
                  </TableCell>
                  <TableCell className="text-right font-mono font-bold">
                    {formatAmount(inv.totalAmount, inv.currency, i18n.language)}
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
                      {m.detailTitle}
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </QueryState>
      )}
    </div>
  )
}
