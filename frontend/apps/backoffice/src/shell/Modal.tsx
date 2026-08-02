import { useEffect, useId, type ReactNode } from 'react'

/**
 * Finestra modale accessibile: sovrapposizione scura, `role="dialog"` + `aria-modal`, titolo collegato,
 * Escape per chiudere. Il design-system non espone (ancora) un componente di dialogo, e da UC 0067 i
 * dialoghi sono due — la conferma distruttiva dei membri e il cambio piano — quindi il pezzo comune vive
 * qui invece di essere copiato: due copie della gestione del focus e di Escape sono due accessibilità
 * diverse che divergono alla prima correzione.
 *
 * Il contenuto è libero: chi la usa decide corpo e pulsanti. `labelledBy`/`describedBy` accettano gli
 * identificativi generati dal chiamante quando il titolo non è una semplice stringa.
 */
export function Modal({
  title,
  onClose,
  children,
  size = 'sm',
  describedBy,
}: {
  title: string
  onClose: () => void
  children: ReactNode
  size?: 'sm' | 'lg'
  describedBy?: string
}) {
  const titleId = useId()

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [onClose])

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={describedBy}
        className={`max-h-[85vh] w-full overflow-y-auto rounded-lg bg-surface p-6 shadow-lg ${
          size === 'lg' ? 'max-w-lg' : 'max-w-sm'
        }`}
      >
        <h2 id={titleId} className="text-lg font-semibold text-fg">
          {title}
        </h2>
        {children}
      </div>
    </div>
  )
}
