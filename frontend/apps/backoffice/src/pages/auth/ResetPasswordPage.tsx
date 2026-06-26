import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Button } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useConfig } from '../../config'
import { resetPassword } from '../../auth/authApi'
import { resetSchema } from '../../auth/schemas'
import { authErrorMessage } from '../../auth/authErrors'
import { AuthLayout } from './AuthLayout'
import { Field } from './Field'

export function ResetPasswordPage() {
  const { t } = useTranslation()
  const config = useConfig()
  const [params] = useSearchParams()
  const token = params.get('token')
  const [done, setDone] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  const form = useForm<z.infer<ReturnType<typeof resetSchema>>>({
    resolver: zodResolver(resetSchema(t)),
    defaultValues: { password: '' },
  })

  const loginLink = (
    <Link to="/login" className="text-accent hover:underline">
      {t('reset.goToLogin')}
    </Link>
  )

  if (!token) {
    return (
      <AuthLayout title={t('reset.title')} footer={loginLink}>
        <p role="alert" className="text-sm text-danger">
          {t('reset.invalid')}
        </p>
      </AuthLayout>
    )
  }

  const onSubmit = form.handleSubmit(async (values) => {
    setFormError(null)
    try {
      await resetPassword(config.authBaseUrl, { token, password: values.password })
      setDone(true)
    } catch (err) {
      setFormError(authErrorMessage(err, t, { 400: t('reset.invalid') }))
    }
  })

  return (
    <AuthLayout title={t('reset.title')} footer={loginLink}>
      {done ? (
        <p role="status" className="text-sm text-success">
          {t('reset.done')}
        </p>
      ) : (
        <form onSubmit={onSubmit} className="space-y-4" noValidate>
          <Field
            id="reset-password"
            type="password"
            autoComplete="new-password"
            label={t('common.newPassword')}
            error={form.formState.errors.password?.message}
            {...form.register('password')}
          />
          {formError && (
            <p role="alert" className="text-sm text-danger">
              {formError}
            </p>
          )}
          <Button type="submit" className="w-full" disabled={form.formState.isSubmitting}>
            {t('reset.submit')}
          </Button>
        </form>
      )}
    </AuthLayout>
  )
}

export default ResetPasswordPage
