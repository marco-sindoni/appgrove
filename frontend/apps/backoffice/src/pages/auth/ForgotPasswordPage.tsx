import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Button } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useConfig } from '../../config'
import { forgotPassword } from '../../auth/authApi'
import { forgotSchema } from '../../auth/schemas'
import { AuthLayout } from './AuthLayout'
import { Field } from './Field'

export function ForgotPasswordPage() {
  const { t } = useTranslation()
  const config = useConfig()
  const [sent, setSent] = useState(false)

  const form = useForm<z.infer<ReturnType<typeof forgotSchema>>>({
    resolver: zodResolver(forgotSchema(t)),
    defaultValues: { email: '' },
  })

  // Risposta SEMPRE neutra (anti-enumeration): non rivela se l'email esiste.
  const onSubmit = form.handleSubmit(async (values) => {
    try {
      await forgotPassword(config.authBaseUrl, values.email)
    } catch {
      // ignora: messaggio neutro identico in ogni caso
    }
    setSent(true)
  })

  return (
    <AuthLayout
      title={t('forgot.title')}
      footer={
        <Link to="/login" className="text-accent hover:underline">
          {t('auth.signIn')}
        </Link>
      }
    >
      {sent ? (
        <p role="status" className="text-sm text-fg-muted">
          {t('forgot.sent')}
        </p>
      ) : (
        <form onSubmit={onSubmit} className="space-y-4" noValidate>
          <p className="text-sm text-fg-muted">{t('forgot.hint')}</p>
          <Field
            id="forgot-email"
            type="email"
            autoComplete="email"
            label={t('common.email')}
            error={form.formState.errors.email?.message}
            {...form.register('email')}
          />
          <Button type="submit" className="w-full" disabled={form.formState.isSubmitting}>
            {t('forgot.submit')}
          </Button>
        </form>
      )}
    </AuthLayout>
  )
}

export default ForgotPasswordPage
