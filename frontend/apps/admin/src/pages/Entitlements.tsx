import {
  PageHeader,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeadCell,
  TableRow,
} from '@appgrove/design-system'
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
    <div className="space-y-[22px]">
      <PageHeader title={t('admin.entitlements.title')} />
      <QueryState
        isLoading={entitlements.isLoading}
        isError={entitlements.isError}
        isEmpty={rows.length === 0}
        onRetry={() => void entitlements.refetch()}
      >
        <Table>
          <TableHead>
            <TableRow>
              <TableHeadCell>{t('admin.entitlements.colTenant')}</TableHeadCell>
              <TableHeadCell>{t('admin.entitlements.colApp')}</TableHeadCell>
              <TableHeadCell>{t('admin.entitlements.colSubscriptionStatus')}</TableHeadCell>
              <TableHeadCell>{t('admin.entitlements.colEntitled')}</TableHeadCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((e, i) => (
              <TableRow key={`${e.tenantId}-${e.appId}-${i}`}>
                <TableCell className="font-semibold">{e.tenantName ?? e.tenantId ?? '—'}</TableCell>
                <TableCell>{e.appName ?? e.appSlug ?? '—'}</TableCell>
                <TableCell className="text-fg-muted">{e.subscriptionStatus ?? '—'}</TableCell>
                <TableCell>
                  <EntitledBadge entitled={e.entitled} />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </QueryState>
    </div>
  )
}
