import { useState } from 'react'
import { useTranslation } from '@appgrove/i18n'
import { CheckoutFlow } from '../billing/CheckoutFlow'
import { PaymentsPanel } from '../billing/PaymentsPanel'
import { SubscriptionsPanel } from '../billing/SubscriptionsPanel'

/**
 * Pagina **Billing** — solo fatturazione (UC 0096): gli abbonamenti del workspace con le azioni
 * self-service (UC 0028) e lo storico dei pagamenti con le ricevute.
 *
 * <p>Fino alla change 0077 questa pagina faceva due mestieri: si intitolava "Get an app" ed era per metà
 * una vetrina d'acquisto costruita sui moduli impacchettati nel frontend. Quella vetrina ora vive nel
 * catalogo reale (UC 0095, pagina **App catalog**) e da qui è sparita: chi cerca "dove si compra" e chi
 * cerca "quanto ho pagato" non finiscono più nello stesso posto.
 *
 * <p>Il flusso di acquisto resta montato qui per il **solo** caso della riattivazione di un abbonamento
 * scaduto: riattivare non è scoprire — è un'azione di fatturazione che parte da una card di abbonamento
 * già in pagina, e mandarla nel catalogo costringerebbe l'utente a cercare l'app che sta già guardando.
 */
export function Billing() {
  const { t } = useTranslation()
  const [reactivateSlug, setReactivateSlug] = useState<string | null>(null)

  return (
    <div className="space-y-[22px]">
      <header className="space-y-1">
        <h1 className="text-[27px] font-extrabold tracking-[-0.025em] text-fg">{t('billing.title')}</h1>
        <p className="text-sm text-fg-muted">{t('billing.subtitle')}</p>
      </header>
      {reactivateSlug ? (
        <CheckoutFlow appSlug={reactivateSlug} onBack={() => setReactivateSlug(null)} />
      ) : (
        <>
          <SubscriptionsPanel onReactivate={setReactivateSlug} />
          <PaymentsPanel />
        </>
      )}
    </div>
  )
}

export default Billing
