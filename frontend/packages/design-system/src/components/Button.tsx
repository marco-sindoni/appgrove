import { forwardRef, type ButtonHTMLAttributes } from 'react'
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '../lib/cn'

export const buttonVariants = cva(
  'inline-flex items-center justify-center gap-2 font-sans font-semibold whitespace-nowrap transition-colors duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 focus-visible:ring-offset-bg disabled:pointer-events-none disabled:opacity-50',
  {
    variants: {
      variant: {
        primary: 'bg-accent text-accent-contrast hover:opacity-90',
        secondary: 'border border-line bg-surface-2 text-fg hover:bg-surface',
        ghost: 'bg-transparent text-fg hover:bg-surface-2',
        danger: 'bg-danger text-white hover:opacity-90',
      },
      size: {
        sm: 'h-8 rounded-md px-3 text-sm',
        md: 'h-10 rounded-md px-4 text-sm',
        lg: 'h-12 rounded-lg px-6 text-base',
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
