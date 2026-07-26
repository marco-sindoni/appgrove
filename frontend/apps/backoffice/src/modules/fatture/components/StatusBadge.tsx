import { Badge } from '@appgrove/design-system'
import { useFattureMessages } from '../i18n'

const STATUS_KEYS = ['issued', 'paid', 'voided'] as const

/** Badge dello stato fattura con tono coerente (bozza/emessa/pagata/annullata). */
export function StatusBadge({ status }: { status?: string }) {
  const m = useFattureMessages()
  // Mappa colori del mockup: pagata=verde, emessa/in attesa=ambra, annullata=rosso, bozza=neutro.
  const tone =
    status === 'paid'
      ? 'success'
      : status === 'voided'
        ? 'danger'
        : status === 'issued'
          ? 'warning'
          : 'neutral'
  const key = (STATUS_KEYS as readonly string[]).includes(status ?? '')
    ? (status as (typeof STATUS_KEYS)[number])
    : 'draft'
  return <Badge tone={tone}>{m.status[key]}</Badge>
}
