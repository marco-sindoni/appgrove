import { Badge } from '@appgrove/design-system'
import { statusLabel } from '../strings'

/** Badge dello stato di un record con tono coerente (bozza/attivo/completato/archiviato). */
export function StatusBadge({ status }: { status?: string }) {
  const tone =
    status === 'done'
      ? 'success'
      : status === 'archived'
        ? 'danger'
        : status === 'active'
          ? 'warning'
          : 'neutral'
  return <Badge tone={tone}>{statusLabel(status)}</Badge>
}
