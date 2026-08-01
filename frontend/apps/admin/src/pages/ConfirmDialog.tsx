import { useEffect, useId, useRef, type ReactNode } from 'react'
import { Button } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'

/**
 * Dialog di conferma accessibile (role="dialog" + aria-modal) per le azioni distruttive (UC 0021).
 * Focus iniziale sul pulsante di conferma; Escape annulla. Niente dipendenze: il design-system non
 * espone ancora un componente Dialog.
 */
export function ConfirmDialog({
  title,
  body,
  confirmLabel,
  tone = 'danger',
  busy = false,
  onConfirm,
  onCancel,
  children,
}: {
  title: string
  body: string
  confirmLabel?: string
  tone?: 'danger' | 'default'
  busy?: boolean
  onConfirm: () => void
  onCancel: () => void
  /** Contenuto aggiuntivo fra il testo e i pulsanti (spiegazioni, campi — es. la motivazione, UC 0076). */
  children?: ReactNode
}) {
  const { t } = useTranslation()
  const titleId = useId()
  const bodyId = useId()
  const confirmRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    confirmRef.current?.focus()
  }, [])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onCancel()
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [onCancel])

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={bodyId}
        className="max-h-[90vh] w-full max-w-md overflow-y-auto rounded-lg bg-surface p-6 shadow-lg"
      >
        <h2 id={titleId} className="text-lg font-semibold text-fg">
          {title}
        </h2>
        <p id={bodyId} className="mt-2 text-sm text-fg-muted">
          {body}
        </p>
        {children && <div className="mt-4 space-y-3">{children}</div>}
        <div className="mt-6 flex justify-end gap-3">
          <Button type="button" variant="ghost" onClick={onCancel} disabled={busy}>
            {t('common.cancel')}
          </Button>
          <Button
            ref={confirmRef}
            type="button"
            variant={tone === 'danger' ? 'danger' : 'primary'}
            onClick={onConfirm}
            disabled={busy}
          >
            {confirmLabel ?? t('admin.confirm')}
          </Button>
        </div>
      </div>
    </div>
  )
}
