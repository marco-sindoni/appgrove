import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Button, Card, CardContent, CardHeader } from '@appgrove/design-system'
import { QueryState } from '../../../shell/QueryState'
import { ConfirmDialog } from '../../../pages/members/ConfirmDialog'
import { useDeleteInvoice, useInvoiceDetail, useUpdateInvoice } from '../api/hooks'
import { StatusBadge } from '../components/StatusBadge'
import { formatAmount, statusLabel, t } from '../strings'

const STATUSES = ['draft', 'issued', 'paid', 'voided'] as const

/** Dettaglio fattura: dati + righe, cambio stato (PATCH) ed eliminazione (soft-delete) con conferma. */
export function InvoiceDetailScreen() {
  const { id } = useParams()
  const navigate = useNavigate()
  const detail = useInvoiceDetail(id)
  const update = useUpdateInvoice()
  const remove = useDeleteInvoice()
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const invoice = detail.data
  const busy = update.isPending || remove.isPending

  const onChangeStatus = async (status: string) => {
    if (!id) return
    setError(null)
    try {
      await update.mutateAsync({ id, body: { status } })
    } catch {
      setError(t.errorGeneric)
    }
  }

  const onDelete = async () => {
    if (!id) return
    setError(null)
    try {
      await remove.mutateAsync(id)
      navigate('..', { relative: 'path' })
    } catch {
      setError(t.errorGeneric)
      setConfirmDelete(false)
    }
  }

  return (
    <div className="space-y-6">
      <Button variant="ghost" size="sm" onClick={() => navigate('..', { relative: 'path' })}>
        {t.backToList}
      </Button>

      {error && (
        <p role="alert" className="text-sm text-danger">
          {error}
        </p>
      )}

      <QueryState
        isLoading={detail.isLoading}
        isError={detail.isError}
        onRetry={() => void detail.refetch()}
      >
        {invoice && (
          <Card>
            <CardHeader>
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <h1 className="text-2xl font-semibold text-fg">
                    {t.detailTitle} <span className="font-mono">{invoice.number}</span>
                  </h1>
                  <p className="mt-1 text-sm text-fg-muted">{invoice.customerName}</p>
                </div>
                <StatusBadge status={invoice.status} />
              </div>
            </CardHeader>
            <CardContent className="space-y-6">
              <dl className="grid grid-cols-2 gap-3 text-sm">
                <div>
                  <dt className="text-fg-muted">{t.colCustomer}</dt>
                  <dd className="text-fg">{invoice.customerName}</dd>
                </div>
                <div>
                  <dt className="text-fg-muted">{t.colIssueDate}</dt>
                  <dd className="text-fg">
                    {invoice.issueDate
                      ? new Date(invoice.issueDate).toLocaleDateString('it-IT')
                      : '—'}
                  </dd>
                </div>
                <div>
                  <dt className="text-fg-muted">{t.colTotal}</dt>
                  <dd className="text-fg">{formatAmount(invoice.totalAmount, invoice.currency)}</dd>
                </div>
              </dl>

              {(invoice.lines?.length ?? 0) > 0 && (
                <table className="w-full text-left text-sm">
                  <thead>
                    <tr className="border-b border-line text-fg-muted">
                      <th scope="col" className="py-2 pr-4 font-medium">{t.fieldLineDescription}</th>
                      <th scope="col" className="py-2 pr-4 font-medium">{t.fieldLineQuantity}</th>
                      <th scope="col" className="py-2 pr-4 font-medium">{t.fieldLineUnitAmount}</th>
                      <th scope="col" className="py-2 pr-4 font-medium">{t.colTotal}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {invoice.lines?.map((line) => (
                      <tr key={line.id} className="border-b border-line/60">
                        <td className="py-2 pr-4">{line.description}</td>
                        <td className="py-2 pr-4">{line.quantity}</td>
                        <td className="py-2 pr-4">{formatAmount(line.unitAmount, invoice.currency)}</td>
                        <td className="py-2 pr-4">{formatAmount(line.lineAmount, invoice.currency)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}

              <div className="flex flex-wrap items-end gap-3 border-t border-line pt-4">
                <div>
                  <label htmlFor="status" className="mb-1 block text-sm font-medium text-fg">
                    {t.changeStatus}
                  </label>
                  <select
                    id="status"
                    className="h-10 rounded-md border border-line bg-surface px-3 text-sm text-fg disabled:opacity-50"
                    value={invoice.status ?? 'draft'}
                    disabled={busy}
                    onChange={(e) => void onChangeStatus(e.target.value)}
                  >
                    {STATUSES.map((s) => (
                      <option key={s} value={s}>
                        {statusLabel(s)}
                      </option>
                    ))}
                  </select>
                </div>
                <Button
                  variant="danger"
                  disabled={busy}
                  onClick={() => setConfirmDelete(true)}
                >
                  {t.delete}
                </Button>
              </div>
            </CardContent>
          </Card>
        )}
      </QueryState>

      {confirmDelete && (
        <ConfirmDialog
          title={t.confirmDeleteTitle}
          body={t.confirmDeleteBody}
          confirmLabel={t.delete}
          busy={busy}
          onConfirm={() => void onDelete()}
          onCancel={() => setConfirmDelete(false)}
        />
      )}
    </div>
  )
}
