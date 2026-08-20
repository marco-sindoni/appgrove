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
 * Scritture nello schema {@code platform} al signup/accept: crea account + identità + appartenenze.
 * Condiviso tra i provider (il {@code sub} lo fornisce il chiamante: Cognito sub in cloud,
 * {@code local-*} in dev). JDBC diretto (pre-tenant, niente discriminator); {@code tenant_id}
 * = account id, sempre esplicito (invariante #1).
 *
 * <p><b>È il file che concentra il rischio di UC 0116</b>: qui nasce ogni persona della piattaforma.
 * Dopo la storia una persona è due righe e non una — l'<b>identità</b> (di piattaforma: indirizzo,
 * identificativo di autenticazione, nome, lingua) e l'<b>appartenenza</b> (di account: ruolo, stato).
 * L'identità si crea <b>solo quando manca</b>: chi esiste già entra in un nuovo account con una
 * appartenenza in più, non con una seconda identità.
 */
@ApplicationScoped
public class PlatformWriter {

    @Inject
    AgroalDataSource ds;

    /**
     * Signup: crea un nuovo account (tenant) + identità (se manca) + appartenenza {@code owner}.
     * {@code sub} = identità del provider. Atomico: o nascono tutte e tre le righe, o nessuna.
     */
    public CreatedUser createAccountWithOwner(String sub, String email, String displayName, String locale) {
        UUID accountId = UUID.randomUUID();
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
                UUID identityId = ensureIdentity(c, sub, email, displayName, lang);
                insertMembership(c, identityId, accountId.toString(), "owner");
                c.commit();
                return new CreatedUser(
                        identityId,
                        new AuthUser(sub, accountId.toString(), "owner", "active", email, displayName, lang));
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Accept invito: crea l'<b>appartenenza</b> nel tenant invitante col ruolo dell'invito, e
     * l'identità solo se quella persona non esiste ancora sulla piattaforma. È il passaggio che
     * scioglie il vincolo: chi ha già un account altrove può essere invitato (UC 0116). Gli esiti
     * comprensibili e non rivelatori del caso «esisteva già» sono di UC 0118.
     */
    public CreatedUser createUserInTenant(
            String sub, String tenantId, String email, String displayName, String role, String locale) {
        String lang = Locales.normalize(locale);
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                UUID identityId = ensureIdentity(c, sub, email, displayName, lang);
                insertMembership(c, identityId, tenantId, role);
                c.commit();
                return new CreatedUser(
                        identityId, new AuthUser(sub, tenantId, role, "active", email, displayName, lang));
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Lingua della persona per le email transazionali (UC 0018). Dopo UC 0116 la lingua sta
     * sull'<b>identità</b>: è della persona, non dell'account, quindi vale in tutti gli account a cui
     * appartiene. Usata dai flussi che partono da un solo indirizzo (rinvio verifica, password
     * dimenticata), dove non c'è altro contesto.
     *
     * <p>Ritorna sempre una lingua supportata: un indirizzo sconosciuto dà {@code en}, come un
     * utente senza preferenza. Non distinguere i due casi è deliberato — questi flussi rispondono in
     * modo neutro per non rivelare se un indirizzo è registrato.
     */
    public String localeOf(String email) {
        String sql = "select locale from platform.identity where lower(email) = lower(?) and deleted_at is null";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return Locales.normalize(rs.next() ? rs.getString("locale") : null);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Esiste già un'identità con quell'indirizzo? Dopo UC 0116 la domanda è distinta da «esiste già una
     * persona in questo account»: una persona può esistere sulla piattaforma senza appartenere all'account
     * che la sta invitando, e anche senza appartenere a nessuno.
     *
     * <p>Serve ai percorsi d'ingresso per rifiutare in modo <b>comprensibile</b> invece di sbattere contro
     * una violazione di indice.
     *
     * <p><b>Guarda anche le righe cancellate, di proposito</b> (UC 0118). L'unicità di
     * {@code platform.identity} su indirizzo è <b>incondizionata</b> — vale anche sulle identità
     * cancellate, esattamente come era su {@code platform.users} — quindi un controllo che le
     * ignorasse lascerebbe passare chi si ripresenta con l'indirizzo di un'identità cancellata, per
     * poi far fallire l'inserimento contro l'indice: un errore del servizio al posto di un messaggio.
     * Il messaggio che ne esce è lo <b>stesso</b> di un indirizzo vivo, quindi non rivela nulla in più.
     */
    public boolean identityExists(String email) {
        String sql = "select 1 from platform.identity where lower(email) = lower(?)";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gli account a cui la persona appartiene <b>attivamente</b>, con il nome, in ordine di anzianità
     * dell'appartenenza (UC 0118): quanto serve alla sfida di scelta dell'account e nulla di più.
     *
     * <p>Lettura di <b>piattaforma</b>: nessun filtro per account, perché la domanda «a quali account
     * appartengo?» per costruzione li attraversa. Gemella di
     * {@code MembershipRepository.activeAccountsOf} nel servizio core — vive anche qui perché il
     * percorso di accesso non può dipendere da una chiamata di rete verso un altro servizio: deve
     * funzionare sempre.
     *
     * <p>Nessun ruolo fra i campi restituiti: l'elenco serve a scegliere <b>dove</b> lavorare, e il
     * ruolo è per applicazione (UC 0117 §4.6).
     */
    public java.util.List<IdentityProvider.AccountRef> activeAccountsOf(String sub) {
        String sql = "select m.tenant_id, a.name from platform.identity i"
                + " join platform.membership m on m.identity_id = i.id"
                + " join platform.accounts a on a.id = m.tenant_id::uuid"
                + " where i.cognito_sub = ? and i.status = 'active' and i.deleted_at is null"
                + " and m.status = 'active' and m.deleted_at is null and a.deleted_at is null"
                + " order by m.created_at, m.id";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sub);
            try (ResultSet rs = ps.executeQuery()) {
                java.util.List<IdentityProvider.AccountRef> out = new java.util.ArrayList<>();
                while (rs.next()) {
                    out.add(new IdentityProvider.AccountRef(rs.getString(1), rs.getString(2)));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Conserva l'account scelto dalla persona, se quell'account è davvero una sua appartenenza
     * <b>attiva</b> (UC 0118). Ritorna falso quando non lo è: chi chiama risponde «non trovato», che è
     * indistinguibile da «non esiste» — l'esistenza di un account non è informazione di chi chiede.
     *
     * <p>La riverifica si fa <b>qui</b> e non si fida dell'elenco mostrato un istante prima: fra la
     * schermata e la scelta l'appartenenza può essere stata revocata, e in quel caso la scelta non
     * deve avere effetto.
     */
    public boolean chooseActiveAccount(String sub, String accountId) {
        String select = "select m.id from platform.identity i"
                + " join platform.membership m on m.identity_id = i.id"
                + " where i.cognito_sub = ? and m.tenant_id = ?"
                + " and i.status = 'active' and i.deleted_at is null"
                + " and m.status = 'active' and m.deleted_at is null";
        try (Connection c = ds.getConnection()) {
            UUID membershipId;
            try (PreparedStatement ps = c.prepareStatement(select)) {
                ps.setString(1, sub);
                ps.setString(2, accountId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                    membershipId = rs.getObject(1, UUID.class);
                }
            }
            exec(c, "update platform.identity set active_membership_id = ?, updated_at = now()"
                            + " where cognito_sub = ?",
                    ps -> {
                        ps.setObject(1, membershipId);
                        ps.setString(2, sub);
                    });
            return true;
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

    /**
     * L'identità della persona, creata solo se manca. La si cerca per identificativo di
     * autenticazione <b>e</b> per indirizzo, perché sono due unicità globali distinte e chi arriva
     * potrebbe presentarsi con l'una o con l'altra già nota.
     */
    private UUID ensureIdentity(Connection c, String sub, String email, String displayName, String locale)
            throws SQLException {
        UUID existing = findIdentity(c, sub, email);
        if (existing != null) {
            return existing;
        }
        UUID identityId = UUID.randomUUID();
        exec(c, "insert into platform.identity(id, cognito_sub, email, display_name, locale, status, "
                        + "created_at, updated_at, created_by) "
                        + "values (?, ?, ?, ?, ?, 'active', now(), now(), 'auth')",
                ps -> {
                    ps.setObject(1, identityId);
                    ps.setString(2, sub);
                    ps.setString(3, email);
                    ps.setString(4, displayName);
                    ps.setString(5, locale);
                });
        return identityId;
    }

    private UUID findIdentity(Connection c, String sub, String email) throws SQLException {
        String sql = "select id from platform.identity"
                + " where (cognito_sub = ? or lower(email) = lower(?)) and deleted_at is null"
                + " order by created_at, id limit 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sub);
            ps.setString(2, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getObject(1, UUID.class) : null;
            }
        }
    }

    /**
     * L'appartenenza della persona a un account. Il vincolo esplicito
     * {@code ux_membership_tenant_identity} rifiuta la seconda appartenenza viva allo stesso account:
     * è il vincolo che serve davvero, e vive nella banca dati, non solo nell'interfaccia.
     *
     * <p>L'appartenenza appena creata diventa anche l'<b>account attivo</b> della persona (UC 0117):
     * si è appena entrati lì, ed è lì che si atterra. Non cambia nulla per chi ha una sola
     * appartenenza — la regola che compone il token ignora il valore conservato quando ce n'è una
     * sola — ma rende raro il caso «più appartenenze e nessuna scelta», che a chiusura non produrrebbe
     * alcun token.
     */
    private void insertMembership(Connection c, UUID identityId, String tenantId, String role)
            throws SQLException {
        UUID membershipId = UUID.randomUUID();
        exec(c, "insert into platform.membership(id, tenant_id, identity_id, role, status, "
                        + "created_at, updated_at, created_by) "
                        + "values (?, ?, ?, ?, 'active', now(), now(), 'auth')",
                ps -> {
                    ps.setObject(1, membershipId);
                    ps.setString(2, tenantId);
                    ps.setObject(3, identityId);
                    ps.setString(4, role);
                });
        exec(c, "update platform.identity set active_membership_id = ?, updated_at = now() where id = ?",
                ps -> {
                    ps.setObject(1, membershipId);
                    ps.setObject(2, identityId);
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

    /** {@code id} = identificativo dell'<b>identità</b> della persona (UC 0116). */
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
