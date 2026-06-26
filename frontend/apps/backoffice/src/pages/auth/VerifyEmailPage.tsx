import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useTranslation } from '@appgrove/i18n'
import { useConfig } from '../../config'
import { useAuthStore } from '../../auth/authStore'
import { verifyEmail } from '../../auth/authApi'
import { AuthLayout } from './AuthLayout'

/**
 * Atterraggio del link di verifica email (`/verify?token=`): verifica server-side + auto-login, poi
 * prosegue allo step Workspace dell'onboarding (Opzione A). Su token invalido/scaduto mostra l'errore.
 */
export function VerifyEmailPage() {
  const { t } = useTranslation()
  const config = useConfig()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const token = params.get('token')
  const setSession = useAuthStore((s) => s.setSession)
  const [failed, setFailed] = useState(false)
  const ran = useRef(false)

  useEffect(() => {
    if (ran.current) return
    ran.current = true
    if (!token) {
      setFailed(true)
      return
    }
    void (async () => {
      try {
        const tokens = await verifyEmail(config.authBaseUrl, token)
        setSession(tokens)
        navigate('/signup?step=workspace', { replace: true })
      } catch {
        setFailed(true)
      }
    })()
  }, [token, config.authBaseUrl, setSession, navigate])

  return (
    <AuthLayout
      title={t('verify.title')}
      footer={
        <Link to="/login" className="text-accent hover:underline">
          {t('auth.signIn')}
        </Link>
      }
    >
      {failed ? (
        <p role="alert" className="text-sm text-danger">
          {t('verify.failed')}
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
