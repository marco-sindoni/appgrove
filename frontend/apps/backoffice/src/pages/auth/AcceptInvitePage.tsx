import { useEffect, useState } from 'react'
import { Link, Navigate, useSearchParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Button } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useConfig } from '../../config'
import { useAuthStore } from '../../auth/authStore'
import { acceptInvitation, lookupInvitation } from '../../auth/authApi'
import { acceptSchema } from '../../auth/schemas'
import { authErrorMessage } from '../../auth/authErrors'
import { AuthLayout } from './AuthLayout'
import { Field } from './Field'

export function AcceptInvitePage() {
  const { t, i18n } = useTranslation()
  const config = useConfig()
  const [params] = useSearchParams()
  const token = params.get('token')
  const status = useAuthStore((s) => s.status)
  const setSession = useAuthStore((s) => s.setSession)
  const [formError, setFormError] = useState<string | null>(null)

  const form = useForm<z.infer<ReturnType<typeof acceptSchema>>>({
    resolver: zodResolver(acceptSchema(t)),
    defaultValues: { password: '', displayName: '' },
  })

  // Che cosa chiedere a chi apre il collegamento (UC 0118). Si interroga il servizio PRIMA di
  // mostrare il modulo: a chi ha già un'identità non si chiede una parola d'accesso nuova — sarebbe
  // una seconda identità mascherata, e il backend la rifiuta. Farglielo scoprire dopo aver premuto
  // «accetta» è il modo peggiore di dirlo.
  const [mode, setMode] = useState<'checking' | 'register' | 'signin'>('checking')
  const [invitedAddress, setInvitedAddress] = useState('')

  useEffect(() => {
    if (!token) return
    let alive = true
    void lookupInvitation(config.authBaseUrl, token)
      .then((r) => {
        if (alive) {
          setMode(r.mode)
          setInvitedAddress(r.email)
        }
      })
      // Token non valido o scaduto: si ripiega sul modulo, che dirà la stessa cosa con lo stesso
      // messaggio. Un guasto di rete non deve trasformarsi in un vicolo cieco.
      .catch(() => {
        if (alive) setMode('register')
      })
    return () => {
      alive = false
    }
  }, [config.authBaseUrl, token])

  if (status === 'authenticated') return <Navigate to="/" replace />

  if (!token) {
    return (
      <AuthLayout title={t('accept.title')}>
        <p role="alert" className="text-sm text-danger">
          {t('accept.missingToken')}
        </p>
      </AuthLayout>
    )
  }

  const onSubmit = form.handleSubmit(async (values) => {
    setFormError(null)
    try {
      const tokens = await acceptInvitation(config.authBaseUrl, {
        token,
        password: values.password,
        displayName: values.displayName || undefined,
        // Qui l'invitato sceglie la propria lingua per la prima volta (UC 0018): sta compilando il
        // modulo nella lingua che gli va bene, ed è quella che vogliamo per le sue email future.
        locale: i18n.language,
      })
      setSession(tokens)
    } catch (err) {
      setFormError(
        authErrorMessage(err, t, {
          410: t('accept.expired'),
          400: t('accept.invalid'),
          // Rete di ritorno dietro il lookup: se quell'indirizzo ha già un'identità il backend
          // rifiuta, e la persona deve sapere dove andare invece di leggere «email già registrata».
          409: t('accept.signinHint', { account: invitedAddress }),
        }),
      )
    }
  })

  if (mode === 'checking') {
    return (
      <AuthLayout title={t('accept.title')}>
        <p role="status" className="text-sm text-fg-muted">
          {t('accept.checking')}
        </p>
      </AuthLayout>
    )
  }

  if (mode === 'signin') {
    // Chi ha già un'identità non si registra di nuovo: si autentica, e l'invito lo aspetta come
    // consenso da dare in testa al cruscotto (UC 0118 §4.4). Dirlo qui è lecito: questa risposta la
    // vede solo chi ha in mano il token dell'invito, cioè la persona invitata, che sa di sé.
    return (
      <AuthLayout title={t('accept.signinTitle')}>
        <div className="space-y-4">
          <p className="text-sm text-fg-muted">{t('accept.signinHint', { account: invitedAddress })}</p>
          <Link
            to="/login"
            className="flex w-full items-center justify-center rounded-md bg-accent px-3 py-2.5 text-[13.5px] font-semibold text-white transition hover:opacity-90"
          >
            {t('accept.signinCta')}
          </Link>
        </div>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout
      title={t('accept.title')}
      footer={
        <Link to="/login" className="text-accent hover:underline">
          {t('auth.signIn')}
        </Link>
      }
    >
      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <p className="text-sm text-fg-muted">{t('accept.hint')}</p>
        <Field
          id="accept-name"
          label={`${t('common.displayName')} (${t('common.optional')})`}
          autoComplete="name"
          error={form.formState.errors.displayName?.message}
          {...form.register('displayName')}
        />
        <Field
          id="accept-password"
          type="password"
          autoComplete="new-password"
          label={t('common.password')}
          error={form.formState.errors.password?.message}
          {...form.register('password')}
        />
        {formError && (
          <p role="alert" className="text-sm text-danger">
            {formError}
          </p>
        )}
        <Button type="submit" className="w-full" disabled={form.formState.isSubmitting}>
          {t('accept.submit')}
        </Button>
      </form>
    </AuthLayout>
  )
}

export default AcceptInvitePage
