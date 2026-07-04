import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Badge, Button, Card, CardContent, CardHeader, CardTitle } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useAppTiers } from './checkoutApi'
import {
  useCancelSubscription,
  useChangeTier,
  useMySubscriptions,
  usePortalSession,
  useResumeSubscription,
} from './subscriptionsApi'
import { limitDescriptors, statusLine } from './subscriptionsView'

type Subscription = NonNullable<
  NonNullable<ReturnType<typeof useMySubscriptions>['data']>['subscriptions']
>[number]

// Le chiavi i18n derivate a runtime (fase, descrittori) sono opache al tipo forte di `t` → cast a `never`.
type TKey = never

const PHASE_TONE: Record<string, 'success' | 'warning' | 'neutral'> = {
  TRIAL: 'success',
  ACTIVE: 'success',
  CANCELING: 'warning',
  GRACE: 'warning',
  ENDED: 'neutral',
}

/**
 * Pannello self-service (UC 0028): elenca gli abbonamenti del tenant (anche non-attivi) con status/piano/
 * cambio schedulato, limiti del piano, azioni (upgrade/downgrade/disdici/riattiva), pulsante portal Paddle,
 * e — a subscription scaduta — riattiva + CTA diritti GDPR. Legge il read-model dedicato `/me/subscriptions`.
 */
export function SubscriptionsPanel({ onReactivate }: { onReactivate: (appSlug: string) => void }) {
  const { t } = useTranslation()
  const query = useMySubscriptions()

  return (
    <section className="space-y-4" aria-label={t('subscriptions.title')}>
      <header className="space-y-1">
        <h2 className="text-xl font-semibold text-fg">{t('subscriptions.title')}</h2>
        <p className="text-sm text-fg-muted">{t('subscriptions.subtitle')}</p>
      </header>

      {query.isLoading && <p className="text-sm text-fg-muted">{t('states.loading')}</p>}
      {query.isError && (
        <p role="alert" className="text-sm text-danger">
          {t('states.error')}
        </p>
      )}
      {query.data && query.data.subscriptions?.length === 0 && (
        <Card>
          <CardContent className="text-fg-muted">{t('subscriptions.empty')}</CardContent>
        </Card>
      )}

      <div className="grid gap-4">
        {query.data?.subscriptions?.map((sub) => (
          <SubscriptionCard key={sub.appSlug} sub={sub} onReactivate={onReactivate} />
        ))}
      </div>
    </section>
  )
}

function SubscriptionCard({
  sub,
  onReactivate,
}: {
  sub: Subscription
  onReactivate: (appSlug: string) => void
}) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const appSlug = sub.appSlug ?? ''
  const [choosing, setChoosing] = useState(false)

  const cancel = useCancelSubscription(appSlug)
  const resume = useResumeSubscription(appSlug)
  const portal = usePortalSession()

  const status = statusLine(sub)
  const limits = limitDescriptors(sub.limits)
  const busy = cancel.isPending || resume.isPending

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between gap-2">
        <CardTitle>{sub.appName ?? sub.appSlug}</CardTitle>
        <Badge tone={PHASE_TONE[sub.phase ?? ''] ?? 'neutral'}>
          {t(`subscriptions.phase.${sub.phase ?? 'ACTIVE'}` as TKey)}
        </Badge>
      </CardHeader>
      <CardContent className="space-y-4">
        {sub.tierKey && (
          <p className="text-sm text-fg">
            {t('subscriptions.tier')}: <span className="font-medium">{sub.tierName ?? sub.tierKey}</span>
          </p>
        )}
        {status && (
          <p className="text-sm text-fg-muted">{t(status.key as TKey, status.params)}</p>
        )}
        {limits.length > 0 && (
          <div className="text-sm text-fg-muted">
            <span className="font-medium text-fg">{t('subscriptions.planLimits')}:</span>{' '}
            {limits.map((l, i) => (
              <span key={l.key + i}>
                {i > 0 && ' · '}
                {t(l.key as TKey, l.params)}
              </span>
            ))}
          </div>
        )}

        {(cancel.isError || resume.isError || portal.isError) && (
          <p role="alert" className="text-sm text-danger">
            {t('subscriptions.actionError')}
          </p>
        )}

        {choosing && (
          <TierChooser
            appSlug={appSlug}
            currentTierKey={sub.tierKey ?? null}
            onDone={() => setChoosing(false)}
          />
        )}

        <div className="flex flex-wrap gap-2">
          {(sub.canUpgrade || sub.canDowngrade) && !choosing && (
            <Button size="sm" variant="secondary" onClick={() => setChoosing(true)}>
              {t('subscriptions.changePlan')}
            </Button>
          )}
          {sub.canCancel && (
            <Button
              size="sm"
              variant="ghost"
              disabled={busy}
              onClick={() => {
                if (window.confirm(t('subscriptions.confirmCancel'))) cancel.mutate()
              }}
            >
              {t('subscriptions.cancel')}
            </Button>
          )}
          {sub.canResume && (
            <Button size="sm" disabled={busy} onClick={() => resume.mutate()}>
              {t('subscriptions.resume')}
            </Button>
          )}
          {sub.canReactivate && (
            <Button size="sm" onClick={() => onReactivate(appSlug)}>
              {t('subscriptions.reactivate')}
            </Button>
          )}
          {sub.portalAvailable && (
            <Button
              size="sm"
              variant="secondary"
              disabled={portal.isPending}
              onClick={() =>
                portal.mutate(undefined, {
                  onSuccess: (res) => {
                    if (res.url) window.open(res.url, '_blank', 'noopener')
                  },
                })
              }
            >
              {t('subscriptions.manage')}
            </Button>
          )}
          {sub.phase === 'ENDED' && (
            <Button size="sm" variant="ghost" onClick={() => navigate('/privacy')}>
              {t('subscriptions.gdpr')}
            </Button>
          )}
        </div>
      </CardContent>
    </Card>
  )
}

/** Scelta del tier di destinazione per il cambio piano (upgrade/downgrade deciso server-side). */
function TierChooser({
  appSlug,
  currentTierKey,
  onDone,
}: {
  appSlug: string
  currentTierKey: string | null
  onDone: () => void
}) {
  const { t } = useTranslation()
  const tiers = useAppTiers(appSlug)
  const change = useChangeTier(appSlug)

  return (
    <div className="rounded-md border border-line p-3">
      {tiers.isLoading && <p className="text-sm text-fg-muted">{t('states.loading')}</p>}
      <div className="flex flex-wrap gap-2">
        {tiers.data?.tiers
          ?.filter((tier) => tier.key !== currentTierKey)
          .map((tier) => (
            <Button
              key={tier.tierId}
              size="sm"
              variant="secondary"
              disabled={change.isPending}
              onClick={() => {
                if (!tier.key) return
                if (window.confirm(t('subscriptions.confirmChange'))) {
                  change.mutate(
                    { targetTierKey: tier.key, billingCycle: 'monthly' },
                    { onSuccess: onDone },
                  )
                }
              }}
            >
              {tier.name}
            </Button>
          ))}
        <Button size="sm" variant="ghost" onClick={onDone}>
          {t('checkout.back')}
        </Button>
      </div>
      {change.isError && (
        <p role="alert" className="mt-2 text-sm text-danger">
          {t('subscriptions.blockedTitle')}
        </p>
      )}
    </div>
  )
}
