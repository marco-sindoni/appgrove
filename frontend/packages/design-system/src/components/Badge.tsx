import { forwardRef, type HTMLAttributes } from 'react'
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '../lib/cn'

export const badgeVariants = cva(
  'inline-flex items-center gap-1.5 rounded-[7px] px-[9px] py-[3px] font-sans text-[11.5px] font-bold leading-[1.35]',
  {
    variants: {
      tone: {
        neutral: 'bg-surface-3 text-fg-faint',
        accent: 'bg-accent/15 text-accent',
        success: 'bg-success/15 text-success',
        warning: 'bg-warning/15 text-warning',
        danger: 'bg-danger/15 text-danger',
        info: 'bg-cat-blue/15 text-cat-blue',
        violet: 'bg-cat-violet/15 text-cat-violet',
      },
    },
    defaultVariants: { tone: 'neutral' },
  },
)

export interface BadgeProps
  extends HTMLAttributes<HTMLSpanElement>,
    VariantProps<typeof badgeVariants> {
  /** Puntino di stato (6px, currentColor) prima del testo, come nelle tabelle del mockup. */
  withDot?: boolean
}

export const Badge = forwardRef<HTMLSpanElement, BadgeProps>(
  ({ className, tone, withDot, children, ...props }, ref) => (
    <span ref={ref} className={cn(badgeVariants({ tone }), className)} {...props}>
      {withDot && (
        <span aria-hidden className="h-1.5 w-1.5 shrink-0 rounded-pill bg-current" />
      )}
      {children}
    </span>
  ),
)
Badge.displayName = 'Badge'
