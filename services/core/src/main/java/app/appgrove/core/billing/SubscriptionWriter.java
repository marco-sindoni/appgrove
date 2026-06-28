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
 * Applica un evento webhook alla {@code subscription} (unica fonte di verità billing, #09 B12).
 *
 * <p>Gira <b>fuori da una richiesta autenticata</b> (consumer asincrono) → niente JWT → il
 * {@code TenantResolver} è fail-closed. Come per {@code TestData}/{@code AdminResource}, si usa SQL
 * nativo con {@code tenant_id} <b>esplicito</b>, preso dai {@code custom_data} del payload <b>firmato</b>
 * (trust dato dalla firma HMAC, non da input client). L'upsert è <b>idempotente</b> sull'indice unico
 * {@code (tenant_id, app_id)}: ri-applicare lo stesso evento converge allo stesso stato (rigore
 * dedup/out-of-order è di UC 0025).
 */
@ApplicationScoped
public class SubscriptionWriter {

    private static final Logger LOG = Logger.getLogger(SubscriptionWriter.class);

    private static final String UPSERT =
            """
            insert into platform.subscription
              (id, tenant_id, app_id, app_tier_id, status,
               current_period_start, current_period_end, cancel_at, trial_end,
               paddle_subscription_id, created_at, updated_at, created_by, updated_by)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now(), 'system', 'system')
            on conflict (tenant_id, app_id) where deleted_at is null
            do update set
              app_tier_id            = excluded.app_tier_id,
              status                 = excluded.status,
              current_period_start   = excluded.current_period_start,
              current_period_end     = excluded.current_period_end,
              cancel_at              = excluded.cancel_at,
              trial_end              = excluded.trial_end,
              paddle_subscription_id = excluded.paddle_subscription_id,
              updated_at             = now(),
              updated_by             = 'system'
            """;

    @Inject
    AgroalDataSource ds;

    public void apply(PaddleWebhookEvent event) {
        // logging strutturato (invariante #4): tenant_id/app_id/user_id sul processing del webhook
        MDC.put(MdcRequestFilter.TENANT_ID, str(event.tenantId()));
        MDC.put("app_id", str(event.appId()));
        MDC.put(MdcRequestFilter.USER_ID, "system/webhook");
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(UPSERT)) {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, event.tenantId());
            ps.setObject(3, event.appId());
            setNullable(ps, 4, event.appTierId());
            ps.setObject(5, event.status().name());
            setTimestamp(ps, 6, event.currentPeriodStart());
            setTimestamp(ps, 7, event.currentPeriodEnd());
            setTimestamp(ps, 8, event.cancelAt());
            setTimestamp(ps, 9, event.trialEnd());
            setNullable(ps, 10, event.paddleSubscriptionId());
            ps.executeUpdate();
            LOG.infof(
                    "webhook.apply event_type=%s status=%s app_id=%s",
                    event.eventType(), event.status(), event.appId());
        } catch (SQLException e) {
            throw new RuntimeException("Upsert subscription fallito (event " + event.eventId() + ")", e);
        } finally {
            MDC.remove(MdcRequestFilter.TENANT_ID);
            MDC.remove("app_id");
            MDC.remove(MdcRequestFilter.USER_ID);
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

    private static String str(Object value) {
        return value == null ? null : value.toString();
    }
}
