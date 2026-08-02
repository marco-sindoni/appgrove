package app.appgrove.core.billing;

import app.appgrove.commons.logging.MdcRequestFilter;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.jboss.logging.Logger;
import org.jboss.logmanager.MDC;

/**
 * Applica un evento webhook a {@code subscription}/{@code accounts} (unica fonte di verità billing,
 * #09 B12) con l'hardening UC 0025. Tutto avviene in <b>una sola transazione</b>:
 *
 * <ol>
 *   <li><b>Dedup</b> (#09 D18b): {@code INSERT ... ON CONFLICT (event_id) DO NOTHING} su
 *       {@code webhook_event}; 0 righe → evento già processato → {@link Outcome#DUPLICATE} (no-op).</li>
 *   <li><b>Apply</b>: {@code customer.*} → {@code accounts.paddle_customer_id}; {@code payout.*} →
 *       {@code payout} + {@code payout_line} (accrediti del fornitore, UC 0071); gli altri eventi del set
 *       (#09 D21) → upsert idempotente di {@code subscription} con <b>guardia out-of-order</b> via
 *       {@code occurred_at} (#09 D18c): un evento più vecchio non sovrascrive → {@link Outcome#SKIPPED_STALE}.</li>
 *   <li><b>Esito</b> registrato su {@code webhook_event} ({@code processed}/{@code skipped_stale}).</li>
 * </ol>
 *
 * <p>Gira <b>fuori da una richiesta autenticata</b> (consumer asincrono) → niente JWT → si usa SQL nativo
 * con {@code tenant_id} <b>esplicito</b> dai {@code custom_data} del payload <b>firmato</b> (trust dalla
 * firma HMAC, non da input client). Un errore di elaborazione rilancia: la transazione fa rollback
 * (incluso il record di dedup) e il messaggio resta in coda per il redrive → DLQ (vedi
 * {@link PaddleWebhookConsumer}).
 */
@ApplicationScoped
public class SubscriptionWriter {

    private static final Logger LOG = Logger.getLogger(SubscriptionWriter.class);

    /** Esito dell'applicazione di un evento (per logging/osservabilità del consumer). */
    public enum Outcome {
        /** Applicato (subscription/accounts aggiornati). */
        PROCESSED,
        /** Già processato (stesso {@code event_id}): no-op idempotente. */
        DUPLICATE,
        /** Evento più vecchio dello stato corrente: non applicato (out-of-order). */
        SKIPPED_STALE
    }

    private static final String DEDUP_INSERT =
            """
            insert into platform.webhook_event
              (id, event_id, event_type, occurred_at, tenant_id, app_id, outcome, received_at)
            values (?, ?, ?, ?, ?, ?, 'received', now())
            on conflict (event_id) do nothing
            """;

    private static final String DEDUP_OUTCOME =
            "update platform.webhook_event set outcome = ?, processed_at = now() where event_id = ?";

    private static final String UPSERT =
            """
            insert into platform.subscription
              (id, tenant_id, app_id, app_tier_id, status,
               current_period_start, current_period_end, cancel_at, trial_end,
               scheduled_tier_id, scheduled_change_at,
               paddle_subscription_id, last_event_occurred_at,
               created_at, updated_at, created_by, updated_by)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now(), 'system', 'system')
            on conflict (tenant_id, app_id) where deleted_at is null
            do update set
              app_tier_id            = excluded.app_tier_id,
              status                 = excluded.status,
              current_period_start   = excluded.current_period_start,
              current_period_end     = excluded.current_period_end,
              cancel_at              = excluded.cancel_at,
              trial_end              = excluded.trial_end,
              scheduled_tier_id      = excluded.scheduled_tier_id,
              scheduled_change_at    = excluded.scheduled_change_at,
              paddle_subscription_id = excluded.paddle_subscription_id,
              last_event_occurred_at = excluded.last_event_occurred_at,
              updated_at             = now(),
              updated_by             = 'system'
            where platform.subscription.last_event_occurred_at is null
               or platform.subscription.last_event_occurred_at <= excluded.last_event_occurred_at
            """;

    /**
     * Storico pagamenti (UC 0096): una riga per transazione del fornitore, idempotente sul suo riferimento
     * e con la <b>stessa</b> guardia out-of-order dell'abbonamento — un evento più vecchio non riscrive un
     * esito più recente (un pagamento riuscito dopo un tentativo fallito non deve tornare "fallito" se i
     * due eventi arrivano nell'ordine sbagliato).
     */
    private static final String UPSERT_TRANSACTION =
            """
            insert into platform.billing_transaction
              (id, tenant_id, app_id, app_tier_id, paddle_transaction_id, status,
               amount, currency, billing_cycle, receipt_url, billed_at, last_event_occurred_at,
               fee_amount, net_amount, fee_source,
               created_at, updated_at, created_by, updated_by)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now(), 'system', 'system')
            on conflict (paddle_transaction_id)
            do update set
              app_id                 = excluded.app_id,
              app_tier_id            = excluded.app_tier_id,
              status                 = excluded.status,
              amount                 = excluded.amount,
              currency               = excluded.currency,
              billing_cycle          = excluded.billing_cycle,
              receipt_url            = excluded.receipt_url,
              billed_at              = excluded.billed_at,
              last_event_occurred_at = excluded.last_event_occurred_at,
              fee_amount             = excluded.fee_amount,
              net_amount             = excluded.net_amount,
              fee_source             = excluded.fee_source,
              updated_at             = now(),
              updated_by             = 'system'
            where platform.billing_transaction.last_event_occurred_at is null
               or platform.billing_transaction.last_event_occurred_at <= excluded.last_event_occurred_at
            """;

    /**
     * Accredito del fornitore (UC 0071): stessa idempotenza e stessa guardia out-of-order della
     * transazione. {@code returning id} serve a riscrivere il dettaglio <b>solo</b> quando l'accredito è
     * stato davvero applicato: su un evento più vecchio l'upsert non tocca nulla e il dettaglio non deve
     * essere riscritto con dati sorpassati.
     */
    private static final String UPSERT_PAYOUT =
            """
            insert into platform.payout
              (id, paddle_payout_id, amount, currency, paid_at, last_event_occurred_at,
               created_at, updated_at, created_by, updated_by)
            values (?, ?, ?, ?, ?, ?, now(), now(), 'system', 'system')
            on conflict (paddle_payout_id)
            do update set
              amount                 = excluded.amount,
              currency               = excluded.currency,
              paid_at                = excluded.paid_at,
              last_event_occurred_at = excluded.last_event_occurred_at,
              updated_at             = now(),
              updated_by             = 'system'
            where platform.payout.last_event_occurred_at is null
               or platform.payout.last_event_occurred_at <= excluded.last_event_occurred_at
            returning id
            """;

    private static final String DELETE_PAYOUT_LINES = "delete from platform.payout_line where payout_id = ?";

    private static final String INSERT_PAYOUT_LINE =
            """
            insert into platform.payout_line (payout_id, paddle_transaction_id, net_amount, currency)
            values (?, ?, ?, ?)
            on conflict (payout_id, paddle_transaction_id) do update set
              net_amount = excluded.net_amount,
              currency   = excluded.currency
            """;

    private static final String UPDATE_CUSTOMER =
            "update platform.accounts set paddle_customer_id = ?, updated_at = now(), updated_by = 'system'"
                    + " where id = ?";

    @Inject
    AgroalDataSource ds;

    @Inject
    PaymentFees fees;

    /** Applica un evento in modo transazionale e ritorna l'esito (mai null). */
    public Outcome apply(PaddleWebhookEvent event) {
        // logging strutturato (invariante #4): tenant_id/app_id/user_id sul processing del webhook.
        // app_id è null per gli eventi customer.* → si omette (MDC.put non accetta valori null).
        mdc(MdcRequestFilter.TENANT_ID, str(event.tenantId()));
        mdc("app_id", str(event.appId()));
        mdc(MdcRequestFilter.USER_ID, "system/webhook");
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                if (!dedupClaim(c, event)) {
                    c.commit();
                    LOG.debugf("webhook.apply event_id=%s duplicato → no-op", event.eventId());
                    return Outcome.DUPLICATE;
                }
                Outcome outcome = applyEffect(c, event);
                recordOutcome(c, event.eventId(), outcome);
                c.commit();
                LOG.infof(
                        "webhook.apply event_type=%s event_id=%s app_id=%s outcome=%s",
                        event.eventType(), event.eventId(), event.appId(), outcome);
                return outcome;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Elaborazione webhook fallita (event " + event.eventId() + ")", e);
        } finally {
            MDC.remove(MdcRequestFilter.TENANT_ID);
            MDC.remove("app_id");
            MDC.remove(MdcRequestFilter.USER_ID);
        }
    }

    /** Inserisce la riga di dedup; false se l'event_id era già presente (duplicato). */
    private boolean dedupClaim(Connection c, PaddleWebhookEvent event) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(DEDUP_INSERT)) {
            ps.setObject(1, UUID.randomUUID());
            ps.setString(2, event.eventId());
            ps.setString(3, event.eventType());
            setTimestamp(ps, 4, event.occurredAt());
            ps.setString(5, str(event.tenantId()));
            setNullable(ps, 6, event.appId());
            return ps.executeUpdate() == 1;
        }
    }

    private void recordOutcome(Connection c, String eventId, Outcome outcome) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(DEDUP_OUTCOME)) {
            ps.setString(1, outcome.name().toLowerCase());
            ps.setString(2, eventId);
            ps.executeUpdate();
        }
    }

    /** Esegue l'effetto dell'evento (customer vs subscription) e ritorna PROCESSED/SKIPPED_STALE. */
    private Outcome applyEffect(Connection c, PaddleWebhookEvent event) throws SQLException {
        if (event.isCustomerEvent()) {
            updateCustomer(c, event);
            return Outcome.PROCESSED;
        }
        // Accredito del fornitore (UC 0071): non ha un conto cliente e non tocca nessun abbonamento —
        // conta soltanto il denaro davvero arrivato. Esce subito, prima del ramo transazione/subscription.
        if (event.isPayoutEvent()) {
            return recordPayout(c, event) ? Outcome.PROCESSED : Outcome.SKIPPED_STALE;
        }
        // Storico pagamenti (UC 0096): indipendente dall'effetto sull'abbonamento, e nella STESSA
        // transazione — o si registrano entrambi o nessuno dei due, altrimenti uno storico e uno stato
        // che si contraddicono sarebbero peggio di un dato mancante.
        recordTransaction(c, event);

        SubscriptionStatus target = WebhookEventMapping.targetStatus(event);
        if (target == null) {
            // evento non sottoscritto/senza effetto su subscription → no-op registrato (meno rumore, #09 D21)
            return Outcome.PROCESSED;
        }
        return upsertSubscription(c, event, target) ? Outcome.PROCESSED : Outcome.SKIPPED_STALE;
    }

    /**
     * Registra la transazione nello storico, se l'evento ne porta una con i dati economici. Un evento di
     * transazione privo di importo o valuta viene ignorato invece di essere completato d'ufficio: un
     * importo inventato dal backend sarebbe un dato falso in una pagina di fatturazione.
     */
    private void recordTransaction(Connection c, PaddleWebhookEvent event) throws SQLException {
        BillingTransactionStatus status = event.transactionStatus();
        PaddleWebhookEvent.TransactionData tx = event.transaction();
        if (status == null || tx == null || tx.currency() == null) {
            return;
        }
        // Commissione e netto (UC 0071). Il numero del fornitore vince sempre; in sua assenza si stima e la
        // riga resta marcata come stimata. Su un tentativo fallito non c'è nulla da trattenere; su uno
        // storno la commissione resta a carico nostro ma l'incasso è tornato indietro, quindi il netto è
        // zero e il denaro restituito riappare come riga negativa in un accredito successivo.
        PaymentFees.Fee fee = status == BillingTransactionStatus.failed ? null : fees.of(tx.amount(), tx.fee());
        int netAmount = status == BillingTransactionStatus.paid && fee != null ? fee.netAmount() : 0;
        try (PreparedStatement ps = c.prepareStatement(UPSERT_TRANSACTION)) {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, event.tenantId());
            setNullable(ps, 3, event.appId());
            setNullable(ps, 4, event.appTierId());
            ps.setString(5, tx.paddleTransactionId());
            ps.setString(6, status.name());
            ps.setInt(7, tx.amount());
            ps.setString(8, tx.currency());
            setNullable(ps, 9, tx.billingCycle());
            setNullable(ps, 10, tx.receiptUrl());
            setTimestamp(ps, 11, tx.billedAt());
            setTimestamp(ps, 12, event.occurredAt());
            if (fee == null) {
                ps.setNull(13, Types.INTEGER);
                ps.setInt(14, 0);
                ps.setNull(15, Types.VARCHAR);
            } else {
                ps.setInt(13, fee.feeAmount());
                ps.setInt(14, netAmount);
                ps.setString(15, fee.source().name());
            }
            ps.executeUpdate();
        }
    }

    /**
     * Registra l'accredito e il suo dettaglio (UC 0071). Il dettaglio si riscrive <b>solo</b> se l'accredito
     * è stato applicato: un evento più vecchio non deve sostituire righe più recenti. Il netto di ogni riga
     * è quello comunicato dal fornitore per <b>quell'</b>accredito, mai ricalcolato dalla transazione —
     * altrimenti uno storno successivo farebbe apparire sbagliato un accredito che allora era corretto.
     *
     * @return {@code false} se l'evento era più vecchio dello stato già registrato (stale)
     */
    private boolean recordPayout(Connection c, PaddleWebhookEvent event) throws SQLException {
        PaddleWebhookEvent.PayoutData payout = event.payout();
        if (payout == null || payout.currency() == null) {
            // Un accredito senza riferimento o senza valuta non è registrabile: non c'è chiave di
            // idempotenza né unità di misura. Non è un errore di pipeline — si registra come no-op.
            return true;
        }
        UUID payoutId;
        try (PreparedStatement ps = c.prepareStatement(UPSERT_PAYOUT)) {
            ps.setObject(1, UUID.randomUUID());
            ps.setString(2, payout.paddlePayoutId());
            ps.setInt(3, payout.amount());
            ps.setString(4, payout.currency());
            setTimestamp(ps, 5, payout.paidAt());
            setTimestamp(ps, 6, event.occurredAt());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false; // stale: nessuna riga aggiornata
                }
                payoutId = rs.getObject(1, UUID.class);
            }
        }
        try (PreparedStatement ps = c.prepareStatement(DELETE_PAYOUT_LINES)) {
            ps.setObject(1, payoutId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement(INSERT_PAYOUT_LINE)) {
            for (PaddleWebhookEvent.PayoutLine line : payout.lines()) {
                ps.setObject(1, payoutId);
                ps.setString(2, line.paddleTransactionId());
                ps.setInt(3, line.netAmount());
                ps.setString(4, line.currency() != null ? line.currency() : payout.currency());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        return true;
    }

    /** Upsert idempotente con guardia out-of-order; true se ha applicato, false se stale. */
    private boolean upsertSubscription(Connection c, PaddleWebhookEvent event, SubscriptionStatus target)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(UPSERT)) {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, event.tenantId());
            ps.setObject(3, event.appId());
            setNullable(ps, 4, event.appTierId());
            ps.setObject(5, target.name());
            setTimestamp(ps, 6, event.currentPeriodStart());
            setTimestamp(ps, 7, event.currentPeriodEnd());
            setTimestamp(ps, 8, event.cancelAt());
            setTimestamp(ps, 9, event.trialEnd());
            setNullable(ps, 10, event.scheduledTierId());
            setTimestamp(ps, 11, event.scheduledChangeAt());
            setNullable(ps, 12, event.paddleSubscriptionId());
            setTimestamp(ps, 13, event.occurredAt());
            return ps.executeUpdate() == 1;
        }
    }

    private void updateCustomer(Connection c, PaddleWebhookEvent event) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(UPDATE_CUSTOMER)) {
            setNullable(ps, 1, event.paddleCustomerId());
            ps.setObject(2, UUID.fromString(event.tenantId()));
            ps.executeUpdate(); // 0 righe (account assente) = no-op accettabile
        }
    }

    private static void setNullable(PreparedStatement ps, int idx, Object value) throws SQLException {
        if (value == null) {
            ps.setNull(idx, Types.OTHER);
        } else {
            ps.setObject(idx, value);
        }
    }

    private static void setTimestamp(PreparedStatement ps, int idx, Instant value) throws SQLException {
        if (value == null) {
            ps.setNull(idx, Types.TIMESTAMP_WITH_TIMEZONE);
        } else {
            ps.setObject(idx, value.atOffset(ZoneOffset.UTC));
        }
    }

    private static void mdc(String key, String value) {
        if (value != null) {
            MDC.put(key, value);
        } else {
            MDC.remove(key);
        }
    }

    private static String str(Object value) {
        return value == null ? null : value.toString();
    }
}
