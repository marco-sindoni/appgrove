package app.appgrove.crm;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;
import java.util.Map;

/**
 * Piccola facciata per i test dell'app crm: assegnazione posti e creazione contatti/interazioni via
 * HTTP reale (RestAssured), così i singoli test restano leggibili. Il dominio è gated dal possesso di
 * un posto, quindi quasi ogni test comincia dando un posto a sé stesso.
 */
final class CrmApi {

    static final String CONTACTS = "/api/crm/v1/contacts";
    static final String SEATS = "/api/crm/v1/seats";
    static final String QUOTA = "/api/crm/v1/quota";

    private CrmApi() {}

    /** Assegna un posto a {@code userId} usando un token owner/admin. Ritorna lo status HTTP. */
    static int assignSeat(String adminToken, String userId) {
        return given().header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(Map.of("userId", userId))
                .when().post(SEATS)
                .then().extract().statusCode();
    }

    /** Dà un posto al proprietario del token (al suo stesso {@code sub}); assume accesso e quota disponibili. */
    static void seatSelf(String tenantId, String... roles) {
        String token = TestTokens.withTenant(tenantId, roles);
        assignSeat(token, TestTokens.subOf(tenantId));
    }

    /** Crea un contatto e ne ritorna l'id. Il chiamante deve già avere un posto. */
    static String createContact(String token, String displayName) {
        return given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("displayName", displayName))
                .when().post(CONTACTS)
                .then().statusCode(201)
                .extract().path("id");
    }
}
