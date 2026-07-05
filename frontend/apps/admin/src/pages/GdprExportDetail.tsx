import { Link, useParams } from 'react-router-dom'
import { Card, CardContent } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useGdprExportDetail } from '../api/hooks'
import { QueryState } from '../shell/QueryState'

const fmtDateTime = (iso?: string | null) => (iso ? new Date(iso).toLocaleString() : '—')

/**
 * Dettaglio di un export GDPR (UC 0034, #13 L75): stato/timeline del job, avanzamento
 * per-servizio e <b>puntatori</b> all'accessorio — chiave S3 dell'archivio (mai il contenuto:
 * niente download admin, minimizzazione) e deep-link alla console (solo se configurata).
 */
export function GdprExportDetail() {
  const { t } = useTranslation()
  const { id = '' } = useParams()
  const detail = useGdprExportDetail(id)
  const request = detail.data?.request

  return (
    <div className="space-y-[22px]">
      <div className="flex items-center justify-between">
        <h1 className="text-[22px] font-extrabold tracking-[-0.02em] text-fg">{t('admin.gdpr.exportDetailTitle')}</h1>
        <Link to="/gdpr" className="text-sm text-accent underline-offset-2 hover:underline">
          ← {t('admin.gdpr.title')}
        </Link>
      </div>
      <Card>
        <CardContent className="space-y-4 py-4">
          <QueryState
            isLoading={detail.isLoading}
            isError={detail.isError}
            isEmpty={!detail.data}
            onRetry={() => void detail.refetch()}
          >
            {request && (
              <dl className="grid gap-3 text-sm sm:grid-cols-2">
                <div>
                  <dt className="font-medium text-fg-muted">{t('admin.gdpr.colAccount')}</dt>
                  <dd>{request.accountName ?? request.tenantId}</dd>
                </div>
                <div>
                  <dt className="font-medium text-fg-muted">{t('admin.gdpr.colStatus')}</dt>
                  <dd>{request.status}</dd>
                </div>
                <div>
                  <dt className="font-medium text-fg-muted">{t('admin.gdpr.colRequested')}</dt>
                  <dd>{fmtDateTime(request.requestedAt)}</dd>
                </div>
                <div>
                  <dt className="font-medium text-fg-muted">{t('admin.gdpr.colCompleted')}</dt>
                  <dd>{fmtDateTime(request.completedAt)}</dd>
                </div>
                <div>
                  <dt className="font-medium text-fg-muted">{t('admin.gdpr.colDue')}</dt>
                  <dd>{fmtDateTime(request.dueAt)}</dd>
                </div>
                {request.error && (
                  <div>
                    <dt className="font-medium text-fg-muted">{t('admin.gdpr.colError')}</dt>
                    <dd className="text-danger">{request.error}</dd>
                  </div>
                )}
              </dl>
            )}

            <div>
              <h2 className="mb-2 text-sm font-semibold text-fg">{t('admin.gdpr.exportItems')}</h2>
              <ul className="space-y-1 text-sm">
                {(detail.data?.items ?? []).map((item) => (
                  <li key={item.appId} className="flex items-center gap-3">
                    <span className="font-medium">{item.appId}</span>
                    <span className="text-fg-muted">{item.status}</span>
                    {item.error && <span className="text-danger">{item.error}</span>}
                  </li>
                ))}
              </ul>
            </div>

            <div className="text-sm">
              <h2 className="mb-2 font-semibold text-fg">{t('admin.gdpr.exportZip')}</h2>
              {detail.data?.zipKey ? (
                <p className="flex flex-wrap items-center gap-3">
                  <code className="rounded bg-surface-2 px-2 py-1">{detail.data.zipKey}</code>
                  {detail.data.s3ConsoleUrl && (
                    <a
                      href={detail.data.s3ConsoleUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="text-accent underline-offset-2 hover:underline"
                    >
                      {t('admin.gdpr.exportS3')}
                    </a>
                  )}
                </p>
              ) : (
                <p className="text-fg-muted">{t('admin.gdpr.exportNoZip')}</p>
              )}
              {request?.logsUrl && (
                <p className="mt-2">
                  <a
                    href={request.logsUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="text-accent underline-offset-2 hover:underline"
                  >
                    {t('admin.gdpr.logsLink')}
                  </a>
                </p>
              )}
            </div>
          </QueryState>
        </CardContent>
      </Card>
    </div>
  )
}
