package app.appgrove.core.newsletter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Retention degli iscritti (UC 0039, #13 E): eliminazione fisica dei disiscritti da oltre 24 mesi,
 * mentre disiscritti recenti e confermati sopravvivono. "adesso" iniettato per non attendere davvero.
 */
@QuarkusTest
class NewsletterRetentionSweeperTest {

    @Inject
    NewsletterRetentionSweeper sweeper;

    @Inject
    AgroalDataSource ds;

    @Test
    void deletesOnlyLongUnsubscribed() {
        Instant now = Instant.now();
        UUID oldUnsub = insert("old-unsub-" + System.nanoTime() + "@example.com", "unsubscribed",
                now.minus(800, ChronoUnit.DAYS));      // > 24 mesi
        UUID recentUnsub = insert("recent-unsub-" + System.nanoTime() + "@example.com", "unsubscribed",
                now.minus(10, ChronoUnit.DAYS));        // < 24 mesi
        UUID confirmed = insert("confirmed-" + System.nanoTime() + "@example.com", "confirmed", null);

        int deleted = sweeper.sweep(now);

        assertEquals(1, deleted, "solo il disiscritto oltre 24 mesi va eliminato");
        assertNull(statusOf(oldUnsub));
        assertEquals("unsubscribed", statusOf(recentUnsub));
        assertEquals("confirmed", statusOf(confirmed));
    }

    private UUID insert(String email, String status, Instant unsubscribedAt) {
        UUID id = UUID.randomUUID();
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "insert into platform.newsletter_subscriber"
                                + "(id,email,status,locale,origin_channel,unsubscribed_at,created_at,updated_at)"
                                + " values (?,?,?,?,?,?,?,?)")) {
            Timestamp now = Timestamp.from(Instant.now());
            ps.setObject(1, id);
            ps.setString(2, email);
            ps.setString(3, status);
            ps.setString(4, "en");
            ps.setString(5, "site");
            ps.setTimestamp(6, unsubscribedAt == null ? null : Timestamp.from(unsubscribedAt));
            ps.setTimestamp(7, now);
            ps.setTimestamp(8, now);
            ps.executeUpdate();
            return id;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String statusOf(UUID id) {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "select status from platform.newsletter_subscriber where id = ?")) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
