import { Card, CardContent, CardHeader, CardTitle } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'

/**
 * Segnaposto della sezione fatturazione. La UI reale (checkout/portale) è di UC dedicati
 * (0024 checkout, 0028 portale self-service) — qui la shell fornisce solo la route/chrome.
 */
export function Billing() {
  const { t } = useTranslation()
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold text-fg">{t('nav.billing')}</h1>
      <Card>
        <CardHeader>
          <CardTitle>{t('nav.billing')}</CardTitle>
        </CardHeader>
        <CardContent className="text-fg-muted">{t('states.empty')}</CardContent>
      </Card>
    </div>
  )
}
