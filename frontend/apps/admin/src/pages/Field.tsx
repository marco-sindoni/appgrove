import { forwardRef } from 'react'
import { Input, type InputProps } from '@appgrove/design-system'

/** Campo form accessibile: label + input + messaggio d'errore (aria-describedby/role=alert). */
export const Field = forwardRef<
  HTMLInputElement,
  InputProps & { id: string; label: string; error?: string }
>(({ id, label, error, ...props }, ref) => (
  <div className="space-y-1">
    <label htmlFor={id} className="text-sm font-medium text-fg">
      {label}
    </label>
    <Input
      id={id}
      ref={ref}
      invalid={!!error}
      aria-describedby={error ? `${id}-error` : undefined}
      {...props}
    />
    {error && (
      <p id={`${id}-error`} role="alert" className="text-sm text-danger">
        {error}
      </p>
    )}
  </div>
))
Field.displayName = 'Field'
