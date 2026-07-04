import { useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Button, Card, CardContent, CardHeader, Input } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useApiClient } from '../../api/apiClient'
import { useCurrentAccount, useCurrentUser } from '../../api/hooks'
import { useAuthStore } from '../../auth/authStore'
import { useMySubscriptions } from '../../billing/subscriptionsApi'
import { QueryState } from '../../shell/QueryState'
import { ConfirmDialog } from '../members/ConfirmDialog'
import {
  downloadProfileExport,
  useCancelAccountDeletion,
  useGdprExportDownload,
  useGdprExportJob,
  useRectifyDisplayName,
  useRequestAccountDeletion,
  useStartGdprExport,
  useWithdrawFromApp,
} from './privacyApi'

/**
 * "I miei dati" (UC 0033): diritti GDPR self-service. Profilo + rettifica (art. 16) ed export del
 * profilo (artt. 15/20) per ogni ruolo; export account e recesso per-app (owner/admin);
 * eliminazione account con grace 14gg (owner); diritti dichiarati (artt. 18/21/22). La pagina
 * resta raggiungibile anche a subscription scaduta (#09 F31): i guard di route non la gateano.
 */
export function PrivacyPage() {
  const { t } = useTranslation()
  const roles = useAuthStore((s) => s.claims?.roles ?? [])
  const isOwner = roles.includes('owner')
  const canManageAccountData = isOwner || roles.includes('admin')

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-fg">{t('privacy.title')}</h1>
        <p className="mt-1 text-sm text-fg-muted">{t('privacy.subtitle')}</p>
      </div>
      <ProfileSection />
      <ProfileExportSection />
      {canManageAccountData && <AccountExportSection />}
      {canManageAccountData && <WithdrawalSection />}
      {isOwner && <DeletionSection />}
      <RightsSection />
    </div>
  )
}

/** Profilo + rettifica del nome visualizzato (art. 16). L'email si cambia via supporto (UC 0017). */
function ProfileSection() {
  const { t } = useTranslation()
  const user = useCurrentUser()
  const rectify = useRectifyDisplayName()

  const schema = useMemo(
    () =>
      z.object({
        displayName: z
          .string()
          .trim()
          .min(1, t('validation.required'))
          .max(255, t('validation.tooLong', { max: 255 })),
      }),
    [t],
  )
  type Values = z.infer<typeof schema>

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    values: { displayName: user.data?.displayName ?? '' },
  })

  const onSubmit = handleSubmit((values) =>
    rectify.mutateAsync(values.displayName).catch(() => undefined),
  )

  return (
    <Card>
      <CardHeader>
        <h2 className="font-sans text-lg font-extrabold tracking-tight text-fg">{t('privacy.profileHeading')}</h2>
      </CardHeader>
      <CardContent>
        <QueryState
          isLoading={user.isLoading}
          isError={user.isError}
          onRetry={() => void user.refetch()}
        >
          <div className="space-y-4">
            <dl className="grid grid-cols-[8rem_1fr] gap-2 text-sm">
              <dt className="text-fg-muted">{t('common.email')}</dt>
              <dd className="font-mono">{user.data?.email}</dd>
            </dl>
            <p className="text-sm text-fg-muted">{t('privacy.profileEmailNote')}</p>
            <form onSubmit={onSubmit} className="max-w-sm space-y-4" noValidate>
              <div className="space-y-1">
                <label htmlFor="privacy-display-name" className="text-sm font-medium text-fg">
                  {t('settings.displayName')}
                </label>
                <Input
                  id="privacy-display-name"
                  invalid={!!errors.displayName}
                  aria-describedby={errors.displayName ? 'privacy-display-name-error' : undefined}
                  {...register('displayName')}
                />
                {errors.displayName && (
                  <p id="privacy-display-name-error" role="alert" className="text-sm text-danger">
                    {errors.displayName.message}
                  </p>
                )}
              </div>
              <div className="flex items-center gap-3">
                <Button type="submit" disabled={isSubmitting || rectify.isPending}>
                  {t('settings.save')}
                </Button>
                {rectify.isSuccess && (
                  <span role="status" className="text-sm text-success">
                    {t('settings.saved')}
                  </span>
                )}
              </div>
            </form>
          </div>
        </QueryState>
      </CardContent>
    </Card>
  )
}

/** Export del profilo utente: download JSON sincrono dal core (artt. 15/20). */
function ProfileExportSection() {
  const { t } = useTranslation()
  const client = useApiClient()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(false)

  const onDownload = async () => {
    setBusy(true)
    setError(false)
    try {
      await downloadProfileExport(client)
    } catch {
      setError(true)
    } finally {
      setBusy(false)
    }
  }

  return (
    <Card>
      <CardHeader>
        <h2 className="font-sans text-lg font-extrabold tracking-tight text-fg">{t('privacy.profileExportHeading')}</h2>
      </CardHeader>
      <CardContent className="space-y-4">
        <p className="text-sm text-fg-muted">{t('privacy.profileExportBody')}</p>
        <Button type="button" onClick={() => void onDownload()} disabled={busy}>
          {t('privacy.profileExportButton')}
        </Button>
        {error && (
          <p role="alert" className="text-sm text-danger">
            {t('errors.generic')}
          </p>
        )}
      </CardContent>
    </Card>
  )
}

/** Export dell'account (UC 0032): avvio, polling stato/progresso, link firmato con scadenza. */
function AccountExportSection() {
  const { t, i18n } = useTranslation()
  const start = useStartGdprExport()
  const [jobId, setJobId] = useState<string | null>(null)
  const job = useGdprExportJob(jobId)
  const download = useGdprExportDownload()

  const status = job.data?.status
  const running = status === 'QUEUED' || status === 'RUNNING'
  const expiresAt = download.data?.expiresAt
  const expiry = expiresAt
    ? new Intl.DateTimeFormat(i18n.language, { dateStyle: 'medium', timeStyle: 'short' }).format(
        new Date(expiresAt),
      )
    : null

  return (
    <Card>
      <CardHeader>
        <h2 className="font-sans text-lg font-extrabold tracking-tight text-fg">{t('privacy.accountExportHeading')}</h2>
      </CardHeader>
      <CardContent className="space-y-4">
        <p className="text-sm text-fg-muted">{t('privacy.accountExportBody')}</p>
        <div className="flex items-center gap-3">
          <Button
            type="button"
            disabled={start.isPending || running}
            onClick={() =>
              start
                .mutateAsync({ kind: 'account' })
                .then((created) => {
                  download.reset()
                  setJobId(created?.id ?? null)
                })
                .catch(() => undefined)
            }
          >
            {t('privacy.accountExportStart')} — {t('privacy.accountExportScopeAll')}
          </Button>
        </div>
        {start.isError && (
          <p role="alert" className="text-sm text-danger">
            {t('errors.generic')}
          </p>
        )}
        {running && (
          <p role="status" aria-live="polite" className="text-sm text-fg-muted">
            {t('privacy.accountExportRunning', {
              completed: job.data?.progress?.completed ?? 0,
              total: job.data?.progress?.total ?? 0,
            })}
          </p>
        )}
        {status === 'FAILED' && (
          <p role="alert" className="text-sm text-danger">
            {t('privacy.accountExportFailed')}
          </p>
        )}
        {status === 'COMPLETED' && (
          <div className="space-y-2">
            <p role="status" className="text-sm text-success">
              {t('privacy.accountExportReady')}
            </p>
            <div className="flex items-center gap-3">
              <Button
                type="button"
                variant="secondary"
                disabled={download.isPending}
                onClick={() =>
                  download
                    .mutateAsync(jobId as string)
                    .then((link) => {
                      if (link.url) window.open(link.url, '_blank', 'noopener')
                    })
                    .catch(() => undefined)
                }
              >
                {t('privacy.accountExportDownload')}
              </Button>
              {expiry && (
                <span className="text-sm text-fg-muted">
                  {t('privacy.accountExportExpires', { expiry })}
                </span>
              )}
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  )
}

/** Recesso per-app: esporta (job per-app) → conferma (dialog) → cancellazione immediata. */
function WithdrawalSection() {
  const { t } = useTranslation()
  const subscriptions = useMySubscriptions()
  const apps = subscriptions.data?.subscriptions ?? []

  return (
    <Card>
      <CardHeader>
        <h2 className="font-sans text-lg font-extrabold tracking-tight text-fg">{t('privacy.withdrawalHeading')}</h2>
      </CardHeader>
      <CardContent className="space-y-4">
        <p className="text-sm text-fg-muted">{t('privacy.withdrawalBody')}</p>
        <QueryState
          isLoading={subscriptions.isLoading}
          isError={subscriptions.isError}
          onRetry={() => void subscriptions.refetch()}
        >
          {apps.length === 0 ? (
            <p className="text-sm text-fg-muted">{t('privacy.withdrawalNoApps')}</p>
          ) : (
            <ul className="space-y-3">
              {apps.map((sub) => (
                <WithdrawalRow
                  key={sub.appSlug}
                  appSlug={sub.appSlug ?? ''}
                  appName={sub.appName ?? sub.appSlug ?? ''}
                />
              ))}
            </ul>
          )}
        </QueryState>
      </CardContent>
    </Card>
  )
}

function WithdrawalRow({ appSlug, appName }: { appSlug: string; appName: string }) {
  const { t } = useTranslation()
  const start = useStartGdprExport()
  const [jobId, setJobId] = useState<string | null>(null)
  const job = useGdprExportJob(jobId)
  const withdraw = useWithdrawFromApp()
  const [confirming, setConfirming] = useState(false)

  const status = job.data?.status
  const exporting = start.isPending || status === 'QUEUED' || status === 'RUNNING'
  const exportReady = status === 'COMPLETED'

  if (withdraw.isSuccess) {
    return (
      <li className="rounded-md border border-line p-4">
        <p role="status" className="text-sm text-success">
          {appName}: {t('privacy.withdrawalDone')}
        </p>
      </li>
    )
  }

  return (
    <li className="flex flex-wrap items-center justify-between gap-3 rounded-md border border-line p-4">
      <span className="text-sm font-medium text-fg">{appName}</span>
      <div className="flex items-center gap-3">
        {!exportReady && (
          <Button
            type="button"
            size="sm"
            variant="secondary"
            disabled={exporting}
            onClick={() =>
              start
                .mutateAsync({ kind: 'app', appId: appSlug })
                .then((created) => setJobId(created?.id ?? null))
                .catch(() => undefined)
            }
          >
            {exporting ? t('privacy.withdrawalExporting') : t('privacy.withdrawalExport')}
          </Button>
        )}
        <Button
          type="button"
          size="sm"
          variant="danger"
          disabled={!exportReady || withdraw.isPending}
          onClick={() => setConfirming(true)}
        >
          {t('privacy.withdrawalConfirm')}
        </Button>
      </div>
      {(start.isError || status === 'FAILED' || withdraw.isError) && (
        <p role="alert" className="w-full text-sm text-danger">
          {status === 'FAILED' ? t('privacy.accountExportFailed') : t('errors.generic')}
        </p>
      )}
      {confirming && (
        <ConfirmDialog
          title={t('privacy.withdrawalDialogTitle', { app: appName })}
          body={t('privacy.withdrawalDialogBody')}
          confirmLabel={t('privacy.withdrawalConfirm')}
          tone="danger"
          busy={withdraw.isPending}
          onConfirm={() =>
            withdraw
              .mutateAsync({ appSlug, exportJobId: jobId as string })
              .then(() => setConfirming(false))
              .catch(() => setConfirming(false))
          }
          onCancel={() => setConfirming(false)}
        />
      )}
    </li>
  )
}

/** Eliminazione account con grace 14gg (#13 E25): conferma → countdown → annulla. OWNER-only. */
function DeletionSection() {
  const { t, i18n } = useTranslation()
  const account = useCurrentAccount()
  const request = useRequestAccountDeletion()
  const cancel = useCancelAccountDeletion()
  const [confirming, setConfirming] = useState(false)

  const pending = account.data?.status === 'pending_deletion'
  const effectiveAt = account.data?.deletionEffectiveAt
  const effectiveDate = effectiveAt
    ? new Intl.DateTimeFormat(i18n.language, { dateStyle: 'long', timeStyle: 'short' }).format(
        new Date(effectiveAt),
      )
    : null

  return (
    <Card>
      <CardHeader>
        <h2 className="font-sans text-lg font-extrabold tracking-tight text-fg">{t('privacy.deletionHeading')}</h2>
      </CardHeader>
      <CardContent className="space-y-4">
        <QueryState
          isLoading={account.isLoading}
          isError={account.isError}
          onRetry={() => void account.refetch()}
        >
          {pending ? (
            <div className="space-y-3">
              <p role="status" className="text-sm font-medium text-danger">
                {t('privacy.deletionPending')}
                {effectiveDate && ` ${t('privacy.deletionEffectiveAt', { date: effectiveDate })}`}
              </p>
              <Button
                type="button"
                variant="secondary"
                disabled={cancel.isPending}
                onClick={() => void cancel.mutateAsync().catch(() => undefined)}
              >
                {t('privacy.deletionCancel')}
              </Button>
            </div>
          ) : (
            <div className="space-y-3">
              {cancel.isSuccess && (
                <p role="status" className="text-sm text-success">
                  {t('privacy.deletionCanceled')}
                </p>
              )}
              <p className="text-sm text-fg-muted">{t('privacy.deletionBody')}</p>
              <Button type="button" variant="danger" onClick={() => setConfirming(true)}>
                {t('privacy.deletionRequest')}
              </Button>
            </div>
          )}
          {(request.isError || cancel.isError) && (
            <p role="alert" className="text-sm text-danger">
              {t('errors.generic')}
            </p>
          )}
        </QueryState>
        {confirming && (
          <ConfirmDialog
            title={t('privacy.deletionDialogTitle')}
            body={t('privacy.deletionDialogBody')}
            confirmLabel={t('privacy.deletionRequest')}
            tone="danger"
            busy={request.isPending}
            onConfirm={() =>
              request
                .mutateAsync()
                .then(() => setConfirming(false))
                .catch(() => setConfirming(false))
            }
            onCancel={() => setConfirming(false)}
          />
        )}
      </CardContent>
    </Card>
  )
}

/** Diritti dichiarati: limitazione (art. 18, canale privacy), opposizione (art. 21), art. 22. */
function RightsSection() {
  const { t } = useTranslation()
  return (
    <Card>
      <CardHeader>
        <h2 className="font-sans text-lg font-extrabold tracking-tight text-fg">{t('privacy.rightsHeading')}</h2>
      </CardHeader>
      <CardContent>
        <ul className="list-disc space-y-2 pl-5 text-sm text-fg-muted">
          <li>{t('privacy.rightsRestriction')}</li>
          <li>{t('privacy.rightsObjection')}</li>
          <li>{t('privacy.rightsAutomated')}</li>
        </ul>
      </CardContent>
    </Card>
  )
}
