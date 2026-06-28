package app.appgrove.core;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Inserisce fixture via JDBC diretto (Agroal), <b>bypassando Hibernate e il TenantResolver</b>:
 * fuori da una richiesta autenticata il resolver è fail-closed, quindi i dati di base vanno scritti
 * con {@code tenant_id} esplicito. Le letture restano poi soggette al discriminator (via REST).
 */
@ApplicationScoped
public class TestData {

    @Inject
    AgroalDataSource ds;

    /** Crea l'account (radice tenant) con {@code id = tenantId}; idempotente. */
    public void account(String tenantId, String name) {
        exec("insert into platform.accounts(id,name,status,created_at,updated_at) values (?,?,?,?,?)"
                        + " on conflict (id) do nothing",
                UUID.fromString(tenantId), name, "active", OffsetDateTime.now(), OffsetDateTime.now());
    }

    /** Crea un utente nel tenant; idempotente su conflitti di unicità (email/cognito_sub). Ritorna l'id. */
    public UUID user(String tenantId, String cognitoSub, String email, String role) {
        UUID id = UUID.randomUUID();
        exec("insert into platform.users(id,tenant_id,cognito_sub,email,role,status,created_at,updated_at)"
                        + " values (?,?,?,?,?,?,?,?) on conflict do nothing",
                id, tenantId, cognitoSub, email, role, "active", OffsetDateTime.now(), OffsetDateTime.now());
        return id;
    }

    /** Inserisce un utente <b>senza</b> guardia di conflitto: usato per provare il vincolo email unica. */
    public void userStrict(String tenantId, String cognitoSub, String email, String role) {
        exec("insert into platform.users(id,tenant_id,cognito_sub,email,role,status,created_at,updated_at)"
                        + " values (?,?,?,?,?,?,?,?)",
                UUID.randomUUID(), tenantId, cognitoSub, email, role, "active",
                OffsetDateTime.now(), OffsetDateTime.now());
    }

    /** Crea un'app di catalogo (FK di subscription.app_id); idempotente. */
    public void app(UUID id, String slug) {
        exec("insert into platform.app(id,slug,name,user_model,status,created_at,updated_at)"
                        + " values (?,?,?,?,?,?,?) on conflict (id) do nothing",
                id, slug, slug, "single_user", "active", OffsetDateTime.now(), OffsetDateTime.now());
    }

    /** Crea un tier di un'app (FK di subscription.app_tier_id); idempotente. */
    public void appTier(UUID id, UUID appId, String key) {
        exec("insert into platform.app_tier(id,app_id,key,name,trial_days,created_at,updated_at)"
                        + " values (?,?,?,?,?,?,?) on conflict (id) do nothing",
                id, appId, key, key, 0, OffsetDateTime.now(), OffsetDateTime.now());
    }

    /** Numero di subscription (non cancellate) per {@code (tenant, app)} — per i test di idempotenza. */
    public int subscriptionCount(String tenantId, UUID appId) {
        return queryInt(
                "select count(*) from platform.subscription where tenant_id = ? and app_id = ? and deleted_at is null",
                tenantId, appId);
    }

    private int queryInt(String sql, Object... params) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void exec(String sql, Object... params) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
