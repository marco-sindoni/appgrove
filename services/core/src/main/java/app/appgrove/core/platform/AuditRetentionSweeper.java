package app.appgrove.core.platform;

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
import java.util.List;
import org.jboss.logging.Logger;

/**
 * Conservazione delle <b>tabelle di prova</b> in schema {@code platform} (UC 0035, #08): hard-delete a
 * 12 mesi delle righe di
 *
 * <ul>
 *   <li>{@code gdpr_purge_audit} — prova dell'erasure (#13 L70);
 *   <li>{@code gdpr_restriction_audit} — prova della limitazione art. 18 (#13 L75);
 *   <li>{@code app_status_audit} — prova della disabilitazione/riabilitazione di un'app (UC 0076);
 *   <li>{@code active_account_audit} — prova del cambio di account attivo di una persona (UC 0117).
 * </ul>
 *
 * <p>La copia forense lunga degli eventi di audit resta sull'archivio S3/Glacier a 12 mesi (UC 0006);
 * qui si minimizzano le righe operative nel database, la cui conservazione a 12 mesi era dichiarata ma
 * non applicata da alcun job (punto aperto tracciato da UC 0034).
 *
 * <p>Il job nasceva come {@code GdprAuditRetentionSweeper} nel pacchetto {@code gdpr}; con
 * {@code app_status_audit} — che di GDPR non ha nulla: è il registro di una leva amministrativa sul
 * catalogo — la regola che applica è semplicemente "le prove di audit nel database durano 12 mesi", ed
 * è quindi di piattaforma. Nome e collocazione seguono la sostanza (UC 0076).
 *
 * <p>Stesso pattern degli altri sweeper: {@code @Scheduled} orario, {@code sweep(Instant)} iniettabile
 * nei test, JDBC. Le copie per-app di {@code gdpr_purge_audit} (schemi {@code app_<id>}) restano un
 * rimando tracciato (UC 0035/0048).
 */
@ApplicationScoped
public class AuditRetentionSweeper {

    private static final Logger LOG = Logger.getLogger(AuditRetentionSweeper.class);

    /** Conservazione delle prove di audit nel DB (#08). */
    static final int RETENTION_MONTHS = 12;

    /** Tabelle spazzate: tutte hanno {@code executed_at} come istante dell'evento. */
    private static final List<String> TABLES = List.of(
            "platform.gdpr_purge_audit",
            "platform.gdpr_restriction_audit",
            "platform.app_status_audit",
            "platform.active_account_audit");

    @Inject
    AgroalDataSource ds;

    @Scheduled(every = "1h", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void run() {
        sweep(Instant.now());
    }

    /** Elimina le righe di audit oltre conservazione rispetto a {@code now}; ritorna quante ne ha eliminate. */
    public int sweep(Instant now) {
        Instant cutoff = now.atZone(ZoneOffset.UTC).minusMonths(RETENTION_MONTHS).toInstant();
        try (Connection c = ds.getConnection()) {
            int total = 0;
            StringBuilder detail = new StringBuilder();
            for (String table : TABLES) {
                int deleted = deleteBefore(c, table, cutoff);
                total += deleted;
                detail.append(' ').append(table).append('=').append(deleted);
            }
            if (total > 0) {
                LOG.infof("audit-retention.purge%s before=%s", detail, cutoff);
            }
            return total;
        } catch (SQLException e) {
            throw new RuntimeException("conservazione delle prove di audit fallita", e);
        }
    }

    private static int deleteBefore(Connection c, String table, Instant cutoff) throws SQLException {
        // `table` è un letterale di questa classe: nessun input esterno entra nel SQL.
        try (PreparedStatement ps = c.prepareStatement("delete from " + table + " where executed_at <= ?")) {
            ps.setTimestamp(1, Timestamp.from(cutoff));
            return ps.executeUpdate();
        }
    }
}
