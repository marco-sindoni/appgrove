import { Badge } from '@appgrove/design-system'
import { use@@APP_CLASS@@Messages } from '../i18n'

const STATUS_KEYS = ['active', 'done', 'archived'] as const

/** Badge dello stato di un record con tono coerente (bozza/attivo/completato/archiviato). */
export function StatusBadge({ status }: { status?: string }) {
  const m = use@@APP_CLASS@@Messages()
  const tone =
    status === 'done'
      ? 'success'
      : status === 'archived'
        ? 'danger'
        : status === 'active'
          ? 'warning'
          : 'neutral'
  const key = (STATUS_KEYS as readonly string[]).includes(status ?? '')
    ? (status as (typeof STATUS_KEYS)[number])
    : 'draft'
  return <Badge tone={tone}>{m.status[key]}</Badge>
}
