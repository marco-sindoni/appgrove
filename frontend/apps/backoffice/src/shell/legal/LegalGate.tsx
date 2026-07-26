import { useState, type ReactNode } from 'react'
import { useLocation } from 'react-router-dom'
import { Button, Icon } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useLegalStatus } from '../../api/hooks'
import { LegalGateScreen } from './LegalGateScreen'

/** Le route con diritti GDPR sempre accessibili (DoD 4): non vanno mai bloccate dal gate. */
function isExempt(pathname: string): boolean {
  return pathname.startsWith('/privacy') || pathname.startsWith('/support')
}

/**
 * Gate di ri-accettazione legale (UC 0056): avvolge l'intero contenuto della shell. Quando ci sono
 * documenti vincolanti pendenti — e la route corrente non è esente — sostituisce tutto con la schermata
 * bloccante {@link LegalGateScreen}; altrimenti rende i `children` (la shell normale).
 *
 * **Fail-open**: mentre lo stato carica o su errore transitorio non si blocca l'utente (un errore di
 * rete non deve chiudere fuori dall'app). Il gate cattura anche l'accettazione iniziale: un utente
 * nuovo non ha accettazioni → i componenti vincolanti risultano pendenti → li accetta qui.
 */
export function LegalGate({ children }: { children: ReactNode }) {
  const { pathname } = useLocation()
  const { data, isLoading, isError } = useLegalStatus()

  if (isLoading || isError || !data) return <>{children}</>

  const pending = data.pending ?? []
  if (pending.length > 0 && !isExempt(pathname)) {
    return <LegalGateScreen pending={pending} />
  }
  return <>{children}</>
}

/**
 * Notifica di aggiornamento MINORE (non bloccante, UC 0056): banner leggero e dismissibile in cima al
 * contenuto della shell quando ci sono `notices`. Lo stato "chiuso" è locale (useState): riappare al
 * reload finché il documento non viene rivisto, coerente con la natura informativa dell'avviso.
 */
export function LegalNoticeBanner() {
  const { t } = useTranslation()
  const { data } = useLegalStatus()
  const [dismissed, setDismissed] = useState(false)

  const notices = data?.notices ?? []
  if (dismissed || notices.length === 0) return null

  return (
    <div
      role="status"
      className="flex items-center gap-3 border-b border-line bg-accent/10 px-4 py-2 text-sm text-fg"
    >
      <Icon name="gavel" size={18} className="text-accent" />
      <div className="min-w-0 flex-1">
        <span className="font-semibold">{t('legal.noticeTitle')}</span>{' '}
        <span className="text-fg-muted">{t('legal.noticeBody')}</span>
      </div>
      <Button
        variant="ghost"
        size="sm"
        aria-label={t('legal.dismiss')}
        onClick={() => setDismissed(true)}
      >
        <Icon name="close" size={18} />
      </Button>
    </div>
  )
}
