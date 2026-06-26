package app.appgrove.core;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Matrice ruoli (UC 0013 §8): owner/admin gestiscono utenti/inviti; member sola lettura del proprio profilo. */
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
                .body(Map.of("email", "role-member@example.test", "role", "member"))
                .when().post(INVITATIONS)
                .then().statusCode(403);
    }

    @Test
    void ownerCanCreateInvitation() {
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT, "owner"))
                .contentType(ContentType.JSON)
                .body(Map.of("email", "role-owner@example.test", "role", "member"))
                .when().post(INVITATIONS)
                .then().statusCode(201);
    }

    @Test
    void adminCanCreateInvitation() {
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT, "admin"))
                .contentType(ContentType.JSON)
                .body(Map.of("email", "role-admin@example.test", "role", "member"))
                .when().post(INVITATIONS)
                .then().statusCode(201);
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
