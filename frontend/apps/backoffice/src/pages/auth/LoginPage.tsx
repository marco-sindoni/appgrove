import { useState } from 'react'
import { Link, Navigate, useLocation } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Button } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useConfig } from '../../config'
import { useAuthStore } from '../../auth/authStore'
import { chooseAccount, login, loginTwoFa, type AccountOption } from '../../auth/authApi'
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
  // Sfida di scelta dell'account (UC 0118): terzo passo dell'accesso, sul modello del secondo
  // fattore. Nessun token in mano finché la persona non ha scelto per conto di chi lavorare.
  const [choice, setChoice] = useState<{ token: string; accounts: AccountOption[] } | null>(null)
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
      handleLogin(await login(config.authBaseUrl, values))
    } catch (err) {
      // 409 sull'accesso non è «email già registrata» (il significato che ha nell'iscrizione) ma
      // «appartieni a più account e nessuno è attivo» (UC 0117). Dopo UC 0118 quel caso ha una
      // schermata (il passo di scelta qui sopra) e il 409 resta la rete di ritorno dei percorsi non
      // interattivi — il rinnovo che fallisce riporta qui: dirlo con la frase sbagliata manderebbe
      // la persona a cercare un problema che non ha.
      setFormError(authErrorMessage(err, t, { 409: t('errors.accountSelectionRequired') }))
    }
  })

  const onTotp = totp.handleSubmit(async (values) => {
    setFormError(null)
    try {
      handleLogin(
        await loginTwoFa(config.authBaseUrl, {
          challengeToken: challengeToken!,
          code: values.code,
        }),
      )
    } catch (err) {
      setFormError(
        authErrorMessage(err, t, {
          401: t('errors.invalidCode'),
          409: t('errors.accountSelectionRequired'),
        }),
      )
    }
  })

  function handleLogin(result: Awaited<ReturnType<typeof login>>) {
    if (result.kind === 'mfa') {
      setChallengeToken(result.challengeToken)
      return
    }
    if (result.kind === 'chooseAccount') {
      // Il secondo fattore, se c'era, è già superato: qui la persona è già provata, e mostrarle
      // l'elenco dei suoi account non rivela niente a nessun altro.
      setChallengeToken(null)
      setChoice({ token: result.choiceToken, accounts: result.accounts })
      return
    }
    setSession(result.tokens)
  }

  const onChooseAccount = async (accountId: string) => {
    setFormError(null)
    try {
      setSession(await chooseAccount(config.authBaseUrl, { choiceToken: choice!.token, accountId }))
    } catch (err) {
      // Tipicamente 404: l'appartenenza è stata revocata fra la schermata e la scelta. Si dice, e si
      // resta sull'elenco: le altre voci possono essere ancora valide.
      setFormError(authErrorMessage(err, t, { 404: t('chooseAccount.error') }))
    }
  }

  if (choice) {
    return (
      <AuthLayout title={t('chooseAccount.title')}>
        <div className="space-y-4">
          <p className="text-sm text-fg-muted">{t('chooseAccount.hint')}</p>
          <ul className="space-y-2">
            {choice.accounts.map((account) => (
              <li key={account.accountId}>
                <button
                  type="button"
                  onClick={() => void onChooseAccount(account.accountId)}
                  className="flex w-full items-center gap-2 rounded-md border border-line bg-surface px-3 py-2.5 text-left text-[13.5px] font-semibold text-fg transition hover:bg-surface-2"
                >
                  {account.accountName}
                </button>
              </li>
            ))}
          </ul>
          {formError && (
            <p role="alert" className="text-sm text-danger">
              {formError}
            </p>
          )}
        </div>
      </AuthLayout>
    )
  }

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
