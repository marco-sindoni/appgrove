import { useState } from 'react'
import { Link, NavLink, useLocation } from 'react-router-dom'
import { Icon, Logo, cn } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useVisibleModules } from '../registry/registry'
import { useAuthStore } from '../auth/authStore'
import { useLogout } from '../auth/useLogout'

/* Voce di primo livello (mockup: 13.5px/600, raggio 9px, icona 20px piena quando attiva). */
const linkClass = ({ isActive }: { isActive: boolean }) =>
  cn(
    'flex items-center gap-[11px] rounded-[9px] px-2.5 py-2 text-[13.5px] font-semibold transition-colors',
    isActive ? 'bg-accent/10 text-accent' : 'text-fg-muted hover:bg-surface-3 hover:text-fg',
  )

/* Sottovoce app (mockup: 12.5px, raggio 8px, icona 17px; attiva → accent + bold). */
const subLinkClass = ({ isActive }: { isActive: boolean }) =>
  cn(
    'flex items-center gap-[9px] rounded-lg px-[9px] py-1.5 text-[12.5px] font-semibold transition-colors',
    isActive ? 'bg-accent/10 font-bold text-accent' : 'text-fg-muted hover:bg-surface-3 hover:text-fg',
  )

/* Tinte del riquadro-icona app dal token categoria del manifest (classi statiche per il purge Tailwind). */
const APP_TINTS: Record<string, string> = {
  'cat-green': 'bg-cat-green/15 text-cat-green',
  'cat-amber': 'bg-cat-amber/15 text-cat-amber',
  'cat-red': 'bg-cat-red/15 text-cat-red',
  'cat-blue': 'bg-cat-blue/15 text-cat-blue',
  'cat-violet': 'bg-cat-violet/15 text-cat-violet',
  'cat-teal': 'bg-cat-teal/15 text-cat-teal',
}

function SectionLabel({ children, className }: { children: string; className?: string }) {
  return (
    <p
      className={cn(
        'px-2.5 pb-[5px] pt-2 text-[10.5px] font-bold uppercase tracking-[.08em] text-fg-faint',
        className,
      )}
    >
      {children}
    </p>
  )
}

function PlatformLink({
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
 * Sidebar (mockup docs/frontend-design/v1): brand con sottotitolo WORKSPACE, `PLATFORM` statica,
 * `YOUR APPS` dinamica (App Registry ∩ entitlement, #03 dec.6) a **due livelli** — gruppo app
 * richiudibile, sottovoci indentate dalla riga verticale — e footer con Settings + scheda utente.
 */
export function Sidebar({ onNavigate }: { onNavigate?: () => void }) {
  const { t } = useTranslation()
  const modules = useVisibleModules()
  const { pathname } = useLocation()
  const logout = useLogout()
  const claims = useAuthStore((s) => s.claims)
  // selettore che ritorna un booleano (primitivo, ref stabile): evita il loop di render di un `[]` nuovo a ogni giro.
  const canManageMembers = useAuthStore((s) => {
    const roles = s.claims?.roles
    return !!roles && (roles.includes('owner') || roles.includes('admin'))
  })
  // Gruppi app espansi di default (i test e le abitudini d'uso vogliono le sezioni raggiungibili subito).
  const [collapsed, setCollapsed] = useState<Record<string, boolean>>({})

  return (
    <nav aria-label={t('nav.platform')} className="flex h-full flex-col">
      <div className="flex items-center gap-[11px] px-[18px] pb-3.5 pt-5">
        <Logo size={34} showWordmark={false} />
        <div className="flex flex-col leading-[1.05]">
          <span className="text-base font-extrabold tracking-[-0.02em] text-fg">appgrove</span>
          <span className="text-[10.5px] font-semibold uppercase tracking-[.04em] text-fg-faint">
            Workspace
          </span>
        </div>
      </div>

      <div className="flex flex-1 flex-col gap-0.5 overflow-y-auto px-3 pb-3 pt-1">
        <SectionLabel>{t('nav.platform')}</SectionLabel>
        <PlatformLink to="/" icon="space_dashboard" label={t('nav.dashboard')} onNavigate={onNavigate} />
        <PlatformLink to="/account" icon="account_circle" label={t('nav.account')} onNavigate={onNavigate} />
        <PlatformLink to="/billing" icon="credit_card" label={t('nav.billing')} onNavigate={onNavigate} />
        {canManageMembers && (
          <PlatformLink to="/members" icon="group" label={t('nav.members')} onNavigate={onNavigate} />
        )}
        <PlatformLink to="/privacy" icon="shield_person" label={t('nav.privacy')} onNavigate={onNavigate} />
        <PlatformLink to="/support" icon="support_agent" label={t('nav.support')} onNavigate={onNavigate} />

        <SectionLabel className="pt-4">{t('nav.yourApps')}</SectionLabel>
        {modules.length === 0 && (
          <p className="px-2.5 py-2 text-[13px] text-fg-muted">{t('states.empty')}</p>
        )}
        {modules.map((mod) => {
          const isOpen = !collapsed[mod.id]
          const onAppRoute = pathname.startsWith(`/app/${mod.id}`)
          return (
            <div key={mod.id}>
              <button
                type="button"
                aria-expanded={isOpen}
                onClick={() => setCollapsed((c) => ({ ...c, [mod.id]: !c[mod.id] }))}
                className={cn(
                  'flex w-full items-center gap-[11px] rounded-[9px] px-2.5 py-2 text-[13.5px] font-semibold transition-colors hover:bg-surface-3',
                  onAppRoute ? 'text-fg' : 'text-fg-muted',
                )}
              >
                <span
                  className={cn(
                    'flex h-6 w-6 shrink-0 items-center justify-center rounded-[7px]',
                    (mod.accentToken && APP_TINTS[mod.accentToken]) ?? 'bg-accent/15 text-accent',
                  )}
                >
                  {mod.icon && <Icon name={mod.icon} size={15} filled />}
                </span>
                <span className="truncate">{mod.name}</span>
                <Icon
                  name="chevron_right"
                  size={18}
                  className={cn(
                    'ml-auto text-fg-faint transition-transform duration-200',
                    isOpen && 'rotate-90',
                  )}
                />
              </button>
              {isOpen && (
                <div className="mb-1 ml-[23px] mt-0.5 flex flex-col gap-px border-l-[1.5px] border-line pl-[11px]">
                  {mod.sections.map((section) => (
                    <NavLink
                      key={section.id}
                      to={`/app/${mod.id}${section.route ? `/${section.route}` : ''}`}
                      end={section.route === ''}
                      className={subLinkClass}
                      onClick={onNavigate}
                    >
                      {section.icon && <Icon name={section.icon} size={17} />}
                      {section.label}
                    </NavLink>
                  ))}
                </div>
              )}
            </div>
          )
        })}
      </div>

      <div className="flex flex-col gap-0.5 border-t border-line p-3">
        <PlatformLink to="/settings" icon="settings" label={t('nav.settings')} onNavigate={onNavigate} />
        <details className="relative">
          <summary
            className="flex cursor-pointer list-none items-center gap-2.5 rounded-[11px] px-2 py-2 hover:bg-surface-3"
            aria-label={t('topbar.userMenu')}
          >
            <span
              aria-hidden
              className="flex h-8 w-8 shrink-0 items-center justify-center rounded-pill bg-gradient-to-br from-cat-violet to-accent text-[13px] font-bold text-white"
            >
              {initialsOf(claims?.name, claims?.email)}
            </span>
            <span className="min-w-0 flex-1 leading-tight">
              <span className="block truncate text-[13px] font-bold text-fg">
                {claims?.name ?? claims?.email ?? ''}
              </span>
              <span className="block truncate text-[11px] text-fg-faint">{claims?.email}</span>
            </span>
            <Icon name="expand_more" size={18} className="text-fg-faint" />
          </summary>
          <div className="absolute bottom-full left-0 z-10 mb-2 w-56 rounded-md border border-line bg-surface p-2 shadow-lg">
            <p className="truncate px-2 py-1 text-xs text-fg-muted">{claims?.email}</p>
            <Link
              to="/security"
              onClick={onNavigate}
              className="flex items-center gap-2 rounded-lg px-2 py-1.5 text-[13px] font-semibold text-fg hover:bg-surface-3"
            >
              <Icon name="shield" size={18} />
              {t('nav.security')}
            </Link>
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
