import { Badge } from '@appgrove/design-system'
import { statusLabel } from '../strings'

/** Badge dello stato fattura con tono coerente (bozza/emessa/pagata/annullata). */
export function StatusBadge({ status }: { status?: string }) {
  const tone =
    status === 'paid'
      ? 'success'
      : status === 'voided'
        ? 'danger'
        : status === 'issued'
          ? 'accent'
          : 'neutral'
  return <Badge tone={tone}>{statusLabel(status)}</Badge>
}
