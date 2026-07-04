import { NavLink } from 'react-router-dom'
import { Icon, Logo, cn } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useVisibleModules } from '../registry/registry'
import { useAuthStore } from '../auth/authStore'

const linkClass = ({ isActive }: { isActive: boolean }) =>
  cn(
    'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
    isActive ? 'bg-accent/10 text-accent' : 'text-fg-muted hover:bg-surface-2 hover:text-fg',
  )

function SectionLabel({ children }: { children: string }) {
  return (
    <p className="px-3 pb-1 pt-4 text-xs font-semibold uppercase tracking-wide text-fg-muted">
      {children}
    </p>
  )
}

/** Sidebar: `PLATFORM` statica + `YOUR APPS` dinamica (App Registry ∩ entitlement, #03 dec.6). */
export function Sidebar({ onNavigate }: { onNavigate?: () => void }) {
  const { t } = useTranslation()
  const modules = useVisibleModules()
  // selettore che ritorna un booleano (primitivo, ref stabile): evita il loop di render di un `[]` nuovo a ogni giro.
  const canManageMembers = useAuthStore((s) => {
    const roles = s.claims?.roles
    return !!roles && (roles.includes('owner') || roles.includes('admin'))
  })

  return (
    <nav aria-label={t('nav.platform')} className="flex h-full flex-col gap-1 p-3">
      <div className="px-2 py-3">
        <Logo size={26} />
      </div>

      <SectionLabel>{t('nav.platform')}</SectionLabel>
      <NavLink to="/" end className={linkClass} onClick={onNavigate}>
        <Icon name="dashboard" size={20} />
        {t('nav.dashboard')}
      </NavLink>
      <NavLink to="/account" className={linkClass} onClick={onNavigate}>
        <Icon name="account_circle" size={20} />
        {t('nav.account')}
      </NavLink>
      <NavLink to="/billing" className={linkClass} onClick={onNavigate}>
        <Icon name="credit_card" size={20} />
        {t('nav.billing')}
      </NavLink>
      {canManageMembers && (
        <NavLink to="/members" className={linkClass} onClick={onNavigate}>
          <Icon name="group" size={20} />
          {t('nav.members')}
        </NavLink>
      )}
      <NavLink to="/settings" className={linkClass} onClick={onNavigate}>
        <Icon name="settings" size={20} />
        {t('nav.settings')}
      </NavLink>
      <NavLink to="/privacy" className={linkClass} onClick={onNavigate}>
        <Icon name="shield_person" size={20} />
        {t('nav.privacy')}
      </NavLink>
      <NavLink to="/support" className={linkClass} onClick={onNavigate}>
        <Icon name="support_agent" size={20} />
        {t('nav.support')}
      </NavLink>

      <SectionLabel>{t('nav.yourApps')}</SectionLabel>
      {modules.length === 0 && (
        <p className="px-3 py-2 text-sm text-fg-muted">{t('states.empty')}</p>
      )}
      {modules.map((mod) => (
        <div key={mod.id}>
          <p className="flex items-center gap-2 px-3 py-2 text-sm font-semibold text-fg">
            {mod.icon && <Icon name={mod.icon} size={18} />}
            {mod.name}
          </p>
          {mod.sections.map((section) => (
            <NavLink
              key={section.id}
              to={`/app/${mod.id}${section.route ? `/${section.route}` : ''}`}
              end={section.route === ''}
              className={(s) => cn(linkClass(s), 'ml-3')}
              onClick={onNavigate}
            >
              {section.icon && <Icon name={section.icon} size={18} />}
              {section.label}
            </NavLink>
          ))}
        </div>
      ))}
    </nav>
  )
}
