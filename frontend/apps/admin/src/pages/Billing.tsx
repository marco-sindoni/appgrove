import {
  Badge,
  PageHeader,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeadCell,
  TableRow,
} from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useBilling } from '../api/hooks'
import { QueryState } from '../shell/QueryState'

const fmtDate = (iso?: string) => (iso ? new Date(iso).toLocaleDateString() : '—')

/* Stato subscription → tono badge (mappa del mockup admin). */
function statusTone(status?: string) {
  if (status === 'active' || status === 'trialing') return 'success' as const
  if (status === 'past_due') return 'danger' as const
  if (status === 'paused' || status === 'scheduled_cancel') return 'warning' as const
  return 'neutral' as const
}

/** Fatturazione (read-only): tenant, app, tier, stato, periodo corrente. */
export function Billing() {
  const { t } = useTranslation()
  const billing = useBilling()
  const rows = billing.data ?? []

  return (
    <div className="space-y-[22px]">
      <PageHeader title={t('admin.billing.title')} />
      <QueryState
        isLoading={billing.isLoading}
        isError={billing.isError}
        isEmpty={rows.length === 0}
        onRetry={() => void billing.refetch()}
      >
        <Table>
          <TableHead>
            <TableRow>
              <TableHeadCell>{t('admin.billing.colTenant')}</TableHeadCell>
              <TableHeadCell>{t('admin.billing.colApp')}</TableHeadCell>
              <TableHeadCell>{t('admin.billing.colTier')}</TableHeadCell>
              <TableHeadCell>{t('admin.billing.colStatus')}</TableHeadCell>
              <TableHeadCell>{t('admin.billing.colPeriod')}</TableHeadCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((b, i) => (
              <TableRow key={`${b.tenantId}-${b.appSlug}-${i}`}>
                <TableCell className="font-semibold">{b.tenantName ?? b.tenantId ?? '—'}</TableCell>
                <TableCell>{b.appName ?? b.appSlug ?? '—'}</TableCell>
                <TableCell className="text-fg-muted">{b.tierKey ?? '—'}</TableCell>
                <TableCell>
                  {b.status ? (
                    <Badge withDot tone={statusTone(b.status)}>
                      {b.status}
                    </Badge>
                  ) : (
                    '—'
                  )}
                </TableCell>
                <TableCell className="font-mono text-xs text-fg-muted">
                  {fmtDate(b.currentPeriodStart)} → {fmtDate(b.currentPeriodEnd)}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </QueryState>
    </div>
  )
}
