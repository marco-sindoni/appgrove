import { useTranslation } from '@appgrove/i18n'

/** Schermata intera per stati di transizione/errore (ripristino sessione, loading, accesso negato). */
export function FullPageMessage({
  messageKey,
  tone = 'status',
}: {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  messageKey: any
  tone?: 'status' | 'error'
}) {
  const { t } = useTranslation()
  return (
    <div className="flex min-h-screen items-center justify-center bg-bg p-6">
      <p role={tone === 'error' ? 'alert' : 'status'} className="text-sm text-fg-muted">
        {t(messageKey)}
      </p>
    </div>
  )
}
