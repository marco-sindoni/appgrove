import { Badge } from '@appgrove/design-system'
import { stageLabel } from '../strings'

/** Badge dello stato della trattativa con tono coerente (nuovo/qualificato/in trattativa/vinto/perso). */
export function StageBadge({ stage }: { stage?: string }) {
  const tone =
    stage === 'won'
      ? 'success'
      : stage === 'lost'
        ? 'danger'
        : stage === 'negotiating'
          ? 'warning'
          : stage === 'qualified'
            ? 'accent'
            : 'neutral'
  return <Badge tone={tone}>{stageLabel(stage)}</Badge>
}
