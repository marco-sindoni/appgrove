package app.appgrove.core.gdpr;

import app.appgrove.commons.audit.AuditLogger;
import app.appgrove.commons.gdpr.GdprQueues;
import app.appgrove.commons.gdpr.TenantPurgeMessage;
import app.appgrove.commons.messaging.MessageQueues;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;

/**
 * Orchestrazione account-level dell'erasure (#13 L71): eliminazione account = purge della
 * piattaforma <b>+</b> {@code purgeData} di ogni app attivata. Pubblica l'evento
 * {@code tenant.offboarded} come messaggio sulle code {@code tenant-purge-<app_id>} di tutti i
 * servizi coinvolti (core compreso: anche la purge piattaforma passa dalla sua coda → un solo
 * code path, audit uniforme). In locale il fan-out è diretto sulle code (ElasticMQ); nel cloud la
 * stessa semantica è la regola EventBridge del bus (#06 H-19, UC 0004).
 *
 * <p>È il punto d'ingresso che UC 0035 invocherà allo scadere della grace di 14 giorni (#13 E25) e
 * che UC 0033/0034 esporranno all'utente/console. Gira anche <b>fuori</b> da una richiesta
 * autenticata (job schedulato) → lettura delle app attivate via JDBC con tenant esplicito.
 */
@ApplicationScoped
public class TenantOffboarding {

    private static final Logger LOG = Logger.getLogger(TenantOffboarding.class);

    @Inject
    AgroalDataSource ds;

    @Inject
    MessageQueues queues;

    @Inject
    ObjectMapper mapper;

    @Inject
    AuditLogger audit;

    /**
     * Avvia l'offboarding del tenant: un messaggio di purge per la piattaforma e per ogni app
     * attivata (incluse subscription soft-deleted: i dati dell'app possono ancora esistere).
     * Ritorna i servizi notificati (per log/test).
     */
    public List<String> offboard(String tenantId, String reason) {
        List<String> targets = new ArrayList<>();
        targets.add(PlatformDataContract.APP_ID);
        targets.addAll(activatedAppSlugs(tenantId));
        for (String target : targets) {
            queues.send(GdprQueues.purgeQueue(target), serialize(new TenantPurgeMessage(tenantId, reason)));
        }
        LOG.infof("gdpr.offboard tenant_id=%s reason=%s targets=%s", tenantId, reason, targets);
        // evento audit (UC 0006): gira anche fuori richiesta (sweeper) → tenant nei details
        audit.success("tenant.offboarded", Map.of(
                "tenant_id", tenantId,
                "reason", reason,
                "targets", String.join(",", targets)));
        return targets;
    }

    private List<String> activatedAppSlugs(String tenantId) {
        List<String> slugs = new ArrayList<>();
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "select distinct a.slug from platform.subscription s"
                                + " join platform.app a on a.id = s.app_id"
                                + " where s.tenant_id = ? order by a.slug")) {
            ps.setString(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    slugs.add(rs.getString(1));
                }
            }
            return slugs;
        } catch (SQLException e) {
            throw new RuntimeException("lettura app attivate fallita per il tenant " + tenantId, e);
        }
    }

    private String serialize(TenantPurgeMessage message) {
        try {
            return mapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serializzazione messaggio purge fallita", e);
        }
    }
}
