package app.appgrove.core.billing;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.appgrove.core.TestData;
import app.appgrove.core.TestTokens;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration UC 0024 (checkout server-initiated + polling). Verifica: catalogo tier/prezzi; il
 * {@code custom_data} (tenant) è server-side dal JWT; il {@code POST} <b>non</b> attiva la subscription
 * (solo il webhook drenato lo fa); customer lazy create→persist→reuse; OWNER-only; isolamento per tenant.
 * Deterministico: coda in-memory, {@code drain()} esplicito.
 */
@QuarkusTest
class CheckoutResourceTest {

    private static final String TENANT_A = "aaaaaaaa-0000-0000-0000-0000000c0001";
    private static final String TENANT_B = "bbbbbbbb-0000-0000-0000-0000000c0002";
    private static final String BASE = "/api/platform/v1/checkout";

    @Inject
    TestData data;

    @Inject
    PaddleWebhookConsumer consumer;

    @Inject
    InMemoryWebhookQueue queue;

    private UUID appId;
    private String appSlug;
    private UUID tierId;

    @BeforeEach
    void setup() {
        queue.clear();
        data.account(TENANT_A, "Acme");
        data.account(TENANT_B, "Bob");
        appId = UUID.randomUUID();
        tierId = UUID.randomUUID();
        appSlug = "demo-" + appId.toString().substring(0, 8);
        data.app(appId, appSlug);
        data.appTier(tierId, appId, "pro");
        data.appPrice(UUID.randomUUID(), tierId, "monthly", "pri_monthly_" + tierId.toString().substring(0, 8), 900);
        data.appPrice(UUID.randomUUID(), tierId, "annual", "pri_annual_" + tierId.toString().substring(0, 8), 9000);
    }

    private String owner(String tenant) {
        return TestTokens.withTenant(tenant, "owner");
    }

    // ── GET tiers ────────────────────────────────────────────────────────────
    @Test
    void tiersReturnsCatalogWithPrices() {
        given().header("Authorization", "Bearer " + owner(TENANT_A))
                .when().get(BASE + "/apps/" + appSlug + "/tiers")
                .then().statusCode(200)
                .body("appId", is(appId.toString()))
                .body("tiers.size()", greaterThanOrEqualTo(1))
                .body("tiers.find { it.key == 'pro' }.prices.billingCycle", hasItem("annual"))
                .body("tiers.find { it.key == 'pro' }.prices.billingCycle", hasItem("monthly"));
    }

    @Test
    void tiersUnknownAppIs404() {
        given().header("Authorization", "Bearer " + owner(TENANT_A))
                .when().get(BASE + "/apps/" + UUID.randomUUID() + "/tiers")
                .then().statusCode(404);
    }

    @Test
    void tiersRequiresAuthentication() {
        given().when().get(BASE + "/apps/" + appSlug + "/tiers").then().statusCode(401);
    }

    // ── POST checkout ──────────────────────────────────────────────────────────
    @Test
    void startCheckoutReturnsToken() {
        given().header("Authorization", "Bearer " + owner(TENANT_A))
                .contentType(ContentType.JSON)
                .body(Map.of("tierKey", "pro", "billingCycle", "annual"))
                .when().post(BASE + "/apps/" + appSlug)
                .then().statusCode(200)
                .body("checkoutToken", notNullValue());
    }

    @Test
    void startCheckoutRequiresOwner() {
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_A, "member"))
                .contentType(ContentType.JSON)
                .body(Map.of("tierKey", "pro", "billingCycle", "annual"))
                .when().post(BASE + "/apps/" + appSlug)
                .then().statusCode(403);
    }

    @Test
    void startCheckoutUnknownTierIs404() {
        given().header("Authorization", "Bearer " + owner(TENANT_A))
                .contentType(ContentType.JSON)
                .body(Map.of("tierKey", "ghost", "billingCycle", "annual"))
                .when().post(BASE + "/apps/" + appSlug)
                .then().statusCode(404);
    }

    @Test
    void startCheckoutInvalidCycleIs400() {
        given().header("Authorization", "Bearer " + owner(TENANT_A))
                .contentType(ContentType.JSON)
                .body(Map.of("tierKey", "pro", "billingCycle", "weekly"))
                .when().post(BASE + "/apps/" + appSlug)
                .then().statusCode(400);
    }

    // ── attivazione SOLO via webhook (#09 C16) ─────────────────────────────────
    @Test
    void postDoesNotActivateUntilWebhookDrained() {
        startCheckout(TENANT_A);
        // senza drenare la coda: nessuna subscription ancora → non attivo
        subscriptionStatus(TENANT_A).statusCode(200).body("active", is(false)).body("status", nullValue());
        assertEquals(null, data.subscriptionStatus(TENANT_A, appId), "il POST non deve scrivere subscription");

        // il consumer drena il webhook sintetico (stub) → attivazione
        consumer.drain();
        subscriptionStatus(TENANT_A).statusCode(200).body("active", is(true));
    }

    // ── tenant dal JWT, non dal client (invariante #1) ─────────────────────────
    @Test
    void activationIsScopedToCallerTenantOnly() {
        startCheckout(TENANT_A);
        consumer.drain();
        // attivato per A (caller), NON per B → custom_data tenant impostato server-side dal JWT
        subscriptionStatus(TENANT_A).statusCode(200).body("active", is(true));
        subscriptionStatus(TENANT_B).statusCode(200).body("active", is(false)).body("status", nullValue());
    }

    // ── customer lazy: create → persist → reuse (#09 C15) ──────────────────────
    @Test
    void lazyCustomerCreatedThenReused() {
        // tenant fresco: il paddle_customer_id non è contaminato da altri test (DB condiviso, no rollback).
        String tenant = "cccccccc-0000-0000-0000-" + UUID.randomUUID().toString().substring(24);
        data.account(tenant, "Lazy");
        assertEquals(null, data.accountPaddleCustomerId(tenant));

        startCheckout(tenant);
        String first = data.accountPaddleCustomerId(tenant);
        assertNotNull(first, "primo checkout deve persistere il paddle_customer_id");

        queue.clear();
        startCheckout(tenant);
        assertEquals(first, data.accountPaddleCustomerId(tenant), "checkout successivi riusano il customer");
    }

    // ── helper ─────────────────────────────────────────────────────────────────
    private void startCheckout(String tenant) {
        given().header("Authorization", "Bearer " + owner(tenant))
                .contentType(ContentType.JSON)
                .body(Map.of("tierKey", "pro", "billingCycle", "annual"))
                .when().post(BASE + "/apps/" + appSlug)
                .then().statusCode(200);
    }

    private io.restassured.response.ValidatableResponse subscriptionStatus(String tenant) {
        return given().header("Authorization", "Bearer " + owner(tenant))
                .when().get(BASE + "/apps/" + appSlug + "/subscription")
                .then();
    }
}
