import { forwardRef, type InputHTMLAttributes } from 'react'
import { cn } from '../lib/cn'

export type CheckboxProps = InputHTMLAttributes<HTMLInputElement>

/**
 * Casella di spunta accessibile: input nativo stilizzato coi token del design system. Nativo di
 * proposito — la semantica di `checkbox` (spazio per spuntare, stato indeterminate, label collegata)
 * arriva gratis e resta corretta con react-hook-form. Il colore di spunta usa `accent-color`.
 */
export const Checkbox = forwardRef<HTMLInputElement, CheckboxProps>(({ className, ...props }, ref) => (
  <input
    ref={ref}
    type="checkbox"
    className={cn(
      'h-4 w-4 shrink-0 cursor-pointer rounded border-2 border-line-strong bg-bg accent-accent',
      'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 focus-visible:ring-offset-bg',
      'disabled:cursor-not-allowed disabled:opacity-50',
      className,
    )}
    {...props}
  />
))
Checkbox.displayName = 'Checkbox'
