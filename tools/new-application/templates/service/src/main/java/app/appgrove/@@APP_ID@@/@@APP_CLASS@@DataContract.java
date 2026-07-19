package app.appgrove.@@APP_ID@@;

import app.appgrove.commons.gdpr.AppDataContract;
import app.appgrove.commons.gdpr.DataManifest;
import app.appgrove.commons.gdpr.DataManifests;
import app.appgrove.commons.gdpr.ExportResult;
import app.appgrove.commons.gdpr.GdprScope;
import app.appgrove.commons.gdpr.PurgeResult;
import app.appgrove.commons.privacy.PersonalData;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Contratto GDPR dell'app @@APP_NAME@@ ("no contratto = no produzione", #13 L74). Export ed erasure
 * operano su <b>tenant esplicito</b> ({@link GdprScope}): sono orchestrati dal core (UC 0032)
 * <b>fuori</b> da una richiesta utente (es. EventBridge purge), dove non c'è JWT. Per questo usano
 * <b>JDBC diretto</b> (Agroal), bypassando Hibernate e il {@code TenantResolver} (fail-closed senza
 * token). Il filtro per {@code tenant_id} è esplicito, mai implicito.
 *
 * <p>La cancellazione è <b>fisica</b>: pseudonimizzare e chiamarla cancellazione non soddisfa il
 * diritto all'oblio. Se una parte dei dati deve sopravvivere per obbligo di legge, va dichiarata nel
 * manifesto con la sua base giuridica, non nascosta dentro una finta anonimizzazione.
 *
 * <p>Il manifesto è <b>derivato</b> dalle annotazioni {@link PersonalData}: unica sorgente di verità.
 */
@ApplicationScoped
public class @@APP_CLASS@@DataContract implements AppDataContract {

    public static final String APP_ID = "@@APP_ID@@";

    @Inject
    AgroalDataSource ds;

    @Override
    public String appId() {
        return APP_ID;
    }

    @Override
    public ExportResult exportData(GdprScope scope) {
        Map<String, List<Map<String, Object>>> entities = new LinkedHashMap<>();
        List<String> steps = List.of("Raccolta record", "Raccolta righe");

        entities.put("item", query(
                "select id, code, contact_name, contact_email, recorded_on, status, currency, total_amount, created_at"
                        + " from @@SCHEMA@@.item where tenant_id = ? order by code",
                scope.tenantId(),
                "id", "code", "contact_name", "contact_email", "recorded_on",
                "status", "currency", "total_amount", "created_at"));

        entities.put("item_line", query(
                "select id, item_id, description, quantity, unit_amount, line_amount"
                        + " from @@SCHEMA@@.item_line where tenant_id = ? order by item_id",
                scope.tenantId(),
                "id", "item_id", "description", "quantity", "unit_amount", "line_amount"));

        return new ExportResult(APP_ID, steps, entities);
    }

    @Override
    public PurgeResult purgeData(GdprScope scope) {
        // Ordine FK-safe: prima le righe, poi i record. Cancellazione FISICA (erasure #13 L70),
        // atomica sulla singola connessione.
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            int lines;
            int items;
            try (PreparedStatement ps = c.prepareStatement(
                    "delete from @@SCHEMA@@.item_line where tenant_id = ?")) {
                ps.setString(1, scope.tenantId());
                lines = ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "delete from @@SCHEMA@@.item where tenant_id = ?")) {
                ps.setString(1, scope.tenantId());
                items = ps.executeUpdate();
            }
            c.commit();
            Map<String, Integer> deleted = new LinkedHashMap<>();
            deleted.put("item_line", lines);
            deleted.put("item", items);
            return new PurgeResult(APP_ID, deleted);
        } catch (SQLException e) {
            throw new RuntimeException("purge @@APP_ID@@ fallita per il tenant " + scope.tenantId(), e);
        }
    }

    @Override
    public DataManifest manifest() {
        List<DataManifest.Entry> entries = new ArrayList<>();
        DataManifests.collectPersonalData(Item.class, "item", entries);
        DataManifests.collectPersonalData(ItemLine.class, "item_line", entries);
        return new DataManifest(APP_ID, entries);
    }

    private List<Map<String, Object>> query(String sql, String tenantId, String... columns) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> record = new LinkedHashMap<>();
                    for (int i = 0; i < columns.length; i++) {
                        record.put(columns[i], rs.getObject(i + 1));
                    }
                    rows.add(record);
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new RuntimeException("export @@APP_ID@@ fallito per il tenant " + tenantId, e);
        }
    }
}
