import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useFieldArray, useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { ApiError } from '@appgrove/api-client'
import { Button, Card, CardContent, CardHeader } from '@appgrove/design-system'
import { useShellContext } from '../../../registry/ShellContext'
import { Field } from '../../../pages/auth/Field'
import { useCreateInvoice } from '../api/hooks'
import type { CreateInvoice } from '../api/client'
import { t } from '../strings'

const schema = z.object({
  customerName: z.string().min(1, t.required),
  customerEmail: z.string().email().optional().or(z.literal('')),
  lines: z.array(
    z.object({
      description: z.string().min(1, t.required),
      quantity: z.coerce.number().min(0),
      unitAmount: z.coerce.number().min(0),
    }),
  ),
})

type FormValues = z.infer<typeof schema>

/** Editor di creazione fattura: cliente + righe; gestisce il 429 (quota) con CTA upgrade. */
export function InvoiceCreateScreen() {
  const navigate = useNavigate()
  const shell = useShellContext()
  const create = useCreateInvoice()
  const [error, setError] = useState<string | null>(null)
  const [quotaReached, setQuotaReached] = useState(false)

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { customerName: '', customerEmail: '', lines: [] },
  })
  const lines = useFieldArray({ control: form.control, name: 'lines' })

  const onSubmit = form.handleSubmit(async (values) => {
    setError(null)
    setQuotaReached(false)
    const body: CreateInvoice = {
      customerName: values.customerName,
      customerEmail: values.customerEmail || undefined,
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
      if (err instanceof ApiError && err.status === 429) {
        setQuotaReached(true)
        setError(t.errorQuota)
      } else {
        setError(t.errorGeneric)
      }
    }
  })

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold text-fg">{t.editorTitle}</h1>

      {error && (
        <div role="alert" className="space-y-2 rounded-md border border-line bg-surface-2 p-3 text-sm">
          <p className="text-danger">{error}</p>
          {quotaReached && (
            <Button size="sm" onClick={() => shell.nav.navigate('/billing')}>
              {t.quotaUpgrade}
            </Button>
          )}
        </div>
      )}

      <form onSubmit={onSubmit} className="space-y-6" noValidate>
        <Card>
          <CardHeader>
            <h2 className="font-sans text-lg font-extrabold tracking-tight text-fg">{t.customerSection}</h2>
          </CardHeader>
          <CardContent className="space-y-4">
            <Field
              id="customerName"
              label={t.fieldCustomerName}
              error={form.formState.errors.customerName?.message}
              {...form.register('customerName')}
            />
            <Field
              id="customerEmail"
              type="email"
              label={t.fieldCustomerEmail}
              error={form.formState.errors.customerEmail?.message}
              {...form.register('customerEmail')}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <h2 className="font-sans text-lg font-extrabold tracking-tight text-fg">{t.linesTitle}</h2>
          </CardHeader>
          <CardContent className="space-y-4">
            {lines.fields.map((field, i) => (
              <div key={field.id} className="flex flex-wrap items-start gap-3">
                <div className="min-w-[14rem] flex-1">
                  <Field
                    id={`line-desc-${i}`}
                    label={t.fieldLineDescription}
                    error={form.formState.errors.lines?.[i]?.description?.message}
                    {...form.register(`lines.${i}.description`)}
                  />
                </div>
                <div className="w-28">
                  <Field
                    id={`line-qty-${i}`}
                    type="number"
                    step="any"
                    label={t.fieldLineQuantity}
                    {...form.register(`lines.${i}.quantity`)}
                  />
                </div>
                <div className="w-36">
                  <Field
                    id={`line-amt-${i}`}
                    type="number"
                    step="any"
                    label={t.fieldLineUnitAmount}
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
                  {t.removeLine}
                </Button>
              </div>
            ))}
            <Button
              type="button"
              variant="secondary"
              size="sm"
              onClick={() => lines.append({ description: '', quantity: 1, unitAmount: 0 })}
            >
              {t.addLine}
            </Button>
          </CardContent>
        </Card>

        <div className="flex gap-3">
          <Button type="submit" disabled={create.isPending}>
            {t.save}
          </Button>
          <Button type="button" variant="ghost" onClick={() => navigate('..', { relative: 'path' })}>
            {t.cancel}
          </Button>
        </div>
      </form>
    </div>
  )
}
