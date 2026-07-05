import { useState } from 'react'
import {
  Badge,
  Button,
  PageHeader,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeadCell,
  TableRow,
} from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useApps, useSetAppStatus, type AppView } from '../api/hooks'
import { QueryState } from '../shell/QueryState'
import { ConfirmDialog } from './ConfirmDialog'

/**
 * Catalogo app — **danger zone** (UC 0021): tabella app (slug, nome, modello, stato) con azione
 * Abilita/Disabilita (PATCH status) protetta da un ConfirmDialog accessibile.
 */
export function Apps() {
  const { t } = useTranslation()
  const apps = useApps()
  const setStatus = useSetAppStatus()
  const rows = apps.data ?? []

  const [confirm, setConfirm] = useState<AppView | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const onConfirm = async () => {
    if (!confirm?.id) return
    setActionError(null)
    const next = confirm.status === 'active' ? 'inactive' : 'active'
    try {
      await setStatus.mutateAsync({ id: confirm.id, status: next })
      setConfirm(null)
    } catch {
      setActionError(t('admin.errors.generic'))
      setConfirm(null)
    }
  }

  const disabling = confirm?.status === 'active'

  return (
    <div className="space-y-[22px]">
      <PageHeader title={t('admin.apps.title')} subtitle={t('admin.apps.subtitle')} />

      {actionError && (
        <p role="alert" className="text-sm text-danger">
          {actionError}
        </p>
      )}

      <QueryState
        isLoading={apps.isLoading}
        isError={apps.isError}
        isEmpty={rows.length === 0}
        onRetry={() => void apps.refetch()}
      >
        <Table>
          <TableHead>
            <TableRow>
              <TableHeadCell>{t('admin.apps.colSlug')}</TableHeadCell>
              <TableHeadCell>{t('admin.apps.colName')}</TableHeadCell>
              <TableHeadCell>{t('admin.apps.colUserModel')}</TableHeadCell>
              <TableHeadCell>{t('admin.apps.colStatus')}</TableHeadCell>
              <TableHeadCell className="text-right">{t('admin.apps.colActions')}</TableHeadCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((app) => (
              <TableRow key={app.id}>
                <TableCell className="font-mono text-xs text-fg-muted">{app.slug ?? '—'}</TableCell>
                <TableCell className="font-semibold">{app.name ?? '—'}</TableCell>
                <TableCell className="text-fg-muted">{app.userModel ?? '—'}</TableCell>
                <TableCell>
                  <Badge withDot tone={app.status === 'active' ? 'success' : 'danger'}>
                    {app.status ?? '—'}
                  </Badge>
                </TableCell>
                <TableCell className="text-right">
                  <Button
                    type="button"
                    variant={app.status === 'active' ? 'danger' : 'secondary'}
                    size="sm"
                    disabled={setStatus.isPending}
                    onClick={() => setConfirm(app)}
                  >
                    {app.status === 'active' ? t('admin.apps.disable') : t('admin.apps.enable')}
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </QueryState>

      {confirm && (
        <ConfirmDialog
          title={disabling ? t('admin.apps.confirmDisableTitle') : t('admin.apps.confirmEnableTitle')}
          body={
            disabling
              ? t('admin.apps.confirmDisableBody', { name: confirm.name ?? confirm.slug })
              : t('admin.apps.confirmEnableBody', { name: confirm.name ?? confirm.slug })
          }
          confirmLabel={disabling ? t('admin.apps.disable') : t('admin.apps.enable')}
          tone={disabling ? 'danger' : 'default'}
          busy={setStatus.isPending}
          onConfirm={() => void onConfirm()}
          onCancel={() => setConfirm(null)}
        />
      )}
    </div>
  )
}
