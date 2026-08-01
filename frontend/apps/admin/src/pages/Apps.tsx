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
import { useAppStatusAudit, useApps, useSetAppStatus, type AppView } from '../api/hooks'
import { QueryState } from '../shell/QueryState'
import { ConfirmDialog } from './ConfirmDialog'

/**
 * Catalogo app — **danger zone** (UC 0021 + UC 0076): tabella app (slug, nome, modello, stato) con
 * l'azione reversibile Disabilita/Riabilita (PATCH status) protetta da un ConfirmDialog accessibile
 * che spiega l'effetto per esteso e raccoglie una **motivazione** facoltativa, più il **registro**
 * delle transizioni (chi, quando, quale app, perché).
 *
 * <p>Il copy distingue in modo esplicito questa pausa — reversibile, dati intatti — dalla dismissione
 * definitiva dell'app (skill `drop-application`): confonderle sarebbe pericoloso.
 */
export function Apps() {
  const { t, i18n } = useTranslation()
  const apps = useApps()
  const audit = useAppStatusAudit()
  const setStatus = useSetAppStatus()
  const rows = apps.data ?? []
  const auditRows = audit.data ?? []

  const [confirm, setConfirm] = useState<AppView | null>(null)
  const [reason, setReason] = useState('')
  const [actionError, setActionError] = useState<string | null>(null)

  const close = () => {
    setConfirm(null)
    setReason('')
  }

  const onConfirm = async () => {
    if (!confirm?.id) return
    setActionError(null)
    const next = confirm.status === 'active' ? 'inactive' : 'active'
    try {
      await setStatus.mutateAsync({ id: confirm.id, status: next, reason: reason.trim() || undefined })
      close()
    } catch {
      setActionError(t('admin.errors.generic'))
      close()
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
                    {app.status === 'active'
                      ? t('admin.apps.statusActive')
                      : t('admin.apps.statusInactive')}
                  </Badge>
                </TableCell>
                <TableCell className="text-right">
                  <Button
                    type="button"
                    variant={app.status === 'active' ? 'danger' : 'secondary'}
                    size="sm"
                    disabled={setStatus.isPending}
                    onClick={() => {
                      setReason('')
                      setConfirm(app)
                    }}
                  >
                    {app.status === 'active' ? t('admin.apps.disable') : t('admin.apps.enable')}
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </QueryState>

      <section className="space-y-3" aria-labelledby="apps-audit-heading">
        <div className="space-y-1">
          <h2 id="apps-audit-heading" className="text-lg font-semibold text-fg">
            {t('admin.apps.auditHeading')}
          </h2>
          <p className="text-sm text-fg-muted">{t('admin.apps.auditSubtitle')}</p>
        </div>
        <QueryState
          isLoading={audit.isLoading}
          isError={audit.isError}
          isEmpty={auditRows.length === 0}
          emptyLabel={t('admin.apps.auditEmpty')}
          onRetry={() => void audit.refetch()}
        >
          <Table>
            <TableHead>
              <TableRow>
                <TableHeadCell>{t('admin.apps.colApp')}</TableHeadCell>
                <TableHeadCell>{t('admin.apps.colAction')}</TableHeadCell>
                <TableHeadCell>{t('admin.apps.colActor')}</TableHeadCell>
                <TableHeadCell>{t('admin.apps.colWhen')}</TableHeadCell>
                <TableHeadCell>{t('admin.apps.colReason')}</TableHeadCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {auditRows.map((entry) => (
                <TableRow key={entry.id}>
                  <TableCell className="font-semibold">{entry.appName ?? entry.appSlug ?? '—'}</TableCell>
                  <TableCell>
                    <Badge tone={entry.toStatus === 'active' ? 'success' : 'danger'}>
                      {entry.toStatus === 'active'
                        ? t('admin.apps.actionEnabled')
                        : t('admin.apps.actionDisabled')}
                    </Badge>
                  </TableCell>
                  <TableCell className="font-mono text-xs text-fg-muted">{entry.actor ?? '—'}</TableCell>
                  <TableCell className="text-fg-muted">{formatWhen(entry.executedAt, i18n.language)}</TableCell>
                  <TableCell className="text-fg-muted">{entry.reason ?? '—'}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </QueryState>
      </section>

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
          onCancel={close}
        >
          <p className="text-sm text-fg-muted">{t('admin.apps.confirmDelayHint')}</p>
          {disabling && (
            <p className="text-sm text-fg-muted">{t('admin.apps.confirmNotDropHint')}</p>
          )}
          <div className="space-y-1">
            <label htmlFor="app-status-reason" className="block text-sm font-medium text-fg">
              {t('admin.apps.reasonLabel')}
            </label>
            <textarea
              id="app-status-reason"
              rows={2}
              maxLength={512}
              placeholder={t('admin.apps.reasonPlaceholder')}
              aria-describedby="app-status-reason-hint"
              className="w-full rounded-md border border-line bg-surface px-3 py-2 text-sm"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
            <p id="app-status-reason-hint" className="text-xs text-fg-muted">
              {t('admin.apps.reasonHint')}
            </p>
          </div>
        </ConfirmDialog>
      )}
    </div>
  )
}

/** Data e ora leggibili della transizione; stringa grezza se non interpretabile. */
function formatWhen(iso: string | null | undefined, locale: string): string {
  if (!iso) return '—'
  const d = new Date(iso)
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleString(locale)
}
