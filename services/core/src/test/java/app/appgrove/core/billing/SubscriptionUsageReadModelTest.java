package app.appgrove.core.billing;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

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
 * Read-model {@code /me/subscriptions} arricchito (UC 0067): oltre ai tetti del piano espone l'<b>uso
 * corrente a giacenza</b> e i <b>piani che la regola di riduzione rifiuterebbe adesso</b>, con la relativa
 * spiegazione.
 *
 * <p>Perché stanno nel read-model e non sono ricalcolati dal frontend: la card deve poter dire "8 su 10" e
 * la finestra "cambia piano" deve poter disabilitare il piano troppo piccolo <b>prima</b> del clic. La
 * regola resta quella che rifiuta il comando ({@link TierChangePolicy}) — un secondo giudizio in TypeScript
 * divergerebbe alla prima modifica.
 */
@QuarkusTest
class SubscriptionUsageReadModelTest {

    private static final String SUBS = "/api/platform/v1/me/subscriptions";
    private static final String DEV = "/api/platform/v1/dev/paddle";

    private static final String STOCK_TEAM = "{\"metric\":\"seats\",\"cap\":10,\"type\":\"stock\"}";
    private static final String STOCK_FREE = "{\"metric\":\"seats\",\"cap\":2,\"type\":\"stock\"}";
    private static final String FLOW_TEAM =
            "{\"metric\":\"invoices\",\"cap\":50,\"type\":\"flow\",\"window\":\"month\"}";

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
        appSlug = "seatsapp-" + appId.toString().substring(0, 8);
        data.app(appId, appSlug);
        teamTier = UUID.randomUUID();
        freeTier = UUID.randomUUID();
        data.appTier(teamTier, appId, "team", STOCK_TEAM);
        data.appTier(freeTier, appId, "free", STOCK_FREE);
        data.appPrice(UUID.randomUUID(), teamTier, "monthly", "pri_team_" + teamTier.toString().substring(0, 8), 1900);
    }

    @Test
    void exposesCurrentStockUsageAndBlocksTheTooSmallTier() {
        String tenant = "aaaaaaaa-0000-0000-0000-000000000067";
        data.account(tenant, "Otto posti");
        activateOn(tenant, teamTier);
        reportUsage(tenant, "seats", 8);

        given().header("Authorization", "Bearer " + owner(tenant))
                .when().get(SUBS)
                .then().statusCode(200)
                .rootPath("subscriptions.find { it.appSlug == '" + appSlug + "' }")
                .body("usage.seats", is(8))
                // free ha 2 posti, ne sono occupati 8 → la finestra deve poterlo disabilitare
                .body("blockedTiers", hasKey("free"))
                .body("blockedTiers.free", containsString("seats"))
                // team ha 10 posti: capiente, non bloccato
                .body("blockedTiers", not(hasKey("team")));
    }

    @Test
    void noUsageReportedMeansNoUsageAndNoBlockedTier() {
        // Un'app che non riporta ancora l'uso non deve far apparire piani bloccati: non c'è alcuna
        // giacenza nota da proteggere, e inventarne una vieterebbe una riduzione legittima.
        String tenant = "bbbbbbbb-0000-0000-0000-000000000067";
        data.account(tenant, "Senza report");
        activateOn(tenant, teamTier);

        given().header("Authorization", "Bearer " + owner(tenant))
                .when().get(SUBS)
                .then().statusCode(200)
                .rootPath("subscriptions.find { it.appSlug == '" + appSlug + "' }")
                .body("usage", anEmptyMap())
                .body("blockedTiers", anEmptyMap());
    }

    @Test
    void flowMetricsNeverBlockAndKeepShowingOnlyTheirCap() {
        // Le metriche a finestra si applicano dal periodo successivo: non bloccano mai la riduzione,
        // qualunque sia il consumo riportato. Verifica che il read-model non le tratti come giacenze.
        UUID flowApp = UUID.randomUUID();
        String flowSlug = "flowapp-" + flowApp.toString().substring(0, 8);
        data.app(flowApp, flowSlug);
        UUID big = UUID.randomUUID();
        UUID small = UUID.randomUUID();
        data.appTier(big, flowApp, "team", FLOW_TEAM);
        data.appTier(small, flowApp, "free", "{\"metric\":\"invoices\",\"cap\":5,\"type\":\"flow\",\"window\":\"month\"}");
        data.appPrice(UUID.randomUUID(), big, "monthly", "pri_flow_" + big.toString().substring(0, 8), 900);

        String tenant = "cccccccc-0000-0000-0000-000000000067";
        data.account(tenant, "A finestra");
        activateOn(flowApp, tenant, big);
        reportUsage(flowSlug, tenant, "invoices", 40);

        given().header("Authorization", "Bearer " + owner(tenant))
                .when().get(SUBS)
                .then().statusCode(200)
                .rootPath("subscriptions.find { it.appSlug == '" + flowSlug + "' }")
                .body("usage.invoices", is(40))
                .body("blockedTiers", anEmptyMap())
                .body("limits.invoices.cap", is(50));
    }

    @Test
    void usageIsScopedToTheCallerTenant() {
        // La giacenza di un tenant non deve comparire nel read-model di un altro, nemmeno sulla stessa app.
        String mine = "dddddddd-0000-0000-0000-000000000067";
        String other = "eeeeeeee-0000-0000-0000-000000000067";
        data.account(mine, "Mio");
        data.account(other, "Altro");
        activateOn(mine, teamTier);
        activateOn(other, teamTier);
        reportUsage(other, "seats", 9);

        given().header("Authorization", "Bearer " + owner(mine))
                .when().get(SUBS)
                .then().statusCode(200)
                .rootPath("subscriptions.find { it.appSlug == '" + appSlug + "' }")
                .body("usage", anEmptyMap())
                .body("blockedTiers", anEmptyMap());
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private static String owner(String tenant) {
        return TestTokens.withTenant(tenant, "owner");
    }

    private void reportUsage(String tenant, String metric, long value) {
        reportUsage(appSlug, tenant, metric, value);
    }

    private void reportUsage(String slug, String tenant, String metric, long value) {
        try {
            queues.send(
                    UsageEvents.USAGE_QUEUE,
                    mapper.writeValueAsString(new UsageEvents.UsageReport(
                            slug, tenant, metric, value, Instant.now().toString())));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        usageConsumer.drain();
    }

    private void activateOn(String tenant, UUID tierId) {
        activateOn(appId, tenant, tierId);
    }

    private void activateOn(UUID app, String tenant, UUID tierId) {
        Map<String, Object> body = new HashMap<>();
        body.put("appId", app.toString());
        body.put("appTierId", tierId.toString());
        given().header("Authorization", "Bearer " + owner(tenant))
                .contentType(ContentType.JSON)
                .body(body)
                .when().post(DEV + "/scenarios/happy_path")
                .then().statusCode(202);
        webhookConsumer.drain();
    }
}
