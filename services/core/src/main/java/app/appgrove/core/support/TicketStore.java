package app.appgrove.core.support;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * Scritture/letture dei ticket eseguite <b>fuori</b> dal tenant del chiamante: l'auto-ticket del
 * consumer risultati (gira senza JWT) e le operazioni del platform-admin (JWT del tenant
 * piattaforma, non del tenant del ticket). JDBC diretto con tenant esplicito — stesso razionale di
 * {@code GdprJobStore}; le letture cross-tenant sono l'eccezione documentata all'invariante #2,
 * ammessa solo perché gated {@code platform-admin} (pattern {@code AdminResource}).
 */
@ApplicationScoped
public class TicketStore {

    private static final Logger LOG = Logger.getLogger(TicketStore.class);

    /** Riga di ticket per la console admin (con nome account per la tabella aggregata). */
    public record TicketRow(
            UUID id,
            String tenantId,
            String accountName,
            TicketType type,
            String subject,
            TicketPriority priority,
            TicketStatus status,
            Instant dueAt,
            UUID exportJobId,
            Instant closedAt,
            String createdBy,
            Instant createdAt,
            Instant updatedAt) {}

    /** Messaggio del thread (vista admin). */
    public record MessageRow(UUID id, TicketAuthor author, String body, Instant createdAt) {}

    @Inject
    AgroalDataSource ds;

    /** Ticket cross-tenant, filtri opzionali su tipo/stato, più recenti prima. */
    public List<TicketRow> list(TicketType type, TicketStatus status) {
        StringBuilder sql = new StringBuilder("""
                select t.id, t.tenant_id, a.name, t.type, t.subject, t.priority, t.status,
                       t.due_at, t.export_job_id, t.closed_at, t.created_by, t.created_at, t.updated_at
                from platform.support_ticket t
                left join platform.accounts a on a.id::text = t.tenant_id
                where t.deleted_at is null
                """);
        List<Object> params = new ArrayList<>();
        if (type != null) {
            sql.append(" and t.type = ?");
            params.add(type.name());
        }
        if (status != null) {
            sql.append(" and t.status = ?");
            params.add(status.name());
        }
        sql.append(" order by t.created_at desc");
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<TicketRow> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(row(rs));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new RuntimeException("lettura ticket admin fallita", e);
        }
    }

    /** Ticket per id, cross-tenant (vista admin). */
    public Optional<TicketRow> find(UUID id) {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement("""
                        select t.id, t.tenant_id, a.name, t.type, t.subject, t.priority, t.status,
                               t.due_at, t.export_job_id, t.closed_at, t.created_by, t.created_at, t.updated_at
                        from platform.support_ticket t
                        left join platform.accounts a on a.id::text = t.tenant_id
                        where t.id = ? and t.deleted_at is null
                        """)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(row(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("lettura ticket " + id + " fallita", e);
        }
    }

    /** Thread del ticket, in ordine cronologico (vista admin). */
    public List<MessageRow> thread(UUID ticketId) {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "select id, author, body, created_at from platform.support_ticket_message"
                                + " where ticket_id = ? and deleted_at is null order by created_at")) {
            ps.setObject(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                List<MessageRow> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(new MessageRow(
                            (UUID) rs.getObject(1),
                            TicketAuthor.valueOf(rs.getString(2)),
                            rs.getString(3),
                            instant(rs.getTimestamp(4))));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new RuntimeException("lettura thread ticket " + ticketId + " fallita", e);
        }
    }

    /** Aggiunge un messaggio al thread con il tenant del ticket (autore admin/system). */
    public MessageRow addMessage(TicketRow ticket, TicketAuthor author, String actor, String body) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "insert into platform.support_ticket_message"
                                + " (id, tenant_id, ticket_id, author, body, created_at, updated_at, created_by)"
                                + " values (?,?,?,?,?,?,?,?)")) {
            ps.setObject(1, id);
            ps.setString(2, ticket.tenantId());
            ps.setObject(3, ticket.id());
            ps.setString(4, author.name());
            ps.setString(5, body);
            ps.setObject(6, OffsetDateTime.ofInstant(now, java.time.ZoneOffset.UTC));
            ps.setObject(7, OffsetDateTime.ofInstant(now, java.time.ZoneOffset.UTC));
            ps.setString(8, actor);
            ps.executeUpdate();
            return new MessageRow(id, author, body, now);
        } catch (SQLException e) {
            throw new RuntimeException("scrittura messaggio ticket " + ticket.id() + " fallita", e);
        }
    }

    /** Aggiorna stato/priorità mantenendo coerente {@code closed_at} (decorrenza retention). */
    public void update(UUID id, TicketStatus status, TicketPriority priority, String actor, Instant now) {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "update platform.support_ticket set status = ?, priority = ?,"
                                + " closed_at = ?, updated_at = ?, updated_by = ?"
                                + " where id = ? and deleted_at is null")) {
            ps.setString(1, status.name());
            ps.setString(2, priority.name());
            ps.setTimestamp(3, status.isTerminal() ? Timestamp.from(now) : null);
            ps.setTimestamp(4, Timestamp.from(now));
            ps.setString(5, actor);
            ps.setObject(6, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("aggiornamento ticket " + id + " fallito", e);
        }
    }

    /**
     * Auto-ticket privacy per un export FAILED (#13 D21): idempotente — l'indice unico su
     * {@code export_job_id} garantisce al più un ticket per job, anche sotto redelivery o con più
     * item falliti dello stesso job. Ritorna l'id del ticket creato, vuoto se già esistente.
     */
    public Optional<UUID> createForFailedExport(UUID jobId) {
        UUID ticketId = UUID.randomUUID();
        Instant now = Instant.now();
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            String tenantId;
            String requester;
            String appId;
            String error;
            try (PreparedStatement ps = c.prepareStatement(
                    "select tenant_id, created_by, app_id, error from platform.gdpr_export_job where id = ?")) {
                ps.setObject(1, jobId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        c.rollback();
                        return Optional.empty();
                    }
                    tenantId = rs.getString(1);
                    requester = rs.getString(2);
                    appId = rs.getString(3);
                    error = rs.getString(4);
                }
            }
            int inserted;
            try (PreparedStatement ps = c.prepareStatement("""
                    insert into platform.support_ticket
                      (id, tenant_id, type, subject, priority, status, due_at, export_job_id,
                       created_at, updated_at, created_by)
                    select ?,?,?,?,?,?,?,?,?,?,?
                    where not exists (select 1 from platform.support_ticket where export_job_id = ?)
                    """)) {
                ps.setObject(1, ticketId);
                ps.setString(2, tenantId);
                ps.setString(3, TicketType.privacy.name());
                ps.setString(4, "Export GDPR fallito" + (appId == null ? "" : " (app " + appId + ")"));
                ps.setString(5, TicketPriority.high.name());
                ps.setString(6, TicketStatus.open.name());
                ps.setTimestamp(7, Timestamp.from(now.plus(SupportTicket.PRIVACY_SLA)));
                ps.setObject(8, jobId);
                ps.setTimestamp(9, Timestamp.from(now));
                ps.setTimestamp(10, Timestamp.from(now));
                ps.setString(11, requester);
                ps.setObject(12, jobId);
                inserted = ps.executeUpdate();
            }
            if (inserted == 0) {
                c.rollback();
                return Optional.empty();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "insert into platform.support_ticket_message"
                            + " (id, tenant_id, ticket_id, author, body, created_at, updated_at)"
                            + " values (?,?,?,?,?,?,?)")) {
                ps.setObject(1, UUID.randomUUID());
                ps.setString(2, tenantId);
                ps.setObject(3, ticketId);
                ps.setString(4, TicketAuthor.system.name());
                ps.setString(5, "L'export GDPR " + jobId + " è fallito"
                        + (error == null ? "." : ": " + error)
                        + " Presa in carico richiesta entro la scadenza legale (art. 12).");
                ps.setTimestamp(6, Timestamp.from(now));
                ps.setTimestamp(7, Timestamp.from(now));
                ps.executeUpdate();
            }
            c.commit();
            return Optional.of(ticketId);
        } catch (SQLException e) {
            throw new RuntimeException("auto-ticket per export fallito " + jobId + " non creato", e);
        }
    }

    /**
     * Retention #13 E: hard-delete (minimizzazione) dei ticket chiusi/risolti da oltre 24 mesi,
     * thread compreso. Ritorna quanti ticket ha eliminato.
     */
    public int sweepExpired(Instant now) {
        Instant cutoff = now.minus(SupportTicket.RETENTION);
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(
                    "delete from platform.support_ticket_message where ticket_id in"
                            + " (select id from platform.support_ticket where closed_at <= ?)")) {
                ps.setTimestamp(1, Timestamp.from(cutoff));
                ps.executeUpdate();
            }
            int deleted;
            try (PreparedStatement ps = c.prepareStatement(
                    "delete from platform.support_ticket where closed_at <= ?")) {
                ps.setTimestamp(1, Timestamp.from(cutoff));
                deleted = ps.executeUpdate();
            }
            c.commit();
            if (deleted > 0) {
                LOG.infof("ticket.retention-sweep deleted=%d cutoff=%s", deleted, cutoff);
            }
            return deleted;
        } catch (SQLException e) {
            throw new RuntimeException("sweep retention ticket fallito", e);
        }
    }

    private TicketRow row(ResultSet rs) throws SQLException {
        return new TicketRow(
                (UUID) rs.getObject(1),
                rs.getString(2),
                rs.getString(3),
                TicketType.valueOf(rs.getString(4)),
                rs.getString(5),
                TicketPriority.valueOf(rs.getString(6)),
                TicketStatus.valueOf(rs.getString(7)),
                instant(rs.getTimestamp(8)),
                (UUID) rs.getObject(9),
                instant(rs.getTimestamp(10)),
                rs.getString(11),
                instant(rs.getTimestamp(12)),
                instant(rs.getTimestamp(13)));
    }

    private static Instant instant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }
}
