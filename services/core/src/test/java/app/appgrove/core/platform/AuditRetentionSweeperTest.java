package app.appgrove.core.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.appgrove.core.TestData;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Conservazione delle tabelle di prova nel DB (UC 0035, #08 — estesa da UC 0076): lo sweeper elimina a
 * 12 mesi le righe di {@code gdpr_purge_audit}, {@code gdpr_restriction_audit} e
 * {@code app_status_audit} in schema platform, e lascia intatte quelle recenti. "Adesso" iniettabile:
 * righe retrodatate, niente attese.
 */
@QuarkusTest
class AuditRetentionSweeperTest {

    private static final String TENANT = "44444444-0000-0000-0000-0000000035c1";
    private static final UUID APP = UUID.fromString("44444444-0000-0000-0000-0000000076a1");

    @Inject
    TestData data;

    @Inject
    AuditRetentionSweeper sweeper;

    @Test
    void expiredAuditRowsAreDeletedRecentOnesKept() {
        data.account(TENANT, "Retention audit");
        data.app(APP, "retention-audit-app");
        // Oltre 12 mesi → da eliminare.
        data.gdprPurgeAudit(TENANT, "platform", OffsetDateTime.now().minusMonths(13));
        data.gdprRestrictionAudit(TENANT, OffsetDateTime.now().minusMonths(13));
        data.appStatusAudit(APP, "active", "inactive", OffsetDateTime.now().minusMonths(13));
        // Entro i 12 mesi → da conservare.
        data.gdprPurgeAudit(TENANT, "platform", OffsetDateTime.now().minusMonths(1));
        data.gdprRestrictionAudit(TENANT, OffsetDateTime.now().minusMonths(1));
        data.appStatusAudit(APP, "inactive", "active", OffsetDateTime.now().minusMonths(1));

        int deleted = sweeper.sweep(Instant.now());

        assertEquals(3, deleted, "le tre righe oltre conservazione vanno eliminate");
        assertEquals(1, data.auditRowCount("platform.gdpr_purge_audit", TENANT), "resta la purge recente");
        assertEquals(
                1, data.auditRowCount("platform.gdpr_restriction_audit", TENANT), "resta la limitazione recente");
        assertEquals(1, data.appStatusAuditCount(APP), "resta la transizione di stato app recente");

        // Idempotenza: un secondo sweep non elimina più nulla.
        assertEquals(0, sweeper.sweep(Instant.now()));
    }
}
