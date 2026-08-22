package app.appgrove.core;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
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

    /**
     * Accesso di una persona a una applicazione (UC 0098); idempotente sulla terna viva. Il
     * {@code tenant_id} è esplicito perché fuori da una richiesta autenticata il resolver è a chiusura.
     */
    public UUID appAccess(String tenantId, UUID appId, UUID identityId, String role) {
        exec("insert into platform.app_access(id,tenant_id,app_id,identity_id,role,created_at,updated_at)"
                        + " values (?,?,?,?,?,?,?)"
                        + " on conflict (tenant_id,app_id,identity_id) where deleted_at is null do nothing",
                UUID.randomUUID(), tenantId, appId, identityId, role,
                OffsetDateTime.now(), OffsetDateTime.now());
        return queryUuid("select id from platform.app_access"
                + " where tenant_id = ? and app_id = ? and identity_id = ? and deleted_at is null",
                tenantId, appId, identityId);
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

    /** Il ruolo dell'appartenenza viva di quella persona in quell'account. */
    public String memberRole(String tenantId, UUID identityId) {
        return queryString("select role from platform.membership where tenant_id = ? and identity_id = ?"
                + " and deleted_at is null", tenantId, identityId);
    }

    /** L'account di un'appartenenza: serve a dire QUALE account è quello attivo. */
    public String tenantOfMembership(UUID membershipId) {
        return queryString("select tenant_id from platform.membership where id = ?", membershipId);
    }

    /** Crea un invito pending nel tenant e ne ritorna l'id — per i collaudi di UC 0118. */
    public UUID invitationId(String tenantId, String email, String role) {
        UUID id = UUID.randomUUID();
        exec("insert into platform.invitations(id,tenant_id,email,role,token_hash,status,expires_at,"
                        + "created_at,updated_at) values (?,?,?,?,?,?,?,?,?)",
                id, tenantId, email, role, "hash-" + id, "pending",
                OffsetDateTime.now().plusDays(7), OffsetDateTime.now(), OffsetDateTime.now());
        return id;
    }

    /** Stato corrente di un invito (UC 0118): pending | accepted | revoked | expired | rejected. */
    public String invitationStatus(UUID invitationId) {
        return queryString("select status from platform.invitations where id = ?", invitationId);
    }

    /** L'identità registrata come «chi ha accettato» sull'invito, o null. */
    public UUID invitationAcceptedBy(UUID invitationId) {
        return queryUuid("select accepted_user_id from platform.invitations where id = ?", invitationId);
    }

    /** L'identità collegata all'invito all'invio (UC 0118), o null se all'invio non esisteva. */
    public UUID invitationIdentityId(UUID invitationId) {
        return queryUuid("select identity_id from platform.invitations where id = ?", invitationId);
    }

    /**
     * Il ruolo memorizzato sull'invito. Da UC 0100 il ruolo non è più nel contratto e vale sempre
     * {@code member}: questo lettore serve a <b>provarlo</b>, perché la colonna esiste ancora ed è
     * l'unico posto dove un valore diverso potrebbe insinuarsi.
     */
    public String invitationRole(UUID invitationId) {
        return queryString("select role from platform.invitations where id = ?", invitationId);
    }

    /** Porta l'invito a scadenza, per i collaudi che devono vederlo rifiutato. */
    public void expireInvitation(UUID invitationId) {
        exec("update platform.invitations set expires_at = ? where id = ?",
                OffsetDateTime.now().minusDays(1), invitationId);
    }

    /** Cancella (soft-delete) un'identità: serve al caso «riuso di un indirizzo cancellato» (UC 0118). */
    public void deleteIdentity(UUID identityId) {
        exec("update platform.identity set deleted_at = ?, updated_at = ? where id = ?",
                OffsetDateTime.now(), OffsetDateTime.now(), identityId);
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

    /** Ruolo dell'appartenenza a un account (owner | member dopo UC 0098). */
    public String membershipRole(String tenantId, UUID identityId) {
        return queryString("select role from platform.membership where tenant_id = ? and identity_id = ?"
                + " and deleted_at is null", tenantId, identityId);
    }

    /** Stato dell'appartenenza a un account (leva dell'owner, distinta da {@link #userStatus}). */
    public String membershipStatus(String tenantId, UUID identityId) {
        return queryString("select status from platform.membership where tenant_id = ? and identity_id = ?"
                + " and deleted_at is null", tenantId, identityId);
    }

    /** Cambia lo stato dell'appartenenza (leva dell'owner): serve al caso «persona non attiva» di UC 0098. */
    public void setMembershipStatus(String tenantId, UUID identityId, String status) {
        exec("update platform.membership set status = ?, updated_at = ?"
                        + " where tenant_id = ? and identity_id = ? and deleted_at is null",
                status, OffsetDateTime.now(), tenantId, identityId);
    }

    /** Accessi vivi di una persona a una applicazione (UC 0098): 0 o 1, per il vincolo della terna. */
    public int appAccessCount(String tenantId, UUID appId, UUID identityId) {
        return queryInt("select count(*) from platform.app_access"
                + " where tenant_id = ? and app_id = ? and identity_id = ? and deleted_at is null",
                tenantId, appId, identityId);
    }

    /** Accessi vivi di una applicazione, indipendentemente dalla persona. */
    public int appAccessCount(String tenantId, UUID appId) {
        return queryInt("select count(*) from platform.app_access"
                + " where tenant_id = ? and app_id = ? and deleted_at is null", tenantId, appId);
    }

    /** Righe di accesso (anche cancellate) di una persona: prova che l'owner non ne ha nessuna. */
    public int appAccessRowsOf(String tenantId, UUID identityId) {
        return queryInt("select count(*) from platform.app_access where tenant_id = ? and identity_id = ?",
                tenantId, identityId);
    }

    /** Ruolo vivo di una persona su una applicazione, o {@code null}. */
    public String appAccessRole(String tenantId, UUID appId, UUID identityId) {
        return queryString("select role from platform.app_access"
                + " where tenant_id = ? and app_id = ? and identity_id = ? and deleted_at is null",
                tenantId, appId, identityId);
    }

    /** Sospende un account con causale amministrativa (per i test di conflitto art. 18). */
    public void suspendAccount(String tenantId, String reason) {
        exec("update platform.accounts set status = 'suspended', suspended_reason = ? where id = ?",
                reason, UUID.fromString(tenantId));
    }

    /**
     * Porta l'account in <b>attesa di eliminazione</b> (UC 0033): lo stato in cui non si invita più nessuno
     * (UC 0103 §5). Solo lo stato, senza il resto del percorso di eliminazione: qui serve la precondizione,
     * non il processo.
     */
    public void setAccountPendingDeletion(String tenantId) {
        exec("update platform.accounts set status = 'pending_deletion' where id = ?",
                UUID.fromString(tenantId));
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

    // ── Account attivo della persona (UC 0117) ───────────────────────────────────────

    /**
     * Scrive il riferimento all'appartenenza attiva sull'identità, <b>senza alcuna verifica</b>: è la
     * leva che permette di provare la cosa che conta — un valore conservato che non corrisponde a
     * un'appartenenza attiva non produce mai un claim con quell'account. Ammette esplicitamente
     * {@code null} e appartenenze altrui: serve a manometterla.
     */
    public void setActiveMembership(UUID identityId, UUID membershipId) {
        exec("update platform.identity set active_membership_id = ?, updated_at = ? where id = ?",
                membershipId, OffsetDateTime.now(), identityId);
    }

    /** Il riferimento all'appartenenza attiva conservato sull'identità (può essere null). */
    public UUID activeMembershipOf(UUID identityId) {
        return queryUuid("select active_membership_id from platform.identity where id = ?", identityId);
    }

    /** Righe del registro dei cambi di account attivo per una persona. */
    public int activeAccountAuditCount(UUID identityId) {
        return queryInt("select count(*) from platform.active_account_audit where identity_id = ?", identityId);
    }

    /** Ultimo cambio registrato per una persona, come {@code from → to} (null se nessuno). */
    public String lastActiveAccountAudit(UUID identityId) {
        return queryString("select coalesce(from_tenant_id, 'nessuno') || ' -> ' || to_tenant_id"
                + " from platform.active_account_audit where identity_id = ?"
                + " order by executed_at desc, id desc limit 1", identityId);
    }

    /**
     * Inserisce una riga nel registro dei cambi di account attivo con {@code executed_at} dato (per
     * lo sweeper di conservazione).
     */
    public void activeAccountAudit(UUID identityId, String fromTenantId, String toTenantId, OffsetDateTime at) {
        exec("insert into platform.active_account_audit"
                        + "(id,identity_id,from_tenant_id,to_tenant_id,executed_at) values (?,?,?,?,?)",
                UUID.randomUUID(), identityId, fromTenantId, toTenantId, at);
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

    /**
     * Quantità della subscription per {@code (tenant, app)}, o {@code -1} se la riga non esiste (UC 0103).
     *
     * <p>Il {@code -1} distingue «nessun abbonamento» da «abbonamento con quantità zero», e la differenza
     * conta: dentro la franchigia l'abbonamento dei posti <b>non deve esistere affatto</b>, e un collaudo
     * che leggesse zero in entrambi i casi non se ne accorgerebbe.
     */
    public int seatSubscriptionQuantity(String tenantId, UUID appId) {
        Integer q = queryInteger(
                "select quantity from platform.subscription where tenant_id = ? and app_id = ?"
                        + " and deleted_at is null",
                tenantId, appId);
        return q == null ? -1 : q;
    }

    /** Riferimento all'addebito che ha autorizzato il posto di un invito, o null se non è costato nulla. */
    public String invitationSeatChargeRef(UUID invitationId) {
        return queryString(
                "select seat_charge_ref from platform.invitations where id = ?", invitationId);
    }

    // ── Riduzione dei posti (UC 0104) ─────────────────────────────────────────

    /**
     * Inserisce una riduzione dei posti in attesa con una persona indicata, senza passare dal servizio:
     * serve ai collaudi che devono <b>trovare la riga</b> (esportazione, purga) e non provarne la creazione.
     * Ritorna l'identificativo della riduzione.
     */
    public UUID seatDowngrade(String tenantId, UUID identityId, UUID requestedBy, OffsetDateTime executeAt) {
        UUID id = UUID.randomUUID();
        exec("insert into platform.seat_downgrade"
                        + "(id,tenant_id,execute_at,status,requested_by,created_at,updated_at,created_by)"
                        + " values (?,?,?,?,?,?,?,?)",
                id, tenantId, executeAt, "pending", requestedBy,
                OffsetDateTime.now(), OffsetDateTime.now(), "test");
        exec("insert into platform.seat_downgrade_item"
                        + "(id,tenant_id,downgrade_id,identity_id,created_at,updated_at,created_by)"
                        + " values (?,?,?,?,?,?,?)",
                UUID.randomUUID(), tenantId, id, identityId,
                OffsetDateTime.now(), OffsetDateTime.now(), "test");
        return id;
    }

    /**
     * Stato dell'ultima riduzione dei posti dell'account, o {@code null} se non ne ha mai avuta una.
     * «L'ultima» e non «quella in attesa»: dopo un annullamento o un'esecuzione la riga resta, e il
     * collaudo deve poter constatare in che stato è finita.
     */
    public String seatDowngradeStatus(String tenantId) {
        return queryString("select status from platform.seat_downgrade where tenant_id = ?"
                + " order by created_at desc, id desc limit 1", tenantId);
    }

    /** Data di esecuzione dell'ultima riduzione dell'account, o {@code null}. */
    public java.time.Instant seatDowngradeExecuteAt(String tenantId) {
        return queryInstant("select execute_at from platform.seat_downgrade where tenant_id = ?"
                + " order by created_at desc, id desc limit 1", tenantId);
    }

    /** Istante di esecuzione registrato sull'ultima riduzione, o {@code null} se non è stata eseguita. */
    public java.time.Instant seatDowngradeExecutedAt(String tenantId) {
        return queryInstant("select executed_at from platform.seat_downgrade where tenant_id = ?"
                + " order by created_at desc, id desc limit 1", tenantId);
    }

    /** Quante persone sono indicate (righe vive) nelle riduzioni dell'account. */
    public int seatDowngradeItemCount(String tenantId) {
        return queryInt("select count(*) from platform.seat_downgrade_item"
                + " where tenant_id = ? and deleted_at is null", tenantId);
    }

    /** Quante riduzioni (in qualunque stato) ha l'account: prova che un annullamento non cancella la storia. */
    public int seatDowngradeCount(String tenantId) {
        return queryInt("select count(*) from platform.seat_downgrade where tenant_id = ?", tenantId);
    }

    /**
     * Retrodata la data di esecuzione dell'ultima riduzione, così che risulti <b>scaduta</b> senza
     * aspettare un mese vero: è la leva con cui si prova l'esecuzione, la ripetizione e la misura.
     */
    public void backdateSeatDowngrade(String tenantId, OffsetDateTime executeAt) {
        exec("update platform.seat_downgrade set execute_at = ? where tenant_id = ? and status = 'pending'",
                executeAt, tenantId);
    }

    /** Fine del periodo in corso dell'abbonamento dei posti, o {@code null} se l'abbonamento non c'è. */
    public java.time.Instant seatSubscriptionPeriodEnd(String tenantId, UUID appId) {
        return queryInstant("select current_period_end from platform.subscription"
                + " where tenant_id = ? and app_id = ? and deleted_at is null", tenantId, appId);
    }

    /** L'appartenenza della persona è ancora viva in quell'account? */
    public boolean membershipAlive(String tenantId, UUID identityId) {
        return queryInt("select count(*) from platform.membership"
                + " where tenant_id = ? and identity_id = ? and deleted_at is null",
                tenantId, identityId) == 1;
    }

    /** Quante appartenenze vive ha l'account: il numero da cui dipende il conto dei posti. */
    public int membershipCount(String tenantId) {
        return queryInt("select count(*) from platform.membership"
                + " where tenant_id = ? and deleted_at is null", tenantId);
    }

    /** Quanti inviti (in qualunque stato, non cancellati) ha l'account. */
    public int invitationCount(String tenantId) {
        return queryInt(
                "select count(*) from platform.invitations where tenant_id = ? and deleted_at is null",
                tenantId);
    }

    private Integer queryInteger(String sql, Object... params) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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

    // ── Listino dei posti (UC 0102) ───────────────────────────────────────────

    /** Crea una versione del listino dei posti e ne ritorna l'id — serve alla selezione per data. */
    public UUID seatPricingVersion(OffsetDateTime effectiveFrom, String currency, String note) {
        UUID id = UUID.randomUUID();
        exec("insert into platform.seat_pricing_version(id,effective_from,currency,note,created_at,updated_at,"
                        + "created_by) values (?,?,?,?,?,?,?)",
                id, effectiveFrom, currency, note, OffsetDateTime.now(), OffsetDateTime.now(), "test");
        return id;
    }

    /** Aggiunge una fascia a una versione del listino dei posti ({@code toSeat} nullo = fascia aperta). */
    public void seatPricingBand(UUID versionId, int fromSeat, Integer toSeat, int unitPriceCents) {
        exec("insert into platform.seat_pricing_band(id,version_id,from_seat,to_seat,unit_price_cents,"
                        + "created_at,updated_at,created_by) values (?,?,?,?,?,?,?,?)",
                UUID.randomUUID(), versionId, fromSeat, toSeat, unitPriceCents,
                OffsetDateTime.now(), OffsetDateTime.now(), "test");
    }

    /** Quante versioni vive del listino dei posti esistono. */
    public int seatPricingVersionCount() {
        return queryInt("select count(*) from platform.seat_pricing_version where deleted_at is null");
    }

    /** Le fasce vive di una versione del listino dei posti, ordinate dal primo posto. */
    public List<int[]> seatPricingBands(UUID versionId) {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "select from_seat, to_seat, unit_price_cents from platform.seat_pricing_band"
                                + " where version_id = ? and deleted_at is null order by from_seat")) {
            ps.setObject(1, versionId);
            List<int[]> bands = new java.util.ArrayList<>();
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    int from = rs.getInt(1);
                    int to = rs.getInt(2);
                    // wasNull() vale per l'ULTIMA colonna letta: va interrogato qui, non dentro un
                    // inizializzatore di array dove l'ordine di valutazione lo riferirebbe ad altro.
                    boolean aperta = rs.wasNull();
                    bands.add(new int[] {from, aperta ? -1 : to, rs.getInt(3)});
                }
            }
            return bands;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Rimuove del tutto una versione del listino dei posti e le sue fasce (pulizia fra collaudi). */
    public void deleteSeatPricingVersion(UUID versionId) {
        exec("delete from platform.seat_pricing_band where version_id = ?", versionId);
        exec("delete from platform.seat_pricing_version where id = ?", versionId);
    }

    /** Porta un invito allo stato indicato: serve a provare che revocati e accettati non occupano posto. */
    public void setInvitationStatus(UUID invitationId, String status) {
        exec("update platform.invitations set status = ?, updated_at = ? where id = ?",
                status, OffsetDateTime.now(), invitationId);
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
