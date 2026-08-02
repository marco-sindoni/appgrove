import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Button, Card, CardContent } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import {
  useAdminReplyTicket,
  useAdminTicket,
  useUpdateAdminTicket,
  type AdminTicketView,
  type TicketPriority,
  type TicketStatus,
} from '../api/hooks'
import { QueryState } from '../shell/QueryState'
import { ConfirmDialog } from './ConfirmDialog'
import { dueState, useTicketLabels } from './ticketLabels'

const fmtDateTime = (iso?: string | null) => (iso ? new Date(iso).toLocaleString() : '—')

/**
 * Dettaglio di una richiesta di assistenza (UC 0034 · UC 0075): filo utente ↔ assistenza con
 * risposta (che porta la richiesta in attesa dell'utente e fa partire l'avviso email) e cambio di
 * stato e priorità.
 *
 * Ops sicure: il contenuto dei messaggi non è mai modificabile, e portare una richiesta a «chiusa»
 * chiede conferma — da lì l'utente non può più rispondere.
 */
export function TicketDetail() {
  const { t } = useTranslation()
  const { id = '' } = useParams()
  const detail = useAdminTicket(id)
  const reply = useAdminReplyTicket()
  const update = useUpdateAdminTicket()
  const labels = useTicketLabels()
  const [body, setBody] = useState('')
  const [actionError, setActionError] = useState<string | null>(null)

  const ticket = detail.data?.ticket
  const thread = detail.data?.thread ?? []
  const closed = ticket?.status === 'closed'
  const due = ticket ? dueState(ticket) : 'none'

  const sendReply = async () => {
    if (!body.trim()) return
    setActionError(null)
    try {
      await reply.mutateAsync({ id, body: body.trim() })
      setBody('')
    } catch {
      setActionError(t('admin.errors.generic'))
    }
  }

  const changeStatus = async (status: TicketStatus, priority: TicketPriority) => {
    setActionError(null)
    try {
      await update.mutateAsync({ id, status, priority })
    } catch {
      setActionError(t('admin.errors.generic'))
    }
  }

  return (
    <div className="space-y-[22px]">
      <div className="flex items-center justify-between">
        <h1 className="text-[22px] font-extrabold tracking-[-0.02em] text-fg">
          {t('admin.tickets.detailTitle')}: {ticket?.subject ?? '…'}
        </h1>
        <Link to="/tickets" className="text-sm text-accent underline-offset-2 hover:underline">
          ← {t('admin.tickets.backToQueue')}
        </Link>
      </div>
      <Card>
        <CardContent className="space-y-4 py-4">
          <QueryState
            isLoading={detail.isLoading}
            isError={detail.isError}
            isEmpty={!detail.data}
            onRetry={() => void detail.refetch()}
          >
            {ticket && (
              <div className="flex flex-wrap items-center gap-4 text-sm text-fg-muted">
                <span>{ticket.accountName ?? ticket.tenantId}</span>
                <span>{t('admin.tickets.colType')}: {labels.type(ticket.type)}</span>
                <span>{t('admin.tickets.colSource')}: {labels.source(ticket.source)}</span>
                <span>{t('admin.tickets.colStatus')}: {labels.status(ticket.status)}</span>
                <span>{t('admin.tickets.colPriority')}: {labels.priority(ticket.priority)}</span>
                {ticket.dueAt && (
                  <span className={due === 'overdue' ? 'font-semibold text-danger' : undefined}>
                    {t('admin.tickets.colDue')}: {fmtDateTime(ticket.dueAt)}
                    {due === 'overdue' && ` — ${t('admin.tickets.overdue')}`}
                    {due === 'dueSoon' && ` — ${t('admin.tickets.dueSoon')}`}
                  </span>
                )}
                {ticket.exportJobId && (
                  <Link
                    to={`/gdpr/exports/${ticket.exportJobId}`}
                    className="text-accent underline-offset-2 hover:underline"
                  >
                    {t('admin.tickets.autoCreated')}
                  </Link>
                )}
                {ticket.logsUrl && (
                  <a
                    href={ticket.logsUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="text-accent underline-offset-2 hover:underline"
                  >
                    {t('admin.tickets.logsLink')}
                  </a>
                )}
              </div>
            )}

            {ticket?.flaggedForReview && (
              <p role="status" className="rounded-md bg-danger/10 px-3 py-2 text-sm text-danger">
                <strong>{t('admin.tickets.flagged')}</strong> — {t('admin.tickets.flaggedHint')}
              </p>
            )}

            <section aria-label={t('admin.tickets.threadHeading')} className="space-y-3">
              {thread.map((message) => (
                <div key={message.id} className="rounded-md border border-line/60 p-3">
                  <p className="text-xs font-medium uppercase tracking-wide text-fg-muted">
                    {labels.author(message.author)} · {fmtDateTime(message.createdAt)}
                  </p>
                  <p className="mt-1 whitespace-pre-wrap text-sm text-fg">{message.body}</p>
                </div>
              ))}
            </section>

            {closed ? (
              <p className="text-sm text-fg-muted">{t('admin.tickets.closedNote')}</p>
            ) : (
              <form
                className="space-y-3"
                onSubmit={(e) => {
                  e.preventDefault()
                  void sendReply()
                }}
              >
                <div className="max-w-lg">
                  <label htmlFor="admin-reply" className="mb-1 block text-sm font-medium text-fg">
                    {t('admin.tickets.replyLabel')}
                  </label>
                  <textarea
                    id="admin-reply"
                    rows={3}
                    maxLength={4000}
                    className="w-full rounded-md border border-line bg-surface px-3 py-2 text-sm"
                    value={body}
                    onChange={(e) => setBody(e.target.value)}
                  />
                  <p className="mt-1 text-xs text-fg-muted">{t('admin.tickets.replyNote')}</p>
                </div>
                <Button type="submit" size="sm" disabled={reply.isPending || !body.trim()}>
                  {t('admin.tickets.replySubmit')}
                </Button>
              </form>
            )}

            {ticket && <StatusForm ticket={ticket} busy={update.isPending} onSubmit={changeStatus} />}

            {actionError && (
              <p role="alert" className="text-sm text-danger">
                {actionError}
              </p>
            )}
          </QueryState>
        </CardContent>
      </Card>
    </div>
  )
}

function StatusForm({
  ticket,
  busy,
  onSubmit,
}: {
  ticket: AdminTicketView
  busy: boolean
  onSubmit: (status: TicketStatus, priority: TicketPriority) => void
}) {
  const { t } = useTranslation()
  const [status, setStatus] = useState<TicketStatus>(ticket.status ?? 'open')
  const [priority, setPriority] = useState<TicketPriority>(ticket.priority ?? 'normal')
  const [confirming, setConfirming] = useState(false)

  const submit = () => {
    // La chiusura è l'unica transizione che l'utente non può disfare: si conferma sempre.
    if (status === 'closed' && ticket.status !== 'closed') {
      setConfirming(true)
      return
    }
    onSubmit(status, priority)
  }

  return (
    <>
      <form
        className="flex flex-wrap items-end gap-3"
        onSubmit={(e) => {
          e.preventDefault()
          submit()
        }}
      >
        <div className="space-y-1">
          <label htmlFor="ticket-status" className="text-sm font-medium text-fg">
            {t('admin.tickets.statusLabel')}
          </label>
          <select
            id="ticket-status"
            className="block rounded-md border border-line bg-surface px-3 py-2 text-sm"
            value={status}
            onChange={(e) => setStatus(e.target.value as TicketStatus)}
          >
            <option value="open">{t('admin.tickets.status.open')}</option>
            <option value="in_progress">{t('admin.tickets.status.in_progress')}</option>
            <option value="waiting_user">{t('admin.tickets.status.waiting_user')}</option>
            <option value="resolved">{t('admin.tickets.status.resolved')}</option>
            <option value="closed">{t('admin.tickets.status.closed')}</option>
          </select>
        </div>
        <div className="space-y-1">
          <label htmlFor="ticket-priority" className="text-sm font-medium text-fg">
            {t('admin.tickets.priorityLabel')}
          </label>
          <select
            id="ticket-priority"
            className="block rounded-md border border-line bg-surface px-3 py-2 text-sm"
            value={priority}
            onChange={(e) => setPriority(e.target.value as TicketPriority)}
          >
            <option value="low">{t('admin.tickets.priority.low')}</option>
            <option value="normal">{t('admin.tickets.priority.normal')}</option>
            <option value="high">{t('admin.tickets.priority.high')}</option>
          </select>
        </div>
        <Button type="submit" size="sm" variant="ghost" disabled={busy}>
          {t('admin.tickets.updateSubmit')}
        </Button>
      </form>
      {confirming && (
        <ConfirmDialog
          title={t('admin.tickets.closeConfirmTitle')}
          body={t('admin.tickets.closeConfirmBody')}
          confirmLabel={t('admin.tickets.closeConfirmAction')}
          busy={busy}
          onConfirm={() => {
            setConfirming(false)
            onSubmit(status, priority)
          }}
          onCancel={() => setConfirming(false)}
        />
      )}
    </>
  )
}
