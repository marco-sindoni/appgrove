package app.appgrove.core.newsletter;

import app.appgrove.core.platform.InvitationTokens;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * Logica della newsletter (UC 0039): iscrizione con double opt-in, conferma, disiscrizione,
 * preferenza dell'utente autenticato. Scrive sempre in coppia lo <b>stato</b> dell'iscritto e la
 * <b>prova</b> nel registro consensi (append-only). Il double opt-in si applica ai canali anonimi
 * (sito/signup); il canale account, autenticato, salta la conferma (identità già provata).
 *
 * <p><b>JDBC diretto</b> (come {@code PlatformWriter}/{@code PlatformDataContract}): lo store è
 * platform-level e l'iscrizione dal sito arriva <b>senza JWT</b>, quindi fuori da una richiesta
 * autenticata; il resolver tenant di Hibernate è fail-closed e non aprirebbe la sessione. Le entità
 * {@link NewsletterSubscriber}/{@link ConsentEvent} restano mappate (Flyway + manifesto {@code @PersonalData}).
 */
@ApplicationScoped
public class NewsletterService {

    private static final Logger LOG = Logger.getLogger(NewsletterService.class);

    /**
     * Identificatore della versione del testo di consenso mostrato all'utente. Il testo vero vive
     * nei contenuti tradotti (sito 5 lingue, SPA en/it); qui si registra solo la versione, così ogni
     * consenso è legato alla formulazione vigente. Da incrementare quando quel testo cambia.
     */
    public static final String CONSENT_VERSION = "newsletter-2026-07";

    /** Validità del token di conferma double opt-in. */
    static final Duration CONFIRM_TTL = Duration.ofDays(7);

    @Inject
    AgroalDataSource ds;

    @Inject
    NewsletterMailer mailer;

    @Inject
    UnsubscribeTokens unsubscribeTokens;

    /** Esito della conferma, con la lingua dell'iscritto per la pagina di ritorno. */
    public record ConfirmOutcome(boolean confirmed, String locale) {}

    /** Esito della disiscrizione, con la lingua dell'iscritto per la pagina di ritorno. */
    public record UnsubscribeOutcome(boolean done, String locale) {}

    private record Row(UUID id, String status, String locale, Instant confirmExpiresAt) {}

    /**
     * Iscrizione da un canale anonimo (sito/signup): porta l'indirizzo a {@code pending} e manda
     * l'email di conferma. Idempotente e neutra: se l'indirizzo è già {@code confirmed} non fa nulla
     * (nessuna nuova email), così l'endpoint pubblico non rivela lo stato di un indirizzo.
     */
    public void subscribeAnonymous(String rawEmail, String rawLocale, ConsentChannel channel) {
        String email = rawEmail == null ? "" : rawEmail.trim();
        String locale = NewsletterEmailRenderer.normalize(rawLocale);
        Instant now = Instant.now();
        String rawToken = InvitationTokens.newToken();
        String tokenHash = InvitationTokens.hash(rawToken);
        Instant expires = now.plus(CONFIRM_TTL);

        UUID subscriberId;
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                Row row = findByEmail(c, email);
                if (row != null && "confirmed".equals(row.status())) {
                    c.rollback();
                    return; // già iscritto e confermato: niente doppioni, risposta neutra
                }
                if (row == null) {
                    subscriberId = UUID.randomUUID();
                    insertSubscriber(c, subscriberId, email, "pending", locale, channel, null,
                            tokenHash, expires, now);
                } else {
                    subscriberId = row.id();
                    update(c, "update platform.newsletter_subscriber set status='pending', locale=?,"
                            + " origin_channel=?, confirm_token_hash=?, confirm_expires_at=?,"
                            + " confirmed_at=null, unsubscribed_at=null, updated_at=? where id=?",
                            ps -> {
                                ps.setString(1, locale);
                                ps.setString(2, channel.name());
                                ps.setString(3, tokenHash);
                                ps.setTimestamp(4, Timestamp.from(expires));
                                ps.setTimestamp(5, Timestamp.from(now));
                                ps.setObject(6, subscriberId);
                            });
                }
                recordEvent(c, subscriberId, ConsentEventType.grant, channel, now);
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("iscrizione newsletter fallita", e);
        }

        LOG.infof("newsletter.subscribe.pending subscriber_id=%s channel=%s", subscriberId, channel);
        mailer.sendConfirmation(email, locale, subscriberId, rawToken); // fuori transazione, fail-soft
    }

    /** Conferma del double opt-in tramite token single-use. Il token è consumato (azzerato). */
    public ConfirmOutcome confirm(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return new ConfirmOutcome(false, "en");
        }
        String tokenHash = InvitationTokens.hash(rawToken);
        Instant now = Instant.now();
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                Row row = findBy(c, "confirm_token_hash = ?", tokenHash);
                if (row == null) {
                    c.rollback();
                    return new ConfirmOutcome(false, "en");
                }
                if (row.confirmExpiresAt() == null || row.confirmExpiresAt().isBefore(now)) {
                    c.rollback();
                    return new ConfirmOutcome(false, row.locale());
                }
                update(c, "update platform.newsletter_subscriber set status='confirmed', confirmed_at=?,"
                        + " confirm_token_hash=null, confirm_expires_at=null, updated_at=? where id=?",
                        ps -> {
                            ps.setTimestamp(1, Timestamp.from(now));
                            ps.setTimestamp(2, Timestamp.from(now));
                            ps.setObject(3, row.id());
                        });
                recordEvent(c, row.id(), ConsentEventType.confirm, ConsentChannel.email, now);
                c.commit();
                LOG.infof("newsletter.confirm.ok subscriber_id=%s", row.id());
                return new ConfirmOutcome(true, row.locale());
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("conferma newsletter fallita", e);
        }
    }

    /** Disiscrizione one-click via token HMAC. Idempotente. */
    public UnsubscribeOutcome unsubscribeByToken(UUID subscriberId, String token) {
        if (subscriberId == null || !unsubscribeTokens.verify(subscriberId, token)) {
            return new UnsubscribeOutcome(false, "en");
        }
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                Row row = findBy(c, "id = ?", subscriberId);
                if (row == null) {
                    c.rollback();
                    return new UnsubscribeOutcome(false, "en");
                }
                if (!"unsubscribed".equals(row.status())) {
                    markUnsubscribed(c, row.id(), ConsentChannel.email, Instant.now());
                }
                c.commit();
                return new UnsubscribeOutcome(true, row.locale());
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("disiscrizione newsletter fallita", e);
        }
    }

    /**
     * Preferenza dell'utente autenticato (toggle in account): niente double opt-in, l'email è già
     * verificata. {@code subscribed=true} porta a {@code confirmed} (evento grant), {@code false} a
     * {@code unsubscribed} (evento revoke). Ritorna lo stato risultante (iscritto sì/no).
     */
    public boolean setPreference(String rawEmail, boolean subscribed, String rawLocale, UUID userId) {
        String email = rawEmail == null ? "" : rawEmail.trim();
        String locale = NewsletterEmailRenderer.normalize(rawLocale);
        Instant now = Instant.now();
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                Row row = findByEmail(c, email);
                if (subscribed) {
                    UUID id;
                    if (row == null) {
                        id = UUID.randomUUID();
                        insertSubscriber(c, id, email, "confirmed", locale, ConsentChannel.account, userId,
                                null, null, now);
                        setConfirmedAt(c, id, now);
                    } else {
                        id = row.id();
                        update(c, "update platform.newsletter_subscriber set status='confirmed', confirmed_at=?,"
                                + " unsubscribed_at=null, confirm_token_hash=null, confirm_expires_at=null,"
                                + " user_id=?, updated_at=? where id=?",
                                ps -> {
                                    ps.setTimestamp(1, Timestamp.from(now));
                                    ps.setObject(2, userId);
                                    ps.setTimestamp(3, Timestamp.from(now));
                                    ps.setObject(4, id);
                                });
                    }
                    recordEvent(c, id, ConsentEventType.grant, ConsentChannel.account, now);
                    c.commit();
                    LOG.infof("newsletter.preference.on subscriber_id=%s user_id=%s", id, userId);
                    return true;
                }
                if (row != null && !"unsubscribed".equals(row.status())) {
                    markUnsubscribed(c, row.id(), ConsentChannel.account, now);
                }
                c.commit();
                return false;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("preferenza newsletter fallita", e);
        }
    }

    /** Stato corrente della preferenza per un indirizzo (per il toggle in account). */
    public boolean isSubscribed(String rawEmail) {
        String email = rawEmail == null ? "" : rawEmail.trim();
        try (Connection c = ds.getConnection()) {
            Row row = findByEmail(c, email);
            return row != null && "confirmed".equals(row.status());
        } catch (SQLException e) {
            throw new RuntimeException("lettura preferenza newsletter fallita", e);
        }
    }

    // ── JDBC ─────────────────────────────────────────────────────────────────

    private Row findByEmail(Connection c, String email) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "select id, status, locale, confirm_expires_at from platform.newsletter_subscriber"
                        + " where lower(email) = lower(?)")) {
            ps.setString(1, email);
            return readRow(ps);
        }
    }

    private Row findBy(Connection c, String whereClause, Object param) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "select id, status, locale, confirm_expires_at from platform.newsletter_subscriber where "
                        + whereClause)) {
            ps.setObject(1, param);
            return readRow(ps);
        }
    }

    private static Row readRow(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            Timestamp expires = rs.getTimestamp("confirm_expires_at");
            return new Row(
                    rs.getObject("id", UUID.class),
                    rs.getString("status"),
                    rs.getString("locale"),
                    expires == null ? null : expires.toInstant());
        }
    }

    private void insertSubscriber(Connection c, UUID id, String email, String status, String locale,
            ConsentChannel channel, UUID userId, String tokenHash, Instant expires, Instant now) throws SQLException {
        update(c, "insert into platform.newsletter_subscriber"
                + "(id, email, status, locale, origin_channel, user_id, confirm_token_hash,"
                + " confirm_expires_at, created_at, updated_at)"
                + " values (?,?,?,?,?,?,?,?,?,?)",
                ps -> {
                    ps.setObject(1, id);
                    ps.setString(2, email);
                    ps.setString(3, status);
                    ps.setString(4, locale);
                    ps.setString(5, channel.name());
                    ps.setObject(6, userId);
                    ps.setString(7, tokenHash);
                    ps.setTimestamp(8, expires == null ? null : Timestamp.from(expires));
                    ps.setTimestamp(9, Timestamp.from(now));
                    ps.setTimestamp(10, Timestamp.from(now));
                });
    }

    private void setConfirmedAt(Connection c, UUID id, Instant now) throws SQLException {
        update(c, "update platform.newsletter_subscriber set confirmed_at=? where id=?", ps -> {
            ps.setTimestamp(1, Timestamp.from(now));
            ps.setObject(2, id);
        });
    }

    private void markUnsubscribed(Connection c, UUID id, ConsentChannel channel, Instant now) throws SQLException {
        update(c, "update platform.newsletter_subscriber set status='unsubscribed', unsubscribed_at=?,"
                + " confirm_token_hash=null, confirm_expires_at=null, updated_at=? where id=?",
                ps -> {
                    ps.setTimestamp(1, Timestamp.from(now));
                    ps.setTimestamp(2, Timestamp.from(now));
                    ps.setObject(3, id);
                });
        recordEvent(c, id, ConsentEventType.revoke, channel, now);
        LOG.infof("newsletter.unsubscribe subscriber_id=%s channel=%s", id, channel);
    }

    private void recordEvent(Connection c, UUID subscriberId, ConsentEventType type, ConsentChannel channel,
            Instant when) throws SQLException {
        update(c, "insert into platform.consent_event"
                + "(id, subscriber_id, event_type, consent_text_version, channel, occurred_at, created_at, updated_at)"
                + " values (?,?,?,?,?,?,?,?)",
                ps -> {
                    ps.setObject(1, UUID.randomUUID());
                    ps.setObject(2, subscriberId);
                    ps.setString(3, type.name());
                    ps.setString(4, CONSENT_VERSION);
                    ps.setString(5, channel.name());
                    ps.setTimestamp(6, Timestamp.from(when));
                    ps.setTimestamp(7, Timestamp.from(when));
                    ps.setTimestamp(8, Timestamp.from(when));
                });
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private static int update(Connection c, String sql, Binder binder) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            return ps.executeUpdate();
        }
    }
}
