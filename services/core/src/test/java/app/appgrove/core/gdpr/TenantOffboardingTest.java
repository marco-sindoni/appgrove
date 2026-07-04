package app.appgrove.core.gdpr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.commons.gdpr.ExportResult;
import app.appgrove.commons.gdpr.GdprQueues;
import app.appgrove.commons.gdpr.GdprScope;
import app.appgrove.commons.gdpr.TenantPurgeMessage;
import app.appgrove.commons.gdpr.TenantPurgeConsumer;
import app.appgrove.core.TestData;
import io.quarkus.test.junit.QuarkusTest;
import java.util.List;
import java.util.UUID;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Orchestrazione account-level dell'erasure (#13 L71, UC 0032 §9): l'offboarding pubblica l'evento
 * di purge per piattaforma + ogni app attivata (in locale = fan-out sulle code; nel cloud = regola
 * EventBridge, UC 0004); il consumer del core purga la piattaforma e registra l'audit (#13 L70).
 * È il punto d'ingresso che UC 0035 invocherà a fine grace 14gg.
 */
@QuarkusTest
class TenantOffboardingTest {

    private static final String TENANT = "77777777-0000-0000-0000-0000000000d1";
    private static final String TENANT_OTHER = "77777777-0000-0000-0000-0000000000d2";
    private static final UUID GDPR_APP_ID = UUID.fromString("99999999-1111-0000-0000-000000000002");
    private static final String GDPR_APP_SLUG = "gdprapp2";

    @Inject
    TenantOffboarding offboarding;

    @Inject
    TenantPurgeConsumer purgeConsumer;

    @Inject
    PlatformDataContract contract;

    @Inject
    TestMessageQueues queues;

    @Inject
    TestData data;

    @BeforeEach
    void reset() {
        queues.clear();
    }

    @Test
    void offboardingFansOutToPlatformAndActivatedAppsThenPurgesWithAudit() {
        data.account(TENANT, "Account in offboarding");
        data.user(TENANT, "sub-off-1", "off-1@example.test", "owner");
        data.invitation(TENANT, "off-invitato@example.test", "member");
        data.app(GDPR_APP_ID, GDPR_APP_SLUG);
        data.subscription(TENANT, GDPR_APP_ID, "canceled");
        data.account(TENANT_OTHER, "Account estraneo");
        data.user(TENANT_OTHER, "sub-off-2", "off-2@example.test", "owner");

        List<String> targets = offboarding.offboard(TENANT, TenantPurgeMessage.REASON_OFFBOARDED);

        // fan-out: piattaforma + app attivata (anche con subscription canceled)
        assertEquals(List.of("platform", GDPR_APP_SLUG), targets);
        assertEquals(1, queues.size(GdprQueues.purgeQueue("platform")));
        assertEquals(1, queues.size(GdprQueues.purgeQueue(GDPR_APP_SLUG)));

        // il consumer del core purga la piattaforma e registra l'audit
        assertEquals(1, purgeConsumer.drain());
        ExportResult after = contract.exportData(new GdprScope(TENANT));
        after.entities().forEach((entity, rows) ->
                assertTrue(rows.isEmpty(), "dati residui post-offboarding in " + entity));
        assertEquals(1, data.gdprPurgeAuditCount(TENANT, "platform"),
                "la purge deve lasciare la riga di audit (prova, #13 L70)");

        // il tenant estraneo è intatto
        ExportResult other = contract.exportData(new GdprScope(TENANT_OTHER));
        assertFalse(other.entities().get("users").isEmpty());

        // idempotenza: un secondo offboarding non fallisce e audita di nuovo (0 righe cancellate)
        offboarding.offboard(TENANT, TenantPurgeMessage.REASON_OFFBOARDED);
        assertEquals(1, purgeConsumer.drain());
        assertEquals(2, data.gdprPurgeAuditCount(TENANT, "platform"));
    }
}
