package app.appgrove.core;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * Contratto OpenAPI (UC 0013 §9, #10 G): lo spec servito espone le API platform.
 * Il drift byte-level e l'oasdiff bloccante sui breaking change vivono in CI (UC 0005).
 */
@QuarkusTest
class OpenApiContractTest {

    @Test
    void specExposesPlatformApi() {
        given().accept("application/yaml")
                .when().get("/q/openapi")
                .then().statusCode(200)
                .body(containsString("/api/platform/v1/accounts"))
                .body(containsString("/api/platform/v1/users"))
                .body(containsString("/api/platform/v1/invitations"));
    }
}
