package app.appgrove.core;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * API account/utenti (UC 0013): account corrente, isolamento utenti per tenant, profilo proprio,
 * patch ruolo, soft-delete, e vincolo email globale ("1 utente→1 tenant", #02 14).
 */
@QuarkusTest
class AccountUserApiTest {

    private static final String ACCOUNTS = "/api/platform/v1/accounts";
    private static final String USERS = "/api/platform/v1/users";
    private static final String TENANT_A = "dddddddd-0000-0000-0000-000000000004";
    private static final String TENANT_B = "eeeeeeee-0000-0000-0000-000000000005";

    @Inject
    TestData data;

    @Test
    void accountMeReturnsTenantAccount() {
        data.account(TENANT_A, "Acme Inc");
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_A, "owner"))
                .when().get(ACCOUNTS + "/me")
                .then().statusCode(200)
                .body("id", is(TENANT_A))
                .body("name", is("Acme Inc"));
    }

    @Test
    void usersAreIsolatedByTenant() {
        data.account(TENANT_A, "Acme");
        data.account(TENANT_B, "Borg");
        data.user(TENANT_A, "sub-iso-a", "iso-a@example.test", "member");
        data.user(TENANT_B, "sub-iso-b", "iso-b@example.test", "member");

        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_A, "owner"))
                .when().get(USERS + "?size=100")
                .then().statusCode(200)
                .body("content.email", hasItem("iso-a@example.test"))
                .body("content.email", not(hasItem("iso-b@example.test")))
                .body("content.tenantId", everyItem(is(TENANT_A)));
    }

    @Test
    void usersMeReturnsOwnProfile() {
        data.account(TENANT_A, "Acme");
        data.user(TENANT_A, TestTokens.subjectFor(TENANT_A), "me-a@example.test", "owner");
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_A, "owner"))
                .when().get(USERS + "/me")
                .then().statusCode(200)
                .body("email", is("me-a@example.test"))
                .body("tenantId", is(TENANT_A));
    }

    @Test
    void patchUserUpdatesRole() {
        data.account(TENANT_A, "Acme");
        UUID id = data.user(TENANT_A, "sub-patch", "patch@example.test", "member");
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_A, "owner"))
                .contentType(ContentType.JSON).body(Map.of("role", "admin"))
                .when().patch(USERS + "/" + id)
                .then().statusCode(200)
                .body("role", is("admin"));
    }

    @Test
    void deleteUserSoftDeletes() {
        data.account(TENANT_A, "Acme");
        UUID id = data.user(TENANT_A, "sub-del", "del@example.test", "member");
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_A, "owner"))
                .when().delete(USERS + "/" + id)
                .then().statusCode(204);
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_A, "owner"))
                .when().get(USERS + "?size=100")
                .then().body("content.email", not(hasItem("del@example.test")));
    }

    @Test
    void emailIsGloballyUnique() {
        data.userStrict(TENANT_A, "sub-uq-a", "dup@example.test", "member");
        assertThrows(RuntimeException.class,
                () -> data.userStrict(TENANT_B, "sub-uq-b", "dup@example.test", "member"));
    }
}
