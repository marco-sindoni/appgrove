import { useState } from 'react'
import { Link } from 'react-router-dom'
import ReactMarkdown from 'react-markdown'
import { Button, Card, CardContent, CardHeader, CardTitle, Checkbox, Icon } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import type { components } from '@appgrove/api-client'
import { useAcceptLegal, useLegalDoc } from '../../api/hooks'

export type ComponentStatus = components['schemas']['ComponentStatusView']

// Le chiavi di traduzione composte dinamicamente non sono nella union tipizzata di `t`: come nel resto
// del backoffice (es. billing/SubscriptionsPanel) si usa questo alias per il cast puntuale.
type TKey = never

/**
 * Una riga della schermata: nome del componente, versione/data, disclosure che carica il documento
 * on-demand ({@link useLegalDoc} abilitato solo ad apertura) e la casella di spunta (accetto / preso
 * atto, a seconda di `act`).
 */
function LegalItem({
  status,
  lang,
  checked,
  onToggle,
}: {
  status: ComponentStatus
  lang: string
  checked: boolean
  onToggle: (value: boolean) => void
}) {
  const { t } = useTranslation()
  const [open, setOpen] = useState(false)
  const doc = useLegalDoc(open ? status.component : undefined, lang)

  const component = status.component ?? ''
  const name = t(`legal.component.${component}` as TKey)
  const checkboxId = `legal-accept-${component}`
  const contentId = `legal-doc-${component}`
  const isAccept = status.act === 'accept'

  return (
    <div className="rounded-md border border-line p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="font-sans font-bold text-fg">{name}</p>
          <p className="text-xs text-fg-muted">
            {t('legal.version', { version: status.version })}
            {' · '}
            {t('legal.updatedOn', { date: status.effectiveDate })}
          </p>
        </div>
        <Button
          type="button"
          variant="secondary"
          size="sm"
          aria-expanded={open}
          aria-controls={open ? contentId : undefined}
          onClick={() => setOpen((value) => !value)}
        >
          {t('legal.viewDocument')}
        </Button>
      </div>

      {open && (
        <div id={contentId} className="mt-3 border-t border-line pt-3">
          {doc.isLoading && (
            <p role="status" className="text-sm text-fg-muted">
              {t('legal.loading')}
            </p>
          )}
          {doc.isError && (
            <div role="alert" className="flex flex-wrap items-center gap-3">
              <p className="text-sm text-danger">{t('legal.error')}</p>
              <Button type="button" variant="secondary" size="sm" onClick={() => doc.refetch()}>
                {t('legal.retry')}
              </Button>
            </div>
          )}
          {doc.data?.markdown && (
            <div className="max-h-72 overflow-y-auto text-sm leading-relaxed text-fg-muted [&_a]:text-accent [&_a]:underline [&_h1]:mb-2 [&_h1]:mt-3 [&_h1]:font-sans [&_h1]:text-base [&_h1]:font-bold [&_h1]:text-fg [&_h2]:mb-2 [&_h2]:mt-3 [&_h2]:font-sans [&_h2]:font-bold [&_h2]:text-fg [&_li]:ml-4 [&_li]:list-disc [&_p]:mb-2">
              <ReactMarkdown>{doc.data.markdown}</ReactMarkdown>
            </div>
          )}
        </div>
      )}

      <label htmlFor={checkboxId} className="mt-3 flex items-center gap-2 text-sm text-fg">
        <Checkbox
          id={checkboxId}
          checked={checked}
          onChange={(event) => onToggle(event.target.checked)}
        />
        <span>{t(isAccept ? 'legal.accept' : 'legal.acknowledge')}</span>
      </label>
    </div>
  )
}

/**
 * Schermata bloccante di ri-accettazione (UC 0056): full-screen, sostituisce l'intero contenuto della
 * shell quando ci sono documenti vincolanti pendenti. L'utente deve spuntare ogni componente prima di
 * poter continuare; resta sempre raggiungibile la pagina "I miei dati" (diritti GDPR, DoD 4).
 */
export function LegalGateScreen({ pending }: { pending: ComponentStatus[] }) {
  const { t, i18n } = useTranslation()
  const accept = useAcceptLegal()
  const [checked, setChecked] = useState<Record<string, boolean>>({})

  const allChecked = pending.every((item) => checked[item.component ?? ''])

  return (
    <div className="flex min-h-screen items-center justify-center bg-bg p-6">
      <Card
        role="dialog"
        aria-modal="true"
        aria-labelledby="legal-gate-title"
        className="w-full max-w-2xl"
      >
        <CardHeader>
          <CardTitle id="legal-gate-title">{t('legal.gateTitle')}</CardTitle>
          <p className="text-sm text-fg-muted">{t('legal.gateIntro')}</p>
        </CardHeader>
        <CardContent className="flex flex-col gap-4 text-fg">
          {pending.map((item) => (
            <LegalItem
              key={item.component}
              status={item}
              lang={i18n.language}
              checked={Boolean(checked[item.component ?? ''])}
              onToggle={(value) =>
                setChecked((state) => ({ ...state, [item.component ?? '']: value }))
              }
            />
          ))}

          <div className="flex flex-wrap items-center justify-between gap-3 pt-2">
            <Link
              to="/privacy"
              className="inline-flex items-center gap-1.5 text-sm text-fg-muted underline hover:text-fg"
            >
              <Icon name="shield_person" size={18} />
              {t('nav.privacy')}
            </Link>
            <Button
              type="button"
              disabled={!allChecked || accept.isPending}
              onClick={() => accept.mutate({ components: pending.map((item) => item.component ?? '') })}
            >
              {t('legal.continue')}
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
