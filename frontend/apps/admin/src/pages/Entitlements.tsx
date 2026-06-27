import { Card, CardContent } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useEntitlements } from '../api/hooks'
import { QueryState } from '../shell/QueryState'
import { EntitledBadge } from './EntitledBadge'

/** Matrice entitlement: tenant, app, subscription status, entitled sì/no (evidenziato con Badge). */
export function Entitlements() {
  const { t } = useTranslation()
  const entitlements = useEntitlements()
  const rows = entitlements.data ?? []

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold text-fg">{t('admin.entitlements.title')}</h1>
      <Card>
        <CardContent className="py-4">
          <QueryState
            isLoading={entitlements.isLoading}
            isError={entitlements.isError}
            isEmpty={rows.length === 0}
            onRetry={() => void entitlements.refetch()}
          >
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-line text-fg-muted">
                    <th scope="col" className="py-2 pr-4 font-medium">{t('admin.entitlements.colTenant')}</th>
                    <th scope="col" className="py-2 pr-4 font-medium">{t('admin.entitlements.colApp')}</th>
                    <th scope="col" className="py-2 pr-4 font-medium">{t('admin.entitlements.colSubscriptionStatus')}</th>
                    <th scope="col" className="py-2 pr-4 font-medium">{t('admin.entitlements.colEntitled')}</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((e, i) => (
                    <tr key={`${e.tenantId}-${e.appId}-${i}`} className="border-b border-line/60">
                      <td className="py-2 pr-4">{e.tenantName ?? e.tenantId ?? '—'}</td>
                      <td className="py-2 pr-4">{e.appName ?? e.appSlug ?? '—'}</td>
                      <td className="py-2 pr-4 text-fg-muted">{e.subscriptionStatus ?? '—'}</td>
                      <td className="py-2 pr-4">
                        <EntitledBadge entitled={e.entitled} />
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
