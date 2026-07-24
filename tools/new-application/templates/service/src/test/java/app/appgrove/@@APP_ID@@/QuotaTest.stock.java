package app.appgrove.@@APP_ID@@;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Quota stock (metrica {@code @@METRIC@@}, tetto @@FREE_CAP@@ contemporanei): @@FREE_CAP@@ record
 * convivono, il successivo è bloccato con 429 problem+json, e — differenza sostanziale con la natura
 * a consumo — <b>cancellarne uno libera subito il posto</b>, senza attendere alcuna finestra.
 *
 * <p>La seconda asserzione è quella che conta: un tetto a giacenza contato a consumo passerebbe il
 * primo controllo e fallirebbe questo, restando bloccato per sempre. Tenant dedicato per non
 * ereditare conteggi da altri test (il database è condiviso dall'intera suite).
 */
@QuarkusTest
class QuotaTest {

    private static final String PATH = "/api/@@APP_ID@@/v1/items";
    private static final String TENANT = "22222222-0000-0000-0000-000000000002";
    private static final int CAP = @@FREE_CAP@@;

    @Test
    void hardLimitReturns429WhenCapIsReached() {
        String token = TestTokens.withTenant(TENANT, "owner");

        String firstId = null;
        for (int i = 0; i < CAP; i++) {
            String id = given().header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(Map.of("contactName", "Contatto " + i))
                    .when().post(PATH)
                    .then().statusCode(201)
                    .extract().path("id");
            if (firstId == null) {
                firstId = id;
            }
        }

        // oltre il tetto → 429 problem+json
        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("contactName", "Contatto oltre il tetto"))
                .when().post(PATH)
                .then().statusCode(429)
                .contentType("application/problem+json")
                .body("status", is(429));

        // giacenza: liberare un posto lo rende IMMEDIATAMENTE disponibile (nessuna finestra da attendere)
        given().header("Authorization", "Bearer " + token)
                .when().delete(PATH + "/" + firstId)
                .then().statusCode(204);

        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("contactName", "Contatto al posto liberato"))
                .when().post(PATH)
                .then().statusCode(201);
    }
}
