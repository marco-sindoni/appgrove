import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Button, cn } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useConfig } from '../../config'
import { useAuthStore } from '../../auth/authStore'
import { signup, resendVerification, refreshSession } from '../../auth/authApi'
import { signupSchema, workspaceSchema } from '../../auth/schemas'
import { authErrorMessage } from '../../auth/authErrors'
import { useCurrentAccount, useUpdateAccountName } from '../../api/hooks'
import { AuthLayout } from './AuthLayout'
import { Field } from './Field'

type Step = 'account' | 'verify' | 'workspace' | 'done'
const STEPS: Step[] = ['account', 'verify', 'workspace', 'done']

/**
 * Wizard di registrazione + onboarding (UC 0017 UC1/UC8, Opzione A): un solo componente coeso che
 * attraversa il confine auth. Account/Verifica sono pre-login; dopo la verifica (auto-login) si prosegue
 * a Workspace (rinomina account) → Done. Lo step "Pick apps" è fuori scope (manca il backend #09).
 */
export function OnboardingWizard() {
  const { t } = useTranslation()
  const [params] = useSearchParams()
  const authenticated = useAuthStore((s) => s.status === 'authenticated')

  // Atterraggio post-verifica (da /verify): riprende allo step Workspace se la sessione è attiva.
  const initial: Step = params.get('step') === 'workspace' && authenticated ? 'workspace' : 'account'
  const [step, setStep] = useState<Step>(initial)
  const [email, setEmail] = useState('')

  const labels: Record<Step, string> = {
    account: t('signup.accountStep'),
    verify: t('signup.verifyStep'),
    workspace: t('signup.workspaceStep'),
    done: t('signup.doneStep'),
  }
  const activeIndex = STEPS.indexOf(step)

  return (
    <AuthLayout
      title={t('signup.title')}
      footer={
        step === 'account' ? (
          <p>
            {t('auth.haveAccount')}{' '}
            <Link to="/login" className="text-accent hover:underline">
              {t('auth.signIn')}
            </Link>
          </p>
        ) : undefined
      }
    >
      <ol className="mb-5 flex items-center justify-between text-xs" aria-label={t('signup.title')}>
        {STEPS.map((s, i) => (
          <li
            key={s}
            aria-current={s === step ? 'step' : undefined}
            className={cn(
              'font-medium',
              i === activeIndex ? 'text-accent' : i < activeIndex ? 'text-fg' : 'text-fg-muted',
            )}
          >
            {labels[s]}
          </li>
        ))}
      </ol>

      {step === 'account' && (
        <AccountStep
          onDone={(e) => {
            setEmail(e)
            setStep('verify')
          }}
        />
      )}
      {step === 'verify' && <VerifyStep email={email} onContinue={() => setStep('workspace')} />}
      {step === 'workspace' && <WorkspaceStep onDone={() => setStep('done')} />}
      {step === 'done' && <DoneStep />}
    </AuthLayout>
  )
}

function AccountStep({ onDone }: { onDone: (email: string) => void }) {
  const { t, i18n } = useTranslation()
  const config = useConfig()
  const [formError, setFormError] = useState<string | null>(null)
  const form = useForm<z.infer<ReturnType<typeof signupSchema>>>({
    resolver: zodResolver(signupSchema(t)),
    defaultValues: { email: '', password: '', displayName: '' },
  })

  const onSubmit = form.handleSubmit(async (values) => {
    setFormError(null)
    try {
      await signup(config.authBaseUrl, {
        email: values.email,
        password: values.password,
        displayName: values.displayName || undefined,
        // Lingua attiva dell'interfaccia (UC 0018): diventa la lingua delle email dell'utente e
        // resta memorizzata sul profilo — la reimpostazione password parte da un solo indirizzo,
        // senza contesto, e non avrebbe altro modo di sapere in che lingua scrivere.
        locale: i18n.language,
      })
      onDone(values.email)
    } catch (err) {
      setFormError(authErrorMessage(err, t))
    }
  })

  return (
    <form onSubmit={onSubmit} className="space-y-4" noValidate>
      <Field
        id="signup-name"
        label={`${t('common.displayName')} (${t('common.optional')})`}
        autoComplete="name"
        error={form.formState.errors.displayName?.message}
        {...form.register('displayName')}
      />
      <Field
        id="signup-email"
        type="email"
        autoComplete="email"
        label={t('common.email')}
        error={form.formState.errors.email?.message}
        {...form.register('email')}
      />
      <Field
        id="signup-password"
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
        {t('signup.submit')}
      </Button>
    </form>
  )
}

function VerifyStep({ email, onContinue }: { email: string; onContinue: () => void }) {
  const { t } = useTranslation()
  const config = useConfig()
  const setSession = useAuthStore((s) => s.setSession)
  const [notYet, setNotYet] = useState(false)
  const [resent, setResent] = useState(false)
  const [busy, setBusy] = useState(false)

  const onCheck = async () => {
    setBusy(true)
    setNotYet(false)
    const tokens = await refreshSession(config.authBaseUrl)
    setBusy(false)
    if (tokens) {
      setSession(tokens)
      onContinue()
    } else {
      setNotYet(true)
    }
  }

  const onResend = async () => {
    if (email) await resendVerification(config.authBaseUrl, email).catch(() => undefined)
    setResent(true)
  }

  return (
    <div className="space-y-4">
      <p className="text-sm text-fg-muted">{t('verify.sent', { email })}</p>
      <p className="text-sm text-fg-muted">{t('verify.pendingHint')}</p>
      {notYet && (
        <p role="alert" className="text-sm text-danger">
          {t('verify.notYet')}
        </p>
      )}
      {resent && (
        <p role="status" className="text-sm text-success">
          {t('verify.resent')}
        </p>
      )}
      <Button type="button" className="w-full" onClick={() => void onCheck()} disabled={busy}>
        {t('verify.continue')}
      </Button>
      <Button type="button" variant="ghost" className="w-full" onClick={() => void onResend()}>
        {t('verify.resend')}
      </Button>
    </div>
  )
}

function WorkspaceStep({ onDone }: { onDone: () => void }) {
  const { t } = useTranslation()
  const account = useCurrentAccount()
  const update = useUpdateAccountName()
  const form = useForm<z.infer<ReturnType<typeof workspaceSchema>>>({
    resolver: zodResolver(workspaceSchema(t)),
    values: { name: account.data?.name ?? '' },
  })

  const onSubmit = form.handleSubmit(async (values) => {
    try {
      await update.mutateAsync(values.name)
      onDone()
    } catch {
      // resta sullo step; l'errore di rete è transitorio
    }
  })

  return (
    <form onSubmit={onSubmit} className="space-y-4" noValidate>
      <p className="text-sm text-fg-muted">{t('workspace.hint')}</p>
      <Field
        id="workspace-name"
        label={t('common.workspaceName')}
        error={form.formState.errors.name?.message}
        {...form.register('name')}
      />
      <Button type="submit" className="w-full" disabled={form.formState.isSubmitting || update.isPending}>
        {t('workspace.save')}
      </Button>
    </form>
  )
}

function DoneStep() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  return (
    <div className="space-y-4 text-center">
      <p className="text-sm text-fg-muted">{t('done.hint')}</p>
      <Button type="button" className="w-full" onClick={() => navigate('/')}>
        {t('done.goToDashboard')}
      </Button>
    </div>
  )
}

export default OnboardingWizard
