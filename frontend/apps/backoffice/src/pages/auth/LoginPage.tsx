import { useState } from 'react'
import { Link, Navigate, useLocation } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Button } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useConfig } from '../../config'
import { useAuthStore } from '../../auth/authStore'
import { login, loginTwoFa } from '../../auth/authApi'
import { loginSchema, totpSchema } from '../../auth/schemas'
import { authErrorMessage } from '../../auth/authErrors'
import { AuthLayout } from './AuthLayout'
import { Field } from './Field'

export function LoginPage() {
  const { t } = useTranslation()
  const config = useConfig()
  const location = useLocation()
  const status = useAuthStore((s) => s.status)
  const setSession = useAuthStore((s) => s.setSession)

  const [challengeToken, setChallengeToken] = useState<string | null>(null)
  const [formError, setFormError] = useState<string | null>(null)

  const creds = useForm<z.infer<ReturnType<typeof loginSchema>>>({
    resolver: zodResolver(loginSchema(t)),
    defaultValues: { email: '', password: '' },
  })
  const totp = useForm<z.infer<ReturnType<typeof totpSchema>>>({
    resolver: zodResolver(totpSchema(t)),
    defaultValues: { code: '' },
  })

  if (status === 'authenticated') {
    const from = (location.state as { from?: string } | null)?.from ?? '/'
    return <Navigate to={from} replace />
  }

  const onCredentials = creds.handleSubmit(async (values) => {
    setFormError(null)
    try {
      const result = await login(config.authBaseUrl, values)
      if (result.kind === 'mfa') setChallengeToken(result.challengeToken)
      else setSession(result.tokens)
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
      setSession(tokens)
    } catch (err) {
      setFormError(authErrorMessage(err, t, { 401: t('errors.invalidCode') }))
    }
  })

  if (challengeToken) {
    return (
      <AuthLayout title={t('login.totpTitle')}>
        <form onSubmit={onTotp} className="space-y-4" noValidate>
          <p className="text-sm text-fg-muted">{t('login.totpHint')}</p>
          <Field
            id="totp-code"
            label={t('common.code')}
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
            {t('login.totpSubmit')}
          </Button>
        </form>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout
      title={t('login.title')}
      footer={
        <div className="space-y-1">
          <Link to="/forgot" className="text-accent hover:underline">
            {t('auth.forgotPassword')}
          </Link>
          <p>
            {t('auth.noAccount')}{' '}
            <Link to="/signup" className="text-accent hover:underline">
              {t('auth.createAccount')}
            </Link>
          </p>
        </div>
      }
    >
      <form onSubmit={onCredentials} className="space-y-4" noValidate>
        <Field
          id="login-email"
          type="email"
          autoComplete="email"
          label={t('common.email')}
          error={creds.formState.errors.email?.message}
          {...creds.register('email')}
        />
        <Field
          id="login-password"
          type="password"
          autoComplete="current-password"
          label={t('common.password')}
          error={creds.formState.errors.password?.message}
          {...creds.register('password')}
        />
        {formError && (
          <p role="alert" className="text-sm text-danger">
            {formError}
          </p>
        )}
        <Button type="submit" className="w-full" disabled={creds.formState.isSubmitting}>
          {t('login.submit')}
        </Button>
      </form>
    </AuthLayout>
  )
}

export default LoginPage
