package app.appgrove.fatture;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Matrice multi-tenancy (UC 0051 §9): righe isolate per tenant (leak detector) e anti-override del
 * tenant. Il tenant deriva solo dal JWT firmato dall'harness; l'isolamento è verificato via GET
 * (filtro automatico del discriminator).
 */
@QuarkusTest
class MultiTenancyTest {

    private static final String PATH = "/api/fatture/v1/invoices";
    private static final String TENANT_A = "aaaaaaaa-0000-0000-0000-0000000000a1";
    private static final String TENANT_B = "bbbbbbbb-0000-0000-0000-0000000000b2";

    @Test
    void rowsAreIsolatedByTenant() {
        String tokenA = TestTokens.withTenant(TENANT_A, "owner");
        String tokenB = TestTokens.withTenant(TENANT_B, "owner");

        create(tokenA, "Cliente-A-mt");
        create(tokenB, "Cliente-B-mt");

        given().header("Authorization", "Bearer " + tokenA)
                .when().get(PATH + "?size=100")
                .then().statusCode(200)
                .body("content.customerName", hasItem("Cliente-A-mt"))
                .body("content.customerName", not(hasItem("Cliente-B-mt")));

        given().header("Authorization", "Bearer " + tokenB)
                .when().get(PATH + "?size=100")
                .then().statusCode(200)
                .body("content.customerName", hasItem("Cliente-B-mt"))
                .body("content.customerName", not(hasItem("Cliente-A-mt")));
    }

    @Test
    void tenantIdInBodyIsIgnored() {
        String tokenA = TestTokens.withTenant(TENANT_A, "owner");
        String tokenB = TestTokens.withTenant(TENANT_B, "owner");

        // il body prova a forzare il tenant B: deve essere ignorato (tenant solo dal JWT)
        given().header("Authorization", "Bearer " + tokenA)
                .contentType(ContentType.JSON)
                .body(Map.of("customerName", "Cliente-override-mt", "tenant_id", TENANT_B, "tenantId", TENANT_B))
                .when().post(PATH)
                .then().statusCode(201);

        given().header("Authorization", "Bearer " + tokenA)
                .when().get(PATH + "?size=100")
                .then().body("content.customerName", hasItem("Cliente-override-mt"));
        given().header("Authorization", "Bearer " + tokenB)
                .when().get(PATH + "?size=100")
                .then().body("content.customerName", not(hasItem("Cliente-override-mt")));
    }

    @Test
    void missingTenantIsForbidden() {
        // token autenticato con ruolo ma senza claim tenant_id → fail-closed 403
        given().header("Authorization", "Bearer " + TestTokens.withRolesNoTenant("owner"))
                .when().get(PATH + "?size=100")
                .then().statusCode(403);
    }

    private static void create(String token, String customerName) {
        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("customerName", customerName))
                .when().post(PATH)
                .then().statusCode(201);
    }
}
