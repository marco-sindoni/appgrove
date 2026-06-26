import { Card, CardContent, CardHeader, CardTitle } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useCurrentUser } from '../api/hooks'
import { QueryState } from '../shell/QueryState'

/** Profilo dell'utente: legge `GET /users/me` via api-client + TanStack Query, con stati compositi. */
export function Account() {
  const { t } = useTranslation()
  const query = useCurrentUser()

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold text-fg">{t('nav.account')}</h1>
      <Card>
        <CardHeader>
          <CardTitle>{t('nav.account')}</CardTitle>
        </CardHeader>
        <CardContent>
          <QueryState
            isLoading={query.isLoading}
            isError={query.isError}
            onRetry={() => void query.refetch()}
          >
            <dl className="grid grid-cols-[8rem_1fr] gap-2 text-sm">
              <dt className="text-fg-muted">Email</dt>
              <dd className="font-mono">{query.data?.email}</dd>
              <dt className="text-fg-muted">{t('settings.displayName')}</dt>
              <dd>{query.data?.displayName}</dd>
              <dt className="text-fg-muted">Role</dt>
              <dd>{query.data?.role}</dd>
            </dl>
          </QueryState>
        </CardContent>
      </Card>
    </div>
  )
}
