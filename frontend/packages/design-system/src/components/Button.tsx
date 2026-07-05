import { forwardRef, type ButtonHTMLAttributes } from 'react'
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '../lib/cn'

export const buttonVariants = cva(
  'inline-flex items-center justify-center gap-1.5 font-sans font-bold whitespace-nowrap transition-[color,background-color,filter] duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 focus-visible:ring-offset-bg disabled:pointer-events-none disabled:opacity-50',
  {
    variants: {
      variant: {
        primary:
          'bg-accent text-accent-contrast shadow-[0_6px_16px_-6px_rgb(var(--ag-accent))] hover:brightness-105',
        secondary: 'border border-line bg-transparent text-fg-muted hover:bg-surface-3',
        ghost: 'bg-transparent text-fg-muted hover:bg-surface-3 hover:text-fg',
        danger: 'border border-danger/45 bg-transparent text-danger hover:bg-danger/10',
      },
      size: {
        sm: 'h-8 rounded-sm px-3 text-[12.5px]',
        md: 'h-10 rounded-md px-4 text-[13px]',
        lg: 'h-12 rounded-md px-6 text-sm',
      },
    },
    defaultVariants: { variant: 'primary', size: 'md' },
  },
)

export interface ButtonProps
  extends ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, type = 'button', ...props }, ref) => (
    <button
      ref={ref}
      type={type}
      className={cn(buttonVariants({ variant, size }), className)}
      {...props}
    />
  ),
)
Button.displayName = 'Button'
