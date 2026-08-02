import type { SVGProps } from 'react'
import { cn } from '../lib/cn'

/**
 * Cornice comune delle illustrazioni del brand kit (UC 0087).
 *
 * Perché esiste. La nota di stile (`ILLUSTRAZIONI.md`) chiede che tutte le figure
 * condividano impianto e marcatura: stesso pannello di fondo ad angoli morbidi, stesse
 * classi, stessa dichiarazione di decoratività. Se ogni figura se le riscrive, la
 * seconda o la terza divergono — non per cattiva volontà, per distrazione. Qui la
 * cornice è una sola, e ogni figura porta solo il proprio disegno.
 *
 * Riquadro 240×160 (formato compatto per le applicazioni web, dove la figura sta dentro
 * una scheda). Le figure di sezione della vetrina restano sul riquadro largo 480×340.
 */
export interface IllustrationProps extends Omit<SVGProps<SVGSVGElement>, 'viewBox'> {
  /** Nome della figura: finisce in `data-illustration`, che è come la si riconosce. */
  name: string
}

export const ILLUSTRATION_VIEWBOX = '0 0 240 160'

export function Illustration({ name, className, children, ...props }: IllustrationProps) {
  return (
    <svg
      viewBox={ILLUSTRATION_VIEWBOX}
      className={cn('ag-illustration h-auto w-full', className)}
      data-illustration={name}
      role="presentation"
      aria-hidden="true"
      xmlns="http://www.w3.org/2000/svg"
      {...props}
    >
      {/* pannello di fondo: la cornice che dà unità a tutte le figure */}
      <rect
        x="4"
        y="4"
        width="232"
        height="152"
        rx="20"
        className="fill-surface-2 stroke-line"
        strokeWidth="1.5"
      />
      {children}
    </svg>
  )
}
