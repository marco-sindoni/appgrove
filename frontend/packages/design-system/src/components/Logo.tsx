import type { HTMLAttributes } from 'react'
import { cn } from '../lib/cn'
import { LOGO_TILE_RADIUS, LOGO_VIEWBOX, logoPathsFor } from '../brand/logo.mjs'

export interface LogoProps extends HTMLAttributes<HTMLSpanElement> {
  /** Mostra il wordmark "appgrove" accanto al mark. */
  showWordmark?: boolean
  /** Dimensione del mark in px. */
  size?: number
  /**
   * Variante a un colore solo (`currentColor`), per gli sfondi difficili: fotografie,
   * aree colorate, superfici in cui l'accento non stacca. La piastrella diventa un
   * contorno e la foglia resta piena.
   */
  mono?: boolean
}

/**
 * Logo appgrove: mark a foglia in una piastrella ad angoli morbidi (accent) + wordmark.
 *
 * Il DISEGNO non vive qui: viene da `../brand/logo.mjs`, l'unico posto in cui è definito
 * (UC 0086, artwork definitivo con UC 0087). Così il generatore dell'immagine social, le
 * icone del browser e ogni altro consumatore non-React disegnano esattamente lo stesso
 * logo. Qui restano solo i colori, presi dai token → si adatta a chiaro/scuro — e la
 * scelta del livello di dettaglio, che dipende dalla dimensione richiesta.
 */
export function Logo({ showWordmark = true, size = 28, mono = false, className, ...props }: LogoProps) {
  const paths = logoPathsFor(size)
  const tile = mono ? 'currentColor' : 'rgb(var(--ag-accent))'
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
        {mono ? (
          <rect
            x="1"
            y="1"
            width="30"
            height="30"
            rx={LOGO_TILE_RADIUS - 1}
            fill="none"
            stroke="currentColor"
            strokeWidth={2}
          />
        ) : (
          <rect width="32" height="32" rx={LOGO_TILE_RADIUS} fill={tile} />
        )}
        {paths.map((path) =>
          path.fill ? (
            <path
              key={path.d}
              d={path.d}
              fill={
                mono
                  ? 'currentColor'
                  : `rgb(var(--ag-${path.on === 'contrast' ? 'accent-contrast' : 'accent'}))`
              }
            />
          ) : (
            <path
              key={path.d}
              d={path.d}
              stroke={mono ? 'currentColor' : 'rgb(var(--ag-accent))'}
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
