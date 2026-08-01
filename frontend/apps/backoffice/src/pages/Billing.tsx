import { useState } from 'react'
import { Button, Card, CardContent, CardHeader, CardTitle } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { MODULES } from '../registry/registry'
import { CheckoutFlow } from '../billing/CheckoutFlow'
import { SubscriptionsPanel } from '../billing/SubscriptionsPanel'

/**
 * Sezione fatturazione / checkout (UC 0024): abbonamenti in corso, scelta dell'app da attivare e flusso
 * di acquisto.
 *
 * <p>La griglia d'acquisto qui sotto è la **vecchia** vetrina, costruita dai moduli impacchettati nel
 * frontend: la vetrina vera, dal catalogo reale del backend, è la pagina App catalog (UC 0095). Le due
 * convivono per una change soltanto — togliere di qui l'acquisto è compito di UC 0096, che possiede la
 * pulizia di Billing.
 */
export function Billing() {
  const { t } = useTranslation()
  const [appSlug, setAppSlug] = useState<string | null>(null)

  return (
    <div className="space-y-[22px]">
      <header className="space-y-1">
        <h1 className="text-[27px] font-extrabold tracking-[-0.025em] text-fg">{t('checkout.title')}</h1>
        <p className="text-sm text-fg-muted">{t('checkout.subtitle')}</p>
      </header>
      {appSlug ? (
        <CheckoutFlow appSlug={appSlug} onBack={() => setAppSlug(null)} />
      ) : (
        <>
          <SubscriptionsPanel onReactivate={setAppSlug} />
          <AppPicker onPick={setAppSlug} />
        </>
      )}
    </div>
  )
}

/** Lista delle app acquistabili (dal registry build-time). Sostituita dal catalogo reale in UC 0096. */
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

export default Billing
