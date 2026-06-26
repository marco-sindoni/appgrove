import { Link } from 'react-router-dom'
import { buttonVariants } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'

export function NotFound() {
  const { t } = useTranslation()
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-bg p-6 text-center">
      <p className="font-mono text-4xl text-fg">404</p>
      <Link to="/" className={buttonVariants({ variant: 'secondary', size: 'sm' })}>
        {t('nav.dashboard')}
      </Link>
    </div>
  )
}
