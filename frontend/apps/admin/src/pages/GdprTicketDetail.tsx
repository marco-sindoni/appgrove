import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Button, Card, CardContent } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useAdminReplyTicket, useAdminTicket, useUpdateAdminTicket } from '../api/hooks'
import { QueryState } from '../shell/QueryState'

const fmtDateTime = (iso?: string | null) => (iso ? new Date(iso).toLocaleString() : '—')

/**
 * Dettaglio ticket (UC 0034, #13 D21): thread utente↔admin con risposta (notifica email lato
 * core) e cambio stato/priorità — ops sicure: il contenuto dei messaggi non è mai editabile.
 */
export function GdprTicketDetail() {
  const { t } = useTranslation()
  const { id = '' } = useParams()
  const detail = useAdminTicket(id)
  const reply = useAdminReplyTicket()
  const update = useUpdateAdminTicket()
  const [body, setBody] = useState('')
  const [actionError, setActionError] = useState<string | null>(null)

  const ticket = detail.data?.ticket
  const thread = detail.data?.thread ?? []
  const closed = ticket?.status === 'closed'

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

  const changeStatus = async (status: string, priority: string) => {
    setActionError(null)
    try {
      await update.mutateAsync({
        id,
        status: status as 'open' | 'in_progress' | 'resolved' | 'closed',
        priority: priority as 'low' | 'normal' | 'high',
      })
    } catch {
      setActionError(t('admin.errors.generic'))
    }
  }

  return (
    <div className="space-y-[22px]">
      <div className="flex items-center justify-between">
        <h1 className="text-[22px] font-extrabold tracking-[-0.02em] text-fg">
          {t('admin.gdpr.ticketDetailTitle')}: {ticket?.subject ?? '…'}
        </h1>
        <Link to="/gdpr" className="text-sm text-accent underline-offset-2 hover:underline">
          ← {t('admin.gdpr.title')}
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
                <span>{t('admin.gdpr.colType')}: {ticket.type}</span>
                <span>{t('admin.gdpr.colStatus')}: {ticket.status}</span>
                <span>{t('admin.gdpr.colPriority')}: {ticket.priority}</span>
                {ticket.dueAt && <span>{t('admin.gdpr.colDue')}: {fmtDateTime(ticket.dueAt)}</span>}
                {ticket.exportJobId && (
                  <Link
                    to={`/gdpr/exports/${ticket.exportJobId}`}
                    className="text-accent underline-offset-2 hover:underline"
                  >
                    {t('admin.gdpr.autoCreated')}
                  </Link>
                )}
                {ticket.logsUrl && (
                  <a
                    href={ticket.logsUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="text-accent underline-offset-2 hover:underline"
                  >
                    {t('admin.gdpr.logsLink')}
                  </a>
                )}
              </div>
            )}

            <section aria-label={t('admin.gdpr.threadHeading')} className="space-y-3">
              {thread.map((message) => (
                <div key={message.id} className="rounded-md border border-line/60 p-3">
                  <p className="text-xs font-medium uppercase tracking-wide text-fg-muted">
                    {message.author === 'admin'
                      ? t('admin.gdpr.author.admin')
                      : message.author === 'system'
                        ? t('admin.gdpr.author.system')
                        : t('admin.gdpr.author.user')}{' '}
                    · {fmtDateTime(message.createdAt)}
                  </p>
                  <p className="mt-1 whitespace-pre-wrap text-sm text-fg">{message.body}</p>
                </div>
              ))}
            </section>

            {!closed && (
              <form
                className="space-y-3"
                onSubmit={(e) => {
                  e.preventDefault()
                  void sendReply()
                }}
              >
                <div className="max-w-lg">
                  <label htmlFor="admin-reply" className="mb-1 block text-sm font-medium text-fg">
                    {t('admin.gdpr.replyLabel')}
                  </label>
                  <textarea
                    id="admin-reply"
                    rows={3}
                    maxLength={4000}
                    className="w-full rounded-md border border-line bg-surface px-3 py-2 text-sm"
                    value={body}
                    onChange={(e) => setBody(e.target.value)}
                  />
                </div>
                <Button type="submit" size="sm" disabled={reply.isPending || !body.trim()}>
                  {t('admin.gdpr.replySubmit')}
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
  ticket: { status?: string; priority?: string }
  busy: boolean
  onSubmit: (status: string, priority: string) => void
}) {
  const { t } = useTranslation()
  const [status, setStatus] = useState(ticket.status ?? 'open')
  const [priority, setPriority] = useState(ticket.priority ?? 'normal')

  return (
    <form
      className="flex flex-wrap items-end gap-3"
      onSubmit={(e) => {
        e.preventDefault()
        onSubmit(status, priority)
      }}
    >
      <div className="space-y-1">
        <label htmlFor="ticket-status" className="text-sm font-medium text-fg">
          {t('admin.gdpr.statusLabel')}
        </label>
        <select
          id="ticket-status"
          className="block rounded-md border border-line bg-surface px-3 py-2 text-sm"
          value={status}
          onChange={(e) => setStatus(e.target.value)}
        >
          <option value="open">{t('admin.gdpr.status.open')}</option>
          <option value="in_progress">{t('admin.gdpr.status.in_progress')}</option>
          <option value="resolved">{t('admin.gdpr.status.resolved')}</option>
          <option value="closed">{t('admin.gdpr.status.closed')}</option>
        </select>
      </div>
      <div className="space-y-1">
        <label htmlFor="ticket-priority" className="text-sm font-medium text-fg">
          {t('admin.gdpr.priorityLabel')}
        </label>
        <select
          id="ticket-priority"
          className="block rounded-md border border-line bg-surface px-3 py-2 text-sm"
          value={priority}
          onChange={(e) => setPriority(e.target.value)}
        >
          <option value="low">{t('admin.gdpr.priority.low')}</option>
          <option value="normal">{t('admin.gdpr.priority.normal')}</option>
          <option value="high">{t('admin.gdpr.priority.high')}</option>
        </select>
      </div>
      <Button type="submit" size="sm" variant="ghost" disabled={busy}>
        {t('admin.gdpr.updateSubmit')}
      </Button>
    </form>
  )
}
