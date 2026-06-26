import type { ReactNode } from 'react'
import { Button } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'

/**
 * Stati compositi (loading/empty/error/success) sopra i primitivi del design-system (UC 0020).
 * Base condivisa del backoffice per liste/tabelle: valutarne l'estrazione in UC 0019 (rinvio tracciato).
 */
export function QueryState({
  isLoading,
  isError,
  isEmpty,
  onRetry,
  children,
}: {
  isLoading: boolean
  isError: boolean
  isEmpty?: boolean
  onRetry?: () => void
  children: ReactNode
}) {
  const { t } = useTranslation()

  if (isLoading) {
    return (
      <p role="status" className="text-sm text-fg-muted">
        {t('states.loading')}
      </p>
    )
  }
  if (isError) {
    return (
      <div role="alert" className="space-y-3">
        <p className="text-sm text-danger">{t('states.error')}</p>
        {onRetry && (
          <Button variant="secondary" size="sm" onClick={onRetry}>
            {t('states.retry')}
          </Button>
        )}
      </div>
    )
  }
  if (isEmpty) {
    return <p className="text-sm text-fg-muted">{t('states.empty')}</p>
  }
  return <>{children}</>
}
