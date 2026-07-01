package app.appgrove.core.billing;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import app.appgrove.core.TestData;
import app.appgrove.core.TestTokens;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration UC 0028 (portale cliente self-service). Verifica il giro
 * command → provider → webhook → read-model: read-model {@code /me/subscriptions} (anche non-attive),
 * upgrade immediato vs downgrade schedulato, disdici/riattiva, portal server-side, OWNER-only, isolamento
 * per tenant. Deterministico (coda in-memory, {@code drain()} esplicito).
 */
@QuarkusTest
class SubscriptionSelfServiceTest {

    private static final String TENANT_A = "aaaaaaaa-0000-0000-0000-0000000d0001";
    private static final String TENANT_B = "bbbbbbbb-0000-0000-0000-0000000d0002";
    private static final String SUBS = "/api/platform/v1/me/subscriptions";
    private static final String PORTAL = "/api/platform/v1/me/portal-session";
    private static final String DEV = "/api/platform/v1/dev/paddle";

    @Inject
    TestData data;

    @Inject
    PaddleWebhookConsumer consumer;

    @Inject
    InMemoryWebhookQueue queue;

    private UUID appId;
    private String appSlug;
    private UUID basicTier;
    private UUID proTier;

    @BeforeEach
    void setup() {
        queue.clear();
        data.account(TENANT_A, "Acme");
        data.account(TENANT_B, "Bob");
        appId = UUID.randomUUID();
        appSlug = "app-" + appId.toString().substring(0, 8);
        data.app(appId, appSlug);
        basicTier = UUID.randomUUID();
        proTier = UUID.randomUUID();
        data.appTier(basicTier, appId, "basic");
        data.appTier(proTier, appId, "pro");
        data.appPrice(UUID.randomUUID(), basicTier, "monthly", "pri_basic_" + basicTier.toString().substring(0, 8), 500);
        data.appPrice(UUID.randomUUID(), proTier, "monthly", "pri_pro_" + proTier.toString().substring(0, 8), 900);
    }

    // ── read-model ─────────────────────────────────────────────────────────────
    @Test
    void listsSubscriptionsIncludingNonActive() {
        activateOn(TENANT_A, basicTier);
        cancelViaScenario(TENANT_A); // porta a canceled (non concede accesso)

        // il read-model del portale mostra ANCHE la subscription canceled (a differenza di /me/entitlements)
        mySubs(TENANT_A)
                .statusCode(200)
                .body("subscriptions.find { it.appSlug == '" + appSlug + "' }.status", is("canceled"))
                .body("subscriptions.find { it.appSlug == '" + appSlug + "' }.phase", is("ENDED"))
                .body("subscriptions.find { it.appSlug == '" + appSlug + "' }.canReactivate", is(true));
    }

    // ── upgrade immediato / downgrade schedulato ───────────────────────────────
    @Test
    void upgradeChangesTierImmediately() {
        activateOn(TENANT_A, basicTier);

        changeTier(TENANT_A, "pro").statusCode(200).body("direction", is("UPGRADE"));
        consumer.drain();

        sub(TENANT_A).body("tierKey", is("pro")).body("scheduledTierKey", nullValue());
    }

    @Test
    void downgradeSchedulesTierChangeAtPeriodEnd() {
        activateOn(TENANT_A, proTier);

        changeTier(TENANT_A, "basic")
                .statusCode(200)
                .body("direction", is("DOWNGRADE"))
                .body("effectiveAt", notNullValue());
        consumer.drain();

        // resta sul tier corrente (pro) fino a fine periodo, ma il downgrade schedulato è persistito e mostrato
        sub(TENANT_A)
                .body("tierKey", is("pro"))
                .body("phase", is("ACTIVE"))
                .body("scheduledTierKey", is("basic"))
                .body("scheduledChangeAt", notNullValue());
    }

    @Test
    void changeToSameTierIsRejected() {
        activateOn(TENANT_A, proTier);
        changeTier(TENANT_A, "pro").statusCode(400);
    }

    // ── disdici / riattiva ─────────────────────────────────────────────────────
    @Test
    void cancelThenResume() {
        activateOn(TENANT_A, proTier);

        given().header("Authorization", "Bearer " + owner(TENANT_A))
                .when().post(SUBS + "/" + appSlug + "/cancel")
                .then().statusCode(200).body("direction", is("CANCEL"));
        consumer.drain();
        sub(TENANT_A).body("cancelAt", notNullValue()).body("phase", is("CANCELING")).body("canResume", is(true));

        given().header("Authorization", "Bearer " + owner(TENANT_A))
                .when().post(SUBS + "/" + appSlug + "/resume")
                .then().statusCode(200).body("direction", is("RESUME"));
        consumer.drain();
        sub(TENANT_A).body("cancelAt", nullValue()).body("phase", is("ACTIVE"));
    }

    // ── permessi & isolamento ──────────────────────────────────────────────────
    @Test
    void changeTierRequiresOwner() {
        activateOn(TENANT_A, basicTier);
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_A, "member"))
                .contentType(ContentType.JSON)
                .body(Map.of("targetTierKey", "pro", "billingCycle", "monthly"))
                .when().post(SUBS + "/" + appSlug + "/change-tier")
                .then().statusCode(403);
    }

    @Test
    void actionsAreTenantScoped() {
        activateOn(TENANT_A, proTier);

        // B non vede le subscription di A
        mySubs(TENANT_B).statusCode(200)
                .body("subscriptions.find { it.appSlug == '" + appSlug + "' }", nullValue());
        // B non può agire sull'abbonamento di A (nessuna sua subscription per quell'app → 404)
        changeTier(TENANT_B, "basic").statusCode(404);
    }

    // ── portal server-side ─────────────────────────────────────────────────────
    @Test
    void portalRequiresPaddleCustomerThenReturnsUrl() {
        String tenant = "cccccccc-0000-0000-0000-" + UUID.randomUUID().toString().substring(24);
        data.account(tenant, "Portal");

        // nessun customer Paddle ancora → 409
        given().header("Authorization", "Bearer " + owner(tenant))
                .when().post(PORTAL).then().statusCode(409);

        // customer.updated (scenario dev) popola accounts.paddle_customer_id
        emitScenario(tenant, "customer");
        consumer.drain();

        given().header("Authorization", "Bearer " + owner(tenant))
                .when().post(PORTAL).then().statusCode(200).body("url", notNullValue());
    }

    // ── helper ─────────────────────────────────────────────────────────────────

    private static String owner(String tenant) {
        return TestTokens.withTenant(tenant, "owner");
    }

    private void activateOn(String tenant, UUID tierId) {
        emit(tenant, "happy_path", body(appId, tierId, null));
        consumer.drain();
    }

    /** Porta la subscription a canceled via scenario dev (per verificare che il read-model la mostri). */
    private void cancelViaScenario(String tenant) {
        emit(tenant, "canceled", body(appId, basicTier, null));
        consumer.drain();
    }

    private void emitScenario(String tenant, String scenario) {
        emit(tenant, scenario, body(appId, null, null));
    }

    private ValidatableResponse changeTier(String tenant, String targetTierKey) {
        return given().header("Authorization", "Bearer " + owner(tenant))
                .contentType(ContentType.JSON)
                .body(Map.of("targetTierKey", targetTierKey, "billingCycle", "monthly"))
                .when().post(SUBS + "/" + appSlug + "/change-tier")
                .then();
    }

    private ValidatableResponse mySubs(String tenant) {
        return given().header("Authorization", "Bearer " + owner(tenant))
                .when().get(SUBS).then();
    }

    /** Estrae la subscription dell'app sotto test dal read-model self-service. */
    private ValidatableResponse sub(String tenant) {
        return mySubs(tenant).statusCode(200)
                .rootPath("subscriptions.find { it.appSlug == '" + appSlug + "' }");
    }

    private static void emit(String tenant, String scenario, Map<String, Object> body) {
        given().header("Authorization", "Bearer " + owner(tenant))
                .contentType(ContentType.JSON)
                .body(body)
                .when().post(DEV + "/scenarios/" + scenario)
                .then().statusCode(202);
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
}
