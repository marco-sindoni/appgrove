package app.appgrove.crm;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * Endpoint quota-status ({@code GET /api/crm/v1/quota}): posti usati/tetto/rimanenza per il tenant del
 * JWT (metrica {@code seats}). Tenant dedicati per non ereditare posti da altri test (database condiviso).
 */
@QuarkusTest
class QuotaStatusTest {

    private static final int CAP = 2;

    @Test
    void freshTenantHasFullQuota() {
        String token = TestTokens.withTenant("55555555-0000-0000-0000-000000000031", "owner");
        given().header("Authorization", "Bearer " + token)
                .when().get(CrmApi.QUOTA)
                .then().statusCode(200)
                .body("metric", is("seats"))
                .body("used", is(0))
                .body("limit", is(CAP))
                .body("remaining", is(CAP));
    }

    @Test
    void usageAndRemainingTrackAssignedSeats() {
        String tenant = "55555555-0000-0000-0000-000000000032";
        String owner = TestTokens.withTenant(tenant, "owner");

        CrmApi.assignSeat(owner, "u1");
        CrmApi.assignSeat(owner, "u2");

        given().header("Authorization", "Bearer " + owner)
                .when().get(CrmApi.QUOTA)
                .then().statusCode(200)
                .body("used", is(2))
                .body("limit", is(CAP))
                .body("remaining", is(0));
    }

    @Test
    void quotaIsScopedToCallerTenant() {
        // Il tenant A occupa 1 posto; il tenant B deve vedere il proprio uso a 0 (isolamento #2).
        String tenantA = "55555555-0000-0000-0000-0000000000a3";
        String tenantB = "55555555-0000-0000-0000-0000000000b3";
        CrmApi.assignSeat(TestTokens.withTenant(tenantA, "owner"), "ua");

        given().header("Authorization", "Bearer " + TestTokens.withTenant(tenantA, "owner"))
                .when().get(CrmApi.QUOTA)
                .then().statusCode(200)
                .body("used", is(1));
        given().header("Authorization", "Bearer " + TestTokens.withTenant(tenantB, "owner"))
                .when().get(CrmApi.QUOTA)
                .then().statusCode(200)
                .body("used", is(0));
    }

    @Test
    void missingTenantIsForbidden() {
        given().header("Authorization", "Bearer " + TestTokens.withRolesNoTenant("owner"))
                .when().get(CrmApi.QUOTA)
                .then().statusCode(403);
    }
}
