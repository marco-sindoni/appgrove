import { Fragment } from 'react'
import { useLocation } from 'react-router-dom'
import { useTranslation } from '@appgrove/i18n'
import { findModule } from '../registry/registry'

/** Breadcrumb derivata dal pathname (label localizzate per le voci note, nome modulo per le app). */
export function Breadcrumb() {
  const { t } = useTranslation()
  const { pathname } = useLocation()
  const segments = pathname.split('/').filter(Boolean)

  const labelFor = (segment: string, prev: string | undefined): string => {
    if (prev === 'app') return findModule(segment)?.name ?? segment
    const known: Record<string, string> = {
      account: t('nav.account'),
      billing: t('nav.billing'),
      settings: t('nav.settings'),
    }
    return known[segment] ?? segment
  }

  return (
    <nav aria-label="Breadcrumb" className="text-sm text-fg-muted">
      <ol className="flex items-center gap-1">
        <li className="font-medium text-fg">{t('nav.dashboard')}</li>
        {segments.map((segment, i) => (
          <Fragment key={i}>
            <li aria-hidden className="text-fg-muted/60">
              /
            </li>
            <li className={i === segments.length - 1 ? 'font-medium text-fg' : undefined}>
              {labelFor(segment, segments[i - 1])}
            </li>
          </Fragment>
        ))}
      </ol>
    </nav>
  )
}
