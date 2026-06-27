import { useState } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Button, Card, CardContent, CardHeader, Logo } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useConfig } from '../config'
import { useAuthStore } from '../auth/authStore'
import { decodeClaims } from '../auth/jwt'
import { login, loginTwoFa, type SessionTokens } from '../auth/authApi'
import { authErrorMessage } from '../auth/authErrors'
import { Field } from './Field'

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type TFn = (key: any, opts?: Record<string, unknown>) => string

const loginSchema = (t: TFn) =>
  z.object({
    email: z.string().trim().min(1, t('admin.validation.required')).email(t('admin.validation.email')),
    password: z.string().min(1, t('admin.validation.required')),
  })

const totpSchema = (t: TFn) =>
  z.object({ code: z.string().trim().regex(/^\d{6}$/, t('admin.validation.code')) })

/**
 * Login della console admin: email+password con challenge 2FA. Dopo l'autenticazione, se il token
 * NON porta il ruolo `platform-admin` la sessione **non** viene impostata e si reindirizza a
 * `/forbidden` (la console è riservata agli admin di piattaforma — UC 0021).
 */
export function LoginPage() {
  const { t } = useTranslation()
  const config = useConfig()
  const location = useLocation()
  const status = useAuthStore((s) => s.status)
  const setSession = useAuthStore((s) => s.setSession)

  const [challengeToken, setChallengeToken] = useState<string | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const [denied, setDenied] = useState(false)

  const creds = useForm<z.infer<ReturnType<typeof loginSchema>>>({
    resolver: zodResolver(loginSchema(t)),
    defaultValues: { email: '', password: '' },
  })
  const totp = useForm<z.infer<ReturnType<typeof totpSchema>>>({
    resolver: zodResolver(totpSchema(t)),
    defaultValues: { code: '' },
  })

  if (denied) return <Navigate to="/forbidden" replace />

  if (status === 'authenticated') {
    const from = (location.state as { from?: string } | null)?.from ?? '/'
    return <Navigate to={from} replace />
  }

  /** Imposta la sessione solo se il token porta `platform-admin`, altrimenti reindirizza a /forbidden. */
  const acceptIfAdmin = (tokens: SessionTokens) => {
    const claims = decodeClaims(tokens.accessToken, tokens.idToken)
    if (claims?.roles.includes('platform-admin')) {
      setSession(tokens)
    } else {
      setDenied(true)
    }
  }

  const onCredentials = creds.handleSubmit(async (values) => {
    setFormError(null)
    try {
      const result = await login(config.authBaseUrl, values)
      if (result.kind === 'mfa') setChallengeToken(result.challengeToken)
      else acceptIfAdmin(result.tokens)
    } catch (err) {
      setFormError(authErrorMessage(err, t))
    }
  })

  const onTotp = totp.handleSubmit(async (values) => {
    setFormError(null)
    try {
      const tokens = await loginTwoFa(config.authBaseUrl, {
        challengeToken: challengeToken!,
        code: values.code,
      })
      acceptIfAdmin(tokens)
    } catch (err) {
      setFormError(authErrorMessage(err, t, { 401: t('admin.errors.invalidCode') }))
    }
  })

  const title = challengeToken ? t('admin.login.totpTitle') : t('admin.login.title')

  return (
    <div className="flex min-h-screen items-center justify-center bg-bg p-6">
      <div className="w-full max-w-sm space-y-4">
        <Card>
          <CardHeader className="items-center gap-2 text-center">
            <Logo size={32} />
            <h1 className="font-sans text-xl font-extrabold tracking-tight text-fg">{title}</h1>
            <span className="inline-flex items-center gap-1 rounded-pill bg-danger/10 px-2.5 py-0.5 text-xs font-semibold uppercase tracking-wide text-danger">
              {t('admin.badge.platformAdmin')}
            </span>
          </CardHeader>
          <CardContent>
            {challengeToken ? (
              <form onSubmit={onTotp} className="space-y-4" noValidate>
                <p className="text-sm text-fg-muted">{t('admin.login.totpHint')}</p>
                <Field
                  id="totp-code"
                  label={t('admin.login.code')}
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  error={totp.formState.errors.code?.message}
                  {...totp.register('code')}
                />
                {formError && (
                  <p role="alert" className="text-sm text-danger">
                    {formError}
                  </p>
                )}
                <Button type="submit" className="w-full" disabled={totp.formState.isSubmitting}>
                  {t('admin.login.totpSubmit')}
                </Button>
              </form>
            ) : (
              <form onSubmit={onCredentials} className="space-y-4" noValidate>
                <Field
                  id="login-email"
                  type="email"
                  autoComplete="email"
                  label={t('admin.login.email')}
                  error={creds.formState.errors.email?.message}
                  {...creds.register('email')}
                />
                <Field
                  id="login-password"
                  type="password"
                  autoComplete="current-password"
                  label={t('admin.login.password')}
                  error={creds.formState.errors.password?.message}
                  {...creds.register('password')}
                />
                {formError && (
                  <p role="alert" className="text-sm text-danger">
                    {formError}
                  </p>
                )}
                <Button type="submit" className="w-full" disabled={creds.formState.isSubmitting}>
                  {t('admin.login.submit')}
                </Button>
              </form>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

export default LoginPage
