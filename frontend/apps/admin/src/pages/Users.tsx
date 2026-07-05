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
import { useUsers } from '../api/hooks'
import { QueryState } from '../shell/QueryState'
import { TenantAvatar } from './TenantAvatar'

/* Ruolo → tono badge (mockup admin: owner viola, admin blu, member neutro). */
function roleTone(role?: string) {
  if (role === 'owner') return 'violet' as const
  if (role === 'admin') return 'info' as const
  return 'neutral' as const
}

/** Elenco utenti cross-tenant: email, nome, ruolo, stato, tenant. */
export function Users() {
  const { t } = useTranslation()
  const users = useUsers()
  const rows = users.data ?? []

  return (
    <div className="space-y-[22px]">
      <PageHeader title={t('admin.users.title')} />
      <QueryState
        isLoading={users.isLoading}
        isError={users.isError}
        isEmpty={rows.length === 0}
        onRetry={() => void users.refetch()}
      >
        <Table>
          <TableHead>
            <TableRow>
              <TableHeadCell>{t('admin.users.colEmail')}</TableHeadCell>
              <TableHeadCell>{t('admin.users.colName')}</TableHeadCell>
              <TableHeadCell>{t('admin.users.colRole')}</TableHeadCell>
              <TableHeadCell>{t('admin.users.colStatus')}</TableHeadCell>
              <TableHeadCell>{t('admin.users.colTenant')}</TableHeadCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((u) => (
              <TableRow key={u.id}>
                <TableCell>
                  <span className="flex items-center gap-3">
                    <TenantAvatar name={u.displayName ?? u.email} className="rounded-pill" />
                    <span className="font-semibold">{u.email}</span>
                  </span>
                </TableCell>
                <TableCell className="text-fg-muted">{u.displayName ?? '—'}</TableCell>
                <TableCell>
                  {u.role ? <Badge tone={roleTone(u.role)}>{u.role}</Badge> : '—'}
                </TableCell>
                <TableCell>
                  {u.status ? (
                    <Badge withDot tone={u.status === 'active' ? 'success' : 'neutral'}>
                      {u.status}
                    </Badge>
                  ) : (
                    '—'
                  )}
                </TableCell>
                <TableCell className="text-fg-muted">{u.tenantName ?? u.tenantId ?? '—'}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </QueryState>
    </div>
  )
}
