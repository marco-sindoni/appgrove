import { useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { ApiError, type InvitationView, type UserView } from '@appgrove/api-client'
import { Badge, Button, Card, CardContent, CardHeader } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useConfig } from '../../config'
import { sendInvitation } from '../../auth/authApi'
import { inviteSchema, type TFn } from '../../auth/schemas'
import {
  useCreateInvitation,
  useCurrentUser,
  useInvitations,
  useMembers,
  useRemoveMember,
  useRevokeInvitation,
  useUpdateMember,
} from '../../api/hooks'
import { QueryState } from '../../shell/QueryState'
import { Field } from '../auth/Field'
import { ConfirmDialog } from './ConfirmDialog'

type Role = 'owner' | 'admin' | 'member'

const roleLabel = (t: TFn, role?: string) =>
  role === 'owner' ? t('members.roleOwner') : role === 'admin' ? t('members.roleAdmin') : t('members.roleMember')

const statusBadge = (t: TFn, status?: string) =>
  status === 'suspended' ? (
    <Badge tone="warning">{t('members.statusSuspended')}</Badge>
  ) : (
    <Badge tone="success">{t('members.statusActive')}</Badge>
  )

function inviteErrorMessage(err: unknown, t: TFn): string {
  if (err instanceof ApiError && err.status === 409) return t('members.emailAlreadyMember')
  return t('errors.generic')
}

interface InviteSuccess {
  email: string
  link: string
  emailed: boolean
}

type Confirm =
  | { kind: 'suspend' | 'remove'; user: UserView }
  | { kind: 'revoke'; invitation: InvitationView }

/**
 * Sezione "Membri" (UC 0059): lista membri + inviti pendenti, invito (POST core + send auth),
 * revoca, cambio ruolo, sospendi/riattiva, rimuovi. Gating UX su self/ultimo-owner (difesa in
 * profondità; l'enforcement vero è nel core via @RolesAllowed). EN/IT, a11y.
 */
export function MembersPage() {
  const { t } = useTranslation()
  const config = useConfig()

  const me = useCurrentUser()
  const members = useMembers()
  const invitations = useInvitations()
  const createInvitation = useCreateInvitation()
  const revokeInvitation = useRevokeInvitation()
  const updateMember = useUpdateMember()
  const removeMember = useRemoveMember()

  const [invite, setInvite] = useState<InviteSuccess | null>(null)
  const [inviteError, setInviteError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [confirm, setConfirm] = useState<Confirm | null>(null)
  const [copied, setCopied] = useState(false)

  const memberList = members.data?.content ?? []
  const inviteList = invitations.data?.content ?? []
  const ownersCount = useMemo(
    () => memberList.filter((m) => m.role === 'owner').length,
    [memberList],
  )

  const isSelf = (u: UserView) => !!me.data?.id && u.id === me.data.id
  const isLastOwner = (u: UserView) => u.role === 'owner' && ownersCount <= 1
  const locked = (u: UserView) => isSelf(u) || isLastOwner(u)

  const form = useForm<z.infer<ReturnType<typeof inviteSchema>>>({
    resolver: zodResolver(inviteSchema(t)),
    defaultValues: { email: '', role: 'member' },
  })

  const onInvite = form.handleSubmit(async (values) => {
    setInviteError(null)
    setActionError(null)
    setInvite(null)
    try {
      const created = await createInvitation.mutateAsync(values)
      const token = created?.token ?? ''
      const link = `${window.location.origin}/accept?token=${token}`
      let emailed = true
      try {
        await sendInvitation(config.authBaseUrl, { email: values.email, token, role: values.role })
      } catch {
        emailed = false
      }
      setInvite({ email: values.email, link, emailed })
      form.reset({ email: '', role: 'member' })
    } catch (err) {
      setInviteError(inviteErrorMessage(err, t))
    }
  })

  const onChangeRole = async (u: UserView, role: Role) => {
    setActionError(null)
    try {
      await updateMember.mutateAsync({ id: u.id as string, role })
    } catch {
      setActionError(t('errors.generic'))
    }
  }

  const onToggleStatus = async (u: UserView) => {
    setActionError(null)
    try {
      await updateMember.mutateAsync({
        id: u.id as string,
        status: u.status === 'suspended' ? 'active' : 'suspended',
      })
    } catch {
      setActionError(t('errors.generic'))
    }
  }

  const onConfirm = async () => {
    if (!confirm) return
    setActionError(null)
    try {
      if (confirm.kind === 'revoke') {
        await revokeInvitation.mutateAsync(confirm.invitation.id as string)
      } else if (confirm.kind === 'remove') {
        await removeMember.mutateAsync(confirm.user.id as string)
      } else {
        await onToggleStatus(confirm.user)
      }
      setConfirm(null)
    } catch {
      setActionError(t('errors.generic'))
      setConfirm(null)
    }
  }

  const copyLink = async (link: string) => {
    try {
      await navigator.clipboard?.writeText(link)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      /* clipboard non disponibile: il link resta visibile e selezionabile */
    }
  }

  const busy =
    createInvitation.isPending ||
    revokeInvitation.isPending ||
    updateMember.isPending ||
    removeMember.isPending

  return (
    <div className="space-y-[22px]">
      <div>
        <h1 className="text-[27px] font-extrabold tracking-[-0.025em] text-fg">{t('members.title')}</h1>
        <p className="mt-1 text-sm text-fg-muted">{t('members.subtitle')}</p>
      </div>

      {actionError && (
        <p role="alert" className="text-sm text-danger">
          {actionError}
        </p>
      )}

      {/* Form invito */}
      <Card>
        <CardHeader>
          <h2 className="font-sans text-lg font-extrabold tracking-tight text-fg">
            {t('members.inviteTitle')}
          </h2>
        </CardHeader>
        <CardContent>
          <form onSubmit={onInvite} className="flex flex-wrap items-start gap-3" noValidate>
            <div className="min-w-[14rem] flex-1">
              <Field
                id="invite-email"
                type="email"
                label={t('members.inviteEmail')}
                autoComplete="off"
                error={form.formState.errors.email?.message}
                {...form.register('email')}
              />
            </div>
            <div>
              <label htmlFor="invite-role" className="mb-1 block text-sm font-medium text-fg">
                {t('members.inviteRole')}
              </label>
              <select
                id="invite-role"
                className="h-10 rounded-md border border-line bg-surface px-3 text-sm text-fg"
                {...form.register('role')}
              >
                <option value="member">{t('members.roleMember')}</option>
                <option value="admin">{t('members.roleAdmin')}</option>
              </select>
            </div>
            <Button type="submit" className="mt-6" disabled={form.formState.isSubmitting}>
              {t('members.inviteSubmit')}
            </Button>
          </form>

          {inviteError && (
            <p role="alert" className="mt-3 text-sm text-danger">
              {inviteError}
            </p>
          )}
          {invite && (
            <div role="status" className="mt-4 space-y-2 rounded-md bg-surface-2 p-3 text-sm">
              <p className={invite.emailed ? 'text-success' : 'text-warning'}>
                {invite.emailed
                  ? t('members.inviteSent', { email: invite.email })
                  : t('members.inviteEmailFailed')}
              </p>
              <p className="text-fg-muted">{t('members.inviteLink')}:</p>
              <div className="flex items-center gap-2">
                <code className="break-all rounded bg-surface px-2 py-1 font-mono text-xs">
                  {invite.link}
                </code>
                <Button type="button" variant="secondary" size="sm" onClick={() => void copyLink(invite.link)}>
                  {copied ? t('members.copied') : t('members.copyLink')}
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Tabella membri */}
      <Card>
        <CardHeader>
          <h2 className="font-sans text-lg font-extrabold tracking-tight text-fg">
            {t('members.membersHeading')}
          </h2>
        </CardHeader>
        <CardContent>
          <QueryState
            isLoading={members.isLoading}
            isError={members.isError}
            onRetry={() => void members.refetch()}
          >
            {memberList.length === 0 ? (
              <p className="text-sm text-fg-muted">{t('members.noMembers')}</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-[13px]">
                  <thead>
                    <tr>
                      <th scope="col" className="border-b border-line py-2.5 pr-4 text-[11px] font-bold uppercase tracking-[.05em] text-fg-faint">{t('members.colEmail')}</th>
                      <th scope="col" className="border-b border-line py-2.5 pr-4 text-[11px] font-bold uppercase tracking-[.05em] text-fg-faint">{t('members.colName')}</th>
                      <th scope="col" className="border-b border-line py-2.5 pr-4 text-[11px] font-bold uppercase tracking-[.05em] text-fg-faint">{t('members.colRole')}</th>
                      <th scope="col" className="border-b border-line py-2.5 pr-4 text-[11px] font-bold uppercase tracking-[.05em] text-fg-faint">{t('members.colStatus')}</th>
                      <th scope="col" className="border-b border-line py-2.5 pr-4 text-[11px] font-bold uppercase tracking-[.05em] text-fg-faint">{t('members.colActions')}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {memberList.map((u) => (
                      <tr key={u.id} className="border-b border-line last:border-b-0">
                        <td className="py-2 pr-4">{u.email}</td>
                        <td className="py-2 pr-4 text-fg-muted">{u.displayName ?? '—'}</td>
                        <td className="py-2 pr-4">
                          {u.role === 'owner' ? (
                            <span>{roleLabel(t, u.role)}</span>
                          ) : (
                            <select
                              aria-label={`${t('members.changeRole')}: ${u.email}`}
                              className="h-9 rounded-md border border-line bg-surface px-2 text-sm text-fg disabled:opacity-50"
                              value={u.role}
                              disabled={isSelf(u) || busy}
                              onChange={(e) => void onChangeRole(u, e.target.value as Role)}
                            >
                              <option value="member">{t('members.roleMember')}</option>
                              <option value="admin">{t('members.roleAdmin')}</option>
                            </select>
                          )}
                        </td>
                        <td className="py-2 pr-4">{statusBadge(t, u.status)}</td>
                        <td className="py-2 pr-4">
                          <div className="flex flex-wrap gap-2">
                            {u.status === 'suspended' ? (
                              <Button
                                type="button"
                                variant="secondary"
                                size="sm"
                                disabled={locked(u) || busy}
                                onClick={() => void onToggleStatus(u)}
                              >
                                {t('members.reactivate')}
                              </Button>
                            ) : (
                              <Button
                                type="button"
                                variant="secondary"
                                size="sm"
                                disabled={locked(u) || busy}
                                onClick={() => setConfirm({ kind: 'suspend', user: u })}
                              >
                                {t('members.suspend')}
                              </Button>
                            )}
                            <Button
                              type="button"
                              variant="danger"
                              size="sm"
                              disabled={locked(u) || busy}
                              onClick={() => setConfirm({ kind: 'remove', user: u })}
                            >
                              {t('members.remove')}
                            </Button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </QueryState>
        </CardContent>
      </Card>

      {/* Tabella inviti pendenti */}
      <Card>
        <CardHeader>
          <h2 className="font-sans text-lg font-extrabold tracking-tight text-fg">
            {t('members.invitesHeading')}
          </h2>
        </CardHeader>
        <CardContent>
          <QueryState
            isLoading={invitations.isLoading}
            isError={invitations.isError}
            onRetry={() => void invitations.refetch()}
          >
            {inviteList.length === 0 ? (
              <p className="text-sm text-fg-muted">{t('members.noInvites')}</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-[13px]">
                  <thead>
                    <tr>
                      <th scope="col" className="border-b border-line py-2.5 pr-4 text-[11px] font-bold uppercase tracking-[.05em] text-fg-faint">{t('members.colEmail')}</th>
                      <th scope="col" className="border-b border-line py-2.5 pr-4 text-[11px] font-bold uppercase tracking-[.05em] text-fg-faint">{t('members.colRole')}</th>
                      <th scope="col" className="border-b border-line py-2.5 pr-4 text-[11px] font-bold uppercase tracking-[.05em] text-fg-faint">{t('members.colExpires')}</th>
                      <th scope="col" className="border-b border-line py-2.5 pr-4 text-[11px] font-bold uppercase tracking-[.05em] text-fg-faint">{t('members.colActions')}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {inviteList.map((inv) => (
                      <tr key={inv.id} className="border-b border-line last:border-b-0">
                        <td className="py-2 pr-4">{inv.email}</td>
                        <td className="py-2 pr-4">{roleLabel(t, inv.role)}</td>
                        <td className="py-2 pr-4 text-fg-muted">
                          {inv.expiresAt ? new Date(inv.expiresAt).toLocaleDateString() : '—'}
                        </td>
                        <td className="py-2 pr-4">
                          <Button
                            type="button"
                            variant="danger"
                            size="sm"
                            disabled={busy}
                            onClick={() => setConfirm({ kind: 'revoke', invitation: inv })}
                          >
                            {t('members.revoke')}
                          </Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </QueryState>
        </CardContent>
      </Card>

      {confirm && (
        <ConfirmDialog
          title={t(
            confirm.kind === 'remove'
              ? 'members.confirmRemoveTitle'
              : confirm.kind === 'revoke'
                ? 'members.confirmRevokeTitle'
                : 'members.confirmSuspendTitle',
          )}
          body={
            confirm.kind === 'remove'
              ? t('members.confirmRemoveBody', { email: confirm.user.email })
              : confirm.kind === 'revoke'
                ? t('members.confirmRevokeBody', { email: confirm.invitation.email })
                : t('members.confirmSuspendBody')
          }
          confirmLabel={t(
            confirm.kind === 'remove'
              ? 'members.remove'
              : confirm.kind === 'revoke'
                ? 'members.revoke'
                : 'members.suspend',
          )}
          busy={busy}
          onConfirm={() => void onConfirm()}
          onCancel={() => setConfirm(null)}
        />
      )}
    </div>
  )
}
