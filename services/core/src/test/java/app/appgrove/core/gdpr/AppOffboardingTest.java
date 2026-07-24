package app.appgrove.core.gdpr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.commons.gdpr.GdprQueues;
import app.appgrove.core.TestData;
import io.quarkus.test.junit.QuarkusTest;
import java.util.List;
import java.util.UUID;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Orchestrazione app-level dell'erasure a supporto di {@code drop-application} (UC 0048): dato un
 * app_id, {@link AppOffboarding} deve enumerare <b>tutti e soli</b> i tenant con dati in quell'app
 * (subscription, incluse le soft-deleted) e accodare una purge per ciascuno sulla coda
 * {@code tenant-purge-<app_id>} — senza toccare la coda di nessun'altra app. La purga vera è poi
 * eseguita dal consumer del servizio dell'app (non dal core), quindi qui si verifica il fan-out.
 */
@QuarkusTest
class AppOffboardingTest {

    // tenant scelti in ordine lessicografico noto (la query ordina per tenant_id)
    private static final String TENANT_A = "88888888-0000-0000-0000-0000000000a1";
    private static final String TENANT_B = "88888888-0000-0000-0000-0000000000b2";
    private static final String TENANT_C = "88888888-0000-0000-0000-0000000000c3";

    private static final UUID DROP_APP_ID = UUID.fromString("99999999-2222-0000-0000-000000000010");
    private static final String DROP_APP_SLUG = "dropapp";
    private static final UUID OTHER_APP_ID = UUID.fromString("99999999-2222-0000-0000-000000000011");
    private static final String OTHER_APP_SLUG = "otherapp";
    // app dedicata al caso "nessun tenant": tenuta separata perché i test @QuarkusTest condividono il
    // DB senza rollback e uno slug già popolato da un altro test non sarebbe più vuoto.
    private static final UUID EMPTY_APP_ID = UUID.fromString("99999999-2222-0000-0000-000000000012");
    private static final String EMPTY_APP_SLUG = "dropappempty";

    @Inject
    AppOffboarding offboarding;

    @Inject
    TestMessageQueues queues;

    @Inject
    TestData data;

    @BeforeEach
    void reset() {
        queues.clear();
    }

    @Test
    void offboardAppFansOutToEveryTenantOfThatAppAndNoOther() {
        // due tenant sull'app da dismettere: uno attivo, uno con subscription soft-deleted (i dati
        // possono esistere ancora) → entrambi devono essere purgati.
        data.account(TENANT_A, "Tenant A");
        data.account(TENANT_B, "Tenant B");
        data.app(DROP_APP_ID, DROP_APP_SLUG);
        data.subscription(TENANT_A, DROP_APP_ID, "active");
        data.subscription(TENANT_B, DROP_APP_ID, "canceled");
        data.softDeleteSubscriptions(TENANT_B, DROP_APP_ID);

        // un terzo tenant su un'app diversa: non deve essere toccato.
        data.account(TENANT_C, "Tenant C");
        data.app(OTHER_APP_ID, OTHER_APP_SLUG);
        data.subscription(TENANT_C, OTHER_APP_ID, "active");

        List<String> tenants = offboarding.offboardApp(DROP_APP_SLUG, AppOffboarding.REASON_APP_OFFBOARDED);

        // enumerazione: entrambi i tenant dell'app, in ordine stabile, nessun altro
        assertEquals(List.of(TENANT_A, TENANT_B), tenants);

        // fan-out: una purge per tenant sulla coda dell'app dismessa, zero su quella dell'altra app
        assertEquals(2, queues.size(GdprQueues.purgeQueue(DROP_APP_SLUG)));
        assertEquals(0, queues.size(GdprQueues.purgeQueue(OTHER_APP_SLUG)));
    }

    @Test
    void offboardAppWithNoTenantsIsANoOp() {
        data.app(EMPTY_APP_ID, EMPTY_APP_SLUG);

        List<String> tenants = offboarding.offboardApp(EMPTY_APP_SLUG, AppOffboarding.REASON_APP_OFFBOARDED);

        assertTrue(tenants.isEmpty(), "nessuna subscription → nessun tenant da purgare");
        assertEquals(0, queues.size(GdprQueues.purgeQueue(EMPTY_APP_SLUG)));
    }
}
