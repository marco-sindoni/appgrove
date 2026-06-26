import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Button, Icon, buttonVariants, cn } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'

const DISMISS_KEY = 'ag-2fa-nudge-dismissed'

/**
 * Banner di nudge "imposta 2FA" (UC 0017 UC10). **Dismissibile** (localStorage): non esiste ancora un
 * endpoint/claim che esponga lo stato `totpEnabled`, quindi il banner non riflette la verità server
 * (rinvio tracciato). Sparisce dopo dismiss o quando l'utente apre la pagina Sicurezza.
 */
export function TwoFaNudge() {
  const { t } = useTranslation()
  const [dismissed, setDismissed] = useState(() => {
    try {
      return localStorage.getItem(DISMISS_KEY) === '1'
    } catch {
      return false
    }
  })
  if (dismissed) return null

  const dismiss = () => {
    try {
      localStorage.setItem(DISMISS_KEY, '1')
    } catch {
      // best-effort
    }
    setDismissed(true)
  }

  return (
    <div
      role="status"
      className="flex items-center gap-3 border-b border-line bg-accent/10 px-4 py-2 text-sm text-fg"
    >
      <Icon name="shield" size={18} className="text-accent" />
      <span className="flex-1">{t('twofa.nudge')}</span>
      <Link to="/security" className={cn(buttonVariants({ variant: 'secondary', size: 'sm' }))}>
        {t('twofa.enable')}
      </Link>
      <Button variant="ghost" size="sm" aria-label={t('twofa.dismiss')} onClick={dismiss}>
        <Icon name="close" size={18} />
      </Button>
    </div>
  )
}
