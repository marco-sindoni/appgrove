package app.appgrove.auth.local;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** Stato credenziali/2FA dev-only (schema {@code auth_local}). Chiave = {@code cognito_sub}. */
@ApplicationScoped
public class CredentialsRepository {

    @Inject
    AgroalDataSource ds;

    public Optional<Cred> find(String sub) {
        String sql = "select cognito_sub, password_hash, email_verified, totp_secret, totp_enabled "
                + "from auth_local.credentials where cognito_sub = ?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sub);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Cred(
                        rs.getString("cognito_sub"),
                        rs.getString("password_hash"),
                        rs.getBoolean("email_verified"),
                        rs.getString("totp_secret"),
                        rs.getBoolean("totp_enabled")));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void create(String sub, String passwordHash, boolean emailVerified) {
        update("insert into auth_local.credentials(cognito_sub, password_hash, email_verified) values (?,?,?) "
                        + "on conflict (cognito_sub) do update set password_hash = excluded.password_hash, "
                        + "email_verified = excluded.email_verified, updated_at = now()",
                ps -> {
                    ps.setString(1, sub);
                    ps.setString(2, passwordHash);
                    ps.setBoolean(3, emailVerified);
                });
    }

    public void setEmailVerified(String sub) {
        update("update auth_local.credentials set email_verified = true, updated_at = now() where cognito_sub = ?",
                ps -> ps.setString(1, sub));
    }

    public void updatePassword(String sub, String passwordHash) {
        update("update auth_local.credentials set password_hash = ?, updated_at = now() where cognito_sub = ?",
                ps -> {
                    ps.setString(1, passwordHash);
                    ps.setString(2, sub);
                });
    }

    public void setTotp(String sub, String secret, boolean enabled) {
        update("update auth_local.credentials set totp_secret = ?, totp_enabled = ?, updated_at = now() where cognito_sub = ?",
                ps -> {
                    ps.setString(1, secret);
                    ps.setBoolean(2, enabled);
                    ps.setString(3, sub);
                });
    }

    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private void update(String sql, Binder binder) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public record Cred(String sub, String passwordHash, boolean emailVerified, String totpSecret, boolean totpEnabled) {}
}
