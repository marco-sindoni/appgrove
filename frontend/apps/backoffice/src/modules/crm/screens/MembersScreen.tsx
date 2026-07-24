import { useState } from 'react'
import { ApiError } from '@appgrove/api-client'
import {
  Badge,
  Button,
  Icon,
  PageHeader,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeadCell,
  TableRow,
} from '@appgrove/design-system'
import { useShellContext } from '../../../registry/ShellContext'
import { ConfirmDialog } from '../../../pages/members/ConfirmDialog'
import { QueryState } from '../../../shell/QueryState'
import { useAssignSeat, useRevokeSeat, useSeats } from '../api/hooks'
import { ContactAvatar } from '../components/ContactAvatar'
import { t } from '../strings'

/**
 * Schermata «Membri»: chi, nel tuo account, ha un posto nel Mini-CRM. Assegnazione e revoca sono
 * riservate a owner/admin lato backend (403 per gli altri). A posti esauriti l'assegnazione riceve
 * 429 e mostra l'invito all'upgrade — enforcement vero è backend, il banner è solo esperienza d'uso.
 */
export function MembersScreen() {
  const shell = useShellContext()
  const seats = useSeats()
  const assign = useAssignSeat()
  const revoke = useRevokeSeat()
  const [userId, setUserId] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [quotaReached, setQuotaReached] = useState(false)
  const [toRevoke, setToRevoke] = useState<string | null>(null)

  const summary = seats.data
  const used = summary?.used ?? 0
  const limit = summary?.limit
  const unlimited = limit == null
  const reached = !unlimited && (summary?.remaining ?? 0) <= 0
  const rows = summary?.seats ?? []

  const onAssign = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!userId.trim()) return
    setError(null)
    setQuotaReached(false)
    try {
      await assign.mutateAsync(userId.trim())
      setUserId('')
    } catch (err) {
      if (err instanceof ApiError && err.status === 429) {
        setQuotaReached(true)
        setError(t.errorSeatsQuota)
      } else {
        setError(t.errorGeneric)
      }
    }
  }

  const onRevoke = async () => {
    if (!toRevoke) return
    setError(null)
    try {
      await revoke.mutateAsync(toRevoke)
    } catch {
      setError(t.errorGeneric)
    } finally {
      setToRevoke(null)
    }
  }

  return (
    <div className="space-y-[22px]">
      <PageHeader
        title={t.membersTitle}
        subtitle={t.membersSubtitle}
        icon="group"
        iconClassName="bg-cat-teal/15 text-cat-teal"
      />

      <div
        role="status"
        className="flex flex-wrap items-center gap-2.5 rounded-[15px] border border-line bg-surface px-4 py-3 text-[13px] shadow-sm"
      >
        <span aria-hidden className="h-2 w-2 rounded-pill bg-cat-teal" />
        <span className="font-semibold text-fg-muted">{t.seatsUsage}:</span>
        {unlimited ? (
          <Badge tone="neutral">{used}</Badge>
        ) : (
          <Badge tone={reached ? 'danger' : 'accent'}>
            {used} / {limit}
          </Badge>
        )}
      </div>

      <form onSubmit={onAssign} className="flex flex-wrap items-end gap-3">
        <div className="min-w-[18rem] flex-1">
          <label htmlFor="userId" className="mb-1 block text-sm font-medium text-fg">
            {t.fieldUserId}
          </label>
          <input
            id="userId"
            value={userId}
            onChange={(e) => setUserId(e.target.value)}
            className="h-10 w-full rounded-md border border-line bg-surface px-3 text-sm text-fg"
          />
        </div>
        <Button type="submit" disabled={assign.isPending || !userId.trim()}>
          <Icon name="add" size={18} />
          {t.assignSeat}
        </Button>
      </form>

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

      <QueryState
        isLoading={seats.isLoading}
        isError={seats.isError}
        onRetry={() => void seats.refetch()}
      >
        {rows.length === 0 ? (
          <div className="rounded-lg border border-line bg-surface px-6 py-12 text-center shadow-sm">
            <Icon name="group" size={42} className="text-fg-faint" />
            <p className="mt-3 text-[15px] font-bold text-fg">{t.membersEmpty}</p>
          </div>
        ) : (
          <Table>
            <TableHead>
              <TableRow>
                <TableHeadCell>{t.fieldUserId}</TableHeadCell>
                <TableHeadCell className="text-right">{t.colActions}</TableHeadCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((seat) => (
                <TableRow key={seat.id}>
                  <TableCell>
                    <span className="flex items-center gap-3">
                      <ContactAvatar name={seat.userId} />
                      <span className="font-mono text-[13px]">{seat.userId}</span>
                    </span>
                  </TableCell>
                  <TableCell className="text-right">
                    <Button
                      variant="danger"
                      size="sm"
                      disabled={revoke.isPending}
                      onClick={() => setToRevoke(seat.userId ?? null)}
                    >
                      {t.revokeSeat}
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </QueryState>

      {toRevoke && (
        <ConfirmDialog
          title={t.confirmRevokeTitle}
          body={t.confirmRevokeBody}
          confirmLabel={t.revokeSeat}
          busy={revoke.isPending}
          onConfirm={() => void onRevoke()}
          onCancel={() => setToRevoke(null)}
        />
      )}
    </div>
  )
}
