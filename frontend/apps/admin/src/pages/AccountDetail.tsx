import { Link, useParams } from 'react-router-dom'
import { Badge, Card, CardContent } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useAccountDetail } from '../api/hooks'
import { QueryState } from '../shell/QueryState'
import { EntitledBadge } from './EntitledBadge'

/** Dettaglio account **read-only**: anagrafica + tabella utenti + tabella entitlement derivato. */
export function AccountDetail() {
  const { t } = useTranslation()
  const { id = '' } = useParams()
  const detail = useAccountDetail(id)

  const users = detail.data?.users ?? []
  const entitlements = detail.data?.entitlements ?? []

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Link to="/accounts" className="text-sm text-accent hover:underline">
          ← {t('admin.accounts.title')}
        </Link>
      </div>

      <QueryState
        isLoading={detail.isLoading}
        isError={detail.isError}
        onRetry={() => void detail.refetch()}
      >
        <div className="space-y-6">
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-2xl font-semibold text-fg">{detail.data?.name ?? id}</h1>
            <Badge tone={detail.data?.status === 'active' ? 'success' : 'neutral'}>
              {detail.data?.status ?? '—'}
            </Badge>
          </div>

          <Card>
            <CardContent className="space-y-3 py-4">
              <h2 className="text-lg font-semibold text-fg">{t('admin.accountDetail.usersHeading')}</h2>
              {users.length === 0 ? (
                <p className="text-sm text-fg-muted">{t('states.empty')}</p>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-sm">
                    <thead>
                      <tr className="border-b border-line text-fg-muted">
                        <th scope="col" className="py-2 pr-4 font-medium">{t('admin.users.colEmail')}</th>
                        <th scope="col" className="py-2 pr-4 font-medium">{t('admin.users.colName')}</th>
                        <th scope="col" className="py-2 pr-4 font-medium">{t('admin.users.colRole')}</th>
                        <th scope="col" className="py-2 pr-4 font-medium">{t('admin.users.colStatus')}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {users.map((u) => (
                        <tr key={u.id} className="border-b border-line/60">
                          <td className="py-2 pr-4">{u.email}</td>
                          <td className="py-2 pr-4 text-fg-muted">{u.displayName ?? '—'}</td>
                          <td className="py-2 pr-4">{u.role ?? '—'}</td>
                          <td className="py-2 pr-4">{u.status ?? '—'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardContent className="space-y-3 py-4">
              <h2 className="text-lg font-semibold text-fg">{t('admin.accountDetail.entitlementsHeading')}</h2>
              {entitlements.length === 0 ? (
                <p className="text-sm text-fg-muted">{t('states.empty')}</p>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-sm">
                    <thead>
                      <tr className="border-b border-line text-fg-muted">
                        <th scope="col" className="py-2 pr-4 font-medium">{t('admin.entitlements.colApp')}</th>
                        <th scope="col" className="py-2 pr-4 font-medium">{t('admin.entitlements.colSubscriptionStatus')}</th>
                        <th scope="col" className="py-2 pr-4 font-medium">{t('admin.entitlements.colEntitled')}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {entitlements.map((e) => (
                        <tr key={`${e.appId}`} className="border-b border-line/60">
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
              )}
            </CardContent>
          </Card>
        </div>
      </QueryState>
    </div>
  )
}
