package app.appgrove.@@APP_ID@@;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Quota flow (metrica {@code @@METRIC@@}, tetto @@FREE_CAP@@/mese): @@FREE_CAP@@ creazioni passano,
 * la successiva è bloccata con 429 problem+json. Tenant dedicato per non ereditare conteggi da altri
 * test (il database è condiviso dall'intera suite).
 */
@QuarkusTest
class QuotaTest {

    private static final String PATH = "/api/@@APP_ID@@/v1/items";
    private static final String TENANT = "22222222-0000-0000-0000-000000000002";
    private static final int CAP = @@FREE_CAP@@;

    @Test
    void hardLimitReturns429WhenCapIsReached() {
        String token = TestTokens.withTenant(TENANT, "owner");

        for (int i = 0; i < CAP; i++) {
            given().header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(Map.of("contactName", "Contatto " + i))
                    .when().post(PATH)
                    .then().statusCode(201);
        }

        // oltre il tetto → 429 problem+json
        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("contactName", "Contatto oltre il tetto"))
                .when().post(PATH)
                .then().statusCode(429)
                .contentType("application/problem+json")
                .body("status", is(429));
    }
}
