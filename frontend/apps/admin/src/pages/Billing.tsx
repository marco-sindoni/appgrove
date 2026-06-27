import { Card, CardContent } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useBilling } from '../api/hooks'
import { QueryState } from '../shell/QueryState'

const fmtDate = (iso?: string) => (iso ? new Date(iso).toLocaleDateString() : '—')

/** Fatturazione (read-only): tenant, app, tier, stato, periodo corrente. */
export function Billing() {
  const { t } = useTranslation()
  const billing = useBilling()
  const rows = billing.data ?? []

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold text-fg">{t('admin.billing.title')}</h1>
      <Card>
        <CardContent className="py-4">
          <QueryState
            isLoading={billing.isLoading}
            isError={billing.isError}
            isEmpty={rows.length === 0}
            onRetry={() => void billing.refetch()}
          >
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-line text-fg-muted">
                    <th scope="col" className="py-2 pr-4 font-medium">{t('admin.billing.colTenant')}</th>
                    <th scope="col" className="py-2 pr-4 font-medium">{t('admin.billing.colApp')}</th>
                    <th scope="col" className="py-2 pr-4 font-medium">{t('admin.billing.colTier')}</th>
                    <th scope="col" className="py-2 pr-4 font-medium">{t('admin.billing.colStatus')}</th>
                    <th scope="col" className="py-2 pr-4 font-medium">{t('admin.billing.colPeriod')}</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((b, i) => (
                    <tr key={`${b.tenantId}-${b.appSlug}-${i}`} className="border-b border-line/60">
                      <td className="py-2 pr-4">{b.tenantName ?? b.tenantId ?? '—'}</td>
                      <td className="py-2 pr-4">{b.appName ?? b.appSlug ?? '—'}</td>
                      <td className="py-2 pr-4 text-fg-muted">{b.tierKey ?? '—'}</td>
                      <td className="py-2 pr-4">{b.status ?? '—'}</td>
                      <td className="py-2 pr-4 text-fg-muted">
                        {fmtDate(b.currentPeriodStart)} → {fmtDate(b.currentPeriodEnd)}
                      </td>
                    </tr>
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
