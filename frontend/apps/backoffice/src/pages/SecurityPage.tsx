import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { QRCodeSVG } from 'qrcode.react'
import { Button, Card, CardContent, CardHeader, CardTitle } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useConfig } from '../config'
import { getAccessToken } from '../auth/authStore'
import { enroll2fa, verify2fa, type EnrollResult } from '../auth/authApi'
import { totpSchema } from '../auth/schemas'
import { authErrorMessage } from '../auth/authErrors'
import { Field } from './auth/Field'

/**
 * Setup 2FA TOTP dal profilo (UC 0017 UC10): enroll → QR/secret → verifica codice → attiva.
 * La **disattivazione** non è implementabile (auth-local non espone `/2fa/disable`) — rinvio tracciato.
 */
export function SecurityPage() {
  const { t } = useTranslation()
  const config = useConfig()
  const [enrollment, setEnrollment] = useState<EnrollResult | null>(null)
  const [enabled, setEnabled] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const form = useForm<z.infer<ReturnType<typeof totpSchema>>>({
    resolver: zodResolver(totpSchema(t)),
    defaultValues: { code: '' },
  })

  const onEnroll = async () => {
    setError(null)
    setBusy(true)
    try {
      const token = getAccessToken()
      if (!token) throw new Error('no token')
      setEnrollment(await enroll2fa(config.authBaseUrl, token))
    } catch (err) {
      setError(authErrorMessage(err, t))
    } finally {
      setBusy(false)
    }
  }

  const onVerify = form.handleSubmit(async (values) => {
    setError(null)
    try {
      const token = getAccessToken()
      if (!token) throw new Error('no token')
      await verify2fa(config.authBaseUrl, token, values.code)
      setEnabled(true)
    } catch (err) {
      setError(authErrorMessage(err, t, { 401: t('errors.invalidCode') }))
    }
  })

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold text-fg">{t('nav.security')}</h1>
      <Card>
        <CardHeader>
          <CardTitle>{t('twofa.title')}</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {enabled ? (
            <p role="status" className="text-sm text-success">
              {t('twofa.enabled')}
            </p>
          ) : !enrollment ? (
            <>
              <p className="text-sm text-fg-muted">{t('twofa.nudge')}</p>
              <Button type="button" onClick={() => void onEnroll()} disabled={busy}>
                {busy ? t('twofa.enrolling') : t('twofa.enable')}
              </Button>
            </>
          ) : (
            <div className="space-y-4">
              <p className="text-sm text-fg-muted">{t('twofa.scanHint')}</p>
              <div className="inline-block rounded-md bg-white p-3">
                <QRCodeSVG value={enrollment.otpauthUri} size={160} />
              </div>
              <p className="text-sm">
                <span className="text-fg-muted">{t('twofa.secretLabel')}: </span>
                <code className="font-mono">{enrollment.secret}</code>
              </p>
              <form onSubmit={onVerify} className="space-y-3" noValidate>
                <Field
                  id="twofa-code"
                  label={t('twofa.codeHint')}
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  error={form.formState.errors.code?.message}
                  {...form.register('code')}
                />
                <Button type="submit" disabled={form.formState.isSubmitting}>
                  {t('twofa.confirm')}
                </Button>
              </form>
            </div>
          )}
          {error && (
            <p role="alert" className="text-sm text-danger">
              {error}
            </p>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
