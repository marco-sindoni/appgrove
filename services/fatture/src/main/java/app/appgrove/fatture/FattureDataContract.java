package app.appgrove.fatture;

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
 * Contratto GDPR dell'app fatture (UC 0051). Export ed erasure operano su <b>tenant esplicito</b>
 * ({@link GdprScope}): sono orchestrati dal core (UC 0032) <b>fuori</b> da una richiesta utente
 * (es. EventBridge purge), dove non c'è JWT. Per questo usano <b>JDBC diretto</b> (Agroal),
 * bypassando Hibernate e il {@code TenantResolver} (fail-closed senza token) — stesso razionale di
 * {@code TestData} nel core. Il filtro per {@code tenant_id} è esplicito.
 * <p>Il manifesto è <b>derivato</b> dalle annotazioni {@link PersonalData}: unica sorgente di verità.
 */
@ApplicationScoped
public class FattureDataContract implements AppDataContract {

    public static final String APP_ID = "fatture";

    @Inject
    AgroalDataSource ds;

    @Override
    public String appId() {
        return APP_ID;
    }

    @Override
    public ExportResult exportData(GdprScope scope) {
        Map<String, List<Map<String, Object>>> entities = new LinkedHashMap<>();
        List<String> steps = List.of("Raccolta fatture", "Raccolta righe fattura");

        entities.put("invoice", query(
                "select id, number, customer_name, customer_email, issue_date, status, currency, total_amount, created_at"
                        + " from app_fatture.invoice where tenant_id = ? order by number",
                scope.tenantId(),
                "id", "number", "customer_name", "customer_email", "issue_date",
                "status", "currency", "total_amount", "created_at"));

        entities.put("invoice_line", query(
                "select id, invoice_id, description, quantity, unit_amount, line_amount"
                        + " from app_fatture.invoice_line where tenant_id = ? order by invoice_id",
                scope.tenantId(),
                "id", "invoice_id", "description", "quantity", "unit_amount", "line_amount"));

        return new ExportResult(APP_ID, steps, entities);
    }

    @Override
    public PurgeResult purgeData(GdprScope scope) {
        // Ordine FK-safe: prima le righe, poi le fatture. Cancellazione FISICA (erasure #13 L70),
        // atomica sulla singola connessione.
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            int lines;
            int invoices;
            try (PreparedStatement ps = c.prepareStatement(
                    "delete from app_fatture.invoice_line where tenant_id = ?")) {
                ps.setString(1, scope.tenantId());
                lines = ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "delete from app_fatture.invoice where tenant_id = ?")) {
                ps.setString(1, scope.tenantId());
                invoices = ps.executeUpdate();
            }
            c.commit();
            Map<String, Integer> deleted = new LinkedHashMap<>();
            deleted.put("invoice_line", lines);
            deleted.put("invoice", invoices);
            return new PurgeResult(APP_ID, deleted);
        } catch (SQLException e) {
            throw new RuntimeException("purge fatture fallita per il tenant " + scope.tenantId(), e);
        }
    }

    @Override
    public DataManifest manifest() {
        List<DataManifest.Entry> entries = new ArrayList<>();
        DataManifests.collectPersonalData(Invoice.class, "invoice", entries);
        DataManifests.collectPersonalData(InvoiceLine.class, "invoice_line", entries);
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
            throw new RuntimeException("export fatture fallito per il tenant " + tenantId, e);
        }
    }
}
