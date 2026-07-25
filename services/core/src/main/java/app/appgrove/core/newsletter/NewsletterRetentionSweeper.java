package app.appgrove.core.newsletter;

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
 * Retention degli iscritti disiscritti (UC 0039, #13 E): cancellazione <b>fisica</b> di chi è
 * {@code unsubscribed} da oltre 24 mesi, insieme alle sue voci del registro consensi (la revoca è
 * conservata quei 24 mesi come prova, poi si minimizza). Stesso pattern di {@code AccountDeletionSweeper}
 * e {@code TicketRetentionSweeper}: "adesso" iniettabile nei test, scheduler applicativo in locale,
 * trigger cloud (EventBridge/cron) di UC 0035.
 */
@ApplicationScoped
public class NewsletterRetentionSweeper {

    private static final Logger LOG = Logger.getLogger(NewsletterRetentionSweeper.class);
    private static final int RETENTION_MONTHS = 24;

    @Inject
    AgroalDataSource ds;

    @Scheduled(every = "1h", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void run() {
        sweep(Instant.now());
    }

    /**
     * Elimina gli iscritti disiscritti oltre retention rispetto a {@code now} (iniettabile nei test).
     * Ritorna quanti iscritti sono stati eliminati.
     */
    public int sweep(Instant now) {
        Instant cutoff = now.atZone(ZoneOffset.UTC).minusMonths(RETENTION_MONTHS).toInstant();
        // Ordine FK-safe: prima gli eventi (referenziano il subscriber), poi il subscriber.
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                exec(c, "delete from platform.consent_event where subscriber_id in ("
                        + " select id from platform.newsletter_subscriber"
                        + " where status = 'unsubscribed' and unsubscribed_at <= ?)", cutoff);
                int deleted = exec(c, "delete from platform.newsletter_subscriber"
                        + " where status = 'unsubscribed' and unsubscribed_at <= ?", cutoff);
                c.commit();
                if (deleted > 0) {
                    LOG.infof("newsletter.retention.purge deleted=%d before=%s", deleted, cutoff);
                }
                return deleted;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("retention newsletter fallita", e);
        }
    }

    private static int exec(Connection c, String sql, Instant cutoff) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(cutoff));
            return ps.executeUpdate();
        }
    }
}
