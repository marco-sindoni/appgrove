import { Badge } from '@appgrove/design-system'
import { statusLabel } from '../strings'

/** Badge dello stato fattura con tono coerente (bozza/emessa/pagata/annullata). */
export function StatusBadge({ status }: { status?: string }) {
  // Mappa colori del mockup: pagata=verde, emessa/in attesa=ambra, annullata=rosso, bozza=neutro.
  const tone =
    status === 'paid'
      ? 'success'
      : status === 'voided'
        ? 'danger'
        : status === 'issued'
          ? 'warning'
          : 'neutral'
  return <Badge tone={tone}>{statusLabel(status)}</Badge>
}
