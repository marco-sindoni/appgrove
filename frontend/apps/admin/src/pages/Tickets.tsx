import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Card, CardContent, PageHeader } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import {
  useAdminTickets,
  type AdminTicketView,
  type TicketPriority,
  type TicketStatus,
  type TicketType,
} from '../api/hooks'
import { QueryState } from '../shell/QueryState'
import { dueState, useTicketLabels } from './ticketLabels'

const fmtDate = (iso?: string | null) => (iso ? new Date(iso).toLocaleDateString() : '—')

const TH =
  'border-b border-line py-2.5 pr-4 text-[11px] font-bold uppercase tracking-[.05em] text-fg-faint'

/**
 * Sezione «Ticket» della console (UC 0075): la coda cross-account delle richieste di assistenza.
 *
 * Prima questa tabella era un riquadro in fondo alla pagina «Diritti GDPR», senza filtro per
 * priorità e senza ordinamento per scadenza. Ora ha una casa propria e mostra a colpo d'occhio le
 * due cose che contano: quale richiesta sta per sforare il termine di legge di un mese, e quale è
 * stata contrassegnata perché il testo potrebbe toccare categorie particolari di dati.
 */
export function Tickets() {
  const { t } = useTranslation()
  const [type, setType] = useState<'' | TicketType>('')
  const [status, setStatus] = useState<'' | TicketStatus>('')
  const [priority, setPriority] = useState<'' | TicketPriority>('')
  const tickets = useAdminTickets({
    type: type || undefined,
    status: status || undefined,
    priority: priority || undefined,
  })
  const rows = tickets.data ?? []
  const labels = useTicketLabels()

  return (
    <div className="space-y-[22px]">
      <PageHeader title={t('admin.tickets.title')} subtitle={t('admin.tickets.subtitle')} />
      <Card>
        <CardContent className="py-4">
          <div className="mb-3 flex flex-wrap items-center gap-3">
            <select
              aria-label={t('admin.tickets.filterType')}
              className="rounded-md border border-line bg-surface-2 px-3 py-1.5 text-sm"
              value={type}
              onChange={(e) => setType(e.target.value as '' | TicketType)}
            >
              <option value="">{t('admin.tickets.filterAll')}</option>
              <option value="support">{t('admin.tickets.type.support')}</option>
              <option value="privacy">{t('admin.tickets.type.privacy')}</option>
            </select>
            <select
              aria-label={t('admin.tickets.filterStatus')}
              className="rounded-md border border-line bg-surface-2 px-3 py-1.5 text-sm"
              value={status}
              onChange={(e) => setStatus(e.target.value as '' | TicketStatus)}
            >
              <option value="">{t('admin.tickets.filterAll')}</option>
              <option value="open">{t('admin.tickets.status.open')}</option>
              <option value="in_progress">{t('admin.tickets.status.in_progress')}</option>
              <option value="waiting_user">{t('admin.tickets.status.waiting_user')}</option>
              <option value="resolved">{t('admin.tickets.status.resolved')}</option>
              <option value="closed">{t('admin.tickets.status.closed')}</option>
            </select>
            <select
              aria-label={t('admin.tickets.filterPriority')}
              className="rounded-md border border-line bg-surface-2 px-3 py-1.5 text-sm"
              value={priority}
              onChange={(e) => setPriority(e.target.value as '' | TicketPriority)}
            >
              <option value="">{t('admin.tickets.filterAll')}</option>
              <option value="high">{t('admin.tickets.priority.high')}</option>
              <option value="normal">{t('admin.tickets.priority.normal')}</option>
              <option value="low">{t('admin.tickets.priority.low')}</option>
            </select>
          </div>
          <QueryState
            isLoading={tickets.isLoading}
            isError={tickets.isError}
            isEmpty={rows.length === 0}
            emptyLabel={t('admin.tickets.empty')}
            onRetry={() => void tickets.refetch()}
          >
            <div className="overflow-x-auto">
              <table className="w-full text-left text-[13px]">
                <thead>
                  <tr>
                    <th scope="col" className={TH}>{t('admin.tickets.colSubject')}</th>
                    <th scope="col" className={TH}>{t('admin.tickets.colAccount')}</th>
                    <th scope="col" className={TH}>{t('admin.tickets.colType')}</th>
                    <th scope="col" className={TH}>{t('admin.tickets.colSource')}</th>
                    <th scope="col" className={TH}>{t('admin.tickets.colPriority')}</th>
                    <th scope="col" className={TH}>{t('admin.tickets.colStatus')}</th>
                    <th scope="col" className={TH}>{t('admin.tickets.colDue')}</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((ticket) => (
                    <TicketRow key={ticket.id} ticket={ticket} labels={labels} />
                  ))}
                </tbody>
              </table>
            </div>
          </QueryState>
        </CardContent>
      </Card>
    </div>
  )
}

function TicketRow({
  ticket,
  labels,
}: {
  ticket: AdminTicketView
  labels: ReturnType<typeof useTicketLabels>
}) {
  const { t } = useTranslation()
  const due = dueState(ticket)

  return (
    <tr className="border-b border-line last:border-b-0">
      <td className="py-2 pr-4">
        <Link
          to={`/tickets/${ticket.id}`}
          className="text-accent underline-offset-2 hover:underline"
        >
          {ticket.subject}
        </Link>
        {ticket.flaggedForReview && (
          <span
            title={t('admin.tickets.flaggedHint')}
            className="ml-2 rounded-full bg-danger/10 px-2 py-0.5 text-[11px] font-semibold text-danger"
          >
            {t('admin.tickets.flagged')}
          </span>
        )}
      </td>
      <td className="py-2 pr-4">{ticket.accountName ?? ticket.tenantId ?? '—'}</td>
      <td className="py-2 pr-4 text-fg-muted">{labels.type(ticket.type)}</td>
      <td className="py-2 pr-4 text-fg-muted">{labels.source(ticket.source)}</td>
      <td className="py-2 pr-4 text-fg-muted">{labels.priority(ticket.priority)}</td>
      <td className="py-2 pr-4">{labels.status(ticket.status)}</td>
      <td className="py-2 pr-4 text-fg-muted">
        {fmtDate(ticket.dueAt)}
        {due !== 'none' && (
          <span
            className={`ml-2 rounded-full px-2 py-0.5 text-[11px] font-semibold ${
              due === 'overdue' ? 'bg-danger/10 text-danger' : 'bg-warning/10 text-warning'
            }`}
          >
            {due === 'overdue' ? t('admin.tickets.overdue') : t('admin.tickets.dueSoon')}
          </span>
        )}
      </td>
    </tr>
  )
}
