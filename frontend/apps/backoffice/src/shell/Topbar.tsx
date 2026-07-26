import { useRef } from 'react'
import {
  Icon,
  cn,
  useTheme,
  ACCENTS,
  ACCENT_COLORS,
  type Accent,
} from '@appgrove/design-system'
import {
  useTranslation,
  LANGUAGES,
  LANGUAGE_LABELS,
  persistLanguage,
  type Language,
} from '@appgrove/i18n'
import { Breadcrumb } from './Breadcrumb'

/**
 * Selettore lingua a tendina (UC 0060): con 5 lingue un controllo segmentato è troppo stretto.
 * Riusa il pattern disclosure `<details>` (come il menu utente) — nessuna nuova dipendenza. La scelta
 * viene persistita così da ritrovarla al ricaricamento.
 */
function LanguageMenu() {
  const { t, i18n } = useTranslation()
  const ref = useRef<HTMLDetailsElement>(null)
  const current = (i18n.language?.slice(0, 2) as Language) ?? 'en'

  const select = (language: Language) => {
    persistLanguage(language)
    void i18n.changeLanguage(language)
    ref.current?.removeAttribute('open')
  }

  return (
    <details ref={ref} className="relative">
      <summary
        aria-label={t('topbar.language')}
        className="flex h-[38px] cursor-pointer list-none items-center gap-1 rounded-[10px] border border-line px-2.5 text-[13px] font-semibold text-fg-muted transition-colors hover:bg-surface-3"
      >
        {current.toUpperCase()}
        <Icon name="expand_more" size={16} />
      </summary>
      <div className="absolute right-0 top-full z-30 mt-1.5 w-40 rounded-md border border-line bg-surface p-1 shadow-lg">
        {LANGUAGES.map((l) => (
          <button
            key={l}
            type="button"
            onClick={() => select(l)}
            aria-current={l === current}
            className={cn(
              'flex w-full items-center justify-between rounded-lg px-2.5 py-1.5 text-left text-[13px] font-semibold transition-colors hover:bg-surface-3',
              l === current ? 'text-accent' : 'text-fg',
            )}
          >
            {LANGUAGE_LABELS[l]}
            {l === current && <Icon name="check" size={16} />}
          </button>
        ))}
      </div>
    </details>
  )
}

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
  const { t } = useTranslation()
  const { theme, accent, setAccent, toggleTheme } = useTheme()

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

        <LanguageMenu />

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
