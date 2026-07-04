package app.appgrove.core.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.appgrove.core.TestData;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Retention dei ticket — 24 mesi dalla chiusura (UC 0034 §9, #13 E): lo sweeper elimina
 * fisicamente (minimizzazione) i ticket chiusi oltre finestra, thread compreso, e lascia
 * intatti quelli recenti o aperti. "Adesso" iniettabile: chiusure retrodatate, niente attese.
 */
@QuarkusTest
class TicketRetentionSweeperTest {

    private static final String TENANT = "11111111-0000-0000-0000-0000000000f1";

    @Inject
    TestData data;

    @Inject
    TicketRetentionSweeper sweeper;

    @Test
    void expiredClosedTicketsAreHardDeletedRecentOnesKept() {
        data.account(TENANT, "Retention Tenant");
        UUID expired = data.ticket(TENANT, "support", "Vecchio", "closed");
        data.ticketMessage(TENANT, expired, "user", "vecchio thread");
        data.backdateTicketClosure(expired, OffsetDateTime.now().minusMonths(25));
        UUID recent = data.ticket(TENANT, "support", "Recente", "closed");
        data.backdateTicketClosure(recent, OffsetDateTime.now().minusMonths(1));
        UUID open = data.ticket(TENANT, "privacy", "Aperto", "open");

        int deleted = sweeper.sweep(Instant.now());

        assertEquals(0, data.ticketCount(expired), "il ticket oltre retention va eliminato");
        assertEquals(1, data.ticketCount(recent), "il ticket chiuso di recente resta");
        assertEquals(1, data.ticketCount(open), "il ticket aperto resta");
        assertEquals(1, deleted, "lo sweep deve riportare l'eliminazione");
    }
}
