import {
  Badge,
  Button,
  Card,
  CardContent,
  Icon,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeadCell,
  TableRow,
} from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { formatPrice } from './checkoutMachine'
import { describePayment, useCanReadPayments, useMyPayments, type Payment } from './paymentsApi'

// Chiavi derivate a runtime (esito, ciclo) — opache al tipo forte di `t`.
type TKey = never

/** Il colore dice quanto la cosa è seria, non solo che cos'è. */
const STATUS_TONE: Record<string, 'success' | 'warning' | 'danger'> = {
  paid: 'success',
  failed: 'warning',
  disputed: 'danger',
}

/** I cicli che sappiamo tradurre; qualunque altro valore si mostra così com'è, senza fingere. */
const KNOWN_CYCLES = ['monthly', 'annual']

/**
 * Storico pagamenti e ricevute (UC 0096) — la seconda metà della pagina Billing, quella che finora non
 * esisteva affatto nel prodotto: l'unica via alle ricevute era il portale del fornitore, dietro un
 * pulsante che compare solo dopo il primo acquisto.
 *
 * <p>La sezione **possiede i propri stati**: caricamento, vuoto, errore con riprova. È deliberato — se lo
 * storico non si legge, gli abbonamenti sopra devono restare visibili e azionabili (UC 0096 §5): due
 * guasti indipendenti, non una pagina tutta rossa.
 */
export function PaymentsPanel() {
  const { t } = useTranslation()
  const canRead = useCanReadPayments()
  const query = useMyPayments()

  return (
    <section className="space-y-4" aria-label={t('payments.title')}>
      <header className="space-y-1">
        <h2 className="text-xl font-semibold text-fg">{t('payments.title')}</h2>
        <p className="text-sm text-fg-muted">{t('payments.subtitle')}</p>
      </header>

      {!canRead && (
        <Card>
          <CardContent className="text-fg-muted">{t('payments.restricted')}</CardContent>
        </Card>
      )}

      {canRead && query.isLoading && (
        <p role="status" className="text-sm text-fg-muted">
          {t('states.loading')}
        </p>
      )}

      {canRead && query.isError && (
        <div role="alert" className="space-y-3">
          <p className="text-sm text-danger">{t('payments.error')}</p>
          <Button variant="secondary" size="sm" onClick={() => void query.refetch()}>
            {t('states.retry')}
          </Button>
        </div>
      )}

      {canRead && query.data && (query.data.payments?.length ?? 0) === 0 && (
        <Card>
          <CardContent className="text-fg-muted">{t('payments.empty')}</CardContent>
        </Card>
      )}

      {canRead && (query.data?.payments?.length ?? 0) > 0 && (
        <Table>
          <TableHead>
            <TableRow>
              <TableHeadCell>{t('payments.colDate')}</TableHeadCell>
              <TableHeadCell>{t('payments.colApp')}</TableHeadCell>
              <TableHeadCell>{t('payments.colDescription')}</TableHeadCell>
              <TableHeadCell>{t('payments.colAmount')}</TableHeadCell>
              <TableHeadCell>{t('payments.colStatus')}</TableHeadCell>
              <TableHeadCell>{t('payments.colReceipt')}</TableHeadCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {query.data?.payments?.map((payment, index) => (
              <PaymentRow key={`${payment.billedAt ?? ''}-${index}`} payment={payment} />
            ))}
          </TableBody>
        </Table>
      )}
    </section>
  )
}

function PaymentRow({ payment }: { payment: Payment }) {
  const { t, i18n } = useTranslation()
  const status = payment.status ?? 'paid'
  const date = payment.billedAt
    ? new Intl.DateTimeFormat(i18n.language, { dateStyle: 'medium' }).format(new Date(payment.billedAt))
    : ''

  return (
    <TableRow>
      <TableCell>{date}</TableCell>
      <TableCell>{payment.appName ?? payment.appSlug ?? ''}</TableCell>
      <TableCell>
        {describePayment(payment, (cycle) =>
          KNOWN_CYCLES.includes(cycle) ? t(`payments.cycle.${cycle}` as TKey) : cycle,
        )}
      </TableCell>
      {/* Importi in carattere a larghezza fissa: incolonnati si confrontano a colpo d'occhio. */}
      <TableCell className="whitespace-nowrap font-mono tabular-nums">
        {formatPrice(payment.amount ?? 0, payment.currency ?? 'EUR', i18n.language)}
      </TableCell>
      <TableCell>
        <Badge tone={STATUS_TONE[status] ?? 'neutral'}>{t(`payments.status.${status}` as TKey)}</Badge>
      </TableCell>
      <TableCell>
        {payment.receiptUrl ? (
          <a
            className="inline-flex items-center gap-1 font-semibold text-accent hover:underline"
            href={payment.receiptUrl}
            target="_blank"
            rel="noopener noreferrer"
          >
            {t('payments.receipt')}
            <Icon name="open_in_new" size={14} />
          </a>
        ) : (
          // Ritardo del fornitore: la riga esiste, senza collegamento e con uno stato onesto.
          <span className="text-fg-faint">{t('payments.receiptPending')}</span>
        )}
      </TableCell>
    </TableRow>
  )
}
