package app.appgrove.core.gdpr;

import io.agroal.api.AgroalDataSource;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * Auto-cancellazione degli account inattivi (UC 0035, flusso E.2, #13 E26): dopo 24 mesi senza
 * attività l'account riceve un <b>avviso email</b> ai proprietari e, se resta inattivo per altri 30
 * giorni, viene offboardato (stesso finale di {@link AccountDeletionSweeper}). Diverso dalla grace
 * di eliminazione volontaria (change 0029): durante l'avviso l'account resta {@code active} e
 * usabile — accedere è il modo per "rispondere" e annullare la cancellazione (fase di recupero).
 *
 * <p>Stesso pattern degli altri sweeper: {@code @Scheduled} orario, {@code sweep(Instant)} con
 * "adesso" iniettabile (test senza attese reali), accesso dati via JDBC con tenant esplicito
 * (fuori richiesta il resolver Hibernate è fail-closed). In locale/cloud gira sullo scheduler
 * applicativo Quarkus nel task del core; il trigger esterno EventBridge/cron serve solo con più
 * task (alta disponibilità E3) ed è tracciato in UC 0035.
 */
@ApplicationScoped
public class AccountInactivitySweeper {

    private static final Logger LOG = Logger.getLogger(AccountInactivitySweeper.class);

    /** Inattività oltre la quale scatta l'avviso (#13 E26). */
    static final int INACTIVITY_MONTHS = 24;

    /** Silenzio dopo l'avviso oltre il quale scatta la cancellazione (#13 E26). */
    static final int WARNING_GRACE_DAYS = 30;

    @Inject
    AgroalDataSource ds;

    @Inject
    TenantOffboarding offboarding;

    @Inject
    Mailer mailer;

    @Scheduled(every = "1h", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void run() {
        sweep(Instant.now());
    }

    /**
     * Esegue le due fasi rispetto a {@code now} (iniettabile nei test): avviso dei nuovi inattivi e
     * cancellazione/recupero di quelli già avvisati. Idempotente e tenant-scoped.
     */
    public void sweep(Instant now) {
        warnNewlyInactive(now);
        purgeOrRecoverWarned(now);
    }

    // ── Fase 1: avviso a 24 mesi di inattività ────────────────────────────────────────────────
    private void warnNewlyInactive(Instant now) {
        Instant cutoff = now.atZone(ZoneOffset.UTC).minusMonths(INACTIVITY_MONTHS).toInstant();
        for (String tenantId : candidates(
                "select id from platform.accounts where status = 'active' and deleted_at is null"
                        + " and inactivity_warned_at is null and last_active_at <= ? order by id",
                cutoff)) {
            Instant deletionAt = now.plus(WARNING_GRACE_DAYS, ChronoUnit.DAYS);
            notifyOwners(tenantId, deletionAt);
            markWarned(tenantId, now);
            LOG.infof("account.inactivity.warned tenant_id=%s inactive_since_before=%s deletion_at=%s",
                    tenantId, cutoff, deletionAt);
        }
    }

    // ── Fase 2: a +30gg dall'avviso, cancellazione (o recupero se è tornato attivo) ────────────
    private void purgeOrRecoverWarned(Instant now) {
        Instant graceCutoff = now.atZone(ZoneOffset.UTC).minusDays(WARNING_GRACE_DAYS).toInstant();
        List<WarnedAccount> warned = warnedBefore(graceCutoff);
        for (WarnedAccount acc : warned) {
            if (acc.lastActiveAt().isAfter(acc.warnedAt())) {
                // Attività ripresa dopo l'avviso → recuperato: azzera l'avviso, nessuna cancellazione.
                clearWarning(acc.tenantId());
                LOG.infof("account.inactivity.recovered tenant_id=%s", acc.tenantId());
            } else {
                offboarding.offboard(acc.tenantId(), "account-inactive-24m");
                markDeleted(acc.tenantId(), now);
                LOG.infof("account.inactivity.purged tenant_id=%s warned_at=%s", acc.tenantId(), acc.warnedAt());
            }
        }
    }

    private record WarnedAccount(String tenantId, Instant lastActiveAt, Instant warnedAt) {}

    private List<String> candidates(String sql, Instant cutoff) {
        List<String> ids = new ArrayList<>();
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(cutoff));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getString(1));
                }
            }
            return ids;
        } catch (SQLException e) {
            throw new RuntimeException("lettura account inattivi fallita", e);
        }
    }

    private List<WarnedAccount> warnedBefore(Instant graceCutoff) {
        List<WarnedAccount> rows = new ArrayList<>();
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "select id, last_active_at, inactivity_warned_at from platform.accounts"
                                + " where status = 'active' and deleted_at is null"
                                + " and inactivity_warned_at is not null and inactivity_warned_at <= ?"
                                + " order by id")) {
            ps.setTimestamp(1, Timestamp.from(graceCutoff));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new WarnedAccount(rs.getString(1),
                            rs.getTimestamp(2).toInstant(), rs.getTimestamp(3).toInstant()));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new RuntimeException("lettura account avvisati fallita", e);
        }
    }

    private List<String> ownerEmails(String tenantId) {
        List<String> emails = new ArrayList<>();
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "select email from platform.users where tenant_id = ? and role = 'owner'"
                                + " and status = 'active' and deleted_at is null order by email")) {
            ps.setString(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    emails.add(rs.getString(1));
                }
            }
            return emails;
        } catch (SQLException e) {
            throw new RuntimeException("lettura proprietari fallita per il tenant " + tenantId, e);
        }
    }

    /**
     * Avviso ai proprietari (ruolo OWNER) via {@link Mailer} — Mailpit in {@code %dev}, SES nel
     * cloud. <b>Fail-soft</b> come {@code TicketNotifier}: un errore di invio non blocca né altera lo
     * stato del job. Testo bilingue italiano/inglese (localizzazione per-lingua rimandata a UC 0018/0060).
     */
    private void notifyOwners(String tenantId, Instant deletionAt) {
        List<String> emails = ownerEmails(tenantId);
        if (emails.isEmpty()) {
            LOG.warnf("account.inactivity.warn-no-owner tenant_id=%s (nessun proprietario a cui inviare)", tenantId);
            return;
        }
        String subject = "[appgrove] Il tuo account sta per essere eliminato per inattività"
                + " / Your account is about to be deleted for inactivity";
        String body = "Il tuo account appgrove è inattivo da oltre " + INACTIVITY_MONTHS + " mesi e verrà"
                + " eliminato definitivamente fra " + WARNING_GRACE_DAYS + " giorni. Per conservarlo è"
                + " sufficiente accedere: l'accesso annulla la cancellazione.\n\n"
                + "Your appgrove account has been inactive for over " + INACTIVITY_MONTHS + " months and"
                + " will be permanently deleted in " + WARNING_GRACE_DAYS + " days. To keep it, simply sign"
                + " in: signing in cancels the deletion.\n";
        for (String to : emails) {
            try {
                mailer.send(Mail.withText(to, subject, body));
            } catch (RuntimeException e) {
                LOG.warnf(e, "account.inactivity.warn-email-failed tenant_id=%s (best-effort)", tenantId);
            }
        }
    }

    private void markWarned(String tenantId, Instant now) {
        update("update platform.accounts set inactivity_warned_at = ?, updated_at = ? where id = ?::uuid",
                tenantId, now, now);
    }

    private void clearWarning(String tenantId) {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "update platform.accounts set inactivity_warned_at = null, updated_at = ?"
                                + " where id = ?::uuid")) {
            ps.setTimestamp(1, Timestamp.from(Instant.now()));
            ps.setString(2, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("azzeramento avviso inattività fallito per il tenant " + tenantId, e);
        }
    }

    private void markDeleted(String tenantId, Instant now) {
        update("update platform.accounts set deleted_at = ?, updated_at = ? where id = ?::uuid",
                tenantId, now, now);
    }

    private void update(String sql, String tenantId, Instant ts1, Instant ts2) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(ts1));
            ps.setTimestamp(2, Timestamp.from(ts2));
            ps.setString(3, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("aggiornamento account inattivo fallito per il tenant " + tenantId, e);
        }
    }
}
