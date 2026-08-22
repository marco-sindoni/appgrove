import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useFieldArray, useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { ApiError, refusalMessage } from '@appgrove/api-client'
import { Button, Card, CardContent, CardHeader } from '@appgrove/design-system'
import { useShellContext } from '../../../registry/ShellContext'
import { Field } from '../../../pages/auth/Field'
import { useCreateItem } from '../api/hooks'
import type { CreateItem } from '../api/client'
import { use@@APP_CLASS@@Messages, type @@APP_CLASS@@Messages } from '../i18n'

// Schema come factory: il messaggio "campo obbligatorio" è localizzato (UC 0060). La forma dei
// dati non dipende dalla lingua, quindi il tipo del form si deriva una volta sola.
const makeSchema = (m: @@APP_CLASS@@Messages) =>
  z.object({
    contactName: z.string().min(1, m.required),
    contactEmail: z.string().email().optional().or(z.literal('')),
    lines: z.array(
      z.object({
        description: z.string().min(1, m.required),
        quantity: z.coerce.number().min(0),
        unitAmount: z.coerce.number().min(0),
      }),
    ),
  })

type FormValues = z.infer<ReturnType<typeof makeSchema>>

/** Editor di creazione: contatto + righe; gestisce il 429 (quota) con invito all'upgrade. */
export function ItemCreateScreen() {
  const navigate = useNavigate()
  const shell = useShellContext()
  const create = useCreateItem()
  const m = use@@APP_CLASS@@Messages()
  const [error, setError] = useState<string | null>(null)
  const [quotaReached, setQuotaReached] = useState(false)

  const schema = useMemo(() => makeSchema(m), [m])
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { contactName: '', contactEmail: '', lines: [] },
  })
  const lines = useFieldArray({ control: form.control, name: 'lines' })

  const onSubmit = form.handleSubmit(async (values) => {
    setError(null)
    setQuotaReached(false)
    const body: CreateItem = {
      contactName: values.contactName,
      contactEmail: values.contactEmail || undefined,
      lines: values.lines.map((l) => ({
        description: l.description,
        quantity: l.quantity,
        unitAmount: l.unitAmount,
      })),
    }
    try {
      await create.mutateAsync(body)
      navigate('..', { relative: 'path' })
    } catch (err) {
      // Il 429 non è un errore generico: è il tetto di piano, e va detto per quello che è.
      if (err instanceof ApiError && err.status === 429) {
        setQuotaReached(true)
        setError(m.errorQuota)
      } else {
        // Un 403 qui non è un guasto: è un varco che ha detto no, e i varchi parlano di cose diverse —
        // il diritto dell'account all'applicazione e il RUOLO della persona su di essa (UC 0099/0101).
        // Solo il server sa quale ha risposto, e la sua frase nomina il ruolo che serve e a chi
        // chiederlo. Scrivere «si è verificato un errore» butterebbe via l'unica informazione utile.
        setError(refusalMessage(err, m.errorGeneric))
      }
    }
  })

  return (
    <div className="space-y-[22px]">
      <h1 className="text-[27px] font-extrabold tracking-[-0.025em] text-fg">{m.editorTitle}</h1>

      {error && (
        <div role="alert" className="space-y-2 rounded-md border border-line bg-surface-2 p-3 text-sm">
          <p className="text-danger">{error}</p>
          {quotaReached && (
            <Button size="sm" onClick={() => shell.nav.navigate('/billing')}>
              {m.quotaUpgrade}
            </Button>
          )}
        </div>
      )}

      <form onSubmit={onSubmit} className="space-y-6" noValidate>
        <Card>
          <CardHeader>
            <h2 className="font-sans text-lg font-extrabold tracking-tight text-fg">
              {m.contactSection}
            </h2>
          </CardHeader>
          <CardContent className="space-y-4">
            <Field
              id="contactName"
              label={m.fieldContactName}
              error={form.formState.errors.contactName?.message}
              {...form.register('contactName')}
            />
            <Field
              id="contactEmail"
              type="email"
              label={m.fieldContactEmail}
              error={form.formState.errors.contactEmail?.message}
              {...form.register('contactEmail')}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <h2 className="font-sans text-lg font-extrabold tracking-tight text-fg">{m.linesTitle}</h2>
          </CardHeader>
          <CardContent className="space-y-4">
            {lines.fields.map((field, i) => (
              <div key={field.id} className="flex flex-wrap items-start gap-3">
                <div className="min-w-[14rem] flex-1">
                  <Field
                    id={`line-desc-${i}`}
                    label={m.fieldLineDescription}
                    error={form.formState.errors.lines?.[i]?.description?.message}
                    {...form.register(`lines.${i}.description`)}
                  />
                </div>
                <div className="w-28">
                  <Field
                    id={`line-qty-${i}`}
                    type="number"
                    step="any"
                    label={m.fieldLineQuantity}
                    {...form.register(`lines.${i}.quantity`)}
                  />
                </div>
                <div className="w-36">
                  <Field
                    id={`line-amt-${i}`}
                    type="number"
                    step="any"
                    label={m.fieldLineUnitAmount}
                    {...form.register(`lines.${i}.unitAmount`)}
                  />
                </div>
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  className="mt-6"
                  onClick={() => lines.remove(i)}
                >
                  {m.removeLine}
                </Button>
              </div>
            ))}
            <Button
              type="button"
              variant="secondary"
              size="sm"
              onClick={() => lines.append({ description: '', quantity: 1, unitAmount: 0 })}
            >
              {m.addLine}
            </Button>
          </CardContent>
        </Card>

        <div className="flex gap-3">
          <Button type="submit" disabled={create.isPending}>
            {m.save}
          </Button>
          <Button type="button" variant="ghost" onClick={() => navigate('..', { relative: 'path' })}>
            {m.cancel}
          </Button>
        </div>
      </form>
    </div>
  )
}
