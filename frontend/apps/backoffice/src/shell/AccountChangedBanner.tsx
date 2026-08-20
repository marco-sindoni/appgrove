import { Button } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useMyMemberships } from '../api/hooks'
import { useAuthStore } from '../auth/authStore'

/**
 * Avviso «l'account attivo è cambiato in un'altra scheda» (UC 0117 §6).
 *
 * Dopo un cambio di account, una scheda rimasta aperta ha in mano un token che punta ancora
 * all'account di prima. **Non è un varco**: quel token vale per un account a cui la persona
 * appartiene davvero, e non le concede nulla che non avesse già. È però **confusione** — e la
 * confusione su chi si sta guardando è essa stessa un rischio, perché si finisce per credere di
 * lavorare in un'azienda mentre si sta lavorando nell'altra.
 *
 * Il confronto è fra l'account del **token in uso** e l'account attivo secondo il server: la lettura
 * delle appartenenze si rinfresca al ritorno sulla scheda, che è esattamente il momento in cui una
 * scheda dimenticata torna sotto gli occhi di qualcuno.
 *
 * Il rimedio è un ricaricamento, non un aggiornamento dello stato in memoria: mezza applicazione con
 * l'account nuovo e mezza col vecchio è il modo peggiore di sbagliare.
 */
export function AccountChangedBanner() {
  const { t } = useTranslation()
  const { data, isSuccess } = useMyMemberships()
  const tenantId = useAuthStore((s) => s.claims?.tenantId)

  // Solo con una lettura riuscita: un guasto di rete non deve produrre un avviso che dice il falso.
  if (!isSuccess || !tenantId || data.activeAccountId === tenantId) {
    return null
  }

  return (
    <div role="alert" className="border-b border-warning/40 bg-warning/10 px-6 py-3">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="min-w-0 text-sm font-semibold text-fg">{t('accountSwitch.changed')}</p>
        <Button size="sm" className="shrink-0" onClick={() => window.location.assign('/')}>
          {t('accountSwitch.reload')}
        </Button>
      </div>
    </div>
  )
}
