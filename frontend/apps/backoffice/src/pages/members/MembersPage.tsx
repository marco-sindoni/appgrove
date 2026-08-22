import { useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { ApiError, type SeatReductionPreview, type UserAppView } from '@appgrove/api-client'
import { Badge, Button, Card, CardContent, CardHeader, Checkbox } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useConfig } from '../../config'
import { sendInvitation } from '../../auth/authApi'
import { inviteSchema, type TFn } from '../../auth/schemas'
import {
  useCancelSeatReduction,
  useCreateInvitation,
  useCurrentUser,
  useInvitations,
  useMembers,
  useRemoveFromSeatReduction,
  useRemoveMember,
  useRequestSeatReduction,
  useRevokeInvitation,
  useSeatReductionPreview,
  useSeats,
  useUpdateMember,
} from '../../api/hooks'
import { formatPrice } from '../../billing/checkoutMachine'
import { QueryState } from '../../shell/QueryState'
import { Field } from '../auth/Field'
import { ConfirmDialog } from './ConfirmDialog'
import { SeatsCard } from './SeatsCard'
import { readSeats, type Seats } from './seats'
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

/**
 * Lo stato di una riga, come etichette. **Possono essere due**: la cessazione programmata e la
 * sospensione sono ortogonali (una riguarda il posto, l'altra l'accesso — UC 0104 §5), e una persona può
 * essere entrambe. Mostrarne una sola avrebbe fatto sparire l'altra dalla schermata.
 *
 * L'etichetta della cessazione porta **la data** e ha tono attenuato: la persona sta lavorando
 * normalmente, e un rosso allarmante accanto al nome di un collega sarebbe fuori misura.
 */
const statusBadges = (t: TFn, row: RosterRow, locale: string) => {
  if (row.status === 'invited') {
    return [<Badge key="invited" tone="info">{t('members.statusInvited')}</Badge>]
  }
  const badges = []
  if (row.endingAt) {
    badges.push(
      <Badge key="ending" tone="neutral">
        {t('members.statusEnding', { date: formatDate(row.endingAt, locale) })}
      </Badge>,
    )
  }
  if (row.suspended) {
    badges.push(<Badge key="suspended" tone="warning">{t('members.statusSuspended')}</Badge>)
  }
  if (badges.length === 0) {
    badges.push(<Badge key="active" tone="success">{t('members.statusActive')}</Badge>)
  }
  return badges
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
function inviteErrorMessage(err: unknown, t: TFn, blockedUntil?: string): string {
  // UC 0103 — l'addebito del posto è stato rifiutato: l'invito NON è stato creato. Il motivo lo dà il
  // fornitore di pagamento e viaggia nel corpo del rifiuto: si mostra così com'è, perché è l'unica
  // informazione con cui chi ha invitato può rimediare («carta scaduta» si rimedia in due minuti,
  // «operazione non riuscita» no). Il testo attorno è nostro e tradotto; il motivo no, ed è giusto: è
  // il fornitore a saperlo, e tradurlo a orecchio significherebbe inventarlo.
  if (err instanceof ApiError && err.status === 402) {
    const reason = err.problem?.detail
    return reason
      ? t('seats.chargeDeclinedWithReason', { reason })
      : t('seats.chargeDeclined')
  }
  if (err instanceof ApiError && err.status === 409) {
    switch (err.problem?.type) {
      // UC 0104 — c'è una riduzione programmata. Il testo offre le DUE vie d'uscita, ed è la parte che
      // conta: un rifiuto senza uscita è un vicolo cieco. La data arriva dal riquadro, non dal messaggio
      // del server: qui si compone il testo tradotto con il dato che l'interfaccia ha già.
      case 'urn:appgrove:seats:reduction-pending':
        return t('seats.reductionInviteBlocked', { date: blockedUntil ?? '' })
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

/**
 * I rifiuti **leciti** della riduzione dei posti (UC 0104 §5), riconosciuti dall'identificativo stabile e
 * non dal messaggio del server — che è in italiano, mentre questa interfaccia parla cinque lingue.
 *
 * Ognuno dice anche *che cosa fare invece*, e non è un ornamento: «non stai pagando alcun posto» senza
 * «rimuovila dall'elenco, è gratuito» lascia l'owner davanti a una porta chiusa senza dirgli che accanto
 * ce n'è una aperta.
 */
function reductionErrorMessage(err: unknown, t: TFn): string {
  if (err instanceof ApiError && err.status === 409) {
    switch (err.problem?.type) {
      case 'urn:appgrove:seats:reduction-owner':
        return t('seats.markErrorOwner')
      case 'urn:appgrove:seats:reduction-already-pending':
        return t('seats.markErrorAlreadyPending')
      case 'urn:appgrove:seats:reduction-not-needed':
        return t('seats.markErrorNotNeeded')
      default:
        return t('seats.markError')
    }
  }
  return t('errors.generic')
}

/**
 * La frase di stima mostrata **prima** della conferma dell'invito (UC 0103 §4): «questa persona sarà il
 * posto numero 4; costo 2,99 € al mese; il tuo totale passerà da 0,00 a 2,99 €».
 *
 * Tre casi, e sono tre frasi diverse perché dicono tre cose diverse:
 *
 * - il posto è **compreso** nella franchigia → non si parla di soldi, si dice quanti posti restano compresi;
 * - il posto è **già pagato** in questo periodo (qualcuno non ha accettato e viene rimpiazzato) → questo
 *   invito non produce alcun addebito, e va detto: è la contropartita del «nessun rimborso»;
 * - il posto **costa** → si dice quanto, e come cambia il totale.
 *
 * Nessuna aritmetica: importi, numero d'ordine del posto e nuovo totale arrivano dal servizio.
 */
function seatEstimate(t: TFn, language: string, seats: Seats | null): string | null {
  if (!seats) return null
  const { next, currency } = seats
  if (next.unitPriceCents === 0) {
    return t('seats.estimateIncluded', { seatNumber: next.seatNumber, count: seats.freeSeats })
  }
  if (next.chargeCents === 0) {
    return t('seats.estimateAlreadyPaid', { seatNumber: next.seatNumber })
  }
  return t('seats.estimate', {
    seatNumber: next.seatNumber,
    price: formatPrice(next.unitPriceCents, currency, language),
    from: formatPrice(seats.dueCents, currency, language),
    to: formatPrice(next.dueCentsAfter, currency, language),
  })
}

/**
 * La frase dell'**effetto della cessazione programmata** (UC 0104 §4.2): «cesseranno il 14 settembre; dal
 * 15 pagherai 17,94 € invece di 24,91 €».
 *
 * Nessuna aritmetica: la data, i posti risultanti e i due importi arrivano dal servizio. Quando la data di
 * esecuzione manca — l'account non paga alcun posto, quindi non c'è un periodo a cui agganciarsi — non si
 * inventa: si dice che non c'è nulla da ridurre e si indica la via giusta, che è la rimozione immediata.
 */
function markEstimate(t: TFn, language: string, preview: SeatReductionPreview): string {
  if (!preview.executeAt) {
    return t('seats.markErrorNotNeeded')
  }
  return t('seats.markConfirmBody', {
    date: formatDate(preview.executeAt, language),
    from: formatPrice(preview.dueCentsNow ?? 0, preview.currency ?? 'EUR', language),
    to: formatPrice(preview.dueCentsAfter ?? 0, preview.currency ?? 'EUR', language),
  })
}

/** La chiave del titolo del dialogo di conferma, per ciascuno dei quattro atti che ne hanno bisogno. */
function confirmTitleKey(confirm: Confirm) {
  switch (confirm.kind) {
    case 'remove':
      return 'members.confirmRemoveTitle' as const
    case 'revoke':
      return 'members.confirmRevokeTitle' as const
    case 'mark':
      return 'seats.markConfirmTitle' as const
    case 'cancelReduction':
      return 'seats.reductionCancelTitle' as const
    default:
      return 'members.confirmSuspendTitle' as const
  }
}

/** La chiave dell'etichetta del pulsante che conferma. */
function confirmActionKey(confirm: Confirm) {
  switch (confirm.kind) {
    case 'remove':
      return 'members.remove' as const
    case 'revoke':
      return 'members.revoke' as const
    case 'mark':
      return 'seats.markSubmit' as const
    case 'cancelReduction':
      return 'seats.reductionCancel' as const
    default:
      return 'members.suspend' as const
  }
}

/**
 * Il corpo del dialogo. Per la cessazione programmata **è la stima**: si conferma leggendo l'effetto, non
 * una domanda generica — «vuoi procedere?» davanti a un cambio di importo non è una conferma informata.
 */
function confirmBody(
  confirm: Confirm,
  t: TFn,
  language: string,
  preview: SeatReductionPreview | undefined,
): string {
  switch (confirm.kind) {
    case 'remove':
      return t('members.confirmRemoveBody', { email: confirm.row.email })
    case 'revoke':
      return t('members.confirmRevokeBody', { email: confirm.row.email })
    case 'mark':
      return preview ? markEstimate(t, language, preview) : t('seats.markPreviewLoading')
    case 'cancelReduction':
      return t('seats.reductionCancelBody')
    default:
      return t('members.confirmSuspendBody')
  }
}

interface InviteSuccess {
  email: string
  link: string
  emailed: boolean
}

type Confirm =
  | { kind: 'suspend' | 'remove'; row: RosterRow }
  | { kind: 'revoke'; row: RosterRow }
  /** Conferma della cessazione programmata: il corpo del dialogo è l'**effetto** (UC 0104 §4.2). */
  | { kind: 'mark' }
  /** Conferma dell'annullamento dell'attesa. */
  | { kind: 'cancelReduction' }

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
  const seats = useSeats()
  const createInvitation = useCreateInvitation()
  const revokeInvitation = useRevokeInvitation()
  const updateMember = useUpdateMember()
  const removeMember = useRemoveMember()
  const requestReduction = useRequestSeatReduction()
  const cancelReduction = useCancelSeatReduction()
  const keepPerson = useRemoveFromSeatReduction()

  const [invite, setInvite] = useState<InviteSuccess | null>(null)
  const [inviteError, setInviteError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [confirm, setConfirm] = useState<Confirm | null>(null)
  const [copied, setCopied] = useState(false)
  /** Righe con il dettaglio delle applicazioni aperto (chiave della riga). */
  const [expanded, setExpanded] = useState<string[]>([])
  /**
   * Le persone selezionate per la cessazione (identificativi). Vive qui e non nelle righe perché la
   * selezione è **una scelta in corso**, non uno stato del gruppo di lavoro: si perde ricaricando, e va
   * bene così.
   */
  const [selected, setSelected] = useState<string[]>([])

  // La stima dell'effetto: dipende dalla selezione, quindi è l'unica lettura della sezione che cambia
  // a ogni casella spuntata. È una lettura e non un atto — chiederla non programma nulla.
  const preview = useSeatReductionPreview(selected)

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
      setInviteError(inviteErrorMessage(err, t, blockedUntil))
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
      } else if (confirm.kind === 'mark') {
        await requestReduction.mutateAsync(selected)
        setSelected([])
      } else if (confirm.kind === 'cancelReduction') {
        await cancelReduction.mutateAsync()
      } else {
        await onToggleStatus(confirm.row)
      }
      setConfirm(null)
    } catch (err) {
      setActionError(reductionErrorMessage(err, t))
      setConfirm(null)
    }
  }

  /** Toglie una singola persona dall'elenco degli indicati. Senza conferma: è l'atto che *salva* qualcuno. */
  const onKeepPerson = async (userId: string) => {
    setActionError(null)
    try {
      await keepPerson.mutateAsync(userId)
    } catch (err) {
      setActionError(reductionErrorMessage(err, t))
    }
  }

  const toggleSelected = (id: string) =>
    setSelected((ids) => (ids.includes(id) ? ids.filter((x) => x !== id) : [...ids, id]))

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

  /**
   * L'invito è **impedito** quando il costo del posto non è noto (lettura in corso o in errore) o quando
   * c'è una riduzione in attesa (UC 0104). Non è una cortesia dell'interfaccia come il gating dei ruoli:
   * è il presidio contro l'invito alla cieca, e il servizio non può farlo da sé perché il servizio non sa
   * se chi invita ha letto il costo.
   */
  const seatSummary = readSeats(seats.data)
  const inviteBlocked = seats.isLoading || seats.isError || !seatSummary || seatSummary.pendingReduction

  const inviteEstimate = seatEstimate(t, i18n.language, seatSummary)

  /** La data fino alla quale gli inviti sono impediti, già formattata: entra nel testo del divieto. */
  const blockedUntil = seatSummary?.reduction
    ? formatDate(seatSummary.reduction.executeAt, i18n.language)
    : undefined

  const busy =
    createInvitation.isPending ||
    revokeInvitation.isPending ||
    updateMember.isPending ||
    removeMember.isPending ||
    requestReduction.isPending ||
    cancelReduction.isPending ||
    keepPerson.isPending

  /**
   * La selezione si può indicare quando ci sono persone scelte, non c'è già un'attesa in corso, e la
   * **stima è arrivata**: si conferma una cessazione solo dopo aver visto che effetto ha, come per
   * l'invito non si conferma senza aver visto il costo.
   */
  const markBlocked =
    selected.length === 0 || !!seatSummary?.pendingReduction || !preview.data || preview.isError

  const th =
    'border-b border-line py-2.5 pr-4 text-[11px] font-bold uppercase tracking-[.05em] text-fg-faint'

  return (
    <div className="space-y-[22px]">
      <div>
        <h1 className="text-[27px] font-extrabold tracking-[-0.025em] text-fg">{t('members.title')}</h1>
        <p className="mt-1 text-sm text-fg-muted">{t('members.subtitle')}</p>
      </div>

      {/* Riquadro dei posti (UC 0103): posti usati e composizione, importo, costo del prossimo posto,
          riduzione in attesa. Sta fra l'intestazione e l'invito perché è l'informazione che si legge
          PRIMA di decidere di invitare qualcuno. */}
      <SeatsCard
        seats={seatSummary}
        isLoading={seats.isLoading}
        // Una risposta che non si capisce vale quanto una lettura fallita: in entrambi i casi non
        // sappiamo quanto costa il posto, e in entrambi i casi non si invita.
        isError={seats.isError || (!seats.isLoading && !seatSummary)}
        onRetry={() => void seats.refetch()}
        onCancelReduction={() => setConfirm({ kind: 'cancelReduction' })}
        onKeepPerson={(userId) => void onKeepPerson(userId)}
        busy={busy}
      />

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
            <Button
              type="submit"
              className="mt-6"
              disabled={form.formState.isSubmitting || inviteBlocked}
              title={inviteBlocked ? t('seats.inviteBlockedHint') : undefined}
            >
              {t('members.inviteSubmit')}
            </Button>
          </form>

          {/* LA STIMA, PRIMA DELLA CONFERMA (UC 0103 §4). Finché il costo non è noto il pulsante è
              spento: mai invitare alla cieca. È il presidio contro la sorpresa in fattura, e vale anche
              quando la lettura è solo lenta — un secondo di attesa costa meno di un addebito inatteso. */}
          {inviteEstimate && (
            <p role="status" className="mt-3 text-[12.5px] font-semibold text-fg">
              {inviteEstimate}
            </p>
          )}

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
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h2 className="font-sans text-lg font-extrabold tracking-tight text-fg">
              {t('members.rosterHeading')}
            </h2>
            {/* Il comando della cessazione programmata (UC 0104): compare solo con qualcuno selezionato,
                perché un pulsante «indica per la cessazione» sempre presente e sempre spento non
                insegna niente a chi non ha capito che prima si scelgono le persone. */}
            {selected.length > 0 && (
              <div className="flex flex-wrap items-center gap-2">
                <span className="text-[12.5px] text-fg-muted">
                  {t('seats.markSelected', { count: selected.length })}
                </span>
                <Button
                  type="button"
                  variant="danger"
                  size="sm"
                  disabled={busy || markBlocked}
                  onClick={() => setConfirm({ kind: 'mark' })}
                >
                  {t('seats.markSubmit')}
                </Button>
              </div>
            )}
          </div>
          {/* La stima appare accanto alla selezione, PRIMA di aprire la conferma: chi sta per far
              cessare tre persone deve vedere l'effetto senza dover cliccare un pulsante distruttivo
              per scoprirlo. */}
          {selected.length > 0 && (
            <p role="status" className="mt-2 text-[12.5px] text-fg">
              {preview.isLoading
                ? t('seats.markPreviewLoading')
                : preview.isError || !preview.data
                  ? t('seats.markPreviewError')
                  : markEstimate(t, i18n.language, preview.data)}
            </p>
          )}
          {/* La combinazione utile a chi vuole escludere qualcuno SUBITO: è la domanda che l'owner si
              farà, e la risposta non è «indica per la cessazione» (che aspetta la fine del mese). */}
          {selected.length > 0 && (
            <p className="mt-1.5 text-[12px] text-fg-muted">{t('seats.markHintAppAccess')}</p>
          )}
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
                      {/* Colonna di selezione: senza intestazione visibile ma con un nome per chi
                          naviga con lettore di schermo. Un'intestazione vuota è un'omissione; una
                          nascosta è una scelta. */}
                      <th scope="col" className={`${th} w-8`}>
                        <span className="sr-only">{t('seats.markSubmit')}</span>
                      </th>
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
                          <td className="py-2 pr-2">
                            {row.selectable && (
                              <Checkbox
                                checked={selected.includes(row.id)}
                                disabled={busy || !!seatSummary?.pendingReduction}
                                aria-label={t('seats.selectLabel', { email: row.email })}
                                onChange={() => toggleSelected(row.id)}
                              />
                            )}
                          </td>
                          <td className="py-2 pr-4">{row.email}</td>
                          <td className="py-2 pr-4 text-fg-muted">{row.displayName ?? '—'}</td>
                          <td className="py-2 pr-4">
                            <div className="flex flex-col gap-0.5">
                              {statusBadges(t, row, i18n.language)}
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
                            <td colSpan={7} className="px-1 py-2.5">
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
          title={t(confirmTitleKey(confirm))}
          body={confirmBody(confirm, t, i18n.language, preview.data)}
          confirmLabel={t(confirmActionKey(confirm))}
          // L'annullamento della riduzione non è un atto distruttivo: rimette tutti al loro posto. Un
          // pulsante rosso su un'azione che non toglie niente a nessuno insegna la paura sbagliata.
          tone={confirm.kind === 'cancelReduction' ? 'default' : 'danger'}
          busy={busy}
          onConfirm={() => void onConfirm()}
          onCancel={() => setConfirm(null)}
        />
      )}
    </div>
  )
}
