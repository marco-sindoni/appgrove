package app.appgrove.core.gdpr;

import app.appgrove.commons.gdpr.AppDataContract;
import app.appgrove.commons.gdpr.DataManifest;
import app.appgrove.commons.gdpr.DataManifests;
import app.appgrove.commons.gdpr.ExportResult;
import app.appgrove.commons.gdpr.GdprScope;
import app.appgrove.commons.gdpr.PurgeResult;
import app.appgrove.core.platform.Account;
import app.appgrove.core.platform.Invitation;
import app.appgrove.core.platform.User;
import app.appgrove.core.support.SupportTicket;
import app.appgrove.core.support.SupportTicketMessage;
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
import java.util.UUID;

/**
 * Contratto GDPR della <b>piattaforma</b> (core, UC 0032): i dati personali "di piattaforma" del
 * tenant — account, utenti, inviti. Come {@code FattureDataContract}: JDBC diretto (gira senza JWT,
 * orchestrato via coda), filtro tenant <b>esplicito</b>, manifesto derivato da {@code @PersonalData}.
 *
 * <p>L'export include anche le righe soft-deleted (JDBC bypassa {@code @SQLRestriction}): art. 15 =
 * tutto ciò che è conservato. La purge è <b>fisica</b> (#13 L70) e cancella anche subscription e job
 * di export del tenant (dati derivati); NON tocca {@code gdpr_purge_audit} (la prova dell'erasure,
 * senza dati personali) né {@code webhook_event} (audit di pipeline dichiarato no-PII, UC 0025).
 */
@ApplicationScoped
public class PlatformDataContract implements AppDataContract {

    public static final String APP_ID = "platform";

    @Inject
    AgroalDataSource ds;

    @Override
    public String appId() {
        return APP_ID;
    }

    @Override
    public ExportResult exportData(GdprScope scope) {
        Map<String, List<Map<String, Object>>> entities = new LinkedHashMap<>();
        List<String> steps = List.of(
                "Raccolta account", "Raccolta utenti", "Raccolta inviti", "Raccolta ticket di supporto");

        entities.put("accounts", query(
                "select id, name, status, paddle_customer_id, created_at"
                        + " from platform.accounts where id = ?",
                UUID.fromString(scope.tenantId()),
                "id", "name", "status", "paddle_customer_id", "created_at"));

        entities.put("users", query(
                "select id, cognito_sub, email, display_name, role, status, created_at"
                        + " from platform.users where tenant_id = ? order by email",
                scope.tenantId(),
                "id", "cognito_sub", "email", "display_name", "role", "status", "created_at"));

        entities.put("invitations", query(
                "select id, email, role, status, expires_at, created_at"
                        + " from platform.invitations where tenant_id = ? order by email",
                scope.tenantId(),
                "id", "email", "role", "status", "expires_at", "created_at"));

        entities.put("support_tickets", query(
                "select id, type, subject, priority, status, due_at, created_at, closed_at"
                        + " from platform.support_ticket where tenant_id = ? order by created_at",
                scope.tenantId(),
                "id", "type", "subject", "priority", "status", "due_at", "created_at", "closed_at"));

        entities.put("support_ticket_messages", query(
                "select id, ticket_id, author, body, created_at"
                        + " from platform.support_ticket_message where tenant_id = ? order by created_at",
                scope.tenantId(),
                "id", "ticket_id", "author", "body", "created_at"));

        return new ExportResult(APP_ID, steps, entities);
    }

    @Override
    public PurgeResult purgeData(GdprScope scope) {
        // Ordine FK-safe: item → job, poi inviti/subscription/utenti, infine l'account (radice).
        // Cancellazione FISICA (erasure #13 L70), atomica sulla singola connessione.
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            Map<String, Integer> deleted = new LinkedHashMap<>();
            // i ticket referenziano gdpr_export_job (auto-ticket): vanno via prima dei job
            deleted.put("support_ticket_message",
                    delete(c, "delete from platform.support_ticket_message where tenant_id = ?", scope.tenantId()));
            deleted.put("support_ticket",
                    delete(c, "delete from platform.support_ticket where tenant_id = ?", scope.tenantId()));
            deleted.put("gdpr_export_job_item",
                    delete(c, "delete from platform.gdpr_export_job_item where tenant_id = ?", scope.tenantId()));
            deleted.put("gdpr_export_job",
                    delete(c, "delete from platform.gdpr_export_job where tenant_id = ?", scope.tenantId()));
            deleted.put("invitations",
                    delete(c, "delete from platform.invitations where tenant_id = ?", scope.tenantId()));
            deleted.put("subscription",
                    delete(c, "delete from platform.subscription where tenant_id = ?", scope.tenantId()));
            deleted.put("users",
                    delete(c, "delete from platform.users where tenant_id = ?", scope.tenantId()));
            deleted.put("accounts",
                    delete(c, "delete from platform.accounts where id = ?", UUID.fromString(scope.tenantId())));
            c.commit();
            return new PurgeResult(APP_ID, deleted);
        } catch (SQLException e) {
            throw new RuntimeException("purge piattaforma fallita per il tenant " + scope.tenantId(), e);
        }
    }

    @Override
    public DataManifest manifest() {
        List<DataManifest.Entry> entries = new ArrayList<>();
        DataManifests.collectPersonalData(Account.class, "accounts", entries);
        DataManifests.collectPersonalData(User.class, "users", entries);
        DataManifests.collectPersonalData(Invitation.class, "invitations", entries);
        DataManifests.collectPersonalData(SupportTicket.class, "support_tickets", entries);
        DataManifests.collectPersonalData(SupportTicketMessage.class, "support_ticket_messages", entries);
        return new DataManifest(APP_ID, entries);
    }

    private static int delete(Connection c, String sql, Object tenantParam) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, tenantParam);
            return ps.executeUpdate();
        }
    }

    private List<Map<String, Object>> query(String sql, Object tenantParam, String... columns) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, tenantParam);
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
            throw new RuntimeException("export piattaforma fallito per il tenant " + tenantParam, e);
        }
    }
}
