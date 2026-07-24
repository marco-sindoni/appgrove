package app.appgrove.crm;

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
 * Contratto GDPR dell'app Mini-CRM ("no contratto = no produzione", #13 L74). Export ed erasure
 * operano su <b>tenant esplicito</b> ({@link GdprScope}): sono orchestrati dal core (UC 0032)
 * <b>fuori</b> da una richiesta utente (es. purge schedulata), dove non c'è JWT. Per questo usano
 * <b>JDBC diretto</b> (Agroal), bypassando Hibernate e il {@code TenantResolver} (fail-closed senza
 * token). Il filtro per {@code tenant_id} è esplicito, mai implicito.
 *
 * <p>Copre tutte e tre le tabelle del dominio — {@code contact}, {@code interaction}, {@code seat} —
 * così l'esportazione e la cancellazione di un account non lasciano dati orfani. La cancellazione è
 * <b>fisica</b>: pseudonimizzare e chiamarla cancellazione non soddisfa il diritto all'oblio.
 *
 * <p>Il manifesto è <b>derivato</b> dalle annotazioni {@link PersonalData} di {@link Contact} e
 * {@link Interaction}: unica sorgente di verità. La tabella {@code seat} non porta dati personali di
 * terzi (solo l'identificativo interno di un membro del tenant, già trattato da core), quindi non ha
 * voci di manifesto, ma è comunque inclusa in export ed erasure perché è un dato del tenant.
 */
@ApplicationScoped
public class CrmDataContract implements AppDataContract {

    public static final String APP_ID = "crm";

    @Inject
    AgroalDataSource ds;

    @Override
    public String appId() {
        return APP_ID;
    }

    @Override
    public ExportResult exportData(GdprScope scope) {
        Map<String, List<Map<String, Object>>> entities = new LinkedHashMap<>();
        List<String> steps = List.of("Raccolta contatti", "Raccolta interazioni", "Raccolta posti");

        entities.put("contact", query(
                "select id, display_name, email, phone, organization, stage, notes, created_at"
                        + " from app_crm.contact where tenant_id = ? order by created_at",
                scope.tenantId(),
                "id", "display_name", "email", "phone", "organization", "stage", "notes", "created_at"));

        entities.put("interaction", query(
                "select id, contact_id, kind, occurred_on, note, created_at"
                        + " from app_crm.interaction where tenant_id = ? order by occurred_on",
                scope.tenantId(),
                "id", "contact_id", "kind", "occurred_on", "note", "created_at"));

        entities.put("seat", query(
                "select id, user_id, created_at, deleted_at from app_crm.seat where tenant_id = ? order by created_at",
                scope.tenantId(),
                "id", "user_id", "created_at", "deleted_at"));

        return new ExportResult(APP_ID, steps, entities);
    }

    @Override
    public PurgeResult purgeData(GdprScope scope) {
        // Ordine FK-safe: prima le interazioni (figlie), poi i contatti; i posti sono indipendenti.
        // Cancellazione FISICA (erasure #13 L70), atomica sulla singola connessione.
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            int interactions = delete(c, "delete from app_crm.interaction where tenant_id = ?", scope.tenantId());
            int contacts = delete(c, "delete from app_crm.contact where tenant_id = ?", scope.tenantId());
            int seats = delete(c, "delete from app_crm.seat where tenant_id = ?", scope.tenantId());
            c.commit();
            Map<String, Integer> deleted = new LinkedHashMap<>();
            deleted.put("interaction", interactions);
            deleted.put("contact", contacts);
            deleted.put("seat", seats);
            return new PurgeResult(APP_ID, deleted);
        } catch (SQLException e) {
            throw new RuntimeException("purge crm fallita per il tenant " + scope.tenantId(), e);
        }
    }

    @Override
    public DataManifest manifest() {
        List<DataManifest.Entry> entries = new ArrayList<>();
        DataManifests.collectPersonalData(Contact.class, "contact", entries);
        DataManifests.collectPersonalData(Interaction.class, "interaction", entries);
        return new DataManifest(APP_ID, entries);
    }

    private static int delete(Connection c, String sql, String tenantId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            return ps.executeUpdate();
        }
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
            throw new RuntimeException("export crm fallito per il tenant " + tenantId, e);
        }
    }
}
