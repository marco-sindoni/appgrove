package app.appgrove.core.billing;

import app.appgrove.commons.logging.MdcRequestFilter;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
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
 *   <li><b>Apply</b>: {@code customer.*} → {@code accounts.paddle_customer_id}; gli altri eventi del set
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

    private static final String UPDATE_CUSTOMER =
            "update platform.accounts set paddle_customer_id = ?, updated_at = now(), updated_by = 'system'"
                    + " where id = ?";

    @Inject
    AgroalDataSource ds;

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
        SubscriptionStatus target = WebhookEventMapping.targetStatus(event);
        if (target == null) {
            // evento non sottoscritto/senza effetto su subscription → no-op registrato (meno rumore, #09 D21)
            return Outcome.PROCESSED;
        }
        return upsertSubscription(c, event, target) ? Outcome.PROCESSED : Outcome.SKIPPED_STALE;
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
