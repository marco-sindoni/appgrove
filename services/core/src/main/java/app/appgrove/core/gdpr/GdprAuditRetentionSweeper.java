package app.appgrove.core.gdpr;

import io.agroal.api.AgroalDataSource;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import org.jboss.logging.Logger;

/**
 * Retention delle tabelle di prova GDPR in schema {@code platform} (UC 0035, #08): hard-delete a 12
 * mesi delle righe di {@code gdpr_purge_audit} (prova dell'erasure, #13 L70) e
 * {@code gdpr_restriction_audit} (prova della limitazione art. 18, #13 L75). La copia forense lunga
 * degli eventi di audit resta sull'archivio S3/Glacier a 12 mesi (UC 0006); qui si minimizzano le
 * righe operative nel database, la cui retention 12 mesi era dichiarata ma non ancora applicata da
 * alcun job (punto aperto tracciato da UC 0034).
 *
 * <p>Stesso pattern degli altri sweeper: {@code @Scheduled} orario, {@code sweep(Instant)}
 * iniettabile nei test, JDBC. Le copie per-app di {@code gdpr_purge_audit} (schemi {@code app_<id>})
 * restano un rimando tracciato (UC 0035/0048).
 */
@ApplicationScoped
public class GdprAuditRetentionSweeper {

    private static final Logger LOG = Logger.getLogger(GdprAuditRetentionSweeper.class);

    /** Retention delle prove di audit nel DB (#08). */
    static final int RETENTION_MONTHS = 12;

    @Inject
    AgroalDataSource ds;

    @Scheduled(every = "1h", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void run() {
        sweep(Instant.now());
    }

    /** Elimina le righe di audit oltre retention rispetto a {@code now}; ritorna quante ne ha eliminate. */
    public int sweep(Instant now) {
        Instant cutoff = now.atZone(ZoneOffset.UTC).minusMonths(RETENTION_MONTHS).toInstant();
        try (Connection c = ds.getConnection()) {
            int purge = deleteBefore(c, "platform.gdpr_purge_audit", cutoff);
            int restriction = deleteBefore(c, "platform.gdpr_restriction_audit", cutoff);
            int total = purge + restriction;
            if (total > 0) {
                LOG.infof("gdpr.audit-retention.purge purge_audit=%d restriction_audit=%d before=%s",
                        purge, restriction, cutoff);
            }
            return total;
        } catch (SQLException e) {
            throw new RuntimeException("retention audit GDPR fallita", e);
        }
    }

    private static int deleteBefore(Connection c, String table, Instant cutoff) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "delete from " + table + " where executed_at <= ?")) {
            ps.setTimestamp(1, Timestamp.from(cutoff));
            return ps.executeUpdate();
        }
    }
}
