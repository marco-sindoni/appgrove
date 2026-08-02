import { useState } from 'react'
import { Badge, Button, SegmentedControl } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { Modal } from '../shell/Modal'
import { formatPrice } from './checkoutMachine'
import { useAppTiers } from './checkoutApi'
import { useChangeTier } from './subscriptionsApi'
import { changeSummary, tierOptions, type TierOption } from './subscriptionsView'

// Le chiavi i18n derivate a runtime (descrittori dei limiti) sono opache al tipo forte di `t`.
type TKey = never

/**
 * Finestra "cambia piano" (UC 0067 §6): a differenza del riquadro di pulsanti che sostituisce, mostra
 * **prezzo e limiti** di ogni piano, marca quello attuale, indica il passo successivo consigliato e
 * **disabilita** i piani che la regola di riduzione rifiuterebbe adesso, spiegando il perché — invece di
 * lasciarli cliccabili e mostrare un errore dopo. Il ciclo di fatturazione è una scelta, non più un valore
 * fisso: si può passare da mensile ad annuale nello stesso gesto.
 *
 * Due passi: scelta, poi conferma che dice cosa succede e da quando. La conferma è nell'applicazione e non
 * più del browser, perché deve contenere la data ed essere tradotta.
 */
export function ChangePlanDialog({
  appSlug,
  currentTierKey,
  currentPeriodEnd,
  blockedTiers,
  onClose,
  onSent,
}: {
  appSlug: string
  currentTierKey: string | null
  currentPeriodEnd: string | null
  blockedTiers: Record<string, string> | null | undefined
  onClose: () => void
  onSent: () => void
}) {
  const { t, i18n } = useTranslation()
  const [cycle, setCycle] = useState('monthly')
  const [selected, setSelected] = useState<TierOption | null>(null)
  const tiers = useAppTiers(appSlug)
  const change = useChangeTier(appSlug)

  const options = tierOptions(tiers.data?.tiers, currentTierKey, cycle, blockedTiers)

  if (selected) {
    const summary = changeSummary(selected.direction, selected.name, currentPeriodEnd, i18n.language)
    return (
      <Modal
        title={t(
          selected.direction === 'downgrade'
            ? 'subscriptions.confirmDowngradeTitle'
            : 'subscriptions.confirmUpgradeTitle',
        )}
        onClose={onClose}
      >
        <p className="mt-2 text-sm text-fg-muted">{t(summary.key as TKey, summary.params)}</p>
        {change.isError && (
          <p role="alert" className="mt-3 text-sm text-danger">
            {t('subscriptions.blockedTitle')}
          </p>
        )}
        <div className="mt-6 flex justify-end gap-3">
          <Button variant="ghost" onClick={() => setSelected(null)} disabled={change.isPending}>
            {t('common.back')}
          </Button>
          <Button
            disabled={change.isPending}
            onClick={() =>
              change.mutate(
                { targetTierKey: selected.key, billingCycle: cycle },
                {
                  onSuccess: () => {
                    onSent()
                    onClose()
                  },
                },
              )
            }
          >
            {t('subscriptions.confirmAction')}
          </Button>
        </div>
      </Modal>
    )
  }

  return (
    <Modal title={t('subscriptions.changePlan')} onClose={onClose} size="lg">
      <div className="mt-4">
        <SegmentedControl
          options={[
            { value: 'monthly', label: t('checkout.monthly') },
            { value: 'annual', label: t('checkout.annual') },
          ]}
          value={cycle}
          onValueChange={setCycle}
          aria-label={t('subscriptions.billingCycle')}
        />
      </div>

      {tiers.isLoading && <p className="mt-4 text-sm text-fg-muted">{t('states.loading')}</p>}
      {tiers.isError && (
        <p role="alert" className="mt-4 text-sm text-danger">
          {t('states.error')}
        </p>
      )}

      <ul className="mt-4 space-y-3">
        {options.map((option) => (
          <li
            key={option.key}
            className={`rounded-md border p-4 ${
              option.blockedReason ? 'border-line opacity-70' : 'border-line'
            }`}
          >
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <span className="font-medium text-fg">{option.name}</span>
                {option.isCurrent && <Badge tone="accent">{t('subscriptions.currentPlan')}</Badge>}
                {option.isRecommended && !option.isCurrent && (
                  <Badge tone="success">{t('subscriptions.recommended')}</Badge>
                )}
              </div>
              <span className="text-sm text-fg-muted">
                {option.amount == null || option.amount === 0
                  ? t('subscriptions.freePlan')
                  : `${formatPrice(option.amount, option.currency ?? 'EUR', i18n.language)} ${t(
                      cycle === 'annual' ? 'checkout.perYear' : 'checkout.perMonth',
                    )}`}
              </span>
            </div>

            {option.limits.length > 0 && (
              <p className="mt-1 text-sm text-fg-muted">
                {option.limits.map((l, i) => (
                  <span key={l.key + i}>
                    {i > 0 && ' · '}
                    {t(l.key as TKey, l.params)}
                  </span>
                ))}
              </p>
            )}

            {option.blockedReason && (
              <p className="mt-2 text-sm text-warning">{option.blockedReason}</p>
            )}

            <div className="mt-3">
              <Button
                size="sm"
                variant="secondary"
                disabled={option.isCurrent || !!option.blockedReason}
                onClick={() => setSelected(option)}
              >
                {option.isCurrent ? t('subscriptions.currentPlan') : t('subscriptions.choosePlan')}
              </Button>
            </div>
          </li>
        ))}
      </ul>

      <div className="mt-6 flex justify-end">
        <Button variant="ghost" onClick={onClose}>
          {t('common.cancel')}
        </Button>
      </div>
    </Modal>
  )
}
