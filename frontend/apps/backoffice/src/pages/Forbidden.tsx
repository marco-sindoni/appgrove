import { Link } from 'react-router-dom'
import { buttonVariants } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'

/** Mostrata quando una route guard nega l'accesso (ruolo/entitlement mancante). */
export function Forbidden() {
  const { t } = useTranslation()
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-bg p-6 text-center">
      <p className="text-sm text-fg-muted">{t('auth.notEntitled')}</p>
      <Link to="/" className={buttonVariants({ variant: 'secondary', size: 'sm' })}>
        {t('nav.dashboard')}
      </Link>
    </div>
  )
}
