package app.appgrove.core.billing;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import app.appgrove.core.TestData;
import app.appgrove.core.TestTokens;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * L1 "catena completa" (UC 0029, #09 D20 L1): payload webhook sintetico <b>firmato</b> → pipeline
 * reale (ingest → coda → consumer) → {@code subscription} → {@code GET /me/entitlements} <b>derivato</b>.
 * A differenza di {@link EntitlementsApiTest}, qui lo stato subscription NON è
 * seminato via SQL: nasce esclusivamente dal webhook, come in produzione. Verifica anche che il
 * linkage tenant venga dal {@code custom_data} del payload firmato e che la derivazione resti
 * tenant-scoped (invarianti #1/#2). Deterministico (coda in-memory, drain esplicito, Testcontainers).
 */
@QuarkusTest
class WebhookEntitlementChainTest {

    private static final String ME = "/api/platform/v1/me/entitlements";

    @Inject
    PaddleSignature signature;

    @Inject
    WebhookIngestService ingest;

    @Inject
    PaddleWebhookConsumer consumer;

    @Inject
    InMemoryWebhookQueue queue;

    @Inject
    TestData data;

    private final Instant base = Instant.now().truncatedTo(ChronoUnit.SECONDS).minusSeconds(5);

    private String tenant;
    private UUID app;
    private String slug;
    private UUID freeTier;
    private UUID proTier;
    private UUID maxTier;

    @BeforeEach
    void catalog() {
        queue.clear();
        tenant = UUID.randomUUID().toString();
        data.account(tenant, "Chain " + tenant.substring(0, 8));
        app = UUID.randomUUID();
        slug = "chain-" + app.toString().substring(0, 8);
        data.app(app, slug);
        // free = tier senza prezzo (baseline freemium); pro/max = tier a pagamento (hanno un price).
        freeTier = UUID.randomUUID();
        proTier = UUID.randomUUID();
        maxTier = UUID.randomUUID();
        data.appTier(freeTier, app, "free", "{\"metric\":\"widgets\",\"cap\":10,\"type\":\"flow\",\"window\":\"month\"}");
        data.appTier(proTier, app, "pro", "{\"metric\":\"widgets\",\"cap\":100,\"type\":\"flow\",\"window\":\"month\"}");
        data.appTier(maxTier, app, "max", "{\"metric\":\"widgets\",\"cap\":1000,\"type\":\"flow\",\"window\":\"month\"}");
        data.appPrice(UUID.randomUUID(), proTier, "year", "pri_chain_pro_" + proTier, 9900);
        data.appPrice(UUID.randomUUID(), maxTier, "year", "pri_chain_max_" + maxTier, 29900);
    }

    @Test
    void freshTenantStartsOnFreeBaseline() {
        // Nessun webhook: l'entitlement effettivo è il tier free (senza prezzo), phase assente.
        entitlements(tenant)
                .body("entitlements.find { it.appSlug == '" + slug + "' }.tierKey", is("free"))
                .body("entitlements.find { it.appSlug == '" + slug + "' }.phase", is(nullValue()))
                .body("entitlements.find { it.appSlug == '" + slug + "' }.limits.widgets.cap", is(10));
    }

    @Test
    void signedWebhookDerivesPaidEntitlement() {
        deliver(WebhookFixtures.subscription(
                "evt_chain_act_" + UUID.randomUUID(), "subscription.activated", "active",
                base, tenant, app, proTier, year(base)));

        entitlements(tenant)
                .body("entitlements.find { it.appSlug == '" + slug + "' }.tierKey", is("pro"))
                .body("entitlements.find { it.appSlug == '" + slug + "' }.phase", is("ACTIVE"))
                .body("entitlements.find { it.appSlug == '" + slug + "' }.accessUntil", is(notNullValue()))
                .body("entitlements.find { it.appSlug == '" + slug + "' }.limits.widgets.cap", is(100))
                .body("entitlements.find { it.appSlug == '" + slug + "' }.limits.widgets.nature", is("flow"));
    }

    @Test
    void tierChangeUpdatesDerivedEntitlementAndQuotaCap() {
        deliver(WebhookFixtures.subscription(
                "evt_chain_up1_" + UUID.randomUUID(), "subscription.activated", "active",
                base, tenant, app, proTier, year(base)));
        deliver(WebhookFixtures.subscription(
                "evt_chain_up2_" + UUID.randomUUID(), "subscription.updated", "active",
                base.plusSeconds(10), tenant, app, maxTier, year(base)));

        entitlements(tenant)
                .body("entitlements.find { it.appSlug == '" + slug + "' }.tierKey", is("max"))
                .body("entitlements.find { it.appSlug == '" + slug + "' }.limits.widgets.cap", is(1000));
    }

    @Test
    void cancellationRevokesAccess() {
        deliver(WebhookFixtures.subscription(
                "evt_chain_cxl1_" + UUID.randomUUID(), "subscription.activated", "active",
                base, tenant, app, proTier, year(base)));
        deliver(WebhookFixtures.subscription(
                "evt_chain_cxl2_" + UUID.randomUUID(), "subscription.canceled", "canceled",
                base.plusSeconds(10), tenant, app, proTier, year(base)));

        // gate 3 (#09 dec.30): subscription canceled → nessun accesso, nemmeno la baseline free.
        entitlements(tenant).body("entitlements.appSlug", not(hasItem(slug)));
    }

    @Test
    void derivedEntitlementIsTenantScoped() {
        String other = UUID.randomUUID().toString();
        data.account(other, "Other " + other.substring(0, 8));

        // il webhook (firmato) attiva pro per il tenant del custom_data, non per chi legge.
        deliver(WebhookFixtures.subscription(
                "evt_chain_iso_" + UUID.randomUUID(), "subscription.activated", "active",
                base, tenant, app, proTier, year(base)));

        entitlements(tenant)
                .body("entitlements.find { it.appSlug == '" + slug + "' }.tierKey", is("pro"));
        entitlements(other)
                .body("entitlements.find { it.appSlug == '" + slug + "' }.tierKey", is("free"));
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private void deliver(String body) {
        ingest.ingest(body, signature.sign(body));
        consumer.drain();
    }

    private io.restassured.response.ValidatableResponse entitlements(String tenantId) {
        return given().header("Authorization", "Bearer " + TestTokens.withTenant(tenantId, "owner"))
                .when().get(ME)
                .then().statusCode(200);
    }

    private static Instant year(Instant from) {
        return from.plus(365, ChronoUnit.DAYS);
    }
}
