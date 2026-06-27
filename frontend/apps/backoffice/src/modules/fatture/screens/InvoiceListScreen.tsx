import { useNavigate } from 'react-router-dom'
import { Button, Card, CardContent } from '@appgrove/design-system'
import { QueryState } from '../../../shell/QueryState'
import { useInvoices } from '../api/hooks'
import { QuotaBanner } from '../components/QuotaBanner'
import { StatusBadge } from '../components/StatusBadge'
import { formatAmount, t } from '../strings'

/** Schermata lista fatture: banner quota, stati loading/empty/error, navigazione a dettaglio/creazione. */
export function InvoiceListScreen() {
  const navigate = useNavigate()
  const invoices = useInvoices()
  const rows = invoices.data?.content ?? []

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-fg">{t.title}</h1>
          <p className="mt-1 text-sm text-fg-muted">{t.subtitle}</p>
        </div>
        <Button onClick={() => navigate('new')}>{t.newInvoice}</Button>
      </div>

      <QuotaBanner />

      <Card>
        <CardContent>
          <QueryState
            isLoading={invoices.isLoading}
            isError={invoices.isError}
            onRetry={() => void invoices.refetch()}
          >
            {rows.length === 0 ? (
              <p className="text-sm text-fg-muted">{t.empty}</p>
            ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-line text-fg-muted">
                    <th scope="col" className="py-2 pr-4 font-medium">{t.colNumber}</th>
                    <th scope="col" className="py-2 pr-4 font-medium">{t.colCustomer}</th>
                    <th scope="col" className="py-2 pr-4 font-medium">{t.colIssueDate}</th>
                    <th scope="col" className="py-2 pr-4 font-medium">{t.colStatus}</th>
                    <th scope="col" className="py-2 pr-4 font-medium">{t.colTotal}</th>
                    <th scope="col" className="py-2 pr-4 font-medium">{t.colActions}</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((inv) => (
                    <tr key={inv.id} className="border-b border-line/60">
                      <td className="py-2 pr-4 font-mono">{inv.number}</td>
                      <td className="py-2 pr-4">{inv.customerName}</td>
                      <td className="py-2 pr-4 text-fg-muted">
                        {inv.issueDate ? new Date(inv.issueDate).toLocaleDateString('it-IT') : '—'}
                      </td>
                      <td className="py-2 pr-4"><StatusBadge status={inv.status} /></td>
                      <td className="py-2 pr-4">{formatAmount(inv.totalAmount, inv.currency)}</td>
                      <td className="py-2 pr-4">
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => navigate(String(inv.id))}
                        >
                          {t.detailTitle}
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            )}
          </QueryState>
        </CardContent>
      </Card>
    </div>
  )
}
