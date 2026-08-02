import type { HTMLAttributes } from 'react'
import { cn } from '../lib/cn'
import { LOGO_PATHS, LOGO_TILE_RADIUS, LOGO_VIEWBOX } from '../brand/logo.mjs'

export interface LogoProps extends HTMLAttributes<HTMLSpanElement> {
  /** Mostra il wordmark "appgrove" accanto al mark. */
  showWordmark?: boolean
  /** Dimensione del mark in px. */
  size?: number
}

/**
 * Logo appgrove: mark a foglia in una piastrella ad angoli morbidi (accent) + wordmark.
 *
 * Il DISEGNO non vive qui: viene da `../brand/logo.mjs`, l'unico posto in cui è definito
 * (UC 0086). Così il generatore dell'immagine social e ogni altro consumatore non-React
 * disegnano esattamente lo stesso logo, e UC 0087 potrà sostituire l'artwork toccando
 * un file solo. Qui restano solo i colori, presi dai token → si adatta a chiaro/scuro.
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
        viewBox={LOGO_VIEWBOX}
        fill="none"
        aria-hidden="true"
        focusable="false"
      >
        <rect width="32" height="32" rx={LOGO_TILE_RADIUS} fill="rgb(var(--ag-accent))" />
        {LOGO_PATHS.map((path) =>
          path.fill ? (
            <path
              key={path.d}
              d={path.d}
              fill={`rgb(var(--ag-${path.on === 'contrast' ? 'accent-contrast' : 'accent'}))`}
            />
          ) : (
            <path
              key={path.d}
              d={path.d}
              stroke="rgb(var(--ag-accent))"
              strokeWidth={path.strokeWidth}
              strokeLinecap="round"
            />
          ),
        )}
      </svg>
      {showWordmark && (
        <span className="font-sans text-lg font-extrabold tracking-tight">appgrove</span>
      )}
    </span>
  )
}
