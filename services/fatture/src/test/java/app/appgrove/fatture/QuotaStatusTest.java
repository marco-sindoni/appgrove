package app.appgrove.fatture;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Endpoint quota-status ({@code GET /api/fatture/v1/quota}, UC 0052): uso/tetto/rimanenza per il
 * tenant del JWT. Tenant dedicati per non ereditare conteggi da altri test (DB condiviso).
 */
@QuarkusTest
class QuotaStatusTest {

    private static final String QUOTA = "/api/fatture/v1/quota";
    private static final String INVOICES = "/api/fatture/v1/invoices";

    @Test
    void freshTenantHasFullQuota() {
        String token = TestTokens.withTenant("55555555-0000-0000-0000-000000000031", "owner");

        given().header("Authorization", "Bearer " + token)
                .when().get(QUOTA)
                .then().statusCode(200)
                .body("metric", is("fatture"))
                .body("used", is(0))
                .body("limit", is(10))
                .body("remaining", is(10));
    }

    @Test
    void usageAndRemainingTrackCreatedInvoices() {
        String token = TestTokens.withTenant("55555555-0000-0000-0000-000000000032", "owner");

        for (int i = 0; i < 3; i++) {
            given().header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(Map.of("customerName", "Cliente " + i))
                    .when().post(INVOICES)
                    .then().statusCode(201);
        }

        given().header("Authorization", "Bearer " + token)
                .when().get(QUOTA)
                .then().statusCode(200)
                .body("used", is(3))
                .body("limit", is(10))
                .body("remaining", is(7));
    }

    @Test
    void quotaIsScopedToCallerTenant() {
        // Il tenant A crea 2 fatture; il tenant B deve vedere il proprio uso a 0 (isolamento #2).
        String tokenA = TestTokens.withTenant("55555555-0000-0000-0000-0000000000a3", "owner");
        String tokenB = TestTokens.withTenant("55555555-0000-0000-0000-0000000000b3", "owner");

        for (int i = 0; i < 2; i++) {
            given().header("Authorization", "Bearer " + tokenA)
                    .contentType(ContentType.JSON)
                    .body(Map.of("customerName", "A " + i))
                    .when().post(INVOICES)
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
