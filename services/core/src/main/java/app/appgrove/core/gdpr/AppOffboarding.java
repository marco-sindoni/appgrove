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
 * Orchestrazione <b>app-level</b> dell'erasure a supporto della dismissione di un'app (skill
 * {@code drop-application}, UC 0048). È il gemello app-scoped di {@link TenantOffboarding} (che è
 * account-level): mentre quello purga <i>una</i> persona da tutte le sue app, questo purga <i>una</i>
 * app da tutti i suoi tenant.
 *
 * <p>Enumera i tenant che hanno dati nell'app (via {@code platform.subscription}, incluse le
 * subscription soft-deleted: i dati dell'app possono esistere anche dopo il recesso) e pubblica per
 * ciascuno un {@link TenantPurgeMessage} sulla coda {@code tenant-purge-<app_id>}. Da lì il percorso è
 * quello già esistente e collaudato: il {@code TenantPurgeConsumer} del servizio dell'app consuma il
 * messaggio, esegue {@code purgeData} (cancellazione fisica, filtro per {@code tenant_id}), purga la
 * proiezione entitlement e scrive l'audit di prova ({@code gdpr_purge_audit}, #13 L70). Questa classe
 * non cancella nulla di persona propria: <b>orchestra</b>.
 *
 * <p>Gira <b>fuori</b> da una richiesta autenticata (comando one-shot {@code offboard-app}, nessun
 * JWT) → lettura dei tenant via JDBC con {@code tenant_id} esplicito, esattamente come
 * {@link TenantOffboarding}. L'<b>esecuzione</b> resta un atto deliberato del runbook di dismissione,
 * non un passo della skill (change 0043, decisione 4/6): la skill produce il branch, questo comando
 * viene lanciato a mano/CI quando la dismissione è confermata.
 */
@ApplicationScoped
public class AppOffboarding {

    private static final Logger LOG = Logger.getLogger(AppOffboarding.class);

    /** Causale dell'evento di purge per la dismissione di un'app (distinta da {@code tenant.offboarded}). */
    public static final String REASON_APP_OFFBOARDED = "app.offboarded";

    @Inject
    AgroalDataSource ds;

    @Inject
    MessageQueues queues;

    @Inject
    ObjectMapper mapper;

    @Inject
    AuditLogger audit;

    /**
     * Avvia l'offboarding dell'app: un messaggio di purge sulla coda {@code tenant-purge-<app_id>} per
     * ogni tenant con dati nell'app. Ritorna i tenant notificati (per log/test), in ordine stabile.
     */
    public List<String> offboardApp(String appId, String reason) {
        List<String> tenants = tenantsWithData(appId);
        for (String tenantId : tenants) {
            queues.send(GdprQueues.purgeQueue(appId), serialize(new TenantPurgeMessage(tenantId, reason)));
        }
        LOG.infof("gdpr.offboardApp app_id=%s reason=%s tenants=%d", appId, reason, tenants.size());
        // evento audit (UC 0006): gira fuori richiesta (comando one-shot) → app_id e conteggio nei details
        audit.success("app.offboarded", Map.of(
                "app_id", appId,
                "reason", reason,
                "tenants", String.valueOf(tenants.size())));
        return tenants;
    }

    /**
     * Tenant che hanno (o hanno avuto) dati nell'app, dedotti dalle subscription. Nessun filtro su
     * {@code deleted_at}: una subscription cancellata o soft-deleted non implica che i dati dell'app
     * siano già spariti, quindi il tenant va comunque purgato.
     */
    private List<String> tenantsWithData(String appId) {
        List<String> tenants = new ArrayList<>();
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "select distinct s.tenant_id from platform.subscription s"
                                + " join platform.app a on a.id = s.app_id"
                                + " where a.slug = ? order by s.tenant_id")) {
            ps.setString(1, appId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tenants.add(rs.getString(1));
                }
            }
            return tenants;
        } catch (SQLException e) {
            throw new RuntimeException("lettura dei tenant dell'app fallita per " + appId, e);
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
