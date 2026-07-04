package app.appgrove.commons.gdpr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Registra l'audit della purge (la <b>prova</b> dell'erasure, #13 L70): una riga per invocazione di
 * {@code purgeData} con i conteggi cancellati per entità. Scrive via JDBC diretto (il consumer gira
 * senza JWT, come {@code SubscriptionWriter}); la tabella — nello schema del servizio — è indicata
 * da {@code appgrove.gdpr.audit-table} (es. {@code platform.gdpr_purge_audit}). L'audit contiene
 * solo identificativi e conteggi, nessun dato personale; sopravvive alla purge (retention audit #08).
 */
@ApplicationScoped
public class GdprPurgeAuditWriter {

    /** Identificatore SQL qualificato (schema.tabella): la config non può iniettare SQL arbitrario. */
    private static final Pattern TABLE_NAME = Pattern.compile("[a-z_][a-z0-9_]*(\\.[a-z_][a-z0-9_]*)?");

    @Inject
    AgroalDataSource ds;

    @Inject
    ObjectMapper mapper;

    @ConfigProperty(name = "appgrove.gdpr.audit-table")
    Optional<String> auditTable;

    public void record(String tenantId, String reason, PurgeResult result) {
        String table = auditTable.orElseThrow(() -> new IllegalStateException(
                "config appgrove.gdpr.audit-table mancante: il servizio consuma la coda purge ma non ha la tabella di audit"));
        if (!TABLE_NAME.matcher(table).matches()) {
            throw new IllegalStateException("appgrove.gdpr.audit-table non è un identificatore valido: " + table);
        }
        String deletedJson;
        try {
            deletedJson = mapper.writeValueAsString(result.deletedByEntity());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serializzazione audit purge fallita", e);
        }
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement("insert into " + table
                        + " (id, tenant_id, app_id, reason, deleted_by_entity, total, executed_at)"
                        + " values (?,?,?,?,?::jsonb,?,?)")) {
            ps.setObject(1, UUID.randomUUID());
            ps.setString(2, tenantId);
            ps.setString(3, result.appId());
            ps.setString(4, reason);
            ps.setString(5, deletedJson);
            ps.setInt(6, result.total());
            ps.setObject(7, OffsetDateTime.now());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("scrittura audit purge fallita per il tenant " + tenantId, e);
        }
    }
}
