package app.appgrove.@@APP_ID@@;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** CRUD dell'API del dominio segnaposto: creazione (codice server-side), lettura, patch, soft-delete. */
@QuarkusTest
class ItemApiTest {

    private static final String PATH = "/api/@@APP_ID@@/v1/items";
    private static final String TENANT = "11111111-0000-0000-0000-000000000001";

    private String token() {
        return TestTokens.withTenant(TENANT, "owner");
    }

    @Test
    void createReadPatchDelete() {
        // create con due righe → codice progressivo, totale calcolato server-side
        String id = given().header("Authorization", "Bearer " + token())
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "contactName", "Mario Rossi",
                        "contactEmail", "mario@example.test",
                        "lines", List.of(
                                Map.of("description", "Voce A", "quantity", 2, "unitAmount", 50),
                                Map.of("description", "Voce B", "quantity", 1, "unitAmount", 20))))
                .when().post(PATH)
                .then().statusCode(201)
                .body("code", matchesPattern("\\d{4}-\\d{4}"))
                .body("status", is("draft"))
                .body("currency", is("EUR"))
                .body("lines.size()", is(2))
                .extract().path("id");

        // get
        given().header("Authorization", "Bearer " + token())
                .when().get(PATH + "/" + id)
                .then().statusCode(200)
                .body("contactName", is("Mario Rossi"));

        // patch stato → active
        given().header("Authorization", "Bearer " + token())
                .contentType(ContentType.JSON)
                .body(Map.of("status", "active"))
                .when().patch(PATH + "/" + id)
                .then().statusCode(200)
                .body("status", is("active"));

        // delete (soft) → poi 404
        given().header("Authorization", "Bearer " + token())
                .when().delete(PATH + "/" + id)
                .then().statusCode(204);
        given().header("Authorization", "Bearer " + token())
                .when().get(PATH + "/" + id)
                .then().statusCode(404);
    }

    @Test
    void firstRecordOfTenantIsNumberedFromOne() {
        String tenant = "11111111-0000-0000-0000-0000000000ff";
        given().header("Authorization", "Bearer " + TestTokens.withTenant(tenant, "owner"))
                .contentType(ContentType.JSON)
                .body(Map.of("contactName", "Primo Record"))
                .when().post(PATH)
                .then().statusCode(201)
                .body("code", endsWith("-0001"));
    }

    @Test
    void invalidPayloadIsRejected() {
        given().header("Authorization", "Bearer " + token())
                .contentType(ContentType.JSON)
                .body(Map.of("contactEmail", "no-name@example.test"))
                .when().post(PATH)
                .then().statusCode(400);
    }
}
