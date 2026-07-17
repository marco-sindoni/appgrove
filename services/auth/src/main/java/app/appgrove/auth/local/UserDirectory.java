package app.appgrove.auth.local;

import app.appgrove.auth.AuthUser;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Lettura utenti dallo schema {@code platform} via JDBC diretto. Il login è <b>pre-tenant</b>:
 * NON si usa l'entità {@code User} tenant-scoped del core (il discriminator richiederebbe un tenant
 * già noto). Replica la lettura DB del Pre-Token-Gen (#02 dec.9).
 */
@ApplicationScoped
public class UserDirectory {

    private static final String COLUMNS =
            "cognito_sub, tenant_id, role, status, email, display_name";

    @Inject
    AgroalDataSource ds;

    public Optional<AuthUser> findByEmail(String email) {
        return query("lower(email) = lower(?)", email);
    }

    public Optional<AuthUser> findBySub(String sub) {
        return query("cognito_sub = ?", sub);
    }

    private Optional<AuthUser> query(String condition, String value) {
        String sql = "select " + COLUMNS + " from platform.users where " + condition + " and deleted_at is null";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new AuthUser(
                        rs.getString("cognito_sub"),
                        rs.getString("tenant_id"),
                        rs.getString("role"),
                        rs.getString("status"),
                        rs.getString("email"),
                        rs.getString("display_name")));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
