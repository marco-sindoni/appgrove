import { Card, CardContent } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useUsers } from '../api/hooks'
import { QueryState } from '../shell/QueryState'

/** Elenco utenti cross-tenant: email, nome, ruolo, stato, tenant. */
export function Users() {
  const { t } = useTranslation()
  const users = useUsers()
  const rows = users.data ?? []

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold text-fg">{t('admin.users.title')}</h1>
      <Card>
        <CardContent className="py-4">
          <QueryState
            isLoading={users.isLoading}
            isError={users.isError}
            isEmpty={rows.length === 0}
            onRetry={() => void users.refetch()}
          >
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-line text-fg-muted">
                    <th scope="col" className="py-2 pr-4 font-medium">{t('admin.users.colEmail')}</th>
                    <th scope="col" className="py-2 pr-4 font-medium">{t('admin.users.colName')}</th>
                    <th scope="col" className="py-2 pr-4 font-medium">{t('admin.users.colRole')}</th>
                    <th scope="col" className="py-2 pr-4 font-medium">{t('admin.users.colStatus')}</th>
                    <th scope="col" className="py-2 pr-4 font-medium">{t('admin.users.colTenant')}</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((u) => (
                    <tr key={u.id} className="border-b border-line/60">
                      <td className="py-2 pr-4">{u.email}</td>
                      <td className="py-2 pr-4 text-fg-muted">{u.displayName ?? '—'}</td>
                      <td className="py-2 pr-4">{u.role ?? '—'}</td>
                      <td className="py-2 pr-4">{u.status ?? '—'}</td>
                      <td className="py-2 pr-4 text-fg-muted">{u.tenantName ?? u.tenantId ?? '—'}</td>
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
