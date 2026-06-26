package app.appgrove.authlocal;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Scritture nello schema {@code platform} che in prod farebbero Cognito + il core al signup/accept:
 * crea account+owner e utenti invitati. JDBC diretto (pre-tenant, niente discriminator); {@code tenant_id}
 * = account id, sempre esplicito (invariante #1). {@code cognito_sub} generato localmente.
 */
@ApplicationScoped
public class PlatformWriter {

    @Inject
    AgroalDataSource ds;

    /** Signup: crea un nuovo account (tenant) + utente owner. */
    public CreatedUser createAccountWithOwner(String email, String displayName) {
        UUID accountId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String sub = "local-" + UUID.randomUUID();
        String name = displayName != null && !displayName.isBlank() ? displayName : email;
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                exec(c, "insert into platform.accounts(id, name, status, created_at, updated_at, created_by) "
                                + "values (?, ?, 'active', now(), now(), 'auth-local')",
                        ps -> {
                            ps.setObject(1, accountId);
                            ps.setString(2, name);
                        });
                insertUser(c, userId, accountId.toString(), sub, email, displayName, "owner");
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
            return new CreatedUser(userId, new AuthUser(sub, accountId.toString(), "owner", "active", email, displayName));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Accept invito: crea l'utente nel tenant invitante col ruolo dell'invito. */
    public CreatedUser createUserInTenant(String tenantId, String email, String displayName, String role) {
        UUID userId = UUID.randomUUID();
        String sub = "local-" + UUID.randomUUID();
        try (Connection c = ds.getConnection()) {
            insertUser(c, userId, tenantId, sub, email, displayName, role);
            return new CreatedUser(userId, new AuthUser(sub, tenantId, role, "active", email, displayName));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<InviteRow> findInvitationByTokenHash(String tokenHash) {
        String sql = "select id, tenant_id, email, role, status, expires_at "
                + "from platform.invitations where token_hash = ? and deleted_at is null";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tokenHash);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new InviteRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("tenant_id"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getString("status"),
                        rs.getTimestamp("expires_at").toInstant()));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void markInvitationAccepted(UUID invitationId, UUID acceptedUserId) {
        String sql = "update platform.invitations set status = 'accepted', accepted_user_id = ?, updated_at = now() "
                + "where id = ?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, acceptedUserId);
            ps.setObject(2, invitationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void insertUser(Connection c, UUID userId, String tenantId, String sub, String email,
            String displayName, String role) throws SQLException {
        exec(c, "insert into platform.users(id, tenant_id, cognito_sub, email, display_name, role, status, "
                        + "created_at, updated_at, created_by) values (?, ?, ?, ?, ?, ?, 'active', now(), now(), 'auth-local')",
                ps -> {
                    ps.setObject(1, userId);
                    ps.setString(2, tenantId);
                    ps.setString(3, sub);
                    ps.setString(4, email);
                    ps.setString(5, displayName);
                    ps.setString(6, role);
                });
    }

    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private void exec(Connection c, String sql, Binder binder) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            ps.executeUpdate();
        }
    }

    public record CreatedUser(UUID id, AuthUser user) {}

    public record InviteRow(UUID id, String tenantId, String email, String role, String status, Instant expiresAt) {
        public boolean isPending() {
            return "pending".equals(status);
        }

        public boolean isExpired(Instant now) {
            return expiresAt != null && expiresAt.isBefore(now);
        }
    }
}
