import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Button, Card, CardContent, CardHeader, Input } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { QueryState } from '../../shell/QueryState'
import { useOpenTicket, useReplyTicket, useTicket, useTickets, type TicketView } from './supportApi'

const fmtDate = (iso?: string) => (iso ? new Date(iso).toLocaleDateString() : '—')

/**
 * "Supporto" (UC 0034, #13 D21): ticketing in-house lato utente — apri un ticket (supporto o
 * privacy), segui i tuoi e rispondi nel thread. Aperta a ogni ruolo del tenant e raggiungibile
 * anche a subscription scaduta (#09 F31): è il canale per esercitare i diritti.
 */
export function SupportPage() {
  const { t } = useTranslation()
  const [selected, setSelected] = useState<string | null>(null)

  return (
    <div className="space-y-[22px]">
      <div>
        <h1 className="text-[27px] font-extrabold tracking-[-0.025em] text-fg">{t('support.title')}</h1>
        <p className="mt-1 text-sm text-fg-muted">{t('support.subtitle')}</p>
      </div>
      {selected ? (
        <TicketDetail id={selected} onBack={() => setSelected(null)} />
      ) : (
        <>
          <OpenTicketSection onOpened={(id) => setSelected(id)} />
          <TicketListSection onSelect={setSelected} />
        </>
      )}
    </div>
  )
}

type OpenTicketForm = { type: 'support' | 'privacy'; subject: string; message: string }

function OpenTicketSection({ onOpened }: { onOpened: (id: string) => void }) {
  const { t } = useTranslation()
  const open = useOpenTicket()
  const { register, handleSubmit, reset } = useForm<OpenTicketForm>({
    defaultValues: { type: 'support', subject: '', message: '' },
  })

  return (
    <Card>
      <CardHeader title={t('support.openHeading')} />
      <CardContent className="py-4">
        <form
          className="space-y-4"
          onSubmit={handleSubmit((values) =>
            open.mutateAsync(values).then((ticket) => {
              reset()
              if (ticket.id) onOpened(ticket.id)
            }),
          )}
        >
          <div className="max-w-xs">
            <label className="mb-1 block text-sm font-medium text-fg" htmlFor="ticket-type">
              {t('support.typeLabel')}
            </label>
            <select
              id="ticket-type"
              className="w-full rounded-md border border-line bg-surface px-3 py-2 text-sm"
              {...register('type')}
            >
              <option value="support">{t('support.typeSupport')}</option>
              <option value="privacy">{t('support.typePrivacy')}</option>
            </select>
          </div>
          <div className="max-w-lg">
            <label className="mb-1 block text-sm font-medium text-fg" htmlFor="ticket-subject">
              {t('support.subjectLabel')}
            </label>
            <Input
              id="ticket-subject"
              maxLength={200}
              {...register('subject', { required: true })}
            />
          </div>
          <div className="max-w-lg">
            <label className="mb-1 block text-sm font-medium text-fg" htmlFor="ticket-message">
              {t('support.messageLabel')}
            </label>
            <textarea
              id="ticket-message"
              rows={4}
              maxLength={4000}
              className="w-full rounded-md border border-line bg-surface px-3 py-2 text-sm"
              {...register('message', { required: true })}
            />
          </div>
          <Button type="submit" size="sm" disabled={open.isPending}>
            {t('support.submit')}
          </Button>
          {open.isError && (
            <p role="alert" className="text-sm text-danger">
              {t('errors.generic')}
            </p>
          )}
        </form>
      </CardContent>
    </Card>
  )
}

function TicketListSection({ onSelect }: { onSelect: (id: string) => void }) {
  const { t } = useTranslation()
  const tickets = useTickets()
  const rows = tickets.data ?? []
  const { statusLabel, typeLabel } = useTicketLabels()

  return (
    <Card>
      <CardHeader title={t('support.listHeading')} />
      <CardContent className="py-4">
        <QueryState
          isLoading={tickets.isLoading}
          isError={tickets.isError}
          isEmpty={rows.length === 0}
          onRetry={() => void tickets.refetch()}
        >
          <div className="overflow-x-auto">
            <table className="w-full text-left text-[13px]">
              <thead>
                <tr>
                  <th scope="col" className="border-b border-line py-2.5 pr-4 text-[11px] font-bold uppercase tracking-[.05em] text-fg-faint">{t('support.colSubject')}</th>
                  <th scope="col" className="border-b border-line py-2.5 pr-4 text-[11px] font-bold uppercase tracking-[.05em] text-fg-faint">{t('support.colType')}</th>
                  <th scope="col" className="border-b border-line py-2.5 pr-4 text-[11px] font-bold uppercase tracking-[.05em] text-fg-faint">{t('support.colStatus')}</th>
                  <th scope="col" className="border-b border-line py-2.5 pr-4 text-[11px] font-bold uppercase tracking-[.05em] text-fg-faint">{t('support.colOpened')}</th>
                  <th scope="col" className="border-b border-line py-2.5 pr-4 text-[11px] font-bold uppercase tracking-[.05em] text-fg-faint">{t('support.colDue')}</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((ticket) => (
                  <tr key={ticket.id} className="border-b border-line last:border-b-0">
                    <td className="py-2 pr-4">
                      <button
                        type="button"
                        className="text-accent underline-offset-2 hover:underline"
                        onClick={() => ticket.id && onSelect(ticket.id)}
                      >
                        {ticket.subject}
                      </button>
                    </td>
                    <td className="py-2 pr-4 text-fg-muted">{typeLabel(ticket)}</td>
                    <td className="py-2 pr-4">{statusLabel(ticket.status)}</td>
                    <td className="py-2 pr-4 text-fg-muted">{fmtDate(ticket.createdAt)}</td>
                    <td className="py-2 pr-4 text-fg-muted">{fmtDate(ticket.dueAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </QueryState>
      </CardContent>
    </Card>
  )
}

function TicketDetail({ id, onBack }: { id: string; onBack: () => void }) {
  const { t } = useTranslation()
  const detail = useTicket(id)
  const reply = useReplyTicket()
  const { register, handleSubmit, reset } = useForm<{ body: string }>({
    defaultValues: { body: '' },
  })
  const { statusLabel, typeLabel, authorLabel } = useTicketLabels()
  const ticket = detail.data?.ticket
  const closed = ticket?.status === 'closed'

  return (
    <Card>
      <CardHeader title={ticket?.subject ?? t('support.title')} />
      <CardContent className="space-y-4 py-4">
        <Button type="button" size="sm" variant="ghost" onClick={onBack}>
          {t('support.back')}
        </Button>
        <QueryState
          isLoading={detail.isLoading}
          isError={detail.isError}
          isEmpty={!detail.data}
          onRetry={() => void detail.refetch()}
        >
          {ticket && (
            <p className="text-sm text-fg-muted">
              {statusLabel(ticket.status)} · {typeLabel(ticket)}
              {ticket.dueAt ? ` · ${t('support.colDue')}: ${fmtDate(ticket.dueAt)}` : ''}
            </p>
          )}
          <section aria-label={t('support.threadHeading')} className="space-y-3">
            {(detail.data?.thread ?? []).map((message) => (
              <div key={message.id} className="rounded-md border border-line/60 p-3">
                <p className="text-xs font-medium uppercase tracking-wide text-fg-muted">
                  {authorLabel(message.author)} · {fmtDate(message.createdAt)}
                </p>
                <p className="mt-1 whitespace-pre-wrap text-sm text-fg">{message.body}</p>
              </div>
            ))}
          </section>
          {closed ? (
            <p className="text-sm text-fg-muted">{t('support.closedNote')}</p>
          ) : (
            <form
              className="space-y-3"
              onSubmit={handleSubmit((values) =>
                reply.mutateAsync({ id, body: values.body }).then(() => reset()),
              )}
            >
              <div className="max-w-lg">
                <label className="mb-1 block text-sm font-medium text-fg" htmlFor="ticket-reply">
                  {t('support.replyLabel')}
                </label>
                <textarea
                  id="ticket-reply"
                  rows={3}
                  maxLength={4000}
                  className="w-full rounded-md border border-line bg-surface px-3 py-2 text-sm"
                  {...register('body', { required: true })}
                />
              </div>
              <Button type="submit" size="sm" disabled={reply.isPending}>
                {t('support.replySubmit')}
              </Button>
              {reply.isError && (
                <p role="alert" className="text-sm text-danger">
                  {t('errors.generic')}
                </p>
              )}
            </form>
          )}
        </QueryState>
      </CardContent>
    </Card>
  )
}

function useTicketLabels() {
  const { t } = useTranslation()
  const statusLabel = (status?: string) => {
    switch (status) {
      case 'open':
        return t('support.status.open')
      case 'in_progress':
        return t('support.status.in_progress')
      case 'resolved':
        return t('support.status.resolved')
      case 'closed':
        return t('support.status.closed')
      default:
        return status ?? '—'
    }
  }
  const typeLabel = (ticket: TicketView) =>
    ticket.type === 'privacy' ? t('support.typePrivacy') : t('support.typeSupport')
  const authorLabel = (author?: string) => {
    switch (author) {
      case 'admin':
        return t('support.author.admin')
      case 'system':
        return t('support.author.system')
      default:
        return t('support.author.user')
    }
  }
  return { statusLabel, typeLabel, authorLabel }
}
