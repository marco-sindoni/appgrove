package app.appgrove.core.gdpr;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.appgrove.core.TestData;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Instant;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

/**
 * Retention delle tabelle di prova GDPR nel DB (UC 0035, #08): lo sweeper elimina a 12 mesi le righe
 * di {@code gdpr_purge_audit} e {@code gdpr_restriction_audit} in schema platform, e lascia intatte
 * quelle recenti. "Adesso" iniettabile: righe retrodatate, niente attese.
 */
@QuarkusTest
class GdprAuditRetentionSweeperTest {

    private static final String TENANT = "44444444-0000-0000-0000-0000000035c1";

    @Inject
    TestData data;

    @Inject
    GdprAuditRetentionSweeper sweeper;

    @Test
    void expiredAuditRowsAreDeletedRecentOnesKept() {
        data.account(TENANT, "Retention audit");
        // Oltre 12 mesi → da eliminare.
        data.gdprPurgeAudit(TENANT, "platform", OffsetDateTime.now().minusMonths(13));
        data.gdprRestrictionAudit(TENANT, OffsetDateTime.now().minusMonths(13));
        // Entro i 12 mesi → da conservare.
        data.gdprPurgeAudit(TENANT, "platform", OffsetDateTime.now().minusMonths(1));
        data.gdprRestrictionAudit(TENANT, OffsetDateTime.now().minusMonths(1));

        int deleted = sweeper.sweep(Instant.now());

        assertEquals(2, deleted, "le due righe oltre retention vanno eliminate");
        assertEquals(1, data.auditRowCount("platform.gdpr_purge_audit", TENANT), "resta la purge recente");
        assertEquals(1, data.auditRowCount("platform.gdpr_restriction_audit", TENANT), "resta la limitazione recente");

        // Idempotenza: un secondo sweep non elimina più nulla.
        assertEquals(0, sweeper.sweep(Instant.now()));
    }
}
