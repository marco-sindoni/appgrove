import { NavLink } from 'react-router-dom'
import { Icon, Logo, cn } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'

const linkClass = ({ isActive }: { isActive: boolean }) =>
  cn(
    'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
    isActive ? 'bg-accent/10 text-accent' : 'text-fg-muted hover:bg-surface-2 hover:text-fg',
  )

/** Sidebar della console admin: sezione unica `PLATFORM ADMIN` con le 6 voci di navigazione (UC 0021). */
export function Sidebar({ onNavigate }: { onNavigate?: () => void }) {
  const { t } = useTranslation()

  return (
    <nav aria-label={t('admin.nav.section')} className="flex h-full flex-col gap-1 p-3">
      <div className="px-2 py-3">
        <Logo size={26} />
      </div>

      <div className="px-3 pb-1 pt-2">
        <span className="inline-flex items-center gap-1 rounded-pill bg-danger/10 px-2.5 py-0.5 font-sans text-xs font-semibold uppercase tracking-wide text-danger">
          {t('admin.badge.platformAdmin')}
        </span>
      </div>

      <NavLink to="/" end className={linkClass} onClick={onNavigate}>
        <Icon name="dashboard" size={20} />
        {t('admin.nav.overview')}
      </NavLink>
      <NavLink to="/accounts" className={linkClass} onClick={onNavigate}>
        <Icon name="apartment" size={20} />
        {t('admin.nav.accounts')}
      </NavLink>
      <NavLink to="/users" className={linkClass} onClick={onNavigate}>
        <Icon name="group" size={20} />
        {t('admin.nav.users')}
      </NavLink>
      <NavLink to="/entitlements" className={linkClass} onClick={onNavigate}>
        <Icon name="verified" size={20} />
        {t('admin.nav.entitlements')}
      </NavLink>
      <NavLink to="/billing" className={linkClass} onClick={onNavigate}>
        <Icon name="credit_card" size={20} />
        {t('admin.nav.billing')}
      </NavLink>
      <NavLink to="/apps" className={linkClass} onClick={onNavigate}>
        <Icon name="grid_view" size={20} />
        {t('admin.nav.apps')}
      </NavLink>
      <NavLink to="/gdpr" className={linkClass} onClick={onNavigate}>
        <Icon name="policy" size={20} />
        {t('admin.nav.gdpr')}
      </NavLink>
    </nav>
  )
}
