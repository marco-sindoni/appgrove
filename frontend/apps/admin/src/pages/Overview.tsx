import { Card, Icon, PageHeader } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useOverview } from '../api/hooks'
import { QueryState } from '../shell/QueryState'

/* KPI del mockup: riquadro icona tinto 34px, valore mono 23px/800, label attenuata. */
function Kpi({
  label,
  value,
  icon,
  tint,
}: {
  label: string
  value: number | undefined
  icon: string
  tint: string
}) {
  return (
    <Card className="p-[18px]">
      <span
        aria-hidden
        className={`mb-3.5 flex h-[34px] w-[34px] items-center justify-center rounded-[10px] ${tint}`}
      >
        <Icon name={icon} size={19} filled />
      </span>
      <p className="font-mono text-[23px] font-extrabold tracking-[-0.02em] text-fg">{value ?? 0}</p>
      <h2 className="mt-0.5 text-[12.5px] font-semibold text-fg-muted">{label}</h2>
    </Card>
  )
}

/** Overview di piattaforma: 4 card KPI (account, utenti, subscription attive, app disabilitate). */
export function Overview() {
  const { t } = useTranslation()
  const overview = useOverview()

  return (
    <div className="space-y-[22px]">
      <PageHeader title={t('admin.overview.title')} />
      <QueryState
        isLoading={overview.isLoading}
        isError={overview.isError}
        onRetry={() => void overview.refetch()}
      >
        <div className="grid grid-cols-1 gap-3.5 sm:grid-cols-2 lg:grid-cols-4">
          <Kpi
            label={t('admin.overview.accounts')}
            value={overview.data?.accounts}
            icon="corporate_fare"
            tint="bg-cat-blue/15 text-cat-blue"
          />
          <Kpi
            label={t('admin.overview.users')}
            value={overview.data?.users}
            icon="group"
            tint="bg-cat-violet/15 text-cat-violet"
          />
          <Kpi
            label={t('admin.overview.activeSubscriptions')}
            value={overview.data?.activeSubscriptions}
            icon="workspaces"
            tint="bg-cat-teal/15 text-cat-teal"
          />
          <Kpi
            label={t('admin.overview.disabledApps')}
            value={overview.data?.disabledApps}
            icon="apps"
            tint="bg-cat-amber/15 text-cat-amber"
          />
        </div>
      </QueryState>
    </div>
  )
}
