import { forwardRef, type HTMLAttributes, type ReactNode } from 'react'
import { cn } from '../lib/cn'
import { Icon } from './Icon'

export interface PageHeaderProps extends HTMLAttributes<HTMLElement> {
  title: ReactNode
  /** Sottotitolo sotto il titolo (13–14px, colore attenuato). */
  subtitle?: ReactNode
  /** Nome Material Symbol: attiva la variante "pagina app" con riquadro icona tinto (mockup Invoices/Calendar). */
  icon?: string
  /** Classi per il riquadro icona: tinta di sfondo + colore del glifo (es. `bg-cat-blue/15 text-cat-blue`). */
  iconClassName?: string
  /** Azioni allineate a destra (pulsanti, segmented control). */
  actions?: ReactNode
}

/**
 * Header di pagina nello stile dei mockup: titolo 27px/800 (o 24px/800 con riquadro icona 44px),
 * sottotitolo attenuato, azioni allineate a destra sulla linea di base del titolo.
 */
export const PageHeader = forwardRef<HTMLElement, PageHeaderProps>(
  ({ title, subtitle, icon, iconClassName, actions, className, ...props }, ref) => (
    <header
      ref={ref}
      className={cn('flex flex-wrap items-end justify-between gap-4', className)}
      {...props}
    >
      <div className="flex min-w-0 items-center gap-3.5">
        {icon && (
          <span
            aria-hidden
            className={cn(
              'flex h-11 w-11 shrink-0 items-center justify-center rounded-[13px]',
              iconClassName ?? 'bg-accent/15 text-accent',
            )}
          >
            <Icon name={icon} size={24} filled />
          </span>
        )}
        <div className="min-w-0">
          <h1
            className={cn(
              'm-0 font-sans font-extrabold text-fg',
              icon ? 'text-2xl tracking-[-0.02em]' : 'text-[27px] tracking-[-0.025em]',
            )}
          >
            {title}
          </h1>
          {subtitle && <p className="m-0 mt-0.5 text-[13px] text-fg-muted">{subtitle}</p>}
        </div>
      </div>
      {actions && <div className="flex shrink-0 items-center gap-2.5">{actions}</div>}
    </header>
  ),
)
PageHeader.displayName = 'PageHeader'
