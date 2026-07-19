import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useTranslation } from '@appgrove/i18n'
import { useConfig } from '../../config'
import { useAuthStore } from '../../auth/authStore'
import { emailActionLinkFrom, verifyEmail } from '../../auth/authApi'
import { AuthLayout } from './AuthLayout'

/**
 * Atterraggio del link di verifica email: verifica server-side + auto-login, poi prosegue allo step
 * Workspace dell'onboarding (Opzione A). Col provider Cognito (UC 0015) la conferma avviene senza
 * auto-login: si mostra l'esito e si rimanda al login. Su token invalido/scaduto mostra l'errore.
 *
 * Accetta **entrambe** le forme del collegamento (UC 0018): `?token=` (provider locale) e
 * `?email=&code=` (email generata da Cognito, dove il codice non esiste ancora quando il messaggio
 * viene composto). Le due convivono: un collegamento vecchio continua a funzionare.
 */
export function VerifyEmailPage() {
  const { t } = useTranslation()
  const config = useConfig()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const link = emailActionLinkFrom(params)
  const setSession = useAuthStore((s) => s.setSession)
  const [state, setState] = useState<'verifying' | 'confirmed' | 'failed'>('verifying')
  const ran = useRef(false)

  useEffect(() => {
    if (ran.current) return
    ran.current = true
    if (!link) {
      setState('failed')
      return
    }
    void (async () => {
      try {
        const tokens = await verifyEmail(config.authBaseUrl, link)
        if (tokens) {
          setSession(tokens)
          navigate('/signup?step=workspace', { replace: true })
        } else {
          setState('confirmed')
        }
      } catch {
        setState('failed')
      }
    })()
    // `link` è ricalcolato a ogni render ma la guardia `ran` fa girare l'effetto una volta sola.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [config.authBaseUrl, setSession, navigate])

  return (
    <AuthLayout
      title={t('verify.title')}
      footer={
        <Link to="/login" className="text-accent hover:underline">
          {t('auth.signIn')}
        </Link>
      }
    >
      {state === 'failed' ? (
        <p role="alert" className="text-sm text-danger">
          {t('verify.failed')}
        </p>
      ) : state === 'confirmed' ? (
        <p role="status" className="text-sm text-fg-muted">
          {t('verify.confirmed')}
        </p>
      ) : (
        <p role="status" className="text-sm text-fg-muted">
          {t('verify.verifying')}
        </p>
      )}
    </AuthLayout>
  )
}

export default VerifyEmailPage
