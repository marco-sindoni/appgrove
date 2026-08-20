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

    /**
     * Crea una persona nel tenant: identità (se manca) + appartenenza (UC 0116). Idempotente.
     * Ritorna l'id dell'<b>identità</b>, che è l'identificativo della persona esposto dall'API.
     */
    public UUID user(String tenantId, String cognitoSub, String email, String role) {
        UUID identityId = identity(cognitoSub, email, null);
        membership(tenantId, identityId, role);
        return identityId;
    }

    /**
     * L'identità della persona, creata solo se manca (cercata per identificativo di autenticazione o
     * per indirizzo, le due unicità globali). Non crea nessuna appartenenza: serve anche a costruire
     * il caso «persona senza account» del ciclo di vita.
     */
    public UUID identity(String cognitoSub, String email, String displayName) {
        exec("insert into platform.identity(id,cognito_sub,email,display_name,locale,status,created_at,updated_at)"
                        + " values (?,?,?,?,?,?,?,?) on conflict do nothing",
                UUID.randomUUID(), cognitoSub, email, displayName, "en", "active",
                OffsetDateTime.now(), OffsetDateTime.now());
        return queryUuid("select id from platform.identity"
                + " where cognito_sub = ? or lower(email) = lower(?) order by created_at, id limit 1",
                cognitoSub, email);
    }

    /** Appartenenza di una persona a un account; idempotente sul vincolo (tenant, identità). Ritorna l'id. */
    public UUID membership(String tenantId, UUID identityId, String role) {
        exec("insert into platform.membership(id,tenant_id,identity_id,role,status,created_at,updated_at)"
                        + " values (?,?,?,?,?,?,?) on conflict do nothing",
                UUID.randomUUID(), tenantId, identityId, role, "active",
                OffsetDateTime.now(), OffsetDateTime.now());
        return queryUuid("select id from platform.membership"
                + " where tenant_id = ? and identity_id = ? and deleted_at is null", tenantId, identityId);
    }

    /**
     * Appartenenza <b>senza</b> guardia di conflitto: usata per provare che il vincolo «non due volte
     * nello stesso account» vive nella banca dati e non solo nell'interfaccia.
     */
    public void membershipStrict(String tenantId, UUID identityId, String role) {
        exec("insert into platform.membership(id,tenant_id,identity_id,role,status,created_at,updated_at)"
                        + " values (?,?,?,?,?,?,?)",
                UUID.randomUUID(), tenantId, identityId, role, "active",
                OffsetDateTime.now(), OffsetDateTime.now());
    }

    /** Come {@link #user} ma con la lingua della persona, che decide la lingua delle email (UC 0018). */
    public UUID userWithLocale(String tenantId, String cognitoSub, String email, String role, String locale) {
        UUID id = user(tenantId, cognitoSub, email, role);
        exec("update platform.identity set locale = ? where id = ?", locale, id);
        return id;
    }

    /**
     * Inserisce un'identità <b>senza</b> guardia di conflitto: usato per provare che l'indirizzo è
     * unico globalmente sulla persona (e non più sull'utente-dentro-l'account).
     */
    public void userStrict(String tenantId, String cognitoSub, String email, String role) {
        exec("insert into platform.identity(id,cognito_sub,email,locale,status,created_at,updated_at)"
                        + " values (?,?,?,?,?,?,?)",
                UUID.randomUUID(), cognitoSub, email, "en", "active",
                OffsetDateTime.now(), OffsetDateTime.now());
        membership(tenantId, queryUuid("select id from platform.identity where cognito_sub = ?", cognitoSub), role);
    }

    /** Chiude (soft-delete) l'appartenenza di una persona a un account: uscita da quell'account. */
    public void closeMembership(String tenantId, UUID identityId) {
        exec("update platform.membership set deleted_at = ?, updated_at = ?"
                        + " where tenant_id = ? and identity_id = ? and deleted_at is null",
                OffsetDateTime.now(), OffsetDateTime.now(), tenantId, identityId);
    }

    /** Gli account a cui appartiene una persona (appartenenze vive), in ordine di anzianità. */
    public java.util.List<String> tenantsOf(UUID identityId) {
        java.util.List<String> tenants = new java.util.ArrayList<>();
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "select tenant_id from platform.membership where identity_id = ?"
                                + " and deleted_at is null order by created_at, id")) {
            ps.setObject(1, identityId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    tenants.add(rs.getString(1));
                }
            }
            return tenants;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Numero di identità vive con quell'indirizzo (deve essere 0 o 1: unicità globale). */
    public int identityCount(String email) {
        return queryInt("select count(*) from platform.identity where lower(email) = lower(?)"
                + " and deleted_at is null", email);
    }

    /** Crea un'app di catalogo (FK di subscription.app_id); idempotente. */
    public void app(UUID id, String slug) {
        exec("insert into platform.app(id,slug,name,user_model,status,created_at,updated_at)"
                        + " values (?,?,?,?,?,?,?) on conflict (id) do nothing",
                id, slug, slug, "single_user", "active", OffsetDateTime.now(), OffsetDateTime.now());
    }

    /** Forza lo stato di un'app di catalogo ({@code active} | {@code inactive}) — leva di UC 0076. */
    public void appStatus(UUID appId, String status) {
        exec("update platform.app set status = ?, updated_at = ? where id = ?",
                status, OffsetDateTime.now(), appId);
    }

    /** Stato corrente di un'app di catalogo. */
    public String appStatus(UUID appId) {
        return queryString("select status from platform.app where id = ?", appId);
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

    /** Riga di storico pagamenti {@code (tenant, app)} — per i test GDPR e di fatturazione (UC 0096). */
    public void billingTransaction(String tenantId, UUID appId, String paddleTransactionId, int amount) {
        exec("insert into platform.billing_transaction"
                        + "(id,tenant_id,app_id,paddle_transaction_id,status,amount,currency,billed_at,"
                        + "created_at,updated_at)"
                        + " values (?,?,?,?,?,?,?,?,?,?) on conflict (paddle_transaction_id) do nothing",
                UUID.randomUUID(), tenantId, appId, paddleTransactionId, "paid", amount, "EUR",
                OffsetDateTime.now(), OffsetDateTime.now(), OffsetDateTime.now());
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

    /**
     * Come {@link #ticket} ma con scadenza legale esplicita — serve ai test dell'ordinamento della
     * coda di amministrazione, che mette per prime le scadenze più vicine (UC 0075).
     */
    public UUID ticketDue(String tenantId, String type, String subject, String status, OffsetDateTime dueAt) {
        UUID id = UUID.randomUUID();
        exec("insert into platform.support_ticket"
                        + "(id,tenant_id,type,subject,priority,status,due_at,created_at,updated_at)"
                        + " values (?,?,?,?,?,?,?,?,?)",
                id, tenantId, type, subject, "normal", status, dueAt,
                OffsetDateTime.now(), OffsetDateTime.now());
        return id;
    }

    /** Provenienza registrata su un ticket (UC 0075: form | event | email). */
    public String ticketSource(UUID ticketId) {
        return queryString("select source from platform.support_ticket where id = ?", ticketId);
    }

    /** Aggiunge un messaggio al thread di un ticket — per i test del ticketing (UC 0034). */
    public void ticketMessage(String tenantId, UUID ticketId, String author, String body) {
        exec("insert into platform.support_ticket_message(id,tenant_id,ticket_id,author,body,created_at,updated_at)"
                        + " values (?,?,?,?,?,?,?)",
                UUID.randomUUID(), tenantId, ticketId, author, body, OffsetDateTime.now(), OffsetDateTime.now());
    }

    /** Accettazione legale (UC 0056): riga nel log tenant/utente-scoped, per i test GDPR. */
    public void legalAcceptance(String tenantId, String userId, String component, String version, int major, String actType) {
        exec("insert into platform.legal_acceptance"
                        + "(id,tenant_id,user_id,component,version,major,act_type,accepted_at,created_at,updated_at)"
                        + " values (?,?,?,?,?,?,?,?,?,?)",
                UUID.randomUUID(), tenantId, userId, component, version, major, actType,
                OffsetDateTime.now(), OffsetDateTime.now(), OffsetDateTime.now());
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

    /**
     * Stato corrente della <b>persona</b> (per i test della limitazione art. 18, UC 0034): dopo
     * UC 0116 la limitazione sospende l'identità, non l'appartenenza a un singolo account.
     */
    public String userStatus(UUID identityId) {
        return queryString("select status from platform.identity where id = ?", identityId);
    }

    /** Stato dell'appartenenza a un account (leva dell'owner, distinta da {@link #userStatus}). */
    public String membershipStatus(String tenantId, UUID identityId) {
        return queryString("select status from platform.membership where tenant_id = ? and identity_id = ?"
                + " and deleted_at is null", tenantId, identityId);
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

    // ── Retention/attività (UC 0035) ─────────────────────────────────────────────────

    /** Retrodata l'ultima attività dell'account (per i test dello sweeper inattività). */
    public void backdateAccountActivity(String tenantId, OffsetDateTime lastActiveAt) {
        exec("update platform.accounts set last_active_at = ? where id = ?", lastActiveAt, UUID.fromString(tenantId));
    }

    /** Imposta l'istante dell'avviso di inattività (per allestire la fase 2 direttamente). */
    public void setInactivityWarnedAt(String tenantId, OffsetDateTime warnedAt) {
        exec("update platform.accounts set inactivity_warned_at = ? where id = ?", warnedAt, UUID.fromString(tenantId));
    }

    /** {@code last_active_at} dell'account, o null (per gli assert del filtro di attività). */
    public java.time.Instant accountLastActiveAt(String tenantId) {
        return queryInstant("select last_active_at from platform.accounts where id = ?", UUID.fromString(tenantId));
    }

    /** {@code inactivity_warned_at} dell'account, o null se nessun avviso pendente. */
    public java.time.Instant accountInactivityWarnedAt(String tenantId) {
        return queryInstant("select inactivity_warned_at from platform.accounts where id = ?",
                UUID.fromString(tenantId));
    }

    /** L'account risulta soft-cancellato ({@code deleted_at} valorizzato)? (per lo sweeper inattività). */
    public boolean accountSoftDeleted(String tenantId) {
        return queryInstant("select deleted_at from platform.accounts where id = ?", UUID.fromString(tenantId)) != null;
    }

    /** Inserisce una riga di prova erasure con {@code executed_at} dato (per lo sweeper retention audit). */
    public void gdprPurgeAudit(String tenantId, String appId, OffsetDateTime executedAt) {
        exec("insert into platform.gdpr_purge_audit(id,tenant_id,app_id,reason,deleted_by_entity,total,executed_at)"
                        + " values (?,?,?,?,?::jsonb,?,?)",
                UUID.randomUUID(), tenantId, appId, "manual", "{}", 0, executedAt);
    }

    /** Inserisce una riga di prova limitazione con {@code executed_at} dato (per lo sweeper retention audit). */
    public void gdprRestrictionAudit(String tenantId, OffsetDateTime executedAt) {
        exec("insert into platform.gdpr_restriction_audit"
                        + "(id,tenant_id,target_kind,target_id,action,actor,executed_at)"
                        + " values (?,?,?,?,?,?,?)",
                UUID.randomUUID(), tenantId, "account", tenantId, "applied", "admin", executedAt);
    }

    /** Righe totali in una tabella di audit per tenant (per lo sweeper retention audit). */
    public int auditRowCount(String table, String tenantId) {
        return queryInt("select count(*) from " + table + " where tenant_id = ?", tenantId);
    }

    // ── Registro transizioni di stato app (UC 0076) ──────────────────────────────────

    /**
     * Inserisce una riga nel registro delle transizioni di stato app con {@code executed_at} dato
     * (per lo sweeper di conservazione). La tabella NON è tenant-scoped: il catalogo è di piattaforma.
     */
    public void appStatusAudit(UUID appId, String fromStatus, String toStatus, OffsetDateTime executedAt) {
        exec("insert into platform.app_status_audit"
                        + "(id,app_id,from_status,to_status,actor,reason,executed_at) values (?,?,?,?,?,?,?)",
                UUID.randomUUID(), appId, fromStatus, toStatus, "admin", null, executedAt);
    }

    /** Righe del registro transizioni per una data app. */
    public int appStatusAuditCount(UUID appId) {
        return queryInt("select count(*) from platform.app_status_audit where app_id = ?", appId);
    }

    private java.time.Instant queryInstant(String sql, Object... params) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                var ts = rs.getTimestamp(1);
                return ts == null ? null : ts.toInstant();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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

    /** Iscritto alla newsletter (UC 0039), platform-level: una riga per l'indirizzo con lo stato dato. */
    public void newsletterSubscriber(String email, String status) {
        exec("insert into platform.newsletter_subscriber"
                        + "(id,email,status,locale,origin_channel,created_at,updated_at) values (?,?,?,?,?,?,?)",
                UUID.randomUUID(), email, status, "en", "site", OffsetDateTime.now(), OffsetDateTime.now());
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

    private UUID queryUuid(String sql, Object... params) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getObject(1, UUID.class) : null;
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
