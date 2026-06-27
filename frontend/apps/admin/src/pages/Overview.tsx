import { Card, CardContent } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useOverview } from '../api/hooks'
import { QueryState } from '../shell/QueryState'

function Kpi({ label, value }: { label: string; value: number | undefined }) {
  return (
    <Card>
      <CardContent className="space-y-1 py-6">
        <h2 className="text-sm font-medium text-fg-muted">{label}</h2>
        <p className="text-3xl font-semibold text-fg">{value ?? 0}</p>
      </CardContent>
    </Card>
  )
}

/** Overview di piattaforma: 4 card KPI (account, utenti, subscription attive, app disabilitate). */
export function Overview() {
  const { t } = useTranslation()
  const overview = useOverview()

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold text-fg">{t('admin.overview.title')}</h1>
      <QueryState
        isLoading={overview.isLoading}
        isError={overview.isError}
        onRetry={() => void overview.refetch()}
      >
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Kpi label={t('admin.overview.accounts')} value={overview.data?.accounts} />
          <Kpi label={t('admin.overview.users')} value={overview.data?.users} />
          <Kpi
            label={t('admin.overview.activeSubscriptions')}
            value={overview.data?.activeSubscriptions}
          />
          <Kpi label={t('admin.overview.disabledApps')} value={overview.data?.disabledApps} />
        </div>
      </QueryState>
    </div>
  )
}
