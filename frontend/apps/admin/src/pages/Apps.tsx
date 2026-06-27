import { useState } from 'react'
import { Badge, Button, Card, CardContent } from '@appgrove/design-system'
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
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-fg">{t('admin.apps.title')}</h1>
        <p className="mt-1 text-sm text-fg-muted">{t('admin.apps.subtitle')}</p>
      </div>

      {actionError && (
        <p role="alert" className="text-sm text-danger">
          {actionError}
        </p>
      )}

      <Card>
        <CardContent className="py-4">
          <QueryState
            isLoading={apps.isLoading}
            isError={apps.isError}
            isEmpty={rows.length === 0}
            onRetry={() => void apps.refetch()}
          >
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-line text-fg-muted">
                    <th scope="col" className="py-2 pr-4 font-medium">{t('admin.apps.colSlug')}</th>
                    <th scope="col" className="py-2 pr-4 font-medium">{t('admin.apps.colName')}</th>
                    <th scope="col" className="py-2 pr-4 font-medium">{t('admin.apps.colUserModel')}</th>
                    <th scope="col" className="py-2 pr-4 font-medium">{t('admin.apps.colStatus')}</th>
                    <th scope="col" className="py-2 pr-4 font-medium">{t('admin.apps.colActions')}</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((app) => (
                    <tr key={app.id} className="border-b border-line/60">
                      <td className="py-2 pr-4 font-mono text-xs">{app.slug ?? '—'}</td>
                      <td className="py-2 pr-4">{app.name ?? '—'}</td>
                      <td className="py-2 pr-4 text-fg-muted">{app.userModel ?? '—'}</td>
                      <td className="py-2 pr-4">
                        <Badge tone={app.status === 'active' ? 'success' : 'danger'}>
                          {app.status ?? '—'}
                        </Badge>
                      </td>
                      <td className="py-2 pr-4">
                        <Button
                          type="button"
                          variant={app.status === 'active' ? 'danger' : 'secondary'}
                          size="sm"
                          disabled={setStatus.isPending}
                          onClick={() => setConfirm(app)}
                        >
                          {app.status === 'active' ? t('admin.apps.disable') : t('admin.apps.enable')}
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </QueryState>
        </CardContent>
      </Card>

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
