import { useMemo } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Button, Card, CardContent, CardHeader, CardTitle, Input } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useCurrentAccount, useUpdateAccountName } from '../api/hooks'

/**
 * Form RHF + Zod (#03 dec.7): lo schema rispecchia le regole Bean Validation di `UpdateAccount`
 * (`@NotBlank`, `@Size(max=255)`) per validazione coerente client/server.
 */
export function Settings() {
  const { t } = useTranslation()
  const account = useCurrentAccount()
  const update = useUpdateAccountName()

  const schema = useMemo(
    () =>
      z.object({
        name: z
          .string()
          .trim()
          .min(1, t('validation.required'))
          .max(255, t('validation.tooLong', { max: 255 })),
      }),
    [t],
  )
  type Values = z.infer<typeof schema>

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    values: { name: account.data?.name ?? '' },
  })

  const onSubmit = handleSubmit((values) => update.mutateAsync(values.name).catch(() => undefined))

  return (
    <div className="space-y-[22px]">
      <h1 className="text-[27px] font-extrabold tracking-[-0.025em] text-fg">{t('settings.title')}</h1>
      <Card>
        <CardHeader>
          <CardTitle>{t('settings.title')}</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={onSubmit} className="max-w-sm space-y-4" noValidate>
            <div className="space-y-1">
              <label htmlFor="account-name" className="text-sm font-medium text-fg">
                {t('settings.displayName')}
              </label>
              <Input
                id="account-name"
                invalid={!!errors.name}
                aria-describedby={errors.name ? 'account-name-error' : undefined}
                {...register('name')}
              />
              {errors.name && (
                <p id="account-name-error" role="alert" className="text-sm text-danger">
                  {errors.name.message}
                </p>
              )}
            </div>
            <div className="flex items-center gap-3">
              <Button type="submit" disabled={isSubmitting || update.isPending}>
                {t('settings.save')}
              </Button>
              {update.isSuccess && <span className="text-sm text-success">{t('settings.saved')}</span>}
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
