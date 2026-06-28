package app.appgrove.core.billing;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.appgrove.core.TestData;
import app.appgrove.core.TestTokens;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * L1-minimo della pipeline locale (UC 0023): scenari sintetici firmati → ingest → consumer →
 * {@code subscription}. Verifica evoluzione stato per scenario, idempotenza, linkage/isolamento tenant
 * e cambio tier su upgrade. Deterministico (coda in-memory, drain esplicito).
 */
@QuarkusTest
class SubscriptionPipelineTest {

    private static final String TENANT_A = "aaaaaaaa-0000-0000-0000-000000000001";
    private static final String TENANT_B = "bbbbbbbb-0000-0000-0000-000000000002";
    private static final String DEV = "/api/platform/v1/dev/paddle";

    @Inject
    TestData data;

    @Inject
    PaddleWebhookConsumer consumer;

    @Inject
    InMemoryWebhookQueue queue;

    @BeforeEach
    void reset() {
        queue.clear();
        data.account(TENANT_A, "Acme");
    }

    @Test
    void happyPathActivatesSubscription() {
        UUID app = newApp();
        emit(TENANT_A, "happy_path", body(app, null, null));
        consumer.drain();

        subscription(TENANT_A, app).statusCode(200).body("status", is("active"));
    }

    @Test
    void reapplyingTheScenarioIsIdempotent() {
        UUID app = newApp();
        emit(TENANT_A, "happy_path", body(app, null, null));
        consumer.drain();
        emit(TENANT_A, "happy_path", body(app, null, null));
        consumer.drain();

        subscription(TENANT_A, app).statusCode(200).body("status", is("active"));
        assertEquals(1, data.subscriptionCount(TENANT_A, app), "nessuna subscription duplicata per (tenant, app)");
    }

    @Test
    void pastDueScenarioEndsPastDue() {
        UUID app = newApp();
        emit(TENANT_A, "past_due", body(app, null, null));
        consumer.drain();

        subscription(TENANT_A, app).statusCode(200).body("status", is("past_due"));
    }

    @Test
    void canceledScenarioEndsCanceled() {
        UUID app = newApp();
        emit(TENANT_A, "canceled", body(app, null, null));
        consumer.drain();

        subscription(TENANT_A, app).statusCode(200).body("status", is("canceled"));
    }

    @Test
    void upgradeChangesTier() {
        UUID app = newApp();
        UUID basic = UUID.randomUUID();
        UUID pro = UUID.randomUUID();
        data.appTier(basic, app, "basic");
        data.appTier(pro, app, "pro");

        emit(TENANT_A, "upgrade", body(app, basic, pro));
        consumer.drain();

        subscription(TENANT_A, app)
                .statusCode(200)
                .body("status", is("active"))
                .body("appTierId", is(pro.toString()));
    }

    @Test
    void subscriptionIsTenantScoped() {
        UUID app = newApp();
        emit(TENANT_A, "happy_path", body(app, null, null));
        consumer.drain();

        // il tenant B non vede la subscription del tenant A (discriminator, invariante #2)
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_B, "owner"))
                .when().get(DEV + "/subscriptions")
                .then().statusCode(200).body("size()", is(0));
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_B, "owner"))
                .when().get(DEV + "/subscriptions/" + app)
                .then().statusCode(404);
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private UUID newApp() {
        UUID app = UUID.randomUUID();
        data.app(app, "app-" + app.toString().substring(0, 8));
        return app;
    }

    private static Map<String, Object> body(UUID appId, UUID appTierId, UUID targetTierId) {
        Map<String, Object> m = new HashMap<>();
        m.put("appId", appId.toString());
        if (appTierId != null) {
            m.put("appTierId", appTierId.toString());
        }
        if (targetTierId != null) {
            m.put("targetTierId", targetTierId.toString());
        }
        return m;
    }

    private static void emit(String tenant, String scenario, Map<String, Object> body) {
        given().header("Authorization", "Bearer " + TestTokens.withTenant(tenant, "owner"))
                .contentType(ContentType.JSON)
                .body(body)
                .when().post(DEV + "/scenarios/" + scenario)
                .then().statusCode(202);
    }

    private static io.restassured.response.ValidatableResponse subscription(String tenant, UUID appId) {
        return given().header("Authorization", "Bearer " + TestTokens.withTenant(tenant, "owner"))
                .when().get(DEV + "/subscriptions/" + appId)
                .then();
    }
}
