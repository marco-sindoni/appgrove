import { useRef, useState } from 'react'
import { Icon, cn } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useQueryClient } from '@tanstack/react-query'
import { MEMBERSHIPS_KEY, useMyMemberships, useSetActiveAccount } from '../api/hooks'

/**
 * Selettore dell'account attivo (UC 0117), nella **barra laterale, sotto il marchio**: l'account è il
 * *contesto* in cui si sta lavorando, come il menu che gli sta sotto, non un comando accessorio come
 * la lingua o il tema.
 *
 * Tre regole portanti, e nessuna è cosmetica:
 *
 * 1. **il nome dell'account attivo è sempre visibile**, anche con una sola appartenenza: con più
 *    account è un elemento di sicurezza percepita — deve essere sempre chiaro per conto di chi si sta
 *    lavorando;
 * 2. **con una sola appartenenza il selettore non viene reso affatto** — non «reso disabilitato». Un
 *    comando che non serve a nulla è rumore, ed è lo stesso principio dei menu assenti per chi non è
 *    owner. È il caso di tutti gli utenti di oggi e deve restare a costo zero;
 * 3. **nessuna etichetta di ruolo** (UC 0117 §4.6): il ruolo è per applicazione, e una etichetta
 *    globale sarebbe falsa appena una persona è abilitata a più di una applicazione. Il prototipo
 *    dell'epica mostra «Sei il titolare / Sei collaboratore»: quella distinzione arriva con UC 0107,
 *    che è la storia che rende il ruolo visibile dove è vero.
 *
 * Il cambio **ricarica l'applicazione** e non aggiorna lo stato in memoria: mezza applicazione con
 * l'account nuovo e mezza col vecchio è il modo peggiore di sbagliare. È il ricaricamento stesso a
 * rinnovare il token — la ripresa della sessione al caricamento chiede un token nuovo, che nasce con
 * l'account appena scelto — così il claim continua a essere calcolato in un posto solo.
 */
export function AccountSwitcher() {
  const { t } = useTranslation()
  const { data } = useMyMemberships()
  const setActive = useSetActiveAccount()
  const queryClient = useQueryClient()
  const details = useRef<HTMLDetailsElement>(null)
  const [failed, setFailed] = useState(false)

  // Lo spec OpenAPI non dichiara obbligatori i campi, quindi le voci incomplete si scartano qui
  // invece di essere inventate a valle: un account senza identificativo non è selezionabile.
  const memberships = (data?.memberships ?? []).flatMap((m) =>
    m.accountId ? [{ accountId: m.accountId, accountName: m.accountName ?? m.accountId }] : [],
  )
  const activeAccountId = data?.activeAccountId
  // Il nome mostrato: quello dell'account attivo; se la scelta non è determinata (più appartenenze e
  // nessuna valida) non si inventa un nome — meglio nulla che il nome sbagliato.
  const activeName = memberships.find((m) => m.accountId === activeAccountId)?.accountName

  // Nessuna lettura riuscita: niente nome e niente comando. Non si mostra un errore qui — la barra
  // laterale non è il posto per un guasto di rete, e un nome inventato sarebbe peggio del silenzio.
  if (!activeName) {
    return null
  }

  if (memberships.length < 2) {
    return (
      <div className="px-[18px] pb-3">
        <p className="truncate text-[13px] font-bold text-fg" data-testid="active-account-name">
          {activeName}
        </p>
      </div>
    )
  }

  const switchTo = async (accountId: string) => {
    setFailed(false)
    details.current?.removeAttribute('open')
    try {
      await setActive.mutateAsync(accountId)
      // Ricaricamento completo sulla radice: la rotta aperta può non esistere nell'account nuovo, e
      // ripartire dal cruscotto è l'unico esito sempre vero.
      window.location.assign('/')
    } catch {
      // Tipicamente 404: l'appartenenza è stata revocata mentre il menu era aperto. Si dice, e si
      // rilegge l'elenco così la voce che non esiste più sparisce.
      setFailed(true)
      void queryClient.invalidateQueries({ queryKey: MEMBERSHIPS_KEY })
    }
  }

  return (
    <div className="px-3 pb-3">
      <details ref={details} className="relative">
        <summary
          aria-label={t('accountSwitch.label')}
          className="flex cursor-pointer list-none items-center gap-2 rounded-[10px] border border-line px-2.5 py-2 hover:bg-surface-3"
        >
          <span className="min-w-0 flex-1 leading-tight">
            <span className="block truncate text-[13px] font-bold text-fg" data-testid="active-account-name">
              {activeName}
            </span>
            <span className="block truncate text-[11px] text-fg-faint">
              {t('accountSwitch.hint', { count: memberships.length })}
            </span>
          </span>
          <Icon name="expand_more" size={18} className="shrink-0 text-fg-faint" />
        </summary>
        <div className="absolute left-0 top-full z-30 mt-1.5 w-full rounded-md border border-line bg-surface p-1 shadow-lg">
          {memberships.map((m) => {
            const isActive = m.accountId === activeAccountId
            return (
              <button
                key={m.accountId}
                type="button"
                disabled={setActive.isPending}
                aria-current={isActive}
                onClick={() => void switchTo(m.accountId)}
                className={cn(
                  'flex w-full items-center gap-2 rounded-lg px-2.5 py-1.5 text-left text-[13px] font-semibold transition-colors hover:bg-surface-3 disabled:opacity-60',
                  isActive ? 'text-accent' : 'text-fg',
                )}
              >
                <span className="min-w-0 flex-1 truncate">{m.accountName}</span>
                {isActive && <Icon name="check" size={16} className="shrink-0" />}
              </button>
            )
          })}
        </div>
      </details>
      {setActive.isPending && (
        <p role="status" className="px-1 pt-1.5 text-[11px] text-fg-muted">
          {t('accountSwitch.switching')}
        </p>
      )}
      {failed && (
        <p role="alert" className="px-1 pt-1.5 text-[11px] text-danger">
          {t('accountSwitch.error')}
        </p>
      )}
    </div>
  )
}
