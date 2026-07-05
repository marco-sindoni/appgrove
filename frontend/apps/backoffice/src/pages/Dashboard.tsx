import { Card, CardContent, CardHeader, CardTitle } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useAuthStore } from '../auth/authStore'

export function Dashboard() {
  const { t } = useTranslation()
  const claims = useAuthStore((s) => s.claims)

  return (
    <div className="space-y-[22px]">
      <h1 className="text-[27px] font-extrabold tracking-[-0.025em] text-fg">{t('nav.dashboard')}</h1>
      <Card>
        <CardHeader>
          <CardTitle>{t('app.name')}</CardTitle>
        </CardHeader>
        <CardContent>
          {claims?.name ? `${claims.name} · ` : ''}
          <span className="font-mono text-sm text-fg-muted">{claims?.tenantId}</span>
        </CardContent>
      </Card>
    </div>
  )
}
