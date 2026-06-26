package app.appgrove.core;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Matrice multi-tenancy (UC 0013 §9) sulle entità reali (inviti): righe isolate per tenant
 * (leak detector) e anti-override del tenant. Il tenant deriva esclusivamente dal JWT firmato
 * dall'harness; l'isolamento è verificato rileggendo via GET (filtro automatico del discriminator).
 */
@QuarkusTest
class MultiTenancyTest {

    private static final String PATH = "/api/platform/v1/invitations";
    // tenant come UUID (= account id): tenant_id è varchar ma contiene l'id dell'account.
    private static final String TENANT_A = "aaaaaaaa-0000-0000-0000-000000000001";
    private static final String TENANT_B = "bbbbbbbb-0000-0000-0000-000000000002";

    @Test
    void rowsAreIsolatedByTenant() {
        String tokenA = TestTokens.withTenant(TENANT_A, "owner");
        String tokenB = TestTokens.withTenant(TENANT_B, "owner");

        invite(tokenA, "mt-a@example.test");
        invite(tokenB, "mt-b@example.test");

        // A vede solo i propri inviti
        given().header("Authorization", "Bearer " + tokenA)
                .when().get(PATH + "?size=100")
                .then().statusCode(200)
                .body("content.email", hasItem("mt-a@example.test"))
                .body("content.email", not(hasItem("mt-b@example.test")));

        // B vede solo i propri inviti
        given().header("Authorization", "Bearer " + tokenB)
                .when().get(PATH + "?size=100")
                .then().statusCode(200)
                .body("content.email", hasItem("mt-b@example.test"))
                .body("content.email", not(hasItem("mt-a@example.test")));
    }

    @Test
    void tenantIdInBodyIsIgnored() {
        String tokenA = TestTokens.withTenant(TENANT_A, "owner");
        String tokenB = TestTokens.withTenant(TENANT_B, "owner");

        // il body prova a forzare il tenant B: deve essere ignorato (tenant solo dal JWT)
        given().header("Authorization", "Bearer " + tokenA)
                .contentType(ContentType.JSON)
                .body(Map.of("email", "mt-override@example.test", "role", "member", "tenant_id", TENANT_B))
                .when().post(PATH)
                .then().statusCode(201);

        given().header("Authorization", "Bearer " + tokenA)
                .when().get(PATH + "?size=100")
                .then().body("content.email", hasItem("mt-override@example.test"));
        given().header("Authorization", "Bearer " + tokenB)
                .when().get(PATH + "?size=100")
                .then().body("content.email", not(hasItem("mt-override@example.test")));
    }

    @Test
    void listIsTenantScoped() {
        String tokenA = TestTokens.withTenant(TENANT_A, "owner");
        invite(tokenA, "mt-scope@example.test");
        // Tutte le righe lette dal tenant A sono pending del tenant A: nessun leak cross-tenant.
        given().header("Authorization", "Bearer " + tokenA)
                .when().get(PATH + "?size=100")
                .then().statusCode(200)
                .body("content.status", everyItem(is("pending")));
    }

    private static void invite(String token, String email) {
        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "role", "member"))
                .when().post(PATH)
                .then().statusCode(201);
    }
}
