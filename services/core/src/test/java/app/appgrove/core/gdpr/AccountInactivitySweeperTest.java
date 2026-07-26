package app.appgrove.core.gdpr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.commons.gdpr.GdprQueues;
import app.appgrove.core.TestData;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Auto-cancellazione account inattivi (UC 0035 §9, #13 E26): a 24 mesi di inattività scatta un solo
 * avviso email ai proprietari (l'account resta usabile); a +30 giorni senza attività l'account è
 * offboardato e soft-cancellato; se torna attivo dopo l'avviso viene recuperato. "Adesso"
 * iniettabile e attività retrodatate: niente attese; idempotente e tenant-scoped.
 */
@QuarkusTest
class AccountInactivitySweeperTest {

    private static final String TENANT_WARN = "33333333-0000-0000-0000-0000000035b1";
    private static final String TENANT_PURGE = "33333333-0000-0000-0000-0000000035b2";
    private static final String TENANT_RECOVER = "33333333-0000-0000-0000-0000000035b3";
    private static final String TENANT_FRESH = "33333333-0000-0000-0000-0000000035b4";
    private static final UUID APP_ID = UUID.fromString("99999999-2222-0000-0000-000000000b35");
    private static final String APP_SLUG = "inactiveapp";

    @Inject
    TestData data;

    @Inject
    TestMessageQueues queues;

    @Inject
    AccountInactivitySweeper sweeper;

    @Inject
    MockMailbox mailbox;

    @BeforeEach
    void reset() {
        queues.clear();
        mailbox.clear();
    }

    @Test
    void warnsInactiveAccountOnceAndKeepsItActive() {
        data.account(TENANT_WARN, "Inattivo da avvisare");
        data.user(TENANT_WARN, "sub-warn", "owner-warn@example.com", "owner");
        data.backdateAccountActivity(TENANT_WARN, OffsetDateTime.now().minusMonths(25));
        // Un account fresco (attivo di recente) non deve essere toccato.
        data.account(TENANT_FRESH, "Attivo di recente");
        data.user(TENANT_FRESH, "sub-fresh", "owner-fresh@example.com", "owner");

        sweeper.sweep(Instant.now());

        assertEquals(1, mailbox.getMailsSentTo("owner-warn@example.com").size(), "un solo avviso al proprietario");
        assertNotNull(data.accountInactivityWarnedAt(TENANT_WARN), "l'avviso è registrato");
        assertFalse(data.accountSoftDeleted(TENANT_WARN), "durante l'avviso l'account resta usabile");
        assertEquals("active", data.accountStatus(TENANT_WARN));
        assertNull(data.accountInactivityWarnedAt(TENANT_FRESH), "l'account attivo non viene avvisato");
        assertTrue(mailbox.getMailsSentTo("owner-fresh@example.com").isEmpty());

        // Secondo sweep subito dopo: nessun secondo avviso (già avvisato, grace non scaduta).
        sweeper.sweep(Instant.now());
        assertEquals(1, mailbox.getMailsSentTo("owner-warn@example.com").size(), "nessun avviso doppio");
    }

    @Test
    void purgesAfterWarningGraceWhenStillInactive() {
        data.account(TENANT_PURGE, "Inattivo da cancellare");
        data.user(TENANT_PURGE, "sub-purge", "owner-purge@example.com", "owner");
        data.app(APP_ID, APP_SLUG);
        data.subscription(TENANT_PURGE, APP_ID, "active");
        data.backdateAccountActivity(TENANT_PURGE, OffsetDateTime.now().minusMonths(26));
        data.setInactivityWarnedAt(TENANT_PURGE, OffsetDateTime.now().minusDays(31));

        sweeper.sweep(Instant.now());

        assertEquals(1, queues.size(GdprQueues.purgeQueue(PlatformDataContract.APP_ID)), "purge piattaforma");
        assertEquals(1, queues.size(GdprQueues.purgeQueue(APP_SLUG)), "purge dell'app attivata");
        assertTrue(data.accountSoftDeleted(TENANT_PURGE), "l'account è soft-cancellato");

        // Idempotenza: l'account soft-cancellato non viene riprocessato.
        sweeper.sweep(Instant.now());
        assertEquals(1, queues.size(GdprQueues.purgeQueue(PlatformDataContract.APP_ID)), "nessun secondo fan-out");
    }

    @Test
    void recoversWhenActivityResumesAfterWarning() {
        data.account(TENANT_RECOVER, "Avvisato ma tornato attivo");
        data.user(TENANT_RECOVER, "sub-recover", "owner-recover@example.com", "owner");
        // Avvisato oltre 30 giorni fa, ma con attività RECENTE (successiva all'avviso).
        data.setInactivityWarnedAt(TENANT_RECOVER, OffsetDateTime.now().minusDays(31));
        data.backdateAccountActivity(TENANT_RECOVER, OffsetDateTime.now().minusDays(1));

        sweeper.sweep(Instant.now());

        assertNull(data.accountInactivityWarnedAt(TENANT_RECOVER), "l'avviso è annullato");
        assertFalse(data.accountSoftDeleted(TENANT_RECOVER), "l'account non viene cancellato");
        assertEquals(0, queues.size(GdprQueues.purgeQueue(PlatformDataContract.APP_ID)), "nessun offboarding");
    }
}
