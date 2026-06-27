package app.appgrove.fatture;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Quota flow (metrica {@code fatture}, tetto 10/mese): 10 creazioni passano, l'11ª è bloccata con
 * 429 problem+json. Tenant dedicato per non ereditare conteggi da altri test.
 */
@QuarkusTest
class QuotaTest {

    private static final String PATH = "/api/fatture/v1/invoices";
    private static final String TENANT = "22222222-0000-0000-0000-000000000002";

    @Test
    void hardLimitReturns429OnEleventhInvoice() {
        String token = TestTokens.withTenant(TENANT, "owner");

        for (int i = 0; i < 10; i++) {
            given().header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(Map.of("customerName", "Cliente " + i))
                    .when().post(PATH)
                    .then().statusCode(201);
        }

        // 11ª: tetto raggiunto → 429 problem+json
        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("customerName", "Cliente 11"))
                .when().post(PATH)
                .then().statusCode(429)
                .contentType("application/problem+json")
                .body("status", is(429));
    }
}
