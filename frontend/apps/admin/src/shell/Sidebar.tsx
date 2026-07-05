import { NavLink } from 'react-router-dom'
import { Icon, cn } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useAuthStore } from '../auth/authStore'
import { useLogout } from '../auth/useLogout'

/* Voce nav del mockup admin: 13.5px/600, raggio 9px, icona 20px piena quando attiva. */
const linkClass = ({ isActive }: { isActive: boolean }) =>
  cn(
    'flex items-center gap-[11px] rounded-[9px] px-2.5 py-2 text-[13.5px] font-semibold transition-colors',
    isActive ? 'bg-accent/10 text-accent' : 'text-fg-muted hover:bg-surface-3 hover:text-fg',
  )

function GroupLabel({ children }: { children: string }) {
  return (
    <p className="px-2.5 pb-[5px] pt-[13px] text-[10px] font-bold uppercase tracking-[.08em] text-fg-faint">
      {children}
    </p>
  )
}

function NavItem({
  to,
  icon,
  label,
  onNavigate,
}: {
  to: string
  icon: string
  label: string
  onNavigate?: () => void
}) {
  return (
    <NavLink to={to} end={to === '/'} className={linkClass} onClick={onNavigate}>
      {({ isActive }) => (
        <>
          <Icon name={icon} size={20} filled={isActive} />
          {label}
        </>
      )}
    </NavLink>
  )
}

function initialsOf(name: string | undefined, email: string | undefined): string {
  const source = name?.trim() || email || ''
  const parts = source.split(/\s+/).filter(Boolean)
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase()
  return source.slice(0, 2).toUpperCase()
}

/**
 * Sidebar della console admin (mockup docs/frontend-design/admin/v1): brand scuro con scudetto accent
 * e sottotitolo PLATFORM ADMIN, navigazione a gruppi (Platform/Catalog/Revenue/Governance),
 * scheda operatore nel footer con menu (logout). UC 0021.
 */
export function Sidebar({ onNavigate }: { onNavigate?: () => void }) {
  const { t } = useTranslation()
  const claims = useAuthStore((s) => s.claims)
  const logout = useLogout()

  return (
    <nav aria-label={t('admin.nav.section')} className="flex h-full flex-col">
      <div className="flex items-center gap-[11px] px-[18px] pb-3 pt-[18px]">
        <span className="relative flex h-[34px] w-[34px] shrink-0 items-center justify-center rounded-[11px] bg-fg">
          <Icon name="eco" size={20} filled className="text-bg" />
          <span className="absolute -bottom-1 -right-1 flex h-4 w-4 items-center justify-center rounded-[5px] bg-accent">
            <Icon name="shield" size={9} filled className="text-accent-contrast" />
          </span>
        </span>
        <div className="flex flex-col leading-[1.1]">
          <span className="text-[15.5px] font-extrabold tracking-[-0.02em] text-fg">appgrove</span>
          <span className="text-[10px] font-extrabold uppercase tracking-[.06em] text-accent">
            {t('admin.badge.platformAdmin')}
          </span>
        </div>
      </div>

      <div className="flex flex-1 flex-col gap-0.5 overflow-y-auto px-3 pb-3">
        <GroupLabel>Platform</GroupLabel>
        <NavItem to="/" icon="space_dashboard" label={t('admin.nav.overview')} onNavigate={onNavigate} />
        <NavItem to="/accounts" icon="corporate_fare" label={t('admin.nav.accounts')} onNavigate={onNavigate} />
        <NavItem to="/users" icon="group" label={t('admin.nav.users')} onNavigate={onNavigate} />
        <NavItem to="/entitlements" icon="apps" label={t('admin.nav.entitlements')} onNavigate={onNavigate} />

        <GroupLabel>Catalog</GroupLabel>
        <NavItem to="/apps" icon="widgets" label={t('admin.nav.apps')} onNavigate={onNavigate} />

        <GroupLabel>Revenue</GroupLabel>
        <NavItem to="/billing" icon="account_balance_wallet" label={t('admin.nav.billing')} onNavigate={onNavigate} />

        <GroupLabel>Governance</GroupLabel>
        <NavItem to="/gdpr" icon="verified_user" label={t('admin.nav.gdpr')} onNavigate={onNavigate} />
      </div>

      <div className="border-t border-line p-3">
        <details className="relative">
          <summary
            className="flex cursor-pointer list-none items-center gap-2.5 rounded-[11px] px-2 py-2 hover:bg-surface-3"
            aria-label={t('topbar.userMenu')}
          >
            <span
              aria-hidden
              className="flex h-8 w-8 shrink-0 items-center justify-center rounded-[9px] bg-fg text-[12px] font-bold text-bg"
            >
              {initialsOf(claims?.name, claims?.email)}
            </span>
            <span className="min-w-0 flex-1 leading-tight">
              <span className="block truncate text-[13px] font-bold text-fg">
                {claims?.name ?? claims?.email ?? ''}
              </span>
              <span className="block truncate text-[11px] text-fg-faint">platform-admin</span>
            </span>
            <Icon name="expand_more" size={18} className="text-fg-faint" />
          </summary>
          <div className="absolute bottom-full left-0 z-10 mb-2 w-56 rounded-md border border-line bg-surface p-2 shadow-lg">
            <p className="truncate px-2 py-1 text-xs text-fg-muted">{claims?.email}</p>
            <button
              type="button"
              onClick={() => void logout()}
              className="flex w-full items-center gap-2 rounded-lg px-2 py-1.5 text-left text-[13px] font-semibold text-fg hover:bg-surface-3"
            >
              <Icon name="logout" size={18} />
              {t('topbar.logout')}
            </button>
          </div>
        </details>
      </div>
    </nav>
  )
}
