import { z } from 'zod'

// Schemi Zod dei form auth, **allineati alle Bean Validation** del backend (@Email/@NotBlank) e alla
// password policy del servizio auth (PasswordPolicy: min 10, ≥1 maiuscola, ≥1 minuscola, ≥1 cifra) — #03 dec.7.
// Le factory prendono `t` per messaggi localizzati (EN/IT).

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type TFn = (key: any, opts?: Record<string, unknown>) => string

export const emailField = (t: TFn) =>
  z.string().trim().min(1, t('validation.required')).email(t('validation.email'))

export const passwordField = (t: TFn) =>
  z
    .string()
    .min(10, t('validation.passwordPolicy'))
    .regex(/[A-Z]/, t('validation.passwordPolicy'))
    .regex(/[a-z]/, t('validation.passwordPolicy'))
    .regex(/[0-9]/, t('validation.passwordPolicy'))

const displayNameField = (t: TFn) =>
  z.string().trim().max(255, t('validation.tooLong', { max: 255 })).optional()

const codeField = (t: TFn) => z.string().trim().regex(/^\d{6}$/, t('validation.code'))

const nameField = (t: TFn) =>
  z.string().trim().min(1, t('validation.required')).max(255, t('validation.tooLong', { max: 255 }))

export const loginSchema = (t: TFn) =>
  z.object({ email: emailField(t), password: z.string().min(1, t('validation.required')) })

export const totpSchema = (t: TFn) => z.object({ code: codeField(t) })

export const signupSchema = (t: TFn) =>
  z.object({ email: emailField(t), password: passwordField(t), displayName: displayNameField(t) })

export const forgotSchema = (t: TFn) => z.object({ email: emailField(t) })

export const resetSchema = (t: TFn) => z.object({ password: passwordField(t) })

export const acceptSchema = (t: TFn) =>
  z.object({ password: passwordField(t), displayName: displayNameField(t) })

export const workspaceSchema = (t: TFn) => z.object({ name: nameField(t) })

/** Form invito membro (UC 0059), allineato a `CreateInvitation` (@Email, @Size(max=320), role admin/member). */
export const inviteSchema = (t: TFn) =>
  z.object({
    email: emailField(t).refine((v) => v.length <= 320, t('validation.tooLong', { max: 320 })),
    role: z.enum(['admin', 'member']),
  })
