import { useEffect, useId, useRef } from 'react'
import { Button } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { Modal } from '../../shell/Modal'

/**
 * Dialog di conferma accessibile per le azioni distruttive (UC 0059). Il guscio (sovrapposizione,
 * `role="dialog"`, Escape) è quello condiviso da {@link Modal}, introdotto con UC 0067 quando il secondo
 * dialogo — il cambio piano — ha reso la copia un debito; qui resta ciò che è proprio della conferma:
 * il corpo, il focus iniziale sul pulsante che conferma e la coppia annulla/conferma.
 */
export function ConfirmDialog({
  title,
  body,
  confirmLabel,
  tone = 'danger',
  busy = false,
  onConfirm,
  onCancel,
}: {
  title: string
  body: string
  confirmLabel?: string
  tone?: 'danger' | 'default'
  busy?: boolean
  onConfirm: () => void
  onCancel: () => void
}) {
  const { t } = useTranslation()
  const bodyId = useId()
  const confirmRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    confirmRef.current?.focus()
  }, [])

  return (
    <Modal title={title} onClose={onCancel} describedBy={bodyId}>
      <p id={bodyId} className="mt-2 text-sm text-fg-muted">
        {body}
      </p>
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
          {confirmLabel ?? t('members.confirm')}
        </Button>
      </div>
    </Modal>
  )
}
