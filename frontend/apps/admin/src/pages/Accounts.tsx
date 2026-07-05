import { Link } from 'react-router-dom'
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
import { useAccounts } from '../api/hooks'
import { QueryState } from '../shell/QueryState'
import { TenantAvatar } from './TenantAvatar'

/** Elenco account (tenant): nome, stato, #utenti, #subscription attive, con link al dettaglio. */
export function Accounts() {
  const { t } = useTranslation()
  const accounts = useAccounts()
  const rows = accounts.data ?? []

  return (
    <div className="space-y-[22px]">
      <PageHeader title={t('admin.accounts.title')} />
      <QueryState
        isLoading={accounts.isLoading}
        isError={accounts.isError}
        isEmpty={rows.length === 0}
        onRetry={() => void accounts.refetch()}
      >
        <Table>
          <TableHead>
            <TableRow>
              <TableHeadCell>{t('admin.accounts.colName')}</TableHeadCell>
              <TableHeadCell>{t('admin.accounts.colStatus')}</TableHeadCell>
              <TableHeadCell>{t('admin.accounts.colUsers')}</TableHeadCell>
              <TableHeadCell>{t('admin.accounts.colActiveSubscriptions')}</TableHeadCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((a) => (
              <TableRow key={a.id} interactive>
                <TableCell>
                  <Link
                    to={`/accounts/${a.id}`}
                    className="flex items-center gap-3 font-semibold text-fg hover:text-accent"
                  >
                    <TenantAvatar name={a.name ?? a.id} />
                    {a.name ?? a.id}
                  </Link>
                </TableCell>
                <TableCell>
                  <Badge withDot tone={a.status === 'active' ? 'success' : 'neutral'}>
                    {a.status ?? '—'}
                  </Badge>
                </TableCell>
                <TableCell className="font-mono text-fg-muted">{a.users ?? 0}</TableCell>
                <TableCell className="font-mono text-fg-muted">{a.activeSubscriptions ?? 0}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </QueryState>
    </div>
  )
}
