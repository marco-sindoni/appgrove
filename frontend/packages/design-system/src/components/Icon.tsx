import type { CSSProperties, HTMLAttributes } from 'react'
import { cn } from '../lib/cn'

export interface IconProps extends Omit<HTMLAttributes<HTMLSpanElement>, 'children'> {
  /** Nome del Material Symbol (es. "eco", "settings", "notifications"). */
  name: string
  /** Variante piena (asse FILL) per gli stati attivi. */
  filled?: boolean
  /** Dimensione in px (optical size dell'icona). */
  size?: number
  /**
   * Etichetta accessibile. Se assente l'icona è decorativa (aria-hidden);
   * se presente diventa role="img" con la label.
   */
  'aria-label'?: string
}

/** Wrapper su Material Symbols Rounded (font caricato da fonts.css). */
export function Icon({
  name,
  filled = false,
  size = 20,
  className,
  style,
  'aria-label': ariaLabel,
  ...props
}: IconProps) {
  const iconStyle: CSSProperties = {
    fontSize: size,
    fontVariationSettings: `'FILL' ${filled ? 1 : 0}`,
    ...style,
  }
  return (
    <span
      className={cn('material-symbols-rounded select-none leading-none', className)}
      style={iconStyle}
      aria-hidden={ariaLabel ? undefined : true}
      role={ariaLabel ? 'img' : undefined}
      aria-label={ariaLabel}
      {...props}
    >
      {name}
    </span>
  )
}
