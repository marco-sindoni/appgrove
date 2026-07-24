package app.appgrove.core.billing;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import app.appgrove.commons.usage.UsageEvents;
import app.appgrove.core.TestData;
import app.appgrove.core.TestTokens;
import app.appgrove.core.gdpr.TestMessageQueues;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Gate <b>stock</b> del downgrade contro l'uso reale (UC 0054), che chiude il punto aperto di UC 0028.
 * Prima di UC 0054 {@code SubscriptionResource.currentUsage()} tornava una mappa vuota e il downgrade non
 * veniva mai bloccato a runtime. Ora l'uso a giacenza arriva per evento dalle app
 * ({@link UsageEvents}, coda {@code app-usage}), core lo materializza ({@link AppUsageStore}) e il gate lo
 * consuma: scendere a un piano con meno posti di quelli occupati è rifiutato con remediation (409);
 * rientrati nel limite, lo stesso downgrade passa ed è schedulato a fine periodo.
 *
 * <p>Deterministico: coda in-memory, {@code drain()} espliciti sia del consumer webhook sia del consumer
 * d'uso.
 */
@QuarkusTest
class DowngradeStockGateTest {

    private static final String SUBS = "/api/platform/v1/me/subscriptions";
    private static final String DEV = "/api/platform/v1/dev/paddle";

    private static final String STOCK_TEAM = "{\"metric\":\"seats\",\"cap\":10,\"type\":\"stock\"}";
    private static final String STOCK_FREE = "{\"metric\":\"seats\",\"cap\":2,\"type\":\"stock\"}";

    @Inject
    TestData data;

    @Inject
    PaddleWebhookConsumer webhookConsumer;

    @Inject
    AppUsageConsumer usageConsumer;

    @Inject
    InMemoryWebhookQueue webhookQueue;

    @Inject
    TestMessageQueues queues;

    @Inject
    AppUsageStore store;

    @Inject
    ObjectMapper mapper;

    private UUID appId;
    private String appSlug;
    private UUID teamTier;
    private UUID freeTier;

    @BeforeEach
    void setup() {
        webhookQueue.clear();
        queues.clear();
        appId = UUID.randomUUID();
        appSlug = "crmlike-" + appId.toString().substring(0, 8);
        data.app(appId, appSlug);
        teamTier = UUID.randomUUID();
        freeTier = UUID.randomUUID();
        data.appTier(teamTier, appId, "team", STOCK_TEAM);
        data.appTier(freeTier, appId, "free", STOCK_FREE);
        // team è a pagamento (prezzo > 0), free non ha prezzo (0): così team→free è un downgrade.
        data.appPrice(UUID.randomUUID(), teamTier, "monthly", "pri_team_" + teamTier.toString().substring(0, 8), 1900);
    }

    @Test
    void downgradeBlockedWhenSeatsExceedTargetCap() {
        String tenant = "aaaaaaaa-0000-0000-0000-00000000d054";
        data.account(tenant, "Troppi posti");
        activateOn(tenant, teamTier);

        // l'app riporta 5 posti occupati: sopra il tetto (2) del piano free
        reportUsage(tenant, 5);

        given().header("Authorization", "Bearer " + owner(tenant))
                .contentType(ContentType.JSON)
                .body(Map.of("targetTierKey", "free", "billingCycle", "monthly"))
                .when().post(SUBS + "/" + appSlug + "/change-tier")
                .then().statusCode(409)
                .body("detail", containsString("seats"));
    }

    @Test
    void downgradeAllowedOnceSeatsAreWithinTargetCap() {
        String tenant = "bbbbbbbb-0000-0000-0000-00000000d054";
        data.account(tenant, "Rientrato");
        activateOn(tenant, teamTier);

        // l'app riporta 2 posti occupati: entro il tetto (2) del piano free
        reportUsage(tenant, 2);

        given().header("Authorization", "Bearer " + owner(tenant))
                .contentType(ContentType.JSON)
                .body(Map.of("targetTierKey", "free", "billingCycle", "monthly"))
                .when().post(SUBS + "/" + appSlug + "/change-tier")
                .then().statusCode(200)
                .body("direction", is("DOWNGRADE"));
    }

    @Test
    void noUsageReportedMeansNoBlock() {
        // Nessun report d'uso: la giacenza nota è vuota → il gate non ha nulla da proteggere e il
        // downgrade passa. È il comportamento corretto per un'app che non riporta ancora l'uso.
        String tenant = "cccccccc-0000-0000-0000-00000000d054";
        data.account(tenant, "Senza report");
        activateOn(tenant, teamTier);

        given().header("Authorization", "Bearer " + owner(tenant))
                .contentType(ContentType.JSON)
                .body(Map.of("targetTierKey", "free", "billingCycle", "monthly"))
                .when().post(SUBS + "/" + appSlug + "/change-tier")
                .then().statusCode(200)
                .body("direction", is("DOWNGRADE"));
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private static String owner(String tenant) {
        return TestTokens.withTenant(tenant, "owner");
    }

    /** Pubblica un report d'uso sulla coda condivisa e lo fa materializzare dal consumer. */
    private void reportUsage(String tenant, long seats) {
        try {
            String body = mapper.writeValueAsString(new UsageEvents.UsageReport(
                    appSlug, tenant, "seats", seats, Instant.now().toString()));
            queues.send(UsageEvents.USAGE_QUEUE, body);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        usageConsumer.drain();
        // sanity: il read-model d'uso ora conosce la giacenza
        org.junit.jupiter.api.Assertions.assertEquals(
                seats, store.usageFor(appSlug, tenant).getOrDefault("seats", -1L));
    }

    private void activateOn(String tenant, UUID tierId) {
        Map<String, Object> b = new HashMap<>();
        b.put("appId", appId.toString());
        b.put("appTierId", tierId.toString());
        given().header("Authorization", "Bearer " + owner(tenant))
                .contentType(ContentType.JSON)
                .body(b)
                .when().post(DEV + "/scenarios/happy_path")
                .then().statusCode(202);
        webhookConsumer.drain();
    }
}
