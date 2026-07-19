package app.appgrove.@@APP_ID@@;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Endpoint quota-status ({@code GET /api/@@APP_ID@@/v1/quota}): uso/tetto/rimanenza per il tenant del
 * JWT. Tenant dedicati per non ereditare conteggi da altri test (database condiviso).
 */
@QuarkusTest
class QuotaStatusTest {

    private static final String QUOTA = "/api/@@APP_ID@@/v1/quota";
    private static final String ITEMS = "/api/@@APP_ID@@/v1/items";
    private static final int CAP = @@FREE_CAP@@;

    @Test
    void freshTenantHasFullQuota() {
        String token = TestTokens.withTenant("55555555-0000-0000-0000-000000000031", "owner");

        given().header("Authorization", "Bearer " + token)
                .when().get(QUOTA)
                .then().statusCode(200)
                .body("metric", is("@@METRIC@@"))
                .body("used", is(0))
                .body("limit", is(CAP))
                .body("remaining", is(CAP));
    }

    @Test
    void usageAndRemainingTrackCreatedRecords() {
        String token = TestTokens.withTenant("55555555-0000-0000-0000-000000000032", "owner");

        for (int i = 0; i < 3; i++) {
            given().header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(Map.of("contactName", "Contatto " + i))
                    .when().post(ITEMS)
                    .then().statusCode(201);
        }

        given().header("Authorization", "Bearer " + token)
                .when().get(QUOTA)
                .then().statusCode(200)
                .body("used", is(3))
                .body("limit", is(CAP))
                .body("remaining", is(CAP - 3));
    }

    @Test
    void quotaIsScopedToCallerTenant() {
        // Il tenant A crea 2 record; il tenant B deve vedere il proprio uso a 0 (isolamento #2).
        String tokenA = TestTokens.withTenant("55555555-0000-0000-0000-0000000000a3", "owner");
        String tokenB = TestTokens.withTenant("55555555-0000-0000-0000-0000000000b3", "owner");

        for (int i = 0; i < 2; i++) {
            given().header("Authorization", "Bearer " + tokenA)
                    .contentType(ContentType.JSON)
                    .body(Map.of("contactName", "A " + i))
                    .when().post(ITEMS)
                    .then().statusCode(201);
        }

        given().header("Authorization", "Bearer " + tokenA)
                .when().get(QUOTA)
                .then().statusCode(200)
                .body("used", is(2));
        given().header("Authorization", "Bearer " + tokenB)
                .when().get(QUOTA)
                .then().statusCode(200)
                .body("used", is(0));
    }

    @Test
    void missingTenantIsForbidden() {
        given().header("Authorization", "Bearer " + TestTokens.withRolesNoTenant("owner"))
                .when().get(QUOTA)
                .then().statusCode(403);
    }
}
