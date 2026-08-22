package app.appgrove.core;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Matrice ruoli: da UC 0100 <b>solo l'owner</b> gestisce persone e inviti dell'account; chiunque altro
 * legge e rettifica il proprio profilo, e nulla più.
 */
@QuarkusTest
class RolesTest {

    private static final String INVITATIONS = "/api/platform/v1/invitations";
    private static final String USERS = "/api/platform/v1/users";
    private static final String TENANT = "cccccccc-0000-0000-0000-000000000003";

    @Inject
    TestData data;

    @Test
    void memberCannotCreateInvitation() {
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT, "member"))
                .contentType(ContentType.JSON)
                .body(Map.of("email", "role-member@example.test"))
                .when().post(INVITATIONS)
                .then().statusCode(403);
    }

    @Test
    void ownerCanCreateInvitation() {
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT, "owner"))
                .contentType(ContentType.JSON)
                .body(Map.of("email", "role-owner@example.test"))
                .when().post(INVITATIONS)
                .then().statusCode(201);
    }

    /**
     * <b>Il collaudo che prova il contrario di prima</b>, e non la sua cancellazione (UC 0100). Fino a
     * questa storia un token che portava {@code admin} poteva invitare: {@code admin} non è più un ruolo
     * di appartenenza (UC 0098) e la gestione delle persone è dell'owner. Cancellare il collaudo vecchio
     * senza rimpiazzarlo avrebbe lasciato riaprire il varco senza che nulla diventasse rosso.
     */
    @Test
    void adminCannotCreateInvitation() {
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT, "admin"))
                .contentType(ContentType.JSON)
                .body(Map.of("email", "role-admin@example.test"))
                .when().post(INVITATIONS)
                .then().statusCode(403);
    }

    @Test
    void adminCannotListUsers() {
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT, "admin"))
                .when().get(USERS)
                .then().statusCode(403);
    }

    @Test
    void adminCannotListInvitations() {
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT, "admin"))
                .when().get(INVITATIONS)
                .then().statusCode(403);
    }

    @Test
    void memberCannotListUsers() {
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT, "member"))
                .when().get(USERS)
                .then().statusCode(403);
    }

    @Test
    void memberCanReadOwnProfile() {
        data.account(TENANT, "Acme");
        data.user(TENANT, TestTokens.subjectFor(TENANT), "role-me@example.test", "member");
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT, "member"))
                .when().get(USERS + "/me")
                .then().statusCode(200)
                .body("email", is("role-me@example.test"));
    }
}
