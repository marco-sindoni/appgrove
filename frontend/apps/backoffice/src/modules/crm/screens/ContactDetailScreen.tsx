import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Button, Card, CardContent, CardHeader, Icon } from '@appgrove/design-system'
import { QueryState } from '../../../shell/QueryState'
import { ConfirmDialog } from '../../../pages/members/ConfirmDialog'
import {
  useContact,
  useCreateInteraction,
  useDeleteContact,
  useInteractions,
  useUpdateContact,
} from '../api/hooks'
import { StageBadge } from '../components/StageBadge'
import { kindLabel, stageLabel, t } from '../strings'

const STAGES = ['lead', 'qualified', 'negotiating', 'won', 'lost'] as const
const KINDS = ['note', 'call', 'email', 'meeting'] as const

/** Dettaglio contatto: dati, cambio stato, eliminazione, e storico + registrazione interazioni. */
export function ContactDetailScreen() {
  const { id } = useParams()
  const navigate = useNavigate()
  const detail = useContact(id)
  const interactions = useInteractions(id)
  const update = useUpdateContact()
  const remove = useDeleteContact()
  const addInteraction = useCreateInteraction(id ?? '')
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [kind, setKind] = useState<string>('note')
  const [note, setNote] = useState('')

  const contact = detail.data
  const busy = update.isPending || remove.isPending

  const onChangeStage = async (stage: string) => {
    if (!id) return
    setError(null)
    try {
      await update.mutateAsync({ id, body: { stage } })
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

  const onAddInteraction = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!note.trim()) return
    setError(null)
    try {
      await addInteraction.mutateAsync({ kind, note })
      setNote('')
    } catch {
      setError(t.errorGeneric)
    }
  }

  const rows = interactions.data ?? []

  return (
    <div className="space-y-6">
      <Button variant="ghost" size="sm" onClick={() => navigate('..', { relative: 'path' })}>
        <Icon name="arrow_back" size={18} />
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
        {contact && (
          <Card>
            <CardHeader>
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <h1 className="text-[22px] font-extrabold tracking-[-0.02em] text-fg">
                    {contact.displayName}
                  </h1>
                  {contact.organization && (
                    <p className="mt-1 text-[13px] text-fg-muted">{contact.organization}</p>
                  )}
                </div>
                <StageBadge stage={contact.stage} />
              </div>
            </CardHeader>
            <CardContent className="space-y-6">
              <dl className="grid grid-cols-2 gap-3 text-sm">
                <div>
                  <dt className="text-fg-muted">{t.fieldEmail}</dt>
                  <dd className="text-fg">{contact.email || '—'}</dd>
                </div>
                <div>
                  <dt className="text-fg-muted">{t.fieldPhone}</dt>
                  <dd className="text-fg">{contact.phone || '—'}</dd>
                </div>
                {contact.notes && (
                  <div className="col-span-2">
                    <dt className="text-fg-muted">{t.fieldNotes}</dt>
                    <dd className="text-fg whitespace-pre-line">{contact.notes}</dd>
                  </div>
                )}
              </dl>

              <div className="flex flex-wrap items-end gap-3 border-t border-line pt-4">
                <div>
                  <label htmlFor="stage" className="mb-1 block text-sm font-medium text-fg">
                    {t.changeStage}
                  </label>
                  <select
                    id="stage"
                    className="h-10 rounded-md border border-line bg-surface px-3 text-sm text-fg disabled:opacity-50"
                    value={contact.stage ?? 'lead'}
                    disabled={busy}
                    onChange={(e) => void onChangeStage(e.target.value)}
                  >
                    {STAGES.map((s) => (
                      <option key={s} value={s}>
                        {stageLabel(s)}
                      </option>
                    ))}
                  </select>
                </div>
                <Button variant="danger" disabled={busy} onClick={() => setConfirmDelete(true)}>
                  {t.delete}
                </Button>
              </div>
            </CardContent>
          </Card>
        )}
      </QueryState>

      {contact && (
        <Card>
          <CardHeader>
            <h2 className="font-sans text-lg font-extrabold tracking-tight text-fg">
              {t.interactionsTitle}
            </h2>
          </CardHeader>
          <CardContent className="space-y-4">
            <form onSubmit={onAddInteraction} className="flex flex-wrap items-end gap-3">
              <div>
                <label htmlFor="kind" className="mb-1 block text-sm font-medium text-fg">
                  {t.fieldInteractionKind}
                </label>
                <select
                  id="kind"
                  className="h-10 rounded-md border border-line bg-surface px-3 text-sm text-fg"
                  value={kind}
                  onChange={(e) => setKind(e.target.value)}
                >
                  {KINDS.map((k) => (
                    <option key={k} value={k}>
                      {kindLabel(k)}
                    </option>
                  ))}
                </select>
              </div>
              <div className="min-w-[16rem] flex-1">
                <label htmlFor="note" className="mb-1 block text-sm font-medium text-fg">
                  {t.fieldInteractionNote}
                </label>
                <input
                  id="note"
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                  className="h-10 w-full rounded-md border border-line bg-surface px-3 text-sm text-fg"
                />
              </div>
              <Button type="submit" disabled={addInteraction.isPending || !note.trim()}>
                {t.addInteraction}
              </Button>
            </form>

            {rows.length === 0 ? (
              <p className="text-sm text-fg-muted">{t.interactionsEmpty}</p>
            ) : (
              <ul className="space-y-2">
                {rows.map((it) => (
                  <li key={it.id} className="rounded-md border border-line bg-surface-2 px-3 py-2 text-sm">
                    <div className="flex items-center justify-between gap-2">
                      <span className="font-semibold text-fg">{kindLabel(it.kind)}</span>
                      <span className="text-fg-muted">
                        {it.occurredOn ? new Date(it.occurredOn).toLocaleDateString('it-IT') : '—'}
                      </span>
                    </div>
                    {it.note && <p className="mt-1 text-fg whitespace-pre-line">{it.note}</p>}
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>
      )}

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
