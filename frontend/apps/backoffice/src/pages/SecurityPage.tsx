import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
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
import { TWOFA_STATUS_KEY, useTwoFaStatus } from '../auth/twoFaApi'
import { Field } from './auth/Field'

/**
 * Setup 2FA TOTP dal profilo (UC 0017 UC10): enroll → QR/secret → verifica codice → attiva.
 * La **disattivazione** non è implementabile (il servizio auth non espone `/2fa/disable`) — rinvio tracciato.
 *
 * <p>Dalla change 0078 la pagina conosce lo **stato reale** del secondo fattore (UC 0097): a chi lo ha
 * già attivo non si propone più di attivarlo, e l'esito della verifica aggiorna la stessa lettura che
 * alimenta l'avviso della Dashboard — così le due superfici non possono dire cose diverse.
 */
export function SecurityPage() {
  const { t } = useTranslation()
  const config = useConfig()
  const queryClient = useQueryClient()
  const status = useTwoFaStatus()
  const [enrollment, setEnrollment] = useState<EnrollResult | null>(null)
  const [justEnabled, setJustEnabled] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const enabled = justEnabled || status.data === true

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
      setJustEnabled(true)
      void queryClient.invalidateQueries({ queryKey: TWOFA_STATUS_KEY })
    } catch (err) {
      setError(authErrorMessage(err, t, { 401: t('errors.invalidCode') }))
    }
  })

  return (
    <div className="space-y-[22px]">
      <h1 className="text-[27px] font-extrabold tracking-[-0.025em] text-fg">{t('nav.security')}</h1>
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
