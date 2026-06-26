import { Navigate, useLocation } from 'react-router-dom'
import { Card, CardContent, CardHeader, CardTitle, Logo } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useAuthStore } from '../auth/authStore'

/**
 * Segnaposto della pagina di login. Le **schermate auth reali** (login/signup/reset/invite/2FA) sono
 * di **UC 0017**: la shell fornisce qui solo la route e la plumbing (store/refresh/interceptor).
 * Se già autenticato, redirige alla destinazione richiesta o alla dashboard.
 */
export function Login() {
  const { t } = useTranslation()
  const location = useLocation()
  const status = useAuthStore((s) => s.status)

  if (status === 'authenticated') {
    const from = (location.state as { from?: string } | null)?.from ?? '/'
    return <Navigate to={from} replace />
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-bg p-6">
      <Card className="w-full max-w-sm">
        <CardHeader className="items-center gap-2">
          <Logo size={32} />
          <CardTitle>{t('auth.signIn')}</CardTitle>
        </CardHeader>
        <CardContent className="text-center text-sm text-fg-muted">
          {t('auth.loginRequired')}
        </CardContent>
      </Card>
    </div>
  )
}
