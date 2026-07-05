import { Fragment } from 'react'
import { useLocation } from 'react-router-dom'
import { Icon } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { findModule } from '../registry/registry'

/** Breadcrumb (mockup: radice attenuata, separatore chevron, foglia in grassetto). Label localizzate, nome modulo per le app. */
export function Breadcrumb() {
  const { t } = useTranslation()
  const { pathname } = useLocation()
  const rawSegments = pathname.split('/').filter(Boolean)

  const known: Record<string, string> = {
    account: t('nav.account'),
    billing: t('nav.billing'),
    settings: t('nav.settings'),
  }
  // Il segmento tecnico `app` (prefisso dei moduli) non è una voce di navigazione: non compare nel
  // breadcrumb; il segmento successivo viene risolto nel nome del modulo.
  const segments = rawSegments
    .map((segment, i) => {
      if (segment === 'app' && rawSegments[i + 1]) return null
      if (rawSegments[i - 1] === 'app') return findModule(segment)?.name ?? segment
      return known[segment] ?? segment
    })
    .filter((s): s is string => s !== null)

  return (
    <nav aria-label="Breadcrumb" className="text-[13px]">
      <ol className="flex items-center gap-2">
        <li className={segments.length === 0 ? 'font-bold text-fg' : 'font-semibold text-fg-faint'}>
          {t('nav.dashboard')}
        </li>
        {segments.map((segment, i) => (
          <Fragment key={i}>
            <li aria-hidden className="flex items-center text-fg-faint">
              <Icon name="chevron_right" size={17} />
            </li>
            <li
              className={
                i === segments.length - 1 ? 'font-bold text-fg' : 'font-semibold text-fg-faint'
              }
            >
              {segment}
            </li>
          </Fragment>
        ))}
      </ol>
    </nav>
  )
}
