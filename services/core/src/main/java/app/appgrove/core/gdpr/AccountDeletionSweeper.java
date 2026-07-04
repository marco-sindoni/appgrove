package app.appgrove.core.gdpr;

import app.appgrove.core.platform.Account;
import io.quarkus.scheduler.Scheduled;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * Job schedulato della grace di eliminazione account (UC 0033, #13 E25): alla scadenza dei 14
 * giorni invoca l'orchestrazione di offboarding della change 0028
 * ({@link TenantOffboarding#offboard}) e marca l'account come eliminato (soft-delete) così lo
 * sweep non lo riprocessa; la rimozione <b>fisica</b> della riga account la fa la purge di
 * piattaforma via coda. Gira fuori da una richiesta autenticata → JDBC con tenant esplicito
 * (come {@code TenantOffboarding}: il resolver Hibernate è fail-closed senza JWT).
 *
 * <p>In locale/test gira sullo scheduler applicativo Quarkus; il trigger cloud (EventBridge/cron)
 * è di UC 0035 (vedi "Punti aperti" di quel use case).
 */
@ApplicationScoped
public class AccountDeletionSweeper {

    private static final Logger LOG = Logger.getLogger(AccountDeletionSweeper.class);

    @Inject
    AgroalDataSource ds;

    @Inject
    TenantOffboarding offboarding;

    @Scheduled(every = "1h", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void run() {
        sweep(Instant.now());
    }

    /**
     * Offboarda gli account la cui grace è scaduta rispetto a {@code now} (iniettabile nei test:
     * niente attese reali, si retrodata la richiesta o si passa un "adesso" futuro).
     * Ritorna i tenant offboardati.
     */
    public List<String> sweep(Instant now) {
        Instant cutoff = now.minus(Account.DELETION_GRACE);
        List<String> expired = expiredTenants(cutoff);
        for (String tenantId : expired) {
            offboarding.offboard(tenantId, "account-deletion-grace-expired");
            markDeleted(tenantId, now);
            LOG.infof("gdpr.account-deletion.purge tenant_id=%s requested_before=%s", tenantId, cutoff);
        }
        return expired;
    }

    private List<String> expiredTenants(Instant cutoff) {
        List<String> tenants = new ArrayList<>();
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "select id from platform.accounts where status = 'pending_deletion'"
                                + " and deletion_requested_at <= ? and deleted_at is null order by id")) {
            ps.setTimestamp(1, Timestamp.from(cutoff));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tenants.add(rs.getString(1));
                }
            }
            return tenants;
        } catch (SQLException e) {
            throw new RuntimeException("lettura account con grace scaduta fallita", e);
        }
    }

    private void markDeleted(String tenantId, Instant now) {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "update platform.accounts set deleted_at = ?, updated_at = ? where id = ?::uuid")) {
            ps.setTimestamp(1, Timestamp.from(now));
            ps.setTimestamp(2, Timestamp.from(now));
            ps.setString(3, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("soft-delete account fallita per il tenant " + tenantId, e);
        }
    }
}
