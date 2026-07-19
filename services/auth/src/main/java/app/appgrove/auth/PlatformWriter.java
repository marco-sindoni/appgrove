package app.appgrove.auth;

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
 * Scritture nello schema {@code platform} al signup/accept: crea account+owner e utenti invitati.
 * Condiviso tra i provider (il {@code sub} lo fornisce il chiamante: Cognito sub in cloud,
 * {@code local-*} in dev). JDBC diretto (pre-tenant, niente discriminator); {@code tenant_id}
 * = account id, sempre esplicito (invariante #1).
 */
@ApplicationScoped
public class PlatformWriter {

    @Inject
    AgroalDataSource ds;

    /** Signup: crea un nuovo account (tenant) + utente owner. {@code sub} = identità del provider. */
    public CreatedUser createAccountWithOwner(String sub, String email, String displayName, String locale) {
        UUID accountId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String name = displayName != null && !displayName.isBlank() ? displayName : email;
        String lang = Locales.normalize(locale);
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                exec(c, "insert into platform.accounts(id, name, status, created_at, updated_at, created_by) "
                                + "values (?, ?, 'active', now(), now(), 'auth')",
                        ps -> {
                            ps.setObject(1, accountId);
                            ps.setString(2, name);
                        });
                insertUser(c, userId, accountId.toString(), sub, email, displayName, "owner", lang);
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
            return new CreatedUser(
                    userId, new AuthUser(sub, accountId.toString(), "owner", "active", email, displayName, lang));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Accept invito: crea l'utente nel tenant invitante col ruolo dell'invito. */
    public CreatedUser createUserInTenant(
            String sub, String tenantId, String email, String displayName, String role, String locale) {
        UUID userId = UUID.randomUUID();
        String lang = Locales.normalize(locale);
        try (Connection c = ds.getConnection()) {
            insertUser(c, userId, tenantId, sub, email, displayName, role, lang);
            return new CreatedUser(userId, new AuthUser(sub, tenantId, role, "active", email, displayName, lang));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Lingua dell'utente per le email transazionali (UC 0018). Usata dai flussi che partono da un
     * solo indirizzo (rinvio verifica, password dimenticata), dove non c'è altro contesto.
     *
     * <p>Ritorna sempre una lingua supportata: un indirizzo sconosciuto dà {@code en}, come un
     * utente senza preferenza. Non distinguere i due casi è deliberato — questi flussi rispondono in
     * modo neutro per non rivelare se un indirizzo è registrato.
     */
    public String localeOf(String email) {
        String sql = "select locale from platform.users where lower(email) = lower(?) and deleted_at is null";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return Locales.normalize(rs.next() ? rs.getString("locale") : null);
            }
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
            String displayName, String role, String locale) throws SQLException {
        exec(c, "insert into platform.users(id, tenant_id, cognito_sub, email, display_name, role, locale, status, "
                        + "created_at, updated_at, created_by) "
                        + "values (?, ?, ?, ?, ?, ?, ?, 'active', now(), now(), 'auth')",
                ps -> {
                    ps.setObject(1, userId);
                    ps.setString(2, tenantId);
                    ps.setString(3, sub);
                    ps.setString(4, email);
                    ps.setString(5, displayName);
                    ps.setString(6, role);
                    ps.setString(7, locale);
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
