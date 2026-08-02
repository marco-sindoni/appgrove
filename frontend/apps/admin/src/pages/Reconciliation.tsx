import {
  Badge,
  Card,
  Icon,
  PageHeader,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeadCell,
  TableRow,
} from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useReconciliation } from '../api/hooks'
import { QueryState } from '../shell/QueryState'

/**
 * Importo in unità minori → testo leggibile. La formattazione sta qui e non nel backend perché la valuta
 * e la lingua le conosce solo chi sta guardando la pagina.
 */
function money(minor: number | undefined, currency: string | null | undefined) {
  const value = (minor ?? 0) / 100
  try {
    return value.toLocaleString(undefined, {
      style: 'currency',
      currency: currency || 'EUR',
      minimumFractionDigits: 2,
    })
  } catch {
    // valuta sconosciuta: meglio un numero nudo che una pagina rotta
    return value.toFixed(2)
  }
}

const fmtDate = (iso?: string | null) => (iso ? new Date(iso).toLocaleDateString() : '—')

/** Esito della quadratura di un accredito → tono del contrassegno. */
function payoutTone(status?: string) {
  if (status === 'matched') return 'success' as const
  if (status === 'mismatch') return 'danger' as const
  return 'warning' as const
}

/* Riquadro di totale: stessa forma dei KPI dell'Overview, con l'importo al posto del conteggio. */
function Total({
  label,
  value,
  icon,
  tint,
}: {
  label: string
  value: string
  icon: string
  tint: string
}) {
  return (
    <Card className="p-[18px]">
      <span
        aria-hidden
        className={`mb-3.5 flex h-[34px] w-[34px] items-center justify-center rounded-[10px] ${tint}`}
      >
        <Icon name={icon} size={19} filled />
      </span>
      <p className="font-mono text-[23px] font-extrabold tracking-[-0.02em] text-fg">{value}</p>
      <h2 className="mt-0.5 text-[12.5px] font-semibold text-fg-muted">{label}</h2>
    </Card>
  )
}

/**
 * Riconciliazione (UC 0071): lordo → commissioni → netto → accredito.
 *
 * <p>Risponde a una domanda che nessun'altra pagina risponde: quanto è <b>davvero entrato</b>. Il
 * fornitore di pagamento incassa dal cliente, trattiene le proprie commissioni e ci accredita il netto
 * con accrediti periodici — quindi il fatturato lordo e il denaro sul conto non coincidono, e la
 * differenza non è una percentuale fissa.
 */
export function Reconciliation() {
  const { t } = useTranslation()
  const q = useReconciliation()
  const view = q.data
  const currency = view?.currency
  const totals = view?.totals
  const periods = view?.periods ?? []
  const payouts = view?.payouts ?? []
  const estimated = totals?.estimatedFeeTransactions ?? 0
  // Etichette della quadratura enumerate a mano: la chiave composta a runtime non è verificabile dai
  // tipi, e una dicitura mancante in una lingua resterebbe invisibile fino a quando qualcuno la vede.
  const matchLabel: Record<string, string> = {
    matched: t('admin.reconciliation.match.matched'),
    mismatch: t('admin.reconciliation.match.mismatch'),
    mixed_currency: t('admin.reconciliation.match.mixed_currency'),
  }
  const feeOverThreshold = periods.some((p) => p.feeOverThreshold)

  return (
    <div className="space-y-[22px]">
      <PageHeader
        title={t('admin.reconciliation.title')}
        subtitle={t('admin.reconciliation.subtitle')}
      />
      <QueryState
        isLoading={q.isLoading}
        isError={q.isError}
        isEmpty={periods.length === 0 && payouts.length === 0}
        emptyLabel={t('admin.reconciliation.empty')}
        onRetry={() => void q.refetch()}
      >
        <div className="space-y-[22px]">
          <div className="grid grid-cols-1 gap-3.5 sm:grid-cols-2 lg:grid-cols-4">
            <Total
              label={t('admin.reconciliation.gross')}
              value={money(totals?.gross, currency)}
              icon="payments"
              tint="bg-cat-blue/15 text-cat-blue"
            />
            <Total
              label={t('admin.reconciliation.fee')}
              value={money(totals?.fee, currency)}
              icon="percent"
              tint="bg-cat-amber/15 text-cat-amber"
            />
            <Total
              label={t('admin.reconciliation.net')}
              value={money(totals?.net, currency)}
              icon="account_balance"
              tint="bg-cat-teal/15 text-cat-teal"
            />
            <Total
              label={t('admin.reconciliation.unsettled')}
              value={money(totals?.unsettled, currency)}
              icon="hourglass_top"
              tint="bg-cat-violet/15 text-cat-violet"
            />
          </div>

          {view?.payoutOverdue && (
            <Card className="border-danger/40 bg-danger/5 p-[18px]" role="alert">
              <p className="text-[13px] font-semibold text-fg">
                {t('admin.reconciliation.overdueTitle')}
              </p>
              <p className="mt-1 text-[12.5px] text-fg-muted">
                {t('admin.reconciliation.overdueBody', {
                  days: view.payoutMaxAgeDays ?? 0,
                  since: fmtDate(totals?.oldestUnsettledAt),
                })}
              </p>
            </Card>
          )}

          {feeOverThreshold && (
            <Card className="border-warning/40 bg-warning/5 p-[18px]" role="status">
              <p className="text-[13px] font-semibold text-fg">
                {t('admin.reconciliation.feeAlertTitle')}
              </p>
              <p className="mt-1 text-[12.5px] text-fg-muted">
                {t('admin.reconciliation.feeAlertBody', { threshold: view?.feeAlertPercent ?? 0 })}
              </p>
            </Card>
          )}

          {estimated > 0 && (
            <p className="text-[12.5px] text-fg-muted">
              {t('admin.reconciliation.estimatedNote', { count: estimated })}
            </p>
          )}

          <section className="space-y-2.5">
            <h2 className="text-[13px] font-semibold text-fg-muted">
              {t('admin.reconciliation.byPeriod')}
            </h2>
            <Table>
              <TableHead>
                <TableRow>
                  <TableHeadCell>{t('admin.reconciliation.colPeriod')}</TableHeadCell>
                  <TableHeadCell>{t('admin.reconciliation.colGross')}</TableHeadCell>
                  <TableHeadCell>{t('admin.reconciliation.colFee')}</TableHeadCell>
                  <TableHeadCell>{t('admin.reconciliation.colNet')}</TableHeadCell>
                  <TableHeadCell>{t('admin.reconciliation.colReversed')}</TableHeadCell>
                  <TableHeadCell>{t('admin.reconciliation.colTransactions')}</TableHeadCell>
                  <TableHeadCell>{t('admin.reconciliation.colFeeShare')}</TableHeadCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {periods.map((p) => (
                  <TableRow key={p.period}>
                    <TableCell className="font-semibold">{p.period}</TableCell>
                    <TableCell className="font-mono text-xs">{money(p.gross, currency)}</TableCell>
                    <TableCell className="font-mono text-xs text-fg-muted">
                      {money(p.fee, currency)}
                    </TableCell>
                    <TableCell className="font-mono text-xs font-semibold">
                      {money(p.net, currency)}
                    </TableCell>
                    <TableCell className="font-mono text-xs text-fg-muted">
                      {money(p.reversed, currency)}
                    </TableCell>
                    <TableCell className="text-fg-muted">{p.transactions}</TableCell>
                    <TableCell>
                      <Badge tone={p.feeOverThreshold ? 'warning' : 'neutral'}>
                        {`${(p.feePercent ?? 0).toFixed(2)}%`}
                      </Badge>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </section>

          <section className="space-y-2.5">
            <h2 className="text-[13px] font-semibold text-fg-muted">
              {t('admin.reconciliation.payouts')}
            </h2>
            <Table>
              <TableHead>
                <TableRow>
                  <TableHeadCell>{t('admin.reconciliation.colPayout')}</TableHeadCell>
                  <TableHeadCell>{t('admin.reconciliation.colPaidAt')}</TableHeadCell>
                  <TableHeadCell>{t('admin.reconciliation.colCovered')}</TableHeadCell>
                  <TableHeadCell>{t('admin.reconciliation.colAmount')}</TableHeadCell>
                  <TableHeadCell>{t('admin.reconciliation.colLinesNet')}</TableHeadCell>
                  <TableHeadCell>{t('admin.reconciliation.colDifference')}</TableHeadCell>
                  <TableHeadCell>{t('admin.reconciliation.colMatch')}</TableHeadCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {payouts.map((p) => (
                  <TableRow key={p.paddlePayoutId}>
                    <TableCell className="font-mono text-xs">{p.paddlePayoutId}</TableCell>
                    <TableCell className="text-fg-muted">{fmtDate(p.paidAt)}</TableCell>
                    <TableCell className="font-mono text-xs text-fg-muted">
                      {p.coveredFrom ? `${fmtDate(p.coveredFrom)} → ${fmtDate(p.coveredTo)}` : '—'}
                    </TableCell>
                    <TableCell className="font-mono text-xs font-semibold">
                      {money(p.amount, p.currency)}
                    </TableCell>
                    <TableCell className="font-mono text-xs text-fg-muted">
                      {money(p.linesNet, p.currency)}
                    </TableCell>
                    <TableCell className="font-mono text-xs">
                      {p.difference == null ? '—' : money(p.difference, p.currency)}
                    </TableCell>
                    <TableCell>
                      <Badge withDot tone={payoutTone(p.status)}>
                        {matchLabel[p.status ?? 'matched'] ?? p.status}
                      </Badge>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </section>
        </div>
      </QueryState>
    </div>
  )
}
