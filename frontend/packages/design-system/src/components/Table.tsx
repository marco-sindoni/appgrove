import { forwardRef, type HTMLAttributes, type TdHTMLAttributes, type ThHTMLAttributes } from 'react'
import { cn } from '../lib/cn'

/**
 * Tabella dati nello stile dei mockup (docs/frontend-design/): contenitore-card con angoli 18px,
 * header colonne in maiuscolo 11px/700 spaziato, righe 13px con hover su surface-2.
 * `Table` rende il contenitore (con scroll orizzontale) + l'elemento `<table>`;
 * dentro si compongono TableHead/TableBody/TableRow/TableHeadCell/TableCell.
 */
export const Table = forwardRef<HTMLTableElement, HTMLAttributes<HTMLTableElement>>(
  ({ className, ...props }, ref) => (
    <div className="overflow-hidden rounded-lg border border-line bg-surface shadow-sm">
      <div className="overflow-x-auto">
        <table
          ref={ref}
          className={cn('w-full border-collapse text-left font-sans', className)}
          {...props}
        />
      </div>
    </div>
  ),
)
Table.displayName = 'Table'

export const TableHead = forwardRef<HTMLTableSectionElement, HTMLAttributes<HTMLTableSectionElement>>(
  ({ className, ...props }, ref) => <thead ref={ref} className={className} {...props} />,
)
TableHead.displayName = 'TableHead'

export const TableBody = forwardRef<HTMLTableSectionElement, HTMLAttributes<HTMLTableSectionElement>>(
  ({ className, ...props }, ref) => <tbody ref={ref} className={className} {...props} />,
)
TableBody.displayName = 'TableBody'

export interface TableRowProps extends HTMLAttributes<HTMLTableRowElement> {
  /** Evidenzia la riga al passaggio del mouse (righe cliccabili). */
  interactive?: boolean
}

export const TableRow = forwardRef<HTMLTableRowElement, TableRowProps>(
  ({ className, interactive, ...props }, ref) => (
    <tr
      ref={ref}
      className={cn(
        'border-b border-line last:border-b-0',
        interactive && 'cursor-pointer transition-colors hover:bg-surface-2',
        className,
      )}
      {...props}
    />
  ),
)
TableRow.displayName = 'TableRow'

export const TableHeadCell = forwardRef<HTMLTableCellElement, ThHTMLAttributes<HTMLTableCellElement>>(
  ({ className, ...props }, ref) => (
    <th
      ref={ref}
      className={cn(
        'border-b border-line px-3 py-[11px] text-[11px] font-bold uppercase tracking-[.05em] text-fg-faint first:pl-[22px] last:pr-[22px]',
        className,
      )}
      {...props}
    />
  ),
)
TableHeadCell.displayName = 'TableHeadCell'

export const TableCell = forwardRef<HTMLTableCellElement, TdHTMLAttributes<HTMLTableCellElement>>(
  ({ className, ...props }, ref) => (
    <td
      ref={ref}
      className={cn(
        'px-3 py-[14px] text-[13px] text-fg first:pl-[22px] last:pr-[22px]',
        className,
      )}
      {...props}
    />
  ),
)
TableCell.displayName = 'TableCell'
