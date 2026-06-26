package app.appgrove.core;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Fail-closed (UC 0013 §8/§9) sull'API inviti: niente token → 401; token con ruolo ma
 * <b>senza</b> {@code tenant_id} → 403 (il resolver non ha un default tenant).
 */
@QuarkusTest
class FailClosedTest {

    private static final String PATH = "/api/platform/v1/invitations";

    @Test
    void noTokenIsUnauthorized() {
        given().contentType(ContentType.JSON).body(Map.of("email", "x@example.test", "role", "member"))
                .when().post(PATH)
                .then().statusCode(401);
    }

    @Test
    void tokenWithoutTenantIsForbidden() {
        given().header("Authorization", "Bearer " + TestTokens.withRolesNoTenant("owner"))
                .contentType(ContentType.JSON).body(Map.of("email", "x@example.test", "role", "member"))
                .when().post(PATH)
                .then().statusCode(403);
    }
}
