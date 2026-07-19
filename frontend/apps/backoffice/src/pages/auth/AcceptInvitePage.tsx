import { useState } from 'react'
import { Link, Navigate, useSearchParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Button } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useConfig } from '../../config'
import { useAuthStore } from '../../auth/authStore'
import { acceptInvitation } from '../../auth/authApi'
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
        authErrorMessage(err, t, { 410: t('accept.expired'), 400: t('accept.invalid') }),
      )
    }
  })

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
