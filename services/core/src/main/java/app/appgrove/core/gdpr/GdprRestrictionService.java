package app.appgrove.core.gdpr;

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
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * Limitazione del trattamento (art. 18, UC 0034, #13 D19): applica/rimuove la <b>sospensione</b>
 * di un account o di un utente con causale dedicata {@code gdpr_restriction} — riusa la meccanica
 * di sospensione esistente ({@code status = suspended}) ma la distingue da una sospensione
 * amministrativa, così la rimozione ripristina solo ciò che la limitazione ha sospeso. Ogni
 * applica/rimuovi lascia la <b>prova di evasione</b> in {@code gdpr_restriction_audit} (#13 L75).
 * JDBC con tenant esplicito: opera il platform-admin, fuori dal tenant bersaglio (eccezione
 * documentata all'invariante #2, gated {@code platform-admin} nella risorsa chiamante).
 */
@ApplicationScoped
public class GdprRestrictionService {

    /** Causale di sospensione della limitazione art. 18. */
    public static final String RESTRICTION_REASON = "gdpr_restriction";

    private static final Logger LOG = Logger.getLogger(GdprRestrictionService.class);

    /** Bersaglio della limitazione. */
    public enum TargetKind {
        account,
        user
    }

    /** Esito di applica/rimuovi. */
    public enum Outcome {
        APPLIED,
        REMOVED,
        /** Bersaglio inesistente. */
        NOT_FOUND,
        /** Stato incompatibile: già limitato, sospeso per altra causale, o non limitato alla rimozione. */
        CONFLICT
    }

    /** Limitazione attiva (per la console): bersaglio + etichetta leggibile. */
    public record RestrictionView(TargetKind targetKind, UUID targetId, String tenantId, String label) {}

    /** Riga del registro prove (applica/rimuovi con attore e ticket collegato). */
    public record RestrictionAuditView(
            UUID id,
            String tenantId,
            TargetKind targetKind,
            String targetId,
            String action,
            UUID ticketId,
            String actor,
            String note,
            Instant executedAt) {}

    @Inject
    AgroalDataSource ds;

    /** Applica la limitazione: sospende il bersaglio con causale dedicata + prova nell'audit. */
    public Outcome apply(TargetKind kind, UUID targetId, UUID ticketId, String note, String actor) {
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            String tenantId = tenantOf(c, kind, targetId);
            if (tenantId == null) {
                c.rollback();
                return Outcome.NOT_FOUND;
            }
            int updated = update(c, kind, targetId,
                    "set status = 'suspended', suspended_reason = '" + RESTRICTION_REASON + "'",
                    "and status = 'active'");
            if (updated == 0) {
                c.rollback();
                return Outcome.CONFLICT;
            }
            audit(c, tenantId, kind, targetId, "applied", ticketId, note, actor);
            c.commit();
            LOG.infof("gdpr.restriction applied target=%s target_id=%s tenant_id=%s actor=%s ticket_id=%s",
                    kind, targetId, tenantId, actor, ticketId);
            return Outcome.APPLIED;
        } catch (SQLException e) {
            throw new RuntimeException("applicazione limitazione fallita per " + kind + " " + targetId, e);
        }
    }

    /** Rimuove la limitazione: solo se la sospensione ha causale {@code gdpr_restriction}. */
    public Outcome remove(TargetKind kind, UUID targetId, String note, String actor) {
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            String tenantId = tenantOf(c, kind, targetId);
            if (tenantId == null) {
                c.rollback();
                return Outcome.NOT_FOUND;
            }
            int updated = update(c, kind, targetId,
                    "set status = 'active', suspended_reason = null",
                    "and suspended_reason = '" + RESTRICTION_REASON + "'");
            if (updated == 0) {
                c.rollback();
                return Outcome.CONFLICT;
            }
            audit(c, tenantId, kind, targetId, "removed", null, note, actor);
            c.commit();
            LOG.infof("gdpr.restriction removed target=%s target_id=%s tenant_id=%s actor=%s",
                    kind, targetId, tenantId, actor);
            return Outcome.REMOVED;
        } catch (SQLException e) {
            throw new RuntimeException("rimozione limitazione fallita per " + kind + " " + targetId, e);
        }
    }

    /** Limitazioni attive (account e utenti con causale {@code gdpr_restriction}). */
    public List<RestrictionView> active() {
        List<RestrictionView> views = new ArrayList<>();
        try (Connection c = ds.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "select id, name from platform.accounts"
                            + " where suspended_reason = ? and deleted_at is null order by name");
                    var rs = bind(ps)) {
                while (rs.next()) {
                    UUID id = (UUID) rs.getObject(1);
                    views.add(new RestrictionView(TargetKind.account, id, id.toString(), rs.getString(2)));
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "select id, tenant_id, email from platform.users"
                            + " where suspended_reason = ? and deleted_at is null order by email");
                    var rs = bind(ps)) {
                while (rs.next()) {
                    views.add(new RestrictionView(
                            TargetKind.user, (UUID) rs.getObject(1), rs.getString(2), rs.getString(3)));
                }
            }
            return views;
        } catch (SQLException e) {
            throw new RuntimeException("lettura limitazioni attive fallita", e);
        }
    }

    /** Registro prove della limitazione, più recenti prima. */
    public List<RestrictionAuditView> auditTrail() {
        List<RestrictionAuditView> rows = new ArrayList<>();
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "select id, tenant_id, target_kind, target_id, action, ticket_id, actor, note, executed_at"
                                + " from platform.gdpr_restriction_audit order by executed_at desc");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new RestrictionAuditView(
                        (UUID) rs.getObject(1),
                        rs.getString(2),
                        TargetKind.valueOf(rs.getString(3)),
                        rs.getString(4),
                        rs.getString(5),
                        (UUID) rs.getObject(6),
                        rs.getString(7),
                        rs.getString(8),
                        rs.getTimestamp(9).toInstant()));
            }
            return rows;
        } catch (SQLException e) {
            throw new RuntimeException("lettura registro limitazioni fallita", e);
        }
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private ResultSet bind(PreparedStatement ps) throws SQLException {
        ps.setString(1, RESTRICTION_REASON);
        return ps.executeQuery();
    }

    /** Tenant del bersaglio (per l'audit): l'account è la radice (tenant = id), l'utente lo porta. */
    private String tenantOf(Connection c, TargetKind kind, UUID targetId) throws SQLException {
        String sql = kind == TargetKind.account
                ? "select id::text from platform.accounts where id = ? and deleted_at is null"
                : "select tenant_id from platform.users where id = ? and deleted_at is null";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, targetId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private int update(Connection c, TargetKind kind, UUID targetId, String set, String guard)
            throws SQLException {
        String table = kind == TargetKind.account ? "platform.accounts" : "platform.users";
        // set/guard sono frammenti costanti di questa classe (nessun input esterno nel SQL)
        try (PreparedStatement ps = c.prepareStatement(
                "update " + table + " " + set + ", updated_at = ? where id = ? and deleted_at is null " + guard)) {
            ps.setTimestamp(1, Timestamp.from(Instant.now()));
            ps.setObject(2, targetId);
            return ps.executeUpdate();
        }
    }

    private void audit(Connection c, String tenantId, TargetKind kind, UUID targetId, String action,
            UUID ticketId, String note, String actor) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "insert into platform.gdpr_restriction_audit"
                        + " (id, tenant_id, target_kind, target_id, action, ticket_id, actor, note, executed_at)"
                        + " values (?,?,?,?,?,?,?,?,?)")) {
            ps.setObject(1, UUID.randomUUID());
            ps.setString(2, tenantId);
            ps.setString(3, kind.name());
            ps.setString(4, targetId.toString());
            ps.setString(5, action);
            ps.setObject(6, ticketId);
            ps.setString(7, actor);
            ps.setString(8, note);
            ps.setTimestamp(9, Timestamp.from(Instant.now()));
            ps.executeUpdate();
        }
    }
}
