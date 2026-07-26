import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Button, Card, CardContent, CardHeader, Icon } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { QueryState } from '../../../shell/QueryState'
import { ConfirmDialog } from '../../../pages/members/ConfirmDialog'
import { useDeleteItem, useItemDetail, useUpdateItem } from '../api/hooks'
import { StatusBadge } from '../components/StatusBadge'
import { formatAmount, formatDate, use@@APP_CLASS@@Messages } from '../i18n'

const STATUSES = ['draft', 'active', 'done', 'archived'] as const

/** Dettaglio record: dati + righe, cambio stato (PATCH) ed eliminazione (soft-delete) con conferma. */
export function ItemDetailScreen() {
  const { id } = useParams()
  const navigate = useNavigate()
  const detail = useItemDetail(id)
  const update = useUpdateItem()
  const remove = useDeleteItem()
  const m = use@@APP_CLASS@@Messages()
  const { i18n } = useTranslation()
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const item = detail.data
  const busy = update.isPending || remove.isPending

  const onChangeStatus = async (status: string) => {
    if (!id) return
    setError(null)
    try {
      await update.mutateAsync({ id, body: { status } })
    } catch {
      setError(m.errorGeneric)
    }
  }

  const onDelete = async () => {
    if (!id) return
    setError(null)
    try {
      await remove.mutateAsync(id)
      navigate('..', { relative: 'path' })
    } catch {
      setError(m.errorGeneric)
      setConfirmDelete(false)
    }
  }

  return (
    <div className="space-y-6">
      <Button variant="ghost" size="sm" onClick={() => navigate('..', { relative: 'path' })}>
        <Icon name="arrow_back" size={18} />
        {m.backToList}
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
        {item && (
          <Card>
            <CardHeader>
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <h1 className="text-[22px] font-extrabold tracking-[-0.02em] text-fg">
                    {m.detailTitle} <span className="font-mono">{item.code}</span>
                  </h1>
                  <p className="mt-1 text-[13px] text-fg-muted">{item.contactName}</p>
                </div>
                <StatusBadge status={item.status} />
              </div>
            </CardHeader>
            <CardContent className="space-y-6">
              <dl className="grid grid-cols-2 gap-3 text-sm">
                <div>
                  <dt className="text-fg-muted">{m.colContact}</dt>
                  <dd className="text-fg">{item.contactName}</dd>
                </div>
                <div>
                  <dt className="text-fg-muted">{m.colRecordedOn}</dt>
                  <dd className="text-fg">{formatDate(item.recordedOn, i18n.language)}</dd>
                </div>
                <div>
                  <dt className="text-fg-muted">{m.colTotal}</dt>
                  <dd className="font-mono font-bold text-fg">
                    {formatAmount(item.totalAmount, item.currency, i18n.language)}
                  </dd>
                </div>
              </dl>

              {(item.lines?.length ?? 0) > 0 && (
                <table className="w-full text-left">
                  <thead>
                    <tr>
                      <th scope="col" className="border-b border-line py-2.5 pr-4 text-[11px] font-bold uppercase tracking-[.05em] text-fg-faint">{m.fieldLineDescription}</th>
                      <th scope="col" className="border-b border-line py-2.5 pr-4 text-[11px] font-bold uppercase tracking-[.05em] text-fg-faint">{m.fieldLineQuantity}</th>
                      <th scope="col" className="border-b border-line py-2.5 pr-4 text-right text-[11px] font-bold uppercase tracking-[.05em] text-fg-faint">{m.fieldLineUnitAmount}</th>
                      <th scope="col" className="border-b border-line py-2.5 text-right text-[11px] font-bold uppercase tracking-[.05em] text-fg-faint">{m.colTotal}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {item.lines?.map((line) => (
                      <tr key={line.id} className="border-b border-line last:border-b-0">
                        <td className="py-3 pr-4 text-[13px] text-fg">{line.description}</td>
                        <td className="py-3 pr-4 font-mono text-[13px] text-fg-muted">{line.quantity}</td>
                        <td className="py-3 pr-4 text-right font-mono text-[13px] text-fg-muted">{formatAmount(line.unitAmount, item.currency, i18n.language)}</td>
                        <td className="py-3 text-right font-mono text-[13px] font-bold text-fg">{formatAmount(line.lineAmount, item.currency, i18n.language)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}

              <div className="flex flex-wrap items-end gap-3 border-t border-line pt-4">
                <div>
                  <label htmlFor="status" className="mb-1 block text-sm font-medium text-fg">
                    {m.changeStatus}
                  </label>
                  <select
                    id="status"
                    className="h-10 rounded-md border border-line bg-surface px-3 text-sm text-fg disabled:opacity-50"
                    value={item.status ?? 'draft'}
                    disabled={busy}
                    onChange={(e) => void onChangeStatus(e.target.value)}
                  >
                    {STATUSES.map((s) => (
                      <option key={s} value={s}>
                        {m.status[s]}
                      </option>
                    ))}
                  </select>
                </div>
                <Button variant="danger" disabled={busy} onClick={() => setConfirmDelete(true)}>
                  {m.delete}
                </Button>
              </div>
            </CardContent>
          </Card>
        )}
      </QueryState>

      {confirmDelete && (
        <ConfirmDialog
          title={m.confirmDeleteTitle}
          body={m.confirmDeleteBody}
          confirmLabel={m.delete}
          busy={busy}
          onConfirm={() => void onDelete()}
          onCancel={() => setConfirmDelete(false)}
        />
      )}
    </div>
  )
}
