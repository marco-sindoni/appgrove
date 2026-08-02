package app.appgrove.core.billing;

import app.appgrove.core.billing.ReconciliationDtos.PayoutView;
import app.appgrove.core.billing.ReconciliationDtos.ReconciliationPeriod;
import app.appgrove.core.billing.ReconciliationDtos.ReconciliationTotals;
import app.appgrove.core.billing.ReconciliationDtos.ReconciliationView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Riconciliazione fra ricavo lordo e denaro davvero accreditato (UC 0071).
 *
 * <p><b>Il problema che risolve.</b> Il fornitore di pagamento è venditore ufficiale verso il cliente:
 * incassa lui, trattiene le proprie commissioni e ci accredita il netto con accrediti periodici che non
 * coincidono con le singole vendite. Lordo e denaro entrato quindi <b>differiscono</b>, e la differenza non
 * è una percentuale costante perché la quota fissa per transazione pesa moltissimo sui piccoli importi.
 *
 * <p><b>Come legge i dati.</b> Query <b>native cross-tenant</b>, come tutta la console amministrativa
 * (UC 0021): sono dati economici della piattaforma, non del singolo cliente, e la superficie è protetta dal
 * ruolo di piattaforma. È la stessa eccezione esplicita e documentata all'invariante #2.
 *
 * <p><b>Due scelte che vale la pena conoscere.</b> Ogni transazione è attribuita al mese del proprio
 * <b>addebito</b>, mai a quello dell'accredito: il contrario farebbe rimbalzare i ricavi di un mese sul
 * successivo a ogni cambio di calendario del fornitore. E la quadratura di un accredito confronta il suo
 * importo con la somma dei netti <b>congelati nelle sue righe di dettaglio</b>, non con quelli ricalcolati
 * dalle transazioni: uno storno successivo non deve far apparire sbagliato un accredito che allora era
 * corretto.
 */
@ApplicationScoped
public class ReconciliationService {

    /** Quanti mesi di storico mostrare: un anno è il minimo per vedere la stagionalità degli annuali. */
    private static final int PERIOD_MONTHS = 12;

    /** Quanti accrediti elencare: due anni di accrediti quindicinali. */
    private static final int PAYOUT_LIMIT = 48;

    /** Esiti della quadratura di un accredito. */
    static final String MATCHED = "matched";

    static final String MISMATCH = "mismatch";

    static final String MIXED_CURRENCY = "mixed_currency";

    @Inject
    EntityManager em;

    /** Soglia di attenzione sul peso delle commissioni: oltre, le micro-transazioni stanno pesando troppo. */
    @ConfigProperty(name = "appgrove.payments.fee-alert-percent", defaultValue = "8.0")
    double feeAlertPercent;

    /** Attesa massima prima di considerare in ritardo un accredito che non arriva. */
    @ConfigProperty(name = "appgrove.payments.payout-max-age", defaultValue = "P14D")
    Duration payoutMaxAge;

    /** La vista completa: totali, righe per mese, accrediti con la loro quadratura. */
    public ReconciliationView view() {
        ReconciliationTotals totals = totals();
        return new ReconciliationView(
                prevalentCurrency(),
                totals,
                periods(),
                payouts(),
                feeAlertPercent,
                isPayoutOverdue(totals),
                payoutMaxAge.toDays());
    }

    /**
     * Vero quando esiste netto non ancora accreditato più vecchio dell'attesa massima: è l'accredito atteso
     * che non è arrivato. Nessuna azione correttiva — è osservabilità, e chi guarda decide.
     */
    public boolean isPayoutOverdue(ReconciliationTotals totals) {
        return totals.unsettled() != 0
                && totals.oldestUnsettledAt() != null
                && totals.oldestUnsettledAt().isBefore(Instant.now().minus(payoutMaxAge));
    }

    /** Totali su tutto lo storico: la quadratura degli accrediti non si lascia tagliare da una finestra. */
    public ReconciliationTotals totals() {
        Object[] t = (Object[]) em.createNativeQuery(
                        """
                        select
                          coalesce(sum(amount) filter (where status = 'paid'), 0),
                          coalesce(sum(coalesce(fee_amount, 0)) filter (where status = 'paid'), 0),
                          coalesce(sum(coalesce(net_amount, 0)) filter (where status = 'paid'), 0),
                          coalesce(sum(amount) filter (where status in ('disputed', 'refunded')), 0),
                          count(*) filter (where status = 'paid'),
                          count(*) filter (where status = 'paid' and fee_source = 'estimated')
                        from platform.billing_transaction
                        where deleted_at is null
                        """)
                .getSingleResult();

        long settled = num(em.createNativeQuery(
                        "select coalesce(sum(amount), 0) from platform.payout where deleted_at is null")
                .getSingleResult());

        // Netto non ancora accreditato: le transazioni riuscite che nessun accredito cita. `not exists` e
        // non una giunzione, perché un accredito può citare più volte la stessa transazione (uno storno
        // successivo la richiama con segno negativo) e una giunzione la conterebbe due volte.
        Object[] u = (Object[]) em.createNativeQuery(
                        """
                        select coalesce(sum(coalesce(t.net_amount, 0)), 0), min(t.billed_at)
                        from platform.billing_transaction t
                        where t.deleted_at is null and t.status = 'paid'
                          and not exists (
                              select 1 from platform.payout_line l
                              where l.paddle_transaction_id = t.paddle_transaction_id)
                        """)
                .getSingleResult();

        return new ReconciliationTotals(
                num(t[0]), num(t[1]), num(t[2]), num(t[3]), settled, num(u[0]), num(t[4]), num(t[5]), instant(u[1]));
    }

    /** Valuta prevalente delle transazioni: serve solo a formattare i totali senza mentire troppo. */
    private String prevalentCurrency() {
        List<?> rows = em.createNativeQuery(
                        """
                        select currency from platform.billing_transaction
                        where deleted_at is null
                        group by currency order by count(*) desc, currency
                        limit 1
                        """)
                .getResultList();
        return rows.isEmpty() ? null : str(rows.get(0));
    }

    /** Righe per mese di addebito, dalla più recente. */
    @SuppressWarnings("unchecked")
    private List<ReconciliationPeriod> periods() {
        List<Object[]> rows = em.createNativeQuery(
                        """
                        select to_char(date_trunc('month', billed_at at time zone 'UTC'), 'YYYY-MM'),
                          coalesce(sum(amount) filter (where status = 'paid'), 0),
                          coalesce(sum(coalesce(fee_amount, 0)) filter (where status = 'paid'), 0),
                          coalesce(sum(coalesce(net_amount, 0)) filter (where status = 'paid'), 0),
                          coalesce(sum(amount) filter (where status in ('disputed', 'refunded')), 0),
                          count(*) filter (where status = 'paid')
                        from platform.billing_transaction
                        where deleted_at is null
                        group by 1
                        order by 1 desc
                        limit :limit
                        """)
                .setParameter("limit", PERIOD_MONTHS)
                .getResultList();
        return rows.stream()
                .map(r -> {
                    long gross = num(r[1]);
                    long fee = num(r[2]);
                    double percent = feePercent(gross, fee);
                    return new ReconciliationPeriod(
                            str(r[0]), gross, fee, num(r[3]), num(r[4]), num(r[5]), percent, percent > feeAlertPercent);
                })
                .toList();
    }

    /** Accrediti con la loro quadratura, dal più recente. */
    @SuppressWarnings("unchecked")
    private List<PayoutView> payouts() {
        List<Object[]> rows = em.createNativeQuery(
                        """
                        select p.paddle_payout_id, p.paid_at, p.currency, p.amount,
                               coalesce(l.net_sum, 0), coalesce(l.line_count, 0),
                               coalesce(l.mixed, false), t.covered_from, t.covered_to
                        from platform.payout p
                        left join lateral (
                            select sum(net_amount) as net_sum, count(*) as line_count,
                                   bool_or(currency is distinct from p.currency) as mixed
                            from platform.payout_line where payout_id = p.id
                        ) l on true
                        left join lateral (
                            select min(bt.billed_at) as covered_from, max(bt.billed_at) as covered_to
                            from platform.payout_line pl
                            join platform.billing_transaction bt
                              on bt.paddle_transaction_id = pl.paddle_transaction_id and bt.deleted_at is null
                            where pl.payout_id = p.id
                        ) t on true
                        where p.deleted_at is null
                        order by p.paid_at desc, p.paddle_payout_id
                        limit :limit
                        """)
                .setParameter("limit", PAYOUT_LIMIT)
                .getResultList();
        return rows.stream().map(ReconciliationService::toPayout).toList();
    }

    private static PayoutView toPayout(Object[] r) {
        long amount = num(r[3]);
        long linesNet = num(r[4]);
        boolean mixed = Boolean.TRUE.equals(r[6]);
        // Valute diverse nello stesso accredito: lo scostamento NON si calcola. Sommare valute diverse
        // produrrebbe un numero che sembra una differenza e non lo è; meglio dire che non è quadrabile e
        // aspettare i dati veri sul cambio applicato (punto aperto di UC 0071).
        String status = mixed ? MIXED_CURRENCY : (amount == linesNet ? MATCHED : MISMATCH);
        return new PayoutView(
                str(r[0]),
                instant(r[1]),
                str(r[2]),
                amount,
                linesNet,
                mixed ? null : amount - linesNet,
                status,
                num(r[5]),
                instant(r[7]),
                instant(r[8]));
    }

    /** Peso delle commissioni sul lordo, in percentuale; 0 quando non c'è lordo su cui misurarlo. */
    static double feePercent(long gross, long fee) {
        return gross <= 0 ? 0d : Math.round(fee * 10_000d / gross) / 100d;
    }

    private static long num(Object o) {
        return o == null ? 0L : ((Number) o).longValue();
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    /** I timestamp delle query native arrivano come {@code Timestamp} o {@code OffsetDateTime} secondo il driver. */
    private static Instant instant(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Instant i) {
            return i;
        }
        if (o instanceof Timestamp ts) {
            return ts.toInstant();
        }
        if (o instanceof OffsetDateTime odt) {
            return odt.toInstant();
        }
        return Instant.parse(o.toString());
    }
}
