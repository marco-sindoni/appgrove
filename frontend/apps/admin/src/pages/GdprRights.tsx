import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Button, Card, CardContent, CardHeader } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import {
  useAdminTickets,
  useApplyRestriction,
  useGdprRequests,
  usePurgeAudit,
  useRemoveRestriction,
  useRestrictions,
  type GdprRequestView,
} from '../api/hooks'
import { QueryState } from '../shell/QueryState'
import { ConfirmDialog } from './ConfirmDialog'
import { Field } from './Field'

const fmtDate = (iso?: string | null) => (iso ? new Date(iso).toLocaleDateString() : '—')


/**
 * Console "Diritti GDPR" (UC 0034, #13 L75): single pane in **aggregazione** su export, recessi
 * per-app, eliminazioni account e ticket privacy, con puntatori all'accessorio (Logs Insights,
 * dettaglio export/ticket); gestione ticket e limitazione del trattamento (art. 18) con prova nel
 * registro. Nessuna impersonation (#03 15): sola lettura + ops sicure.
 */
export function GdprRights() {
  const { t } = useTranslation()

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-fg">{t('admin.gdpr.title')}</h1>
        <p className="mt-1 text-sm text-fg-muted">{t('admin.gdpr.subtitle')}</p>
      </div>
      <RequestsSection />
      <TicketsSection />
      <RestrictionsSection />
      <PurgeAuditSection />
    </div>
  )
}

function RequestsSection() {
  const { t } = useTranslation()
  const [type, setType] = useState<string>('')
  const requests = useGdprRequests(type || undefined)
  const rows = requests.data ?? []
  const typeLabel = (kind?: string) => {
    switch (kind) {
      case 'export':
        return t('admin.gdpr.types.export')
      case 'withdrawal':
        return t('admin.gdpr.types.withdrawal')
      case 'account_deletion':
        return t('admin.gdpr.types.account_deletion')
      case 'privacy_ticket':
        return t('admin.gdpr.types.privacy_ticket')
      default:
        return kind ?? '—'
    }
  }

  return (
    <Card>
      <CardContent className="py-4">
        <div className="mb-3 flex items-center gap-3">
          <label htmlFor="gdpr-type-filter" className="text-sm font-medium text-fg">
            {t('admin.gdpr.filterLabel')}
          </label>
          <select
            id="gdpr-type-filter"
            className="rounded-md border border-line bg-surface px-3 py-1.5 text-sm"
            value={type}
            onChange={(e) => setType(e.target.value)}
          >
            <option value="">{t('admin.gdpr.filterAll')}</option>
            <option value="export">{t('admin.gdpr.types.export')}</option>
            <option value="withdrawal">{t('admin.gdpr.types.withdrawal')}</option>
            <option value="account_deletion">{t('admin.gdpr.types.account_deletion')}</option>
            <option value="privacy_ticket">{t('admin.gdpr.types.privacy_ticket')}</option>
          </select>
        </div>
        <QueryState
          isLoading={requests.isLoading}
          isError={requests.isError}
          isEmpty={rows.length === 0}
          onRetry={() => void requests.refetch()}
        >
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-line text-fg-muted">
                  <th scope="col" className="py-2 pr-4 font-medium">{t('admin.gdpr.colType')}</th>
                  <th scope="col" className="py-2 pr-4 font-medium">{t('admin.gdpr.colAccount')}</th>
                  <th scope="col" className="py-2 pr-4 font-medium">{t('admin.gdpr.colApp')}</th>
                  <th scope="col" className="py-2 pr-4 font-medium">{t('admin.gdpr.colStatus')}</th>
                  <th scope="col" className="py-2 pr-4 font-medium">{t('admin.gdpr.colRequested')}</th>
                  <th scope="col" className="py-2 pr-4 font-medium">{t('admin.gdpr.colDue')}</th>
                  <th scope="col" className="py-2 pr-4 font-medium">{t('admin.gdpr.colCompleted')}</th>
                  <th scope="col" className="py-2 pr-4 font-medium">{t('admin.gdpr.colLinks')}</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((request, i) => (
                  <tr key={`${request.type}-${request.refId}-${i}`} className="border-b border-line/60">
                    <td className="py-2 pr-4">{typeLabel(request.type)}</td>
                    <td className="py-2 pr-4">{request.accountName ?? request.tenantId ?? '—'}</td>
                    <td className="py-2 pr-4 text-fg-muted">{request.appId ?? '—'}</td>
                    <td className="py-2 pr-4">{request.status ?? '—'}</td>
                    <td className="py-2 pr-4 text-fg-muted">{fmtDate(request.requestedAt)}</td>
                    <td className="py-2 pr-4 text-fg-muted">{fmtDate(request.dueAt)}</td>
                    <td className="py-2 pr-4 text-fg-muted">{fmtDate(request.completedAt)}</td>
                    <td className="py-2 pr-4">
                      <RequestLinks request={request} />
                    </td>
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

function RequestLinks({ request }: { request: GdprRequestView }) {
  const { t } = useTranslation()
  const detailTo =
    request.type === 'export'
      ? `/gdpr/exports/${request.refId}`
      : request.type === 'privacy_ticket'
        ? `/gdpr/tickets/${request.refId}`
        : null
  return (
    <span className="flex items-center gap-3">
      {detailTo && (
        <Link to={detailTo} className="text-accent underline-offset-2 hover:underline">
          {t('admin.gdpr.detailLink')}
        </Link>
      )}
      {request.logsUrl && (
        <a
          href={request.logsUrl}
          target="_blank"
          rel="noreferrer"
          className="text-accent underline-offset-2 hover:underline"
        >
          {t('admin.gdpr.logsLink')}
        </a>
      )}
    </span>
  )
}

function TicketsSection() {
  const { t } = useTranslation()
  const [type, setType] = useState<'' | 'support' | 'privacy'>('')
  const [status, setStatus] = useState<'' | 'open' | 'in_progress' | 'resolved' | 'closed'>('')
  const tickets = useAdminTickets({ type: type || undefined, status: status || undefined })
  const rows = tickets.data ?? []

  return (
    <Card>
      <CardHeader title={t('admin.gdpr.ticketsHeading')} />
      <CardContent className="py-4">
        <div className="mb-3 flex flex-wrap items-center gap-3">
          <select
            aria-label={t('admin.gdpr.colType')}
            className="rounded-md border border-line bg-surface px-3 py-1.5 text-sm"
            value={type}
            onChange={(e) => setType(e.target.value as '' | 'support' | 'privacy')}
          >
            <option value="">{t('admin.gdpr.filterAll')}</option>
            <option value="support">{t('admin.gdpr.ticketType.support')}</option>
            <option value="privacy">{t('admin.gdpr.ticketType.privacy')}</option>
          </select>
          <select
            aria-label={t('admin.gdpr.colStatus')}
            className="rounded-md border border-line bg-surface px-3 py-1.5 text-sm"
            value={status}
            onChange={(e) => setStatus(e.target.value as '' | 'open' | 'in_progress' | 'resolved' | 'closed')}
          >
            <option value="">{t('admin.gdpr.filterAll')}</option>
            <option value="open">{t('admin.gdpr.status.open')}</option>
            <option value="in_progress">{t('admin.gdpr.status.in_progress')}</option>
            <option value="resolved">{t('admin.gdpr.status.resolved')}</option>
            <option value="closed">{t('admin.gdpr.status.closed')}</option>
          </select>
        </div>
        <QueryState
          isLoading={tickets.isLoading}
          isError={tickets.isError}
          isEmpty={rows.length === 0}
          onRetry={() => void tickets.refetch()}
        >
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-line text-fg-muted">
                  <th scope="col" className="py-2 pr-4 font-medium">{t('admin.gdpr.colSubject')}</th>
                  <th scope="col" className="py-2 pr-4 font-medium">{t('admin.gdpr.colAccount')}</th>
                  <th scope="col" className="py-2 pr-4 font-medium">{t('admin.gdpr.colType')}</th>
                  <th scope="col" className="py-2 pr-4 font-medium">{t('admin.gdpr.colPriority')}</th>
                  <th scope="col" className="py-2 pr-4 font-medium">{t('admin.gdpr.colStatus')}</th>
                  <th scope="col" className="py-2 pr-4 font-medium">{t('admin.gdpr.colDue')}</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((ticket) => (
                  <tr key={ticket.id} className="border-b border-line/60">
                    <td className="py-2 pr-4">
                      <Link
                        to={`/gdpr/tickets/${ticket.id}`}
                        className="text-accent underline-offset-2 hover:underline"
                      >
                        {ticket.subject}
                      </Link>
                    </td>
                    <td className="py-2 pr-4">{ticket.accountName ?? ticket.tenantId ?? '—'}</td>
                    <td className="py-2 pr-4 text-fg-muted">{ticket.type}</td>
                    <td className="py-2 pr-4 text-fg-muted">{ticket.priority}</td>
                    <td className="py-2 pr-4">{ticket.status}</td>
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

function RestrictionsSection() {
  const { t } = useTranslation()
  const restrictions = useRestrictions()
  const apply = useApplyRestriction()
  const remove = useRemoveRestriction()
  const [form, setForm] = useState({ targetKind: 'account' as 'account' | 'user', targetId: '', ticketId: '', note: '' })
  const [confirmApply, setConfirmApply] = useState(false)
  const [confirmRemove, setConfirmRemove] = useState<{ targetKind: string; targetId: string } | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const active = restrictions.data?.active ?? []
  const trail = restrictions.data?.auditTrail ?? []

  const doApply = async () => {
    setActionError(null)
    try {
      await apply.mutateAsync({
        targetKind: form.targetKind,
        targetId: form.targetId.trim(),
        ...(form.ticketId.trim() ? { ticketId: form.ticketId.trim() } : {}),
        ...(form.note.trim() ? { note: form.note.trim() } : {}),
      })
      setForm({ targetKind: 'account', targetId: '', ticketId: '', note: '' })
    } catch {
      setActionError(t('admin.errors.generic'))
    } finally {
      setConfirmApply(false)
    }
  }

  const doRemove = async () => {
    if (!confirmRemove) return
    setActionError(null)
    try {
      await remove.mutateAsync(confirmRemove)
    } catch {
      setActionError(t('admin.errors.generic'))
    } finally {
      setConfirmRemove(null)
    }
  }

  return (
    <Card>
      <CardHeader title={t('admin.gdpr.restrictionsHeading')} />
      <CardContent className="space-y-4 py-4">
        <p className="text-sm text-fg-muted">{t('admin.gdpr.restrictionsBody')}</p>

        <QueryState
          isLoading={restrictions.isLoading}
          isError={restrictions.isError}
          isEmpty={false}
          onRetry={() => void restrictions.refetch()}
        >
          {active.length === 0 ? (
            <p className="text-sm text-fg-muted">{t('admin.gdpr.restrictionsEmpty')}</p>
          ) : (
            <ul className="space-y-2">
              {active.map((restriction) => (
                <li
                  key={`${restriction.targetKind}-${restriction.targetId}`}
                  className="flex items-center justify-between rounded-md border border-line/60 px-3 py-2 text-sm"
                >
                  <span>
                    <span className="font-medium">{restriction.label ?? restriction.targetId}</span>{' '}
                    <span className="text-fg-muted">
                      ({restriction.targetKind === 'account'
                        ? t('admin.gdpr.restrictionKindAccount')
                        : t('admin.gdpr.restrictionKindUser')}{' '}
                      · {restriction.targetId})
                    </span>
                  </span>
                  <Button
                    type="button"
                    size="sm"
                    variant="ghost"
                    onClick={() =>
                      setConfirmRemove({
                        targetKind: restriction.targetKind ?? 'account',
                        targetId: restriction.targetId ?? '',
                      })
                    }
                  >
                    {t('admin.gdpr.restrictionRemove')}
                  </Button>
                </li>
              ))}
            </ul>
          )}
        </QueryState>

        <form
          className="grid max-w-2xl gap-3 sm:grid-cols-2"
          onSubmit={(e) => {
            e.preventDefault()
            if (form.targetId.trim()) setConfirmApply(true)
          }}
        >
          <div className="space-y-1">
            <label htmlFor="restriction-kind" className="text-sm font-medium text-fg">
              {t('admin.gdpr.restrictionKind')}
            </label>
            <select
              id="restriction-kind"
              className="w-full rounded-md border border-line bg-surface px-3 py-2 text-sm"
              value={form.targetKind}
              onChange={(e) => setForm({ ...form, targetKind: e.target.value as 'account' | 'user' })}
            >
              <option value="account">{t('admin.gdpr.restrictionKindAccount')}</option>
              <option value="user">{t('admin.gdpr.restrictionKindUser')}</option>
            </select>
          </div>
          <Field
            id="restriction-target"
            label={t('admin.gdpr.restrictionTargetId')}
            value={form.targetId}
            onChange={(e) => setForm({ ...form, targetId: e.target.value })}
          />
          <Field
            id="restriction-ticket"
            label={t('admin.gdpr.restrictionTicketId')}
            value={form.ticketId}
            onChange={(e) => setForm({ ...form, ticketId: e.target.value })}
          />
          <Field
            id="restriction-note"
            label={t('admin.gdpr.restrictionNote')}
            value={form.note}
            onChange={(e) => setForm({ ...form, note: e.target.value })}
          />
          <div className="sm:col-span-2">
            <Button type="submit" size="sm" disabled={apply.isPending || !form.targetId.trim()}>
              {t('admin.gdpr.restrictionApply')}
            </Button>
          </div>
        </form>
        {actionError && (
          <p role="alert" className="text-sm text-danger">
            {actionError}
          </p>
        )}

        {trail.length > 0 && (
          <div className="overflow-x-auto">
            <h3 className="mb-2 text-sm font-semibold text-fg">{t('admin.gdpr.auditHeading')}</h3>
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-line text-fg-muted">
                  <th scope="col" className="py-2 pr-4 font-medium">{t('admin.gdpr.colWhen')}</th>
                  <th scope="col" className="py-2 pr-4 font-medium">{t('admin.gdpr.colTarget')}</th>
                  <th scope="col" className="py-2 pr-4 font-medium">{t('admin.gdpr.colAction')}</th>
                  <th scope="col" className="py-2 pr-4 font-medium">{t('admin.gdpr.colActor')}</th>
                </tr>
              </thead>
              <tbody>
                {trail.map((entry) => (
                  <tr key={entry.id} className="border-b border-line/60">
                    <td className="py-2 pr-4 text-fg-muted">{fmtDate(entry.executedAt)}</td>
                    <td className="py-2 pr-4">
                      {entry.targetKind} · {entry.targetId}
                    </td>
                    <td className="py-2 pr-4">{entry.action}</td>
                    <td className="py-2 pr-4 text-fg-muted">{entry.actor}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {confirmApply && (
          <ConfirmDialog
            title={t('admin.gdpr.restrictionConfirmTitle')}
            body={t('admin.gdpr.restrictionConfirmBody')}
            confirmLabel={t('admin.gdpr.restrictionApply')}
            tone="default"
            busy={apply.isPending}
            onConfirm={() => void doApply()}
            onCancel={() => setConfirmApply(false)}
          />
        )}
        {confirmRemove && (
          <ConfirmDialog
            title={t('admin.gdpr.restrictionRemoveConfirmTitle')}
            body={t('admin.gdpr.restrictionRemoveConfirmBody')}
            confirmLabel={t('admin.gdpr.restrictionRemove')}
            tone="default"
            busy={remove.isPending}
            onConfirm={() => void doRemove()}
            onCancel={() => setConfirmRemove(null)}
          />
        )}
      </CardContent>
    </Card>
  )
}

function PurgeAuditSection() {
  const { t } = useTranslation()
  const audit = usePurgeAudit()
  const rows = audit.data ?? []

  return (
    <Card>
      <CardHeader title={t('admin.gdpr.purgeAuditHeading')} />
      <CardContent className="py-4">
        <QueryState
          isLoading={audit.isLoading}
          isError={audit.isError}
          isEmpty={rows.length === 0}
          onRetry={() => void audit.refetch()}
        >
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-line text-fg-muted">
                  <th scope="col" className="py-2 pr-4 font-medium">{t('admin.gdpr.colWhen')}</th>
                  <th scope="col" className="py-2 pr-4 font-medium">{t('admin.gdpr.colAccount')}</th>
                  <th scope="col" className="py-2 pr-4 font-medium">{t('admin.gdpr.colApp')}</th>
                  <th scope="col" className="py-2 pr-4 font-medium">{t('admin.gdpr.colReason')}</th>
                  <th scope="col" className="py-2 pr-4 font-medium">{t('admin.gdpr.colDeleted')}</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((entry) => (
                  <tr key={entry.id} className="border-b border-line/60">
                    <td className="py-2 pr-4 text-fg-muted">{fmtDate(entry.executedAt)}</td>
                    <td className="py-2 pr-4">{entry.tenantId}</td>
                    <td className="py-2 pr-4 text-fg-muted">{entry.appId}</td>
                    <td className="py-2 pr-4 text-fg-muted">{entry.reason}</td>
                    <td className="py-2 pr-4">{entry.total}</td>
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
