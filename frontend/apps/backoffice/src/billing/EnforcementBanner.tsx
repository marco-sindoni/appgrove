import { useNavigate } from 'react-router-dom'
import { Button } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useEnforcementStore } from './enforcementStore'

/**
 * Banner globale azionabile per gli esiti di enforcement (UC 0028, chiude il punto aperto di UC 0027):
 * **429** quota → "fai upgrade"; **402** entitlement/scaduto → "vai agli abbonamenti" (dove si riattiva o si
 * esportano/eliminano i dati, #09 F31). Alimentato dal `QueryClient` via {@link useEnforcementStore}. Solo UX:
 * l'enforcement vero è backend (UC 0027).
 */
export function EnforcementBanner() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const kind = useEnforcementStore((s) => s.kind)
  const clear = useEnforcementStore((s) => s.clear)
  if (!kind) return null

  const isQuota = kind === 'quota'
  const title = isQuota ? t('enforcement.quotaTitle') : t('enforcement.entitlementTitle')
  const body = isQuota ? t('enforcement.quotaBody') : t('enforcement.entitlementBody')
  const cta = isQuota ? t('enforcement.quotaCta') : t('enforcement.entitlementCta')

  return (
    <div role="alert" className="border-b border-warning/40 bg-warning/10 px-6 py-3">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="min-w-0">
          <p className="text-sm font-semibold text-fg">{title}</p>
          <p className="text-sm text-fg-muted">{body}</p>
        </div>
        <div className="flex shrink-0 gap-2">
          <Button
            size="sm"
            onClick={() => {
              clear()
              navigate('/billing')
            }}
          >
            {cta}
          </Button>
          <Button size="sm" variant="ghost" onClick={clear}>
            {t('enforcement.dismiss')}
          </Button>
        </div>
      </div>
    </div>
  )
}
