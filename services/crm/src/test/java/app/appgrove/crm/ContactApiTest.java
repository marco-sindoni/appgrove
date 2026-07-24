package app.appgrove.crm;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * CRUD dei contatti + interazioni del mini-CRM (UC 0054). Ogni caso comincia dando un posto a sé
 * stesso: il dominio è gated dal possesso di un posto (senza, sarebbe 403), quindi la creazione di un
 * contatto presuppone un posto assegnato.
 */
@QuarkusTest
class ContactApiTest {

    private static final String TENANT = "11111111-0000-0000-0000-000000000001";

    private String token() {
        return TestTokens.withTenant(TENANT, "owner");
    }

    @Test
    void createReadPatchDelete() {
        CrmApi.seatSelf(TENANT, "owner");

        String id = given().header("Authorization", "Bearer " + token())
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "displayName", "Mario Rossi",
                        "email", "mario@example.test",
                        "phone", "+39 06 123456",
                        "organization", "ACME S.p.A."))
                .when().post(CrmApi.CONTACTS)
                .then().statusCode(201)
                .body("stage", is("lead"))
                .body("displayName", is("Mario Rossi"))
                .extract().path("id");

        given().header("Authorization", "Bearer " + token())
                .when().get(CrmApi.CONTACTS + "/" + id)
                .then().statusCode(200)
                .body("email", is("mario@example.test"))
                .body("organization", is("ACME S.p.A."));

        // patch: avanza lo stato della trattativa
        given().header("Authorization", "Bearer " + token())
                .contentType(ContentType.JSON)
                .body(Map.of("stage", "qualified", "notes", "Chiamato, interessato"))
                .when().patch(CrmApi.CONTACTS + "/" + id)
                .then().statusCode(200)
                .body("stage", is("qualified"))
                .body("notes", is("Chiamato, interessato"));

        // aggiunge un'interazione
        given().header("Authorization", "Bearer " + token())
                .contentType(ContentType.JSON)
                .body(Map.of("kind", "call", "note", "Prima telefonata"))
                .when().post(CrmApi.CONTACTS + "/" + id + "/interactions")
                .then().statusCode(201)
                .body("kind", is("call"));

        given().header("Authorization", "Bearer " + token())
                .when().get(CrmApi.CONTACTS + "/" + id + "/interactions")
                .then().statusCode(200)
                .body("size()", is(1))
                .body("[0].note", is("Prima telefonata"));

        // soft-delete → poi 404
        given().header("Authorization", "Bearer " + token())
                .when().delete(CrmApi.CONTACTS + "/" + id)
                .then().statusCode(204);
        given().header("Authorization", "Bearer " + token())
                .when().get(CrmApi.CONTACTS + "/" + id)
                .then().statusCode(404);
    }

    @Test
    void searchFiltersByTextAndStage() {
        String tenant = "11111111-0000-0000-0000-0000000000ff";
        String token = TestTokens.withTenant(tenant, "owner");
        CrmApi.seatSelf(tenant, "owner");

        CrmApi.createContact(token, "Alfa Uno");
        String beta = CrmApi.createContact(token, "Beta Due");
        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("stage", "won"))
                .when().patch(CrmApi.CONTACTS + "/" + beta)
                .then().statusCode(200);

        given().header("Authorization", "Bearer " + token)
                .when().get(CrmApi.CONTACTS + "?q=alfa")
                .then().statusCode(200)
                .body("content.size()", is(1))
                .body("content[0].displayName", is("Alfa Uno"));

        given().header("Authorization", "Bearer " + token)
                .when().get(CrmApi.CONTACTS + "?stage=won")
                .then().statusCode(200)
                .body("content.size()", is(1))
                .body("content[0].displayName", is("Beta Due"));
    }

    @Test
    void invalidPayloadIsRejected() {
        CrmApi.seatSelf(TENANT, "owner");
        // manca il nome obbligatorio
        given().header("Authorization", "Bearer " + token())
                .contentType(ContentType.JSON)
                .body(Map.of("email", "no-name@example.test"))
                .when().post(CrmApi.CONTACTS)
                .then().statusCode(400);
    }
}
