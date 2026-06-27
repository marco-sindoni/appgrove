import { Badge } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'

/** Badge sì/no per l'entitlement derivato (evidenzia true=success, false=neutral). */
export function EntitledBadge({ entitled }: { entitled?: boolean }) {
  const { t } = useTranslation()
  return (
    <Badge tone={entitled ? 'success' : 'neutral'}>
      {entitled ? t('admin.entitled.yes') : t('admin.entitled.no')}
    </Badge>
  )
}
