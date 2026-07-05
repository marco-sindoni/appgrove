import { Badge, Button } from '@appgrove/design-system'
import { useShellContext } from '../../../registry/ShellContext'
import { useFattureQuota } from '../api/hooks'
import { t } from '../strings'

/**
 * Banner quota della metrica `fatture` (UC 0052): mostra consumo/limite e, a tetto raggiunto, una CTA
 * di upgrade. È **solo UX** — l'enforcement vero è il 429 lato backend (#09 A6/F30). Naviga al billing
 * della shell tramite il contratto `nav` (il modulo non tocca il router globale).
 */
export function QuotaBanner() {
  const shell = useShellContext()
  const quota = useFattureQuota()

  if (quota.isLoading || quota.isError || !quota.data) return null

  const { used = 0, limit, remaining } = quota.data
  const unlimited = limit == null
  const reached = !unlimited && (remaining ?? 0) <= 0

  return (
    <div
      role="status"
      className="flex flex-wrap items-center justify-between gap-3 rounded-[15px] border border-line bg-surface px-4 py-3 shadow-sm"
    >
      <div className="flex items-center gap-2.5 text-[13px] text-fg">
        <span aria-hidden className="h-2 w-2 rounded-pill bg-cat-blue" />
        <span className="font-semibold text-fg-muted">{t.quotaLabel}:</span>
        {unlimited ? (
          <Badge tone="neutral">{used}</Badge>
        ) : (
          <Badge tone={reached ? 'danger' : 'accent'}>
            {used} / {limit}
          </Badge>
        )}
        {reached && <span className="text-danger">{t.quotaReached}</span>}
      </div>
      {reached && (
        <Button size="sm" onClick={() => shell.nav.navigate('/billing')}>
          {t.quotaUpgrade}
        </Button>
      )}
    </div>
  )
}
