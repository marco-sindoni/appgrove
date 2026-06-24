package app.appgrove.core;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Fail-closed (UC 0012 §5/§9): niente token → 401; token senza tenant_id → 403 (nessun default tenant). */
@QuarkusTest
class FailClosedTest {

    private static final String PATH = "/api/_demo/widgets";

    @Test
    void noTokenIsUnauthorized() {
        given().contentType(ContentType.JSON).body(Map.of("name", "x"))
                .when().post(PATH)
                .then().statusCode(401);
    }

    @Test
    void tokenWithoutTenantIsForbidden() {
        given().header("Authorization", "Bearer " + TestTokens.withoutTenant())
                .contentType(ContentType.JSON).body(Map.of("name", "x"))
                .when().post(PATH)
                .then().statusCode(403);
    }
}
