import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Badge, Button, Card, CardContent, CardHeader, CardTitle } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { ConfirmDialog } from '../pages/members/ConfirmDialog'
import { ChangePlanDialog } from './ChangePlanDialog'
import {
  useCanManageBilling,
  useCancelSubscription,
  useMySubscriptions,
  usePortalSession,
  useResumeSubscription,
} from './subscriptionsApi'
import {
  formatDate,
  isStillPending,
  limitDescriptors,
  quotaUsages,
  snapshotOf,
  statusLine,
  type PendingSnapshot,
} from './subscriptionsView'

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
 * Sezione "Abbonamenti" del backoffice (UC 0028, completata da UC 0067): una card per app con stato,
 * piano, fine periodo, cambio già programmato, **consumo della quota**, avvisi di pagamento in ritardo e
 * di scadenza, e le azioni self-service — cambia piano, disdici, riattiva, gestisci pagamento e fatture.
 *
 * Vive dentro la pagina Fatturazione (UC 0096), che è la pagina di sola fatturazione: scoprire e comprare
 * le app è un'altra cosa e sta nel catalogo.
 */
export function SubscriptionsPanel({ onReactivate }: { onReactivate: (appSlug: string) => void }) {
  const { t } = useTranslation()
  // Comandi inviati e non ancora riflessi dal read-model: finché ce n'è uno la lista si rilegge da sola.
  const [pending, setPending] = useState<Record<string, PendingSnapshot>>({})
  const query = useMySubscriptions(Object.keys(pending).length > 0)
  const subscriptions = query.data?.subscriptions

  useEffect(() => {
    if (!subscriptions) return
    setPending((current) => {
      const next: Record<string, PendingSnapshot> = {}
      for (const [slug, snap] of Object.entries(current)) {
        if (isStillPending(snap, subscriptions.find((s) => s.appSlug === slug))) next[slug] = snap
      }
      return Object.keys(next).length === Object.keys(current).length ? current : next
    })
  }, [subscriptions])

  return (
    <section className="space-y-4" aria-label={t('subscriptions.title')}>
      <header className="space-y-1">
        <h2 className="text-xl font-semibold text-fg">{t('subscriptions.title')}</h2>
        <p className="text-sm text-fg-muted">{t('subscriptions.subtitle')}</p>
      </header>

      {query.isLoading && <SubscriptionsSkeleton />}
      {query.isError && (
        <div role="alert" className="space-y-3 rounded-md border border-danger/40 bg-danger/10 p-4">
          <p className="text-sm text-danger">{t('states.error')}</p>
          <Button size="sm" variant="secondary" onClick={() => void query.refetch()}>
            {t('states.retry')}
          </Button>
        </div>
      )}
      {query.data && subscriptions?.length === 0 && <EmptyState />}

      <div className="grid gap-4">
        {subscriptions?.map((sub) => (
          <SubscriptionCard
            key={sub.appSlug}
            sub={sub}
            updating={!!pending[sub.appSlug ?? '']}
            onCommandSent={() =>
              setPending((current) => ({ ...current, [sub.appSlug ?? '']: snapshotOf(sub) }))
            }
            onReactivate={onReactivate}
          />
        ))}
      </div>
    </section>
  )
}

/**
 * Scheletro di caricamento: due card grigie con la forma di quelle vere. Una riga "Caricamento…" al posto
 * di una lista fa saltare il contenuto quando arriva; questo no.
 */
function SubscriptionsSkeleton() {
  const { t } = useTranslation()
  return (
    <div role="status" aria-label={t('states.loading')} className="grid gap-4">
      {[0, 1].map((i) => (
        <Card key={i}>
          <CardContent className="space-y-3">
            <div className="h-5 w-1/3 animate-pulse rounded bg-surface-3" />
            <div className="h-4 w-1/2 animate-pulse rounded bg-surface-3" />
            <div className="h-2 w-full animate-pulse rounded bg-surface-3" />
          </CardContent>
        </Card>
      ))}
    </div>
  )
}

/**
 * Nessun abbonamento: si dice dove si trovano le app, non si mostra una griglia d'acquisto (UC 0096).
 * Scoprire le app e pagarle sono due cose diverse, e la prima ha finalmente una pagina propria.
 */
function EmptyState() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  return (
    <Card>
      <CardContent className="space-y-3">
        <p className="text-fg-muted">{t('subscriptions.empty')}</p>
        <Button size="sm" onClick={() => navigate('/catalog')}>
          {t('subscriptions.emptyCta')}
        </Button>
      </CardContent>
    </Card>
  )
}

function SubscriptionCard({
  sub,
  updating,
  onCommandSent,
  onReactivate,
}: {
  sub: Subscription
  updating: boolean
  onCommandSent: () => void
  onReactivate: (appSlug: string) => void
}) {
  const { t, i18n } = useTranslation()
  const navigate = useNavigate()
  const appSlug = sub.appSlug ?? ''
  const [choosing, setChoosing] = useState(false)
  const [confirmingCancel, setConfirmingCancel] = useState(false)
  const canManage = useCanManageBilling()

  const cancel = useCancelSubscription(appSlug)
  const resume = useResumeSubscription(appSlug)
  const portal = usePortalSession()

  const status = statusLine(sub, i18n.language)
  const limits = limitDescriptors(sub.limits)
  const quotas = quotaUsages(sub.limits, sub.usage)
  const busy = cancel.isPending || resume.isPending
  // Il ritardo di pagamento è uno stato del fornitore, non una fase: la fase resta "attiva" perché
  // l'accesso c'è ancora (finestra di tolleranza), ed è proprio per questo che va detto a parole.
  const pastDue = sub.status === 'past_due'
  const ended = sub.phase === 'ENDED'

  const openPortal = () =>
    portal.mutate(undefined, {
      onSuccess: (res) => {
        if (res.url) window.open(res.url, '_blank', 'noopener')
      },
    })

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between gap-2">
        <CardTitle>{sub.appName ?? sub.appSlug}</CardTitle>
        <div className="flex items-center gap-2">
          {sub.appDisabled && <Badge tone="warning">{t('subscriptions.appDisabledBadge')}</Badge>}
          <Badge tone={PHASE_TONE[sub.phase ?? ''] ?? 'neutral'}>
            {t(`subscriptions.phase.${sub.phase ?? 'ACTIVE'}` as TKey)}
          </Badge>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {/*
          App messa in pausa dalla piattaforma (UC 0076): l'abbonamento resta elencato e valido, ma
          senza questo avviso il pannello direbbe "Attivo" per un'app che la barra laterale — a
          ragione — non mostra più. Meglio dire cosa sta succedendo che lasciare l'utente a indovinare.
        */}
        {sub.appDisabled && (
          <div role="status" className="rounded-md border border-warning/40 bg-warning/10 p-3">
            <p className="text-sm font-medium text-fg">{t('subscriptions.appDisabledTitle')}</p>
            <p className="mt-1 text-sm text-fg-muted">{t('subscriptions.appDisabledBody')}</p>
          </div>
        )}

        {/*
          Pagamento in ritardo: l'accesso resta per la finestra di tolleranza gestita dal fornitore, ma
          l'unica azione utile — aggiornare la carta — è nel portale, quindi l'avviso ci porta.
        */}
        {pastDue && (
          <div role="alert" className="rounded-md border border-warning/40 bg-warning/10 p-3">
            <p className="text-sm font-medium text-fg">{t('subscriptions.dunningTitle')}</p>
            <p className="mt-1 text-sm text-fg-muted">{t('subscriptions.dunningBody')}</p>
            {sub.portalAvailable && (
              <Button
                size="sm"
                className="mt-2"
                disabled={portal.isPending || !canManage}
                onClick={openPortal}
              >
                {t('subscriptions.dunningCta')}
              </Button>
            )}
          </div>
        )}

        {/*
          Abbonamento scaduto: i diritti sui dati personali non dipendono dall'abbonamento e restano
          esercitabili sempre (#09 F31) — vanno offerti qui, accanto alla riattivazione, non nascosti.
        */}
        {ended && (
          <div role="status" className="rounded-md border border-line bg-surface-2 p-3">
            <p className="text-sm font-medium text-fg">{t('subscriptions.expiredTitle')}</p>
            <p className="mt-1 text-sm text-fg-muted">{t('subscriptions.expiredBody')}</p>
          </div>
        )}

        {sub.tierKey && (
          <p className="text-sm text-fg">
            {t('subscriptions.tier')}: <span className="font-medium">{sub.tierName ?? sub.tierKey}</span>
          </p>
        )}
        {status && <p className="text-sm text-fg-muted">{t(status.key as TKey, status.params)}</p>}

        {/* Consumo misurato: "8 su 10 posti" con la barra, e un avviso quando ci si avvicina al tetto. */}
        {quotas.map((quota) => (
          <div key={quota.metric} className="space-y-1">
            <p className="text-sm text-fg-muted">
              {t('subscriptions.quotaUsage', {
                used: quota.used,
                cap: quota.cap,
                metric: quota.metric,
              })}
            </p>
            <div className="h-2 w-full overflow-hidden rounded bg-surface-3">
              <div
                className={`h-full ${quota.level === 'ok' ? 'bg-accent' : quota.level === 'warn' ? 'bg-warning' : 'bg-danger'}`}
                style={{ width: `${Math.round(quota.ratio * 100)}%` }}
              />
            </div>
            {quota.level !== 'ok' && (
              <p role="status" className="text-sm text-warning">
                {t(quota.level === 'full' ? 'subscriptions.quotaFull' : 'subscriptions.quotaWarn', {
                  metric: quota.metric,
                })}
              </p>
            )}
          </div>
        ))}

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

        {/*
          Comando inviato ma non ancora riconciliato: la riga la scrive il webhook, non la richiesta. Fino
          ad allora si dice "in aggiornamento" invece di mostrare un successo che il dato non conferma.
        */}
        {updating && (
          <p role="status" className="text-sm text-fg-muted">
            {t('subscriptions.updating')}
          </p>
        )}

        {(cancel.isError || resume.isError || portal.isError) && (
          <p role="alert" className="text-sm text-danger">
            {t('subscriptions.actionError')}
          </p>
        )}

        {!canManage && (sub.canUpgrade || sub.canDowngrade || sub.canCancel || sub.canResume) && (
          <p className="text-sm text-fg-muted">{t('subscriptions.ownerOnly')}</p>
        )}

        <div className="flex flex-wrap gap-2">
          {(sub.canUpgrade || sub.canDowngrade) && (
            <Button
              size="sm"
              variant="secondary"
              disabled={!canManage || updating}
              onClick={() => setChoosing(true)}
            >
              {t('subscriptions.changePlan')}
            </Button>
          )}
          {sub.canCancel && (
            <Button
              size="sm"
              variant="ghost"
              disabled={busy || !canManage || updating}
              onClick={() => setConfirmingCancel(true)}
            >
              {t('subscriptions.cancel')}
            </Button>
          )}
          {sub.canResume && (
            <Button
              size="sm"
              disabled={busy || !canManage || updating}
              onClick={() => resume.mutate(undefined, { onSuccess: onCommandSent })}
            >
              {t('subscriptions.resume')}
            </Button>
          )}
          {sub.canReactivate && (
            <Button size="sm" disabled={!canManage} onClick={() => onReactivate(appSlug)}>
              {t('subscriptions.reactivate')}
            </Button>
          )}
          {sub.portalAvailable && (
            <Button
              size="sm"
              variant="secondary"
              disabled={portal.isPending || !canManage}
              onClick={openPortal}
            >
              {t('subscriptions.manage')}
            </Button>
          )}
          {/*
            App sospesa dalla piattaforma: l'avviso spiega cosa sta succedendo, questo pulsante dà una
            via d'uscita a chi vuole sapere quando tornerà disponibile o come stanno le cose per il suo
            addebito — domanda commerciale che UC 0076 lascia fuori dal prodotto, e che quindi non
            possiamo rispondere in una card senza promettere qualcosa che non manteniamo.
          */}
          {sub.appDisabled && (
            <Button size="sm" variant="secondary" onClick={() => navigate('/support')}>
              {t('subscriptions.contactSupport')}
            </Button>
          )}
          {ended && (
            <Button size="sm" variant="ghost" onClick={() => navigate('/privacy')}>
              {t('subscriptions.gdpr')}
            </Button>
          )}
        </div>
      </CardContent>

      {choosing && (
        <ChangePlanDialog
          appSlug={appSlug}
          currentTierKey={sub.tierKey ?? null}
          currentPeriodEnd={sub.currentPeriodEnd ?? null}
          blockedTiers={sub.blockedTiers}
          onClose={() => setChoosing(false)}
          onSent={onCommandSent}
        />
      )}

      {confirmingCancel && (
        <ConfirmDialog
          title={t('subscriptions.confirmCancelTitle')}
          body={t('subscriptions.confirmCancelBody', {
            date: formatDate(sub.currentPeriodEnd, i18n.language),
          })}
          confirmLabel={t('subscriptions.confirmAction')}
          busy={cancel.isPending}
          onConfirm={() =>
            cancel.mutate(undefined, {
              onSuccess: () => {
                onCommandSent()
                setConfirmingCancel(false)
              },
            })
          }
          onCancel={() => setConfirmingCancel(false)}
        />
      )}
    </Card>
  )
}
