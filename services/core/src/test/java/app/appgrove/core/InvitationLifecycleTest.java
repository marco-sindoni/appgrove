package app.appgrove.core;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Ciclo di vita inviti (UC 0013 §5): create→list→revoke, duplicato pending (409), ruolo non invitabile (400). */
@QuarkusTest
class InvitationLifecycleTest {

    private static final String PATH = "/api/platform/v1/invitations";
    private static final String TENANT = "ffffffff-0000-0000-0000-000000000006";

    private String owner() {
        return "Bearer " + TestTokens.withTenant(TENANT, "owner");
    }

    @Test
    void createReturnsRawToken() {
        given().header("Authorization", owner())
                .contentType(ContentType.JSON).body(Map.of("email", "lc-token@example.test", "role", "member"))
                .when().post(PATH)
                .then().statusCode(201)
                .body("token", notNullValue())
                .body("status", org.hamcrest.Matchers.is("pending"));
    }

    @Test
    void createListRevoke() {
        String id = given().header("Authorization", owner())
                .contentType(ContentType.JSON).body(Map.of("email", "lc-flow@example.test", "role", "admin"))
                .when().post(PATH)
                .then().statusCode(201)
                .extract().path("id");

        given().header("Authorization", owner())
                .when().get(PATH + "?size=100")
                .then().statusCode(200)
                .body("content.email", hasItem("lc-flow@example.test"));

        given().header("Authorization", owner())
                .when().delete(PATH + "/" + id)
                .then().statusCode(204);

        given().header("Authorization", owner())
                .when().get(PATH + "?size=100")
                .then().body("content.email", not(hasItem("lc-flow@example.test")));
    }

    @Test
    void duplicatePendingIsConflict() {
        given().header("Authorization", owner())
                .contentType(ContentType.JSON).body(Map.of("email", "lc-dup@example.test", "role", "member"))
                .when().post(PATH).then().statusCode(201);
        given().header("Authorization", owner())
                .contentType(ContentType.JSON).body(Map.of("email", "lc-dup@example.test", "role", "member"))
                .when().post(PATH).then().statusCode(409);
    }

    @Test
    void ownerRoleIsNotInvitable() {
        given().header("Authorization", owner())
                .contentType(ContentType.JSON).body(Map.of("email", "lc-owner@example.test", "role", "owner"))
                .when().post(PATH)
                .then().statusCode(400);
    }
}
