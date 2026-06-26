import {
  Button,
  Icon,
  SegmentedControl,
  Switch,
  useTheme,
  ACCENTS,
  type Accent,
} from '@appgrove/design-system'
import { useTranslation, LANGUAGES, type Language } from '@appgrove/i18n'
import { useAuthStore } from '../auth/authStore'
import { useLogout } from '../auth/useLogout'
import { Breadcrumb } from './Breadcrumb'

/** Topbar permanente: breadcrumb + accent picker + lingua + tema + notifiche + menu utente (#03 IA). */
export function Topbar({ onOpenSidebar }: { onOpenSidebar?: () => void }) {
  const { t, i18n } = useTranslation()
  const { theme, accent, setAccent, toggleTheme } = useTheme()
  const claims = useAuthStore((s) => s.claims)
  const logout = useLogout()

  const language = (i18n.language?.slice(0, 2) as Language) ?? 'en'

  return (
    <header className="flex items-center gap-3 border-b border-line bg-surface px-4 py-3">
      <Button
        variant="ghost"
        size="sm"
        className="lg:hidden"
        aria-label={t('nav.platform')}
        onClick={onOpenSidebar}
      >
        <Icon name="menu" size={22} />
      </Button>

      <div className="flex-1">
        <Breadcrumb />
      </div>

      <SegmentedControl
        aria-label={t('topbar.accent')}
        value={accent}
        options={ACCENTS.map((a) => ({ value: a, label: a.toUpperCase().slice(0, 1) }))}
        onValueChange={(v) => setAccent(v as Accent)}
      />

      <SegmentedControl
        aria-label={t('topbar.language')}
        value={language}
        options={LANGUAGES.map((l) => ({ value: l, label: l.toUpperCase() }))}
        onValueChange={(v) => void i18n.changeLanguage(v)}
      />

      <label className="flex items-center gap-2 text-sm text-fg-muted">
        <Icon name="dark_mode" size={18} aria-hidden />
        <Switch
          checked={theme === 'dark'}
          onCheckedChange={toggleTheme}
          aria-label={t('topbar.toggleTheme')}
        />
      </label>

      <Button variant="ghost" size="sm" aria-label={t('topbar.notifications')}>
        <Icon name="notifications" size={20} />
      </Button>

      <details className="relative">
        <summary
          className="flex cursor-pointer list-none items-center gap-2 rounded-md px-2 py-1 text-sm text-fg hover:bg-surface-2"
          aria-label={t('topbar.userMenu')}
        >
          <Icon name="account_circle" size={22} />
          <span className="hidden max-w-[12ch] truncate sm:inline">
            {claims?.name ?? claims?.email ?? ''}
          </span>
        </summary>
        <div className="absolute right-0 z-10 mt-2 w-48 rounded-lg border border-line bg-surface p-2 shadow-lg">
          <p className="truncate px-2 py-1 text-xs text-fg-muted">{claims?.email}</p>
          <Button
            variant="ghost"
            size="sm"
            className="w-full justify-start"
            onClick={() => void logout()}
          >
            <Icon name="logout" size={18} />
            {t('topbar.logout')}
          </Button>
        </div>
      </details>
    </header>
  )
}
