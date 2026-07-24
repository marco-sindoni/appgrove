package app.appgrove.core.billing;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Proiezione in core dell'<b>uso a giacenza</b> riportato dalle app (UC 0054, tabella
 * {@code platform.app_usage_stock}). È la controparte, nel verso app → core, della proiezione locale
 * degli entitlement: core NON conosce i posti occupati di un'app, li riceve per evento e li conserva
 * qui per il solo scopo di alimentare il gate del downgrade ({@link TierChangePolicy}).
 *
 * <p>Gira <b>fuori</b> da una richiesta autenticata (consumer di coda) o dentro una mutazione già
 * validata: usa SQL nativo con {@code tenant_id} esplicito, mai da input client, come gli altri
 * componenti di billing che scrivono senza JWT.
 */
@ApplicationScoped
public class AppUsageStore {

    private static final String UPSERT =
            "insert into platform.app_usage_stock (app_slug, tenant_id, metric, value, reported_at, updated_at)"
                    + " values (?, ?, ?, ?, ?, now())"
                    + " on conflict (app_slug, tenant_id, metric) do update"
                    + "   set value = excluded.value, reported_at = excluded.reported_at, updated_at = now()"
                    // Un report più vecchio dell'ultimo applicato non deve sovrascriverlo: le consegne
                    // della coda non sono ordinate, e una misura arretrata "riesumerebbe" una giacenza
                    // superata. A parità di istante vince l'ultimo arrivato (idempotente).
                    + " where excluded.reported_at >= platform.app_usage_stock.reported_at";

    private static final String READ_BY_APP_TENANT =
            "select metric, value from platform.app_usage_stock where app_slug = ? and tenant_id = ?";

    @Inject
    AgroalDataSource ds;

    /** Applica un report d'uso (upsert), ignorando i report più vecchi dell'ultimo noto. */
    public void record(String appSlug, String tenantId, String metric, long value, Instant reportedAt) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(UPSERT)) {
            ps.setString(1, appSlug);
            ps.setString(2, tenantId);
            ps.setString(3, metric);
            ps.setLong(4, Math.max(0, value));
            ps.setTimestamp(5, Timestamp.from(reportedAt));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "app_usage_stock upsert fallito app=" + appSlug + " tenant=" + tenantId, e);
        }
    }

    /** Uso corrente {@code metrica → giacenza} del tenant per l'app; vuoto se nessun report è arrivato. */
    public Map<String, Long> usageFor(String appSlug, String tenantId) {
        Map<String, Long> out = new LinkedHashMap<>();
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(READ_BY_APP_TENANT)) {
            ps.setString(1, appSlug);
            ps.setString(2, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.put(rs.getString(1), rs.getLong(2));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "app_usage_stock lettura fallita app=" + appSlug + " tenant=" + tenantId, e);
        }
        return out;
    }
}
