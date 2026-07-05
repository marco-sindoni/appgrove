import {
  Icon,
  SegmentedControl,
  cn,
  useTheme,
  ACCENTS,
  ACCENT_COLORS,
  type Accent,
} from '@appgrove/design-system'
import { useTranslation, LANGUAGES, type Language } from '@appgrove/i18n'
import { Breadcrumb } from './Breadcrumb'

/* Pulsante-icona 38px del mockup (tema, notifiche): bordo sottile, raggio 10px, hover su surface-3. */
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
      className="relative flex h-[38px] w-[38px] items-center justify-center rounded-[10px] border border-line text-fg-muted transition-colors hover:bg-surface-3"
    >
      <Icon name={icon} size={20} />
      {children}
    </button>
  )
}

/** Topbar (mockup): 64px, sfondo traslucido sfocato, breadcrumb a sinistra; a destra pallini accent, lingua, tema, notifiche (#03 IA). */
export function Topbar({ onOpenSidebar }: { onOpenSidebar?: () => void }) {
  const { t, i18n } = useTranslation()
  const { theme, accent, setAccent, toggleTheme } = useTheme()

  const language = (i18n.language?.slice(0, 2) as Language) ?? 'en'

  return (
    <header className="sticky top-0 z-20 flex h-16 shrink-0 items-center gap-4 border-b border-line bg-surface/80 px-[26px] backdrop-blur-md">
      <button
        type="button"
        aria-label={t('nav.platform')}
        onClick={onOpenSidebar}
        className="flex h-9 w-9 items-center justify-center rounded-[9px] border border-line text-fg-muted hover:bg-surface-3 lg:hidden"
      >
        <Icon name="menu" size={20} />
      </button>

      <div className="min-w-0 flex-1">
        <Breadcrumb />
      </div>

      <div className="flex items-center gap-2.5">
        <div
          role="radiogroup"
          aria-label={t('topbar.accent')}
          className="flex items-center gap-1.5 rounded-pill bg-surface-3 p-1"
        >
          {ACCENTS.map((a) => (
            <button
              key={a}
              type="button"
              role="radio"
              aria-checked={accent === a}
              aria-label={a}
              onClick={() => setAccent(a as Accent)}
              className={cn(
                'h-[18px] w-[18px] rounded-pill border-2 shadow-[0_0_0_1px_rgb(var(--ag-border))]',
                accent === a ? 'border-surface' : 'border-transparent',
              )}
              style={{ background: ACCENT_COLORS[a] }}
            />
          ))}
        </div>

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
            className="absolute right-[9px] top-2 h-[7px] w-[7px] rounded-pill border-[1.5px] border-surface bg-accent"
          />
        </IconButton>
      </div>
    </header>
  )
}
