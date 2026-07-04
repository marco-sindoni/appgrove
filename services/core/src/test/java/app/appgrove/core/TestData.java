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

    /** Tier con descrittore {@code limits} jsonb ({@code {metric,cap,type,window}}) — per la catena L1 (UC 0029). */
    public void appTier(UUID id, UUID appId, String key, String limitsJson) {
        exec("insert into platform.app_tier(id,app_id,key,name,trial_days,limits,created_at,updated_at)"
                        + " values (?,?,?,?,?,?::jsonb,?,?) on conflict (id) do nothing",
                id, appId, key, key, 0, limitsJson, OffsetDateTime.now(), OffsetDateTime.now());
    }

    /** Crea un price (tier × ciclo) di catalogo (FK app_price → app_tier); idempotente. Per UC 0024. */
    public void appPrice(UUID id, UUID appTierId, String billingCycle, String paddlePriceId, int amount) {
        exec("insert into platform.app_price"
                        + "(id,app_tier_id,billing_cycle,paddle_price_id,amount,currency,created_at,updated_at)"
                        + " values (?,?,?,?,?,?,?,?) on conflict (id) do nothing",
                id, appTierId, billingCycle, paddlePriceId, amount, "EUR",
                OffsetDateTime.now(), OffsetDateTime.now());
    }

    /** Crea una subscription {@code (tenant, app)} nello stato dato — per i test GDPR (UC 0032). */
    public void subscription(String tenantId, UUID appId, String status) {
        exec("insert into platform.subscription(id,tenant_id,app_id,status,created_at,updated_at)"
                        + " values (?,?,?,?,?,?) on conflict do nothing",
                UUID.randomUUID(), tenantId, appId, status, OffsetDateTime.now(), OffsetDateTime.now());
    }

    /** Crea un invito pending nel tenant — per i test GDPR (UC 0032). */
    public void invitation(String tenantId, String email, String role) {
        exec("insert into platform.invitations(id,tenant_id,email,role,token_hash,status,expires_at,"
                        + "created_at,updated_at) values (?,?,?,?,?,?,?,?,?) on conflict do nothing",
                UUID.randomUUID(), tenantId, email, role, "hash-" + UUID.randomUUID(), "pending",
                OffsetDateTime.now().plusDays(7), OffsetDateTime.now(), OffsetDateTime.now());
    }

    /** Crea un ticket di supporto nel tenant — per i test del ticketing (UC 0034). Ritorna l'id. */
    public UUID ticket(String tenantId, String type, String subject, String status) {
        UUID id = UUID.randomUUID();
        exec("insert into platform.support_ticket(id,tenant_id,type,subject,priority,status,created_at,updated_at)"
                        + " values (?,?,?,?,?,?,?,?)",
                id, tenantId, type, subject, "normal", status, OffsetDateTime.now(), OffsetDateTime.now());
        return id;
    }

    /** Aggiunge un messaggio al thread di un ticket — per i test del ticketing (UC 0034). */
    public void ticketMessage(String tenantId, UUID ticketId, String author, String body) {
        exec("insert into platform.support_ticket_message(id,tenant_id,ticket_id,author,body,created_at,updated_at)"
                        + " values (?,?,?,?,?,?,?)",
                UUID.randomUUID(), tenantId, ticketId, author, body, OffsetDateTime.now(), OffsetDateTime.now());
    }

    /** Retrodata la chiusura di un ticket (per i test dello sweeper retention, UC 0034). */
    public void backdateTicketClosure(UUID ticketId, OffsetDateTime closedAt) {
        exec("update platform.support_ticket set status='closed', closed_at=? where id=?", closedAt, ticketId);
    }

    /** Numero di ticket per export job (per l'idempotenza dell'auto-ticket, UC 0034). */
    public int ticketCountForExportJob(UUID jobId) {
        return queryInt("select count(*) from platform.support_ticket where export_job_id = ?", jobId);
    }

    /** Numero di ticket esistenti per id (per lo sweeper retention, UC 0034). */
    public int ticketCount(UUID ticketId) {
        return queryInt("select count(*) from platform.support_ticket where id = ?", ticketId);
    }

    /** Soft-delete delle subscription {@code (tenant, app)} — simula il recesso per-app (UC 0034). */
    public void softDeleteSubscriptions(String tenantId, UUID appId) {
        exec("update platform.subscription set deleted_at = ? where tenant_id = ? and app_id = ?",
                OffsetDateTime.now(), tenantId, appId);
    }

    /** Stato corrente dell'account (per i test della limitazione art. 18, UC 0034). */
    public String accountStatus(String tenantId) {
        return queryString("select status from platform.accounts where id = ?", UUID.fromString(tenantId));
    }

    /** Causale di sospensione dell'account (per i test della limitazione art. 18, UC 0034). */
    public String accountSuspendedReason(String tenantId) {
        return queryString("select suspended_reason from platform.accounts where id = ?",
                UUID.fromString(tenantId));
    }

    /** Stato corrente di un utente (per i test della limitazione art. 18, UC 0034). */
    public String userStatus(UUID userId) {
        return queryString("select status from platform.users where id = ?", userId);
    }

    /** Sospende un account con causale amministrativa (per i test di conflitto art. 18). */
    public void suspendAccount(String tenantId, String reason) {
        exec("update platform.accounts set status = 'suspended', suspended_reason = ? where id = ?",
                reason, UUID.fromString(tenantId));
    }

    /** Righe di audit purge (prova erasure #13 L70) per tenant — per i test GDPR (UC 0032). */
    public int gdprPurgeAuditCount(String tenantId, String appId) {
        return queryInt(
                "select count(*) from platform.gdpr_purge_audit where tenant_id = ? and app_id = ?",
                tenantId, appId);
    }

    /** Numero di subscription (non cancellate) per {@code (tenant, app)} — per i test di idempotenza. */
    public int subscriptionCount(String tenantId, UUID appId) {
        return queryInt(
                "select count(*) from platform.subscription where tenant_id = ? and app_id = ? and deleted_at is null",
                tenantId, appId);
    }

    /** Stato della subscription per {@code (tenant, app)}, o null se assente (per gli assert L1 0025). */
    public String subscriptionStatus(String tenantId, UUID appId) {
        return queryString(
                "select status from platform.subscription where tenant_id = ? and app_id = ? and deleted_at is null",
                tenantId, appId);
    }

    /** Fine periodo corrente della subscription (per asserire l'avanzamento sul rinnovo). */
    public java.time.Instant subscriptionPeriodEnd(String tenantId, UUID appId) {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "select current_period_end from platform.subscription"
                                + " where tenant_id = ? and app_id = ? and deleted_at is null")) {
            ps.setObject(1, tenantId);
            ps.setObject(2, appId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    var ts = rs.getTimestamp(1);
                    return ts == null ? null : ts.toInstant();
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** {@code paddle_customer_id} dell'account (per asserire la cattura da customer.*). */
    public String accountPaddleCustomerId(String tenantId) {
        return queryString("select paddle_customer_id from platform.accounts where id = ?", UUID.fromString(tenantId));
    }

    /** Esito registrato in {@code webhook_event} per un event_id (processed | skipped_stale | received). */
    public String webhookOutcome(String eventId) {
        return queryString("select outcome from platform.webhook_event where event_id = ?", eventId);
    }

    /** Numero di righe di dedup per un event_id (deve restare 1 anche con re-delivery). */
    public int webhookEventCount(String eventId) {
        return queryInt("select count(*) from platform.webhook_event where event_id = ?", eventId);
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

    private String queryString(String sql, Object... params) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
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
