import { useState } from 'react'
import { refusalMessage } from '@appgrove/api-client'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Button, Card, CardContent, CardHeader } from '@appgrove/design-system'
import { Field } from '../../../pages/auth/Field'
import { useCreateContact } from '../api/hooks'
import type { CreateContact } from '../api/client'
import { t } from '../strings'

const schema = z.object({
  displayName: z.string().min(1, t.required),
  email: z.string().email().optional().or(z.literal('')),
  phone: z.string().optional(),
  organization: z.string().optional(),
  notes: z.string().optional(),
})

type FormValues = z.infer<typeof schema>

/** Editor di creazione di un contatto. */
export function ContactCreateScreen() {
  const navigate = useNavigate()
  const create = useCreateContact()
  const [error, setError] = useState<string | null>(null)

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { displayName: '', email: '', phone: '', organization: '', notes: '' },
  })

  const onSubmit = form.handleSubmit(async (values) => {
    setError(null)
    const body: CreateContact = {
      displayName: values.displayName,
      email: values.email || undefined,
      phone: values.phone || undefined,
      organization: values.organization || undefined,
      notes: values.notes || undefined,
    }
    try {
      await create.mutateAsync(body)
      navigate('..', { relative: 'path' })
    } catch (err) {
      setError(refusalMessage(err, t.errorGeneric))
    }
  })

  return (
    <div className="space-y-[22px]">
      <h1 className="text-[27px] font-extrabold tracking-[-0.025em] text-fg">{t.editorTitle}</h1>

      {error && (
        <p role="alert" className="text-sm text-danger">
          {error}
        </p>
      )}

      <form onSubmit={onSubmit} className="space-y-6" noValidate>
        <Card>
          <CardHeader>
            <h2 className="font-sans text-lg font-extrabold tracking-tight text-fg">
              {t.contactSection}
            </h2>
          </CardHeader>
          <CardContent className="space-y-4">
            <Field
              id="displayName"
              label={t.fieldName}
              error={form.formState.errors.displayName?.message}
              {...form.register('displayName')}
            />
            <Field
              id="email"
              type="email"
              label={t.fieldEmail}
              error={form.formState.errors.email?.message}
              {...form.register('email')}
            />
            <Field id="phone" label={t.fieldPhone} {...form.register('phone')} />
            <Field id="organization" label={t.fieldOrganization} {...form.register('organization')} />
            <Field id="notes" label={t.fieldNotes} {...form.register('notes')} />
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
