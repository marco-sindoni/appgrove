import { Link } from 'react-router-dom'
import { buttonVariants } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'

/** Mostrata quando manca il ruolo `platform-admin`: la console è riservata agli admin di piattaforma. */
export function Forbidden() {
  const { t } = useTranslation()
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-bg p-6 text-center">
      <p className="text-sm text-fg-muted">{t('admin.forbidden')}</p>
      <Link to="/login" className={buttonVariants({ variant: 'secondary', size: 'sm' })}>
        {t('admin.login.title')}
      </Link>
    </div>
  )
}
