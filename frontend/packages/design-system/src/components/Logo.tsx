import type { HTMLAttributes } from 'react'
import { cn } from '../lib/cn'

export interface LogoProps extends HTMLAttributes<HTMLSpanElement> {
  /** Mostra il wordmark "appgrove" accanto al mark. */
  showWordmark?: boolean
  /** Dimensione del mark in px. */
  size?: number
}

/**
 * Logo appgrove: mark a foglia `eco` in un quadrato ad angoli morbidi (accent) + wordmark.
 * Placeholder on-brand (artwork finale escluso da UC 0019). Si adatta a light/dark via i token.
 */
export function Logo({ showWordmark = true, size = 28, className, ...props }: LogoProps) {
  return (
    <span
      className={cn('inline-flex items-center gap-2 text-fg', className)}
      role="img"
      aria-label="appgrove"
      {...props}
    >
      <svg
        width={size}
        height={size}
        viewBox="0 0 32 32"
        fill="none"
        aria-hidden="true"
        focusable="false"
      >
        <rect width="32" height="32" rx="9" fill="rgb(var(--ag-accent))" />
        <path
          d="M22 9c0 6.5-3.8 11-9.5 11-1 0-1.9-.15-2.5-.4C10.4 13.2 14.8 9.6 22 9Z"
          fill="rgb(var(--ag-accent-contrast))"
        />
        <path
          d="M10 22c1.2-3.4 3.4-5.8 6.6-7.2"
          stroke="rgb(var(--ag-accent))"
          strokeWidth="1.4"
          strokeLinecap="round"
        />
      </svg>
      {showWordmark && (
        <span className="font-sans text-lg font-extrabold tracking-tight">appgrove</span>
      )}
    </span>
  )
}
