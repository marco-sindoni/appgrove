package app.appgrove.core.platform;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Timbra {@code platform.accounts.last_active_at} dall'attività autenticata (UC 0035): è il segnale
 * su cui l'{@code AccountInactivitySweeper} misura l'inattività (24 mesi, #13 E26). Per evitare una
 * scrittura per ogni richiesta, il timbro è <b>throttlato</b> con una cache in memoria per-account
 * (finestra {@code appgrove.activity.stamp-throttle}, default 6h): a {@code desired_count=1} il task
 * ECS è unico, quindi la cache è autorevole. L'aggiornamento è <b>best-effort</b>: non deve mai far
 * fallire la richiesta (lo invoca {@link ActivityStampFilter} sul percorso di risposta).
 */
@ApplicationScoped
public class AccountActivityTracker {

    private static final Logger LOG = Logger.getLogger(AccountActivityTracker.class);

    @Inject
    AgroalDataSource ds;

    @ConfigProperty(name = "appgrove.activity.stamp-throttle", defaultValue = "PT6H")
    Duration throttle;

    /** Ultimo timbro noto per tenant (in memoria): evita lo hit sul DB dentro la finestra. */
    private final ConcurrentHashMap<String, Instant> lastStamped = new ConcurrentHashMap<>();

    /**
     * Decide se l'attività del tenant va ri-timbrata ora rispetto alla finestra di throttle, e in
     * caso aggiorna la cache. Pura (nessun accesso al DB) → seam testabile senza database.
     */
    boolean shouldStamp(String tenantId, Instant now) {
        Instant prev = lastStamped.get(tenantId);
        if (prev != null && prev.isAfter(now.minus(throttle))) {
            return false;
        }
        lastStamped.put(tenantId, now);
        return true;
    }

    /** Timbra {@code last_active_at} del tenant (throttlato, best-effort): mai propaga eccezioni. */
    public void touch(String tenantId, Instant now) {
        try {
            if (!shouldStamp(tenantId, now)) {
                return;
            }
            update(tenantId, now);
        } catch (RuntimeException e) {
            LOG.debugf(e, "activity.stamp fallita tenant_id=%s (best-effort, ignorata)", tenantId);
        }
    }

    private void update(String tenantId, Instant now) {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "update platform.accounts set last_active_at = ?"
                                + " where id = ?::uuid and deleted_at is null")) {
            ps.setTimestamp(1, Timestamp.from(now));
            ps.setString(2, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("aggiornamento last_active_at fallito per il tenant " + tenantId, e);
        }
    }
}
