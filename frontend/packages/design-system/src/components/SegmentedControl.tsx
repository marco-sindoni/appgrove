import type { ReactNode } from 'react'
import * as ToggleGroup from '@radix-ui/react-toggle-group'
import { cn } from '../lib/cn'

export interface SegmentedOption {
  value: string
  label: ReactNode
}

export interface SegmentedControlProps {
  options: SegmentedOption[]
  value?: string
  defaultValue?: string
  onValueChange?: (value: string) => void
  'aria-label'?: string
  className?: string
}

/**
 * Segmented control per scelte binarie/poche opzioni (mensile/annuale, light/dark, viste calendario).
 * Su Radix ToggleGroup single-select; impedisce il deselect (resta sempre un'opzione attiva).
 */
export function SegmentedControl({
  options,
  value,
  defaultValue,
  onValueChange,
  'aria-label': ariaLabel,
  className,
}: SegmentedControlProps) {
  return (
    <ToggleGroup.Root
      type="single"
      value={value}
      defaultValue={defaultValue ?? options[0]?.value}
      onValueChange={(v) => {
        if (v) onValueChange?.(v)
      }}
      aria-label={ariaLabel}
      className={cn(
        'inline-flex items-center gap-1 rounded-md border border-line bg-surface-2 p-1',
        className,
      )}
    >
      {options.map((opt) => (
        <ToggleGroup.Item
          key={opt.value}
          value={opt.value}
          className={cn(
            'rounded-sm px-3 py-1 font-sans text-sm font-semibold text-fg-muted transition-colors',
            'hover:text-fg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent',
            'data-[state=on]:bg-surface data-[state=on]:text-fg data-[state=on]:shadow-sm',
          )}
        >
          {opt.label}
        </ToggleGroup.Item>
      ))}
    </ToggleGroup.Root>
  )
}
