import { useLocation } from 'react-router-dom'
import { Icon, SegmentedControl, useTheme } from '@appgrove/design-system'
import { useTranslation, LANGUAGES, type Language } from '@appgrove/i18n'

/* Pulsante-icona 36px del mockup admin: bordo sottile, raggio 9px, hover su surface-3. */
function IconButton({
  label,
  icon,
  onClick,
  children,
}: {
  label: string
  icon: string
  onClick?: () => void
  children?: React.ReactNode
}) {
  return (
    <button
      type="button"
      aria-label={label}
      onClick={onClick}
      className="relative flex h-9 w-9 items-center justify-center rounded-[9px] border border-line text-fg-muted transition-colors hover:bg-surface-3"
    >
      <Icon name={icon} size={19} />
      {children}
    </button>
  )
}

/** Topbar admin (mockup): 62px traslucida, breadcrumb con pill PLATFORM, lingua + tema + notifiche. */
export function Topbar({ onOpenSidebar }: { onOpenSidebar?: () => void }) {
  const { t, i18n } = useTranslation()
  const { theme, toggleTheme } = useTheme()
  const { pathname } = useLocation()

  const language = (i18n.language?.slice(0, 2) as Language) ?? 'en'

  const segment = pathname.split('/').filter(Boolean)[0]
  const pageLabel: Record<string, string> = {
    accounts: t('admin.nav.accounts'),
    users: t('admin.nav.users'),
    entitlements: t('admin.nav.entitlements'),
    billing: t('admin.nav.billing'),
    apps: t('admin.nav.apps'),
    gdpr: t('admin.nav.gdpr'),
  }
  const crumb = segment ? (pageLabel[segment] ?? segment) : t('admin.nav.overview')

  return (
    <header className="sticky top-0 z-20 flex h-[62px] shrink-0 items-center gap-3.5 border-b border-line bg-surface/80 px-[22px] backdrop-blur-md">
      <button
        type="button"
        aria-label={t('admin.nav.section')}
        onClick={onOpenSidebar}
        className="flex h-9 w-9 items-center justify-center rounded-[9px] border border-line text-fg-muted hover:bg-surface-3 lg:hidden"
      >
        <Icon name="menu" size={20} />
      </button>

      <nav aria-label="Breadcrumb" className="flex min-w-0 flex-1 items-center gap-2">
        <span className="flex items-center gap-1 rounded-[7px] bg-accent/10 px-[9px] py-1 text-[11px] font-extrabold tracking-[.03em] text-accent">
          <Icon name="shield" size={14} filled />
          PLATFORM
        </span>
        <Icon name="chevron_right" size={16} className="text-fg-faint" aria-hidden />
        <span className="truncate text-[13px] font-bold text-fg">{crumb}</span>
      </nav>

      <div className="flex items-center gap-[9px]">
        <SegmentedControl
          aria-label={t('topbar.language')}
          value={language}
          options={LANGUAGES.map((l) => ({ value: l, label: l.toUpperCase() }))}
          onValueChange={(v) => void i18n.changeLanguage(v)}
        />

        <IconButton
          label={t('topbar.toggleTheme')}
          icon={theme === 'dark' ? 'light_mode' : 'dark_mode'}
          onClick={toggleTheme}
        />

        <IconButton label={t('topbar.notifications')} icon="notifications">
          <span
            aria-hidden
            className="absolute right-2 top-1.5 h-[7px] w-[7px] rounded-pill border-[1.5px] border-surface bg-accent"
          />
        </IconButton>
      </div>
    </header>
  )
}
