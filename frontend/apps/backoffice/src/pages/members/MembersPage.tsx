import { useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { ApiError, type UserAppView } from '@appgrove/api-client'
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
import { buildRoster, type RosterRow } from './roster'

/**
 * Sezione «Members» come **registro delle persone** dell'account (UC 0100).
 *
 * Che cosa è cambiato rispetto a UC 0059, e perché — perché la prima reazione di chi conosceva la
 * schermata di prima sarà «manca qualcosa»:
 *
 * - **una** tabella invece di due. Le persone e gli inviti in attesa erano due elenchi della stessa
 *   cosa, e chi guardava doveva sommare a mente per sapere quante persone aveva. Chi ha un invito in
 *   attesa è una persona dell'account che sta arrivando, e occupa già un posto;
 * - **nessun ruolo**. Il ruolo di piattaforma ha due soli valori (UC 0098) e non dice che cosa una
 *   persona possa *fare*: lo dice il ruolo su ciascuna applicazione. Una colonna che ripete «Membro»
 *   su tutte le righe tranne una non è informazione, è arredamento;
 * - la colonna **applicazioni**, che è l'informazione che quella colonna nascondeva: su quante e quali
 *   applicazioni ciascuno è abilitato, in **sola lettura**.
 *
 * L'enforcement vero è nel core (`@RolesAllowed(owner)` su persone e inviti); il gating qui è cortesia.
 */

const statusBadge = (t: TFn, row: RosterRow) => {
  if (row.status === 'invited') {
    return <Badge tone="info">{t('members.statusInvited')}</Badge>
  }
  if (row.status === 'suspended') {
    return <Badge tone="warning">{t('members.statusSuspended')}</Badge>
  }
  return <Badge tone="success">{t('members.statusActive')}</Badge>
}

const formatDate = (iso: string | undefined, locale: string) =>
  iso ? new Date(iso).toLocaleDateString(locale) : '—'

/**
 * Le due collisioni **lecite** dell'invito (UC 0118 §5) hanno due messaggi distinti, e la causa la
 * dice il campo `type` del corpo problem+json — non il testo del server, che è in italiano mentre
 * questa interfaccia parla cinque lingue.
 *
 * Ciò che **non** esiste, e non deve esistere, è un messaggio per «questa persona ha già un account
 * appgrove»: sarebbe comodo per chi invita e rivelerebbe a un'azienda l'esistenza di un rapporto fra
 * quella persona e la piattaforma, che non le appartiene. Il server risponde `201` in quel caso,
 * esattamente come per un indirizzo sconosciuto: qui non c'è nulla da distinguere, ed è voluto. La
 * tentazione tornerà a ogni revisione di questa pagina.
 */
function inviteErrorMessage(err: unknown, t: TFn): string {
  if (err instanceof ApiError && err.status === 409) {
    switch (err.problem?.type) {
      case 'urn:appgrove:invitation:already-member':
        return t('members.emailAlreadyMemberOnly')
      case 'urn:appgrove:invitation:already-invited':
        return t('members.emailAlreadyInvited')
      default:
        // Rifiuto 409 senza identificativo (versione precedente del servizio): il messaggio storico,
        // che copre entrambi i casi senza mentire.
        return t('members.emailAlreadyMember')
    }
  }
  return t('errors.generic')
}

interface InviteSuccess {
  email: string
  link: string
  emailed: boolean
}

type Confirm =
  | { kind: 'suspend' | 'remove'; row: RosterRow }
  | { kind: 'revoke'; row: RosterRow }

/**
 * Il dettaglio delle applicazioni di una persona, in **sola lettura**.
 *
 * Non è un collegamento: la schermata dove si cambia il ruolo su una applicazione è di UC 0111 e non
 * esiste ancora, e un collegamento verso il nulla è peggio della sua assenza. La frase dice comunque
 * *dove* si cambia, così l'assenza del comando non si legge come una dimenticanza.
 */
function AppsDetail({ row, t }: { row: RosterRow; t: TFn }) {
  if (row.apps.length === 0) {
    return <p className="text-[12.5px] text-fg-muted">{t('members.appsNoneHint')}</p>
  }
  const roleLabel = (a: UserAppView) =>
    a.implicit || !a.role ? t('members.appsImplicit') : t(`roles.${a.role}` as 'roles.viewer')
  return (
    <div className="space-y-1.5">
      <ul className="flex flex-wrap gap-2">
        {row.apps.map((a) => (
          <li key={a.appId ?? a.app} className="flex items-center gap-1.5 text-[12.5px]">
            <span className="font-semibold text-fg">{a.app}</span>
            <Badge tone="neutral">{roleLabel(a)}</Badge>
          </li>
        ))}
      </ul>
      <p className="text-[12px] text-fg-muted">{t('members.appsManagedInApp')}</p>
    </div>
  )
}

export function MembersPage() {
  const { t, i18n } = useTranslation()
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
  /** Righe con il dettaglio delle applicazioni aperto (chiave della riga). */
  const [expanded, setExpanded] = useState<string[]>([])

  const roster = useMemo(
    () =>
      buildRoster({
        members: members.data?.content,
        invitations: invitations.data?.content,
        meId: me.data?.id as string | undefined,
      }),
    [members.data, invitations.data, me.data],
  )

  const form = useForm<z.infer<ReturnType<typeof inviteSchema>>>({
    resolver: zodResolver(inviteSchema(t)),
    defaultValues: { email: '' },
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
        // Lingua di chi invita (UC 0018): l'invitato non è ancora un utente, non ha una preferenza
        // da leggere. È l'approssimazione migliore disponibile al momento dell'invio.
        await sendInvitation(config.authBaseUrl, {
          email: values.email,
          token,
          locale: i18n.language,
        })
      } catch {
        emailed = false
      }
      setInvite({ email: values.email, link, emailed })
      form.reset({ email: '' })
    } catch (err) {
      setInviteError(inviteErrorMessage(err, t))
    }
  })

  const onToggleStatus = async (row: RosterRow) => {
    setActionError(null)
    try {
      await updateMember.mutateAsync({
        id: row.id,
        status: row.status === 'suspended' ? 'active' : 'suspended',
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
        await revokeInvitation.mutateAsync(confirm.row.id)
      } else if (confirm.kind === 'remove') {
        await removeMember.mutateAsync(confirm.row.id)
      } else {
        await onToggleStatus(confirm.row)
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

  const toggleExpanded = (key: string) =>
    setExpanded((keys) => (keys.includes(key) ? keys.filter((k) => k !== key) : [...keys, key]))

  const busy =
    createInvitation.isPending ||
    revokeInvitation.isPending ||
    updateMember.isPending ||
    removeMember.isPending

  const th =
    'border-b border-line py-2.5 pr-4 text-[11px] font-bold uppercase tracking-[.05em] text-fg-faint'

  return (
    <div className="space-y-[22px]">
      <div>
        <h1 className="text-[27px] font-extrabold tracking-[-0.025em] text-fg">{t('members.title')}</h1>
        <p className="mt-1 text-sm text-fg-muted">{t('members.subtitle')}</p>
      </div>

      {/* Riquadro dei posti (posti usati su totali, costo del posto successivo, riduzione in attesa):
          arriva con UC 0103 e va QUI, fra l'intestazione e l'elenco. Il posto è lasciato apposta, per
          non rimaneggiare la struttura una seconda volta. */}

      {actionError && (
        <p role="alert" className="text-sm text-danger">
          {actionError}
        </p>
      )}

      {/* Invito: solo l'indirizzo. Nessun selettore di ruolo, e la riga sotto spiega perché. */}
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
            <Button type="submit" className="mt-6" disabled={form.formState.isSubmitting}>
              {t('members.inviteSubmit')}
            </Button>
          </form>

          {/* La differenza più visibile rispetto a prima è una SPARIZIONE: il selettore del ruolo. Una
              sparizione non spiegata si legge come un difetto, quindi si spiega. */}
          <p className="mt-3 text-[12.5px] text-fg-muted">{t('members.noRoleHint')}</p>

          {/* Il posto è dell'ACCOUNT, non della persona (UC 0118 §7): la stessa persona in due
              account occupa un posto in ciascuno, perché ogni account paga le persone che usano le
              *sue* applicazioni. Va scritto qui perché la prima reazione sarà «ma la paga già
              l'altra azienda» — e una regola che il cliente scopre in fattura è una regola sbagliata. */}
          <p className="mt-1.5 text-[12.5px] text-fg-muted">{t('members.seatNote')}</p>

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

      {/* Elenco UNICO: persone e inviti in attesa nella stessa tabella, l'owner in testa. */}
      <Card>
        <CardHeader>
          <h2 className="font-sans text-lg font-extrabold tracking-tight text-fg">
            {t('members.rosterHeading')}
          </h2>
        </CardHeader>
        <CardContent>
          <QueryState
            isLoading={members.isLoading || invitations.isLoading}
            isError={members.isError || invitations.isError}
            onRetry={() => {
              void members.refetch()
              void invitations.refetch()
            }}
          >
            {roster.length === 0 ? (
              <p className="text-sm text-fg-muted">{t('members.noMembers')}</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-[13px]">
                  <thead>
                    <tr>
                      <th scope="col" className={th}>{t('members.colEmail')}</th>
                      <th scope="col" className={th}>{t('members.colName')}</th>
                      <th scope="col" className={th}>{t('members.colStatus')}</th>
                      <th scope="col" className={th}>{t('members.colApps')}</th>
                      <th scope="col" className={th}>{t('members.colJoined')}</th>
                      <th scope="col" className={th}>{t('members.colActions')}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {roster.map((row) => {
                      const open = expanded.includes(row.key)
                      return [
                        <tr key={row.key} className="border-b border-line last:border-b-0">
                          <td className="py-2 pr-4">{row.email}</td>
                          <td className="py-2 pr-4 text-fg-muted">{row.displayName ?? '—'}</td>
                          <td className="py-2 pr-4">
                            <div className="flex flex-col gap-0.5">
                              {statusBadge(t, row)}
                              {row.status === 'invited' && (
                                <span className="text-[11.5px] text-fg-faint">
                                  {t('members.inviteExpiresOn', {
                                    date: formatDate(row.expiresAt, i18n.language),
                                  })}
                                </span>
                              )}
                            </div>
                          </td>
                          <td className="py-2 pr-4">
                            {row.kind === 'invitation' ? (
                              <span className="text-fg-faint">—</span>
                            ) : (
                              <Button
                                type="button"
                                variant="ghost"
                                size="sm"
                                aria-expanded={open}
                                aria-label={t('members.appsDetailLabel', { email: row.email })}
                                onClick={() => toggleExpanded(row.key)}
                              >
                                {row.apps.length === 0
                                  ? t('members.appsNone')
                                  : t('members.appsCount', { count: row.apps.length })}
                              </Button>
                            )}
                          </td>
                          <td className="py-2 pr-4 text-fg-muted">
                            {row.kind === 'invitation' ? '—' : formatDate(row.joinedAt, i18n.language)}
                          </td>
                          <td className="py-2 pr-4">
                            <div
                              className="flex flex-wrap gap-2"
                              title={
                                row.isSelf
                                  ? t('members.selfHint')
                                  : row.locked
                                    ? t('members.lastOwnerHint')
                                    : undefined
                              }
                            >
                              {row.kind === 'invitation' ? (
                                <Button
                                  type="button"
                                  variant="danger"
                                  size="sm"
                                  disabled={busy}
                                  onClick={() => setConfirm({ kind: 'revoke', row })}
                                >
                                  {t('members.revoke')}
                                </Button>
                              ) : (
                                <>
                                  {row.status === 'suspended' ? (
                                    <Button
                                      type="button"
                                      variant="secondary"
                                      size="sm"
                                      disabled={row.locked || busy}
                                      onClick={() => void onToggleStatus(row)}
                                    >
                                      {t('members.reactivate')}
                                    </Button>
                                  ) : (
                                    <Button
                                      type="button"
                                      variant="secondary"
                                      size="sm"
                                      disabled={row.locked || busy}
                                      onClick={() => setConfirm({ kind: 'suspend', row })}
                                    >
                                      {t('members.suspend')}
                                    </Button>
                                  )}
                                  <Button
                                    type="button"
                                    variant="danger"
                                    size="sm"
                                    disabled={row.locked || busy}
                                    onClick={() => setConfirm({ kind: 'remove', row })}
                                  >
                                    {t('members.remove')}
                                  </Button>
                                </>
                              )}
                            </div>
                          </td>
                        </tr>,
                        open ? (
                          <tr key={`${row.key}:apps`} className="border-b border-line bg-surface-2">
                            <td colSpan={6} className="px-1 py-2.5">
                              <AppsDetail row={row} t={t} />
                            </td>
                          </tr>
                        ) : null,
                      ]
                    })}
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
              ? t('members.confirmRemoveBody', { email: confirm.row.email })
              : confirm.kind === 'revoke'
                ? t('members.confirmRevokeBody', { email: confirm.row.email })
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
