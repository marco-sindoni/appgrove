import { Link } from 'react-router-dom'
import { Badge, Card, CardContent } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useAccounts } from '../api/hooks'
import { QueryState } from '../shell/QueryState'

/** Elenco account (tenant): nome, stato, #utenti, #subscription attive, con link al dettaglio. */
export function Accounts() {
  const { t } = useTranslation()
  const accounts = useAccounts()
  const rows = accounts.data ?? []

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold text-fg">{t('admin.accounts.title')}</h1>
      <Card>
        <CardContent className="py-4">
          <QueryState
            isLoading={accounts.isLoading}
            isError={accounts.isError}
            isEmpty={rows.length === 0}
            onRetry={() => void accounts.refetch()}
          >
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-line text-fg-muted">
                    <th scope="col" className="py-2 pr-4 font-medium">{t('admin.accounts.colName')}</th>
                    <th scope="col" className="py-2 pr-4 font-medium">{t('admin.accounts.colStatus')}</th>
                    <th scope="col" className="py-2 pr-4 font-medium">{t('admin.accounts.colUsers')}</th>
                    <th scope="col" className="py-2 pr-4 font-medium">{t('admin.accounts.colActiveSubscriptions')}</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((a) => (
                    <tr key={a.id} className="border-b border-line/60">
                      <td className="py-2 pr-4">
                        <Link to={`/accounts/${a.id}`} className="text-accent hover:underline">
                          {a.name ?? a.id}
                        </Link>
                      </td>
                      <td className="py-2 pr-4">
                        <Badge tone={a.status === 'active' ? 'success' : 'neutral'}>{a.status ?? '—'}</Badge>
                      </td>
                      <td className="py-2 pr-4 text-fg-muted">{a.users ?? 0}</td>
                      <td className="py-2 pr-4 text-fg-muted">{a.activeSubscriptions ?? 0}</td>
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
