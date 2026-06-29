import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Badge,
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  SegmentedControl,
} from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { MODULES } from '../registry/registry'
import { useAppTiers, useStartCheckout, useAppSubscriptionStatus } from '../billing/checkoutApi'
import { createPaddle } from '../billing/paddle'
import {
  annualFreeMonths,
  formatPrice,
  phaseFromPoll,
  shouldPoll,
  shouldReassure,
  type CheckoutPhase,
} from '../billing/checkoutMachine'

type Cycle = 'monthly' | 'annual'

/**
 * Sezione fatturazione / checkout (UC 0024): scelta app → scelta tier (default annuale + sconto + trial)
 * → checkout server-initiated → overlay Paddle (stub in locale) → UX a polling rassicurante. L'attivazione
 * avviene **solo** via webhook; qui si fa polling dello stato finché `active`.
 */
export function Billing() {
  const { t } = useTranslation()
  const [appSlug, setAppSlug] = useState<string | null>(null)

  return (
    <div className="space-y-6">
      <header className="space-y-1">
        <h1 className="text-2xl font-semibold text-fg">{t('checkout.title')}</h1>
        <p className="text-sm text-fg-muted">{t('checkout.subtitle')}</p>
      </header>
      {appSlug ? (
        <CheckoutFlow appSlug={appSlug} onBack={() => setAppSlug(null)} />
      ) : (
        <AppPicker onPick={setAppSlug} />
      )}
    </div>
  )
}

/** Lista delle app acquistabili (dal registry build-time). La vetrina real-catalog è un follow-up (UC 0024). */
function AppPicker({ onPick }: { onPick: (slug: string) => void }) {
  const { t } = useTranslation()
  if (MODULES.length === 0) {
    return (
      <Card>
        <CardContent className="text-fg-muted">{t('checkout.noApps')}</CardContent>
      </Card>
    )
  }
  return (
    <section aria-label={t('checkout.chooseApp')} className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {MODULES.map((m) => (
        <Card key={m.id}>
          <CardHeader>
            <CardTitle>{m.name}</CardTitle>
          </CardHeader>
          <CardContent>
            <Button onClick={() => onPick(m.id)}>{t('checkout.subscribe')}</Button>
          </CardContent>
        </Card>
      ))}
    </section>
  )
}

function CheckoutFlow({ appSlug, onBack }: { appSlug: string; onBack: () => void }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const tiersQuery = useAppTiers(appSlug)
  const startCheckout = useStartCheckout(appSlug)

  const [cycle, setCycle] = useState<Cycle>('annual')
  const [phase, setPhase] = useState<CheckoutPhase>('idle')
  const [activatingSince, setActivatingSince] = useState<number | null>(null)
  const [now, setNow] = useState(() => Date.now())
  const paddleRef = useRef<ReturnType<typeof createPaddle> | null>(null)

  // Polling dello stato: attivo solo durante l'attivazione, si ferma da sé quando active.
  const statusQuery = useAppSubscriptionStatus(appSlug, shouldPoll(phase))
  useEffect(() => {
    setPhase((p) => phaseFromPoll(p, statusQuery.data))
  }, [statusQuery.data])

  // Tic dell'orologio per la soglia "rassicurante" (mai un errore) mentre si attende il webhook.
  useEffect(() => {
    if (phase !== 'activating') return
    const id = setInterval(() => setNow(Date.now()), 1_000)
    return () => clearInterval(id)
  }, [phase])

  function subscribe(tierKey: string | undefined) {
    if (!tierKey) return
    setPhase('submitting')
    startCheckout.mutate(
      { tierKey, billingCycle: cycle },
      {
        onSuccess: (res) => {
          const token = res.checkoutToken
          if (!token) {
            setPhase('error')
            return
          }
          const paddle = createPaddle({
            eventCallback: (event) => {
              if (event.name === 'checkout.completed') {
                // SOLO UX: l'attivazione vera arriva dal webhook → si passa al polling (#09 C16/C17).
                setActivatingSince(Date.now())
                setNow(Date.now())
                setPhase('activating')
              }
            },
          })
          paddleRef.current = paddle
          paddle.Checkout.open({ transactionToken: token })
        },
        onError: () => setPhase('error'),
      },
    )
  }

  if (phase === 'activating' || phase === 'active') {
    return (
      <ActivationState
        phase={phase}
        reassuring={shouldReassure(phase, activatingSince, now)}
        onOpenApp={() => navigate(`/app/${appSlug}`)}
      />
    )
  }

  if (phase === 'error') {
    return (
      <Card>
        <CardHeader>
          <CardTitle>{t('checkout.errorTitle')}</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <p className="text-sm text-fg-muted">{t('checkout.errorBody')}</p>
          <div className="flex gap-2">
            <Button onClick={() => setPhase('idle')}>{t('checkout.retry')}</Button>
            <Button variant="ghost" onClick={onBack}>
              {t('checkout.back')}
            </Button>
          </div>
        </CardContent>
      </Card>
    )
  }

  return (
    <section className="space-y-4">
      <div className="flex items-center justify-between">
        <Button variant="ghost" size="sm" onClick={onBack}>
          ← {t('checkout.back')}
        </Button>
        <SegmentedControl
          aria-label={t('checkout.annual')}
          value={cycle}
          onValueChange={(v) => setCycle(v as Cycle)}
          options={[
            { value: 'monthly', label: t('checkout.monthly') },
            { value: 'annual', label: t('checkout.annual') },
          ]}
        />
      </div>

      {tiersQuery.isLoading && <p className="text-sm text-fg-muted">{t('states.loading')}</p>}
      {tiersQuery.isError && (
        <p role="alert" className="text-sm text-danger">
          {t('states.error')}
        </p>
      )}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {tiersQuery.data?.tiers?.map((tier) => (
          <TierCard
            key={tier.tierId}
            tier={tier}
            cycle={cycle}
            submitting={phase === 'submitting'}
            onSubscribe={() => subscribe(tier.key)}
          />
        ))}
      </div>
    </section>
  )
}

type TierData = NonNullable<NonNullable<ReturnType<typeof useAppTiers>['data']>['tiers']>[number]

function TierCard({
  tier,
  cycle,
  submitting,
  onSubscribe,
}: {
  tier: TierData
  cycle: Cycle
  submitting: boolean
  onSubscribe: () => void
}) {
  const { t } = useTranslation()
  const prices = tier.prices ?? []
  const price = prices.find((p) => p.billingCycle === cycle)
  const monthly = prices.find((p) => p.billingCycle === 'monthly')?.amount ?? null
  const annual = prices.find((p) => p.billingCycle === 'annual')?.amount ?? null
  const freeMonths = annualFreeMonths(monthly, annual)

  return (
    <Card>
      <CardHeader className="space-y-2">
        <CardTitle>{tier.name}</CardTitle>
        <div className="flex flex-wrap gap-2">
          {(tier.trialDays ?? 0) > 0 && (
            <Badge tone="success">{t('checkout.trialDays', { days: tier.trialDays ?? 0 })}</Badge>
          )}
          {cycle === 'annual' && freeMonths > 0 && <Badge tone="accent">{t('checkout.save2Months')}</Badge>}
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {price && (
          <p className="text-xl font-semibold text-fg">
            {formatPrice(price.amount ?? 0, price.currency ?? 'EUR')}
            <span className="text-sm font-normal text-fg-muted">
              {cycle === 'annual' ? t('checkout.perYear') : t('checkout.perMonth')}
            </span>
          </p>
        )}
        <Button className="w-full" disabled={submitting || !price} onClick={onSubscribe}>
          {t('checkout.subscribe')}
        </Button>
      </CardContent>
    </Card>
  )
}

function ActivationState({
  phase,
  reassuring,
  onOpenApp,
}: {
  phase: 'activating' | 'active'
  reassuring: boolean
  onOpenApp: () => void
}) {
  const { t } = useTranslation()
  if (phase === 'active') {
    return (
      <Card>
        <CardContent className="space-y-4 py-8 text-center">
          <p className="text-lg font-semibold text-fg">{t('checkout.activated')}</p>
          <Button onClick={onOpenApp}>{t('checkout.openApp')}</Button>
        </CardContent>
      </Card>
    )
  }
  return (
    <Card>
      <CardContent className="flex flex-col items-center gap-3 py-10 text-center" aria-live="polite">
        <span
          aria-hidden="true"
          className="size-8 animate-spin rounded-full border-2 border-line border-t-accent"
        />
        <p className="text-base font-medium text-fg">{t('checkout.activating')}</p>
        <p className="max-w-sm text-sm text-fg-muted">
          {reassuring ? t('checkout.stillActivating') : t('checkout.activatingHint')}
        </p>
      </CardContent>
    </Card>
  )
}

export default Billing
