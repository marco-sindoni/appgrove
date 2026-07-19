package app.appgrove.@@APP_ID@@;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Matrice multi-tenancy (#10 15): righe isolate per tenant (rilevatore di fuoriuscite) e anti-override
 * del tenant. Il tenant deriva solo dal JWT firmato dall'harness; l'isolamento è verificato via GET
 * (filtro automatico del discriminator).
 *
 * <p>Questa classe è il presidio dell'invariante #1/#2 dell'app: non va indebolita né resa
 * condizionale quando il dominio segnaposto verrà sostituito da quello reale — va riscritta sulle
 * entità reali.
 */
@QuarkusTest
class MultiTenancyTest {

    private static final String PATH = "/api/@@APP_ID@@/v1/items";
    private static final String TENANT_A = "aaaaaaaa-0000-0000-0000-0000000000a1";
    private static final String TENANT_B = "bbbbbbbb-0000-0000-0000-0000000000b2";

    @Test
    void rowsAreIsolatedByTenant() {
        String tokenA = TestTokens.withTenant(TENANT_A, "owner");
        String tokenB = TestTokens.withTenant(TENANT_B, "owner");

        create(tokenA, "Contatto-A-mt");
        create(tokenB, "Contatto-B-mt");

        given().header("Authorization", "Bearer " + tokenA)
                .when().get(PATH + "?size=100")
                .then().statusCode(200)
                .body("content.contactName", hasItem("Contatto-A-mt"))
                .body("content.contactName", not(hasItem("Contatto-B-mt")));

        given().header("Authorization", "Bearer " + tokenB)
                .when().get(PATH + "?size=100")
                .then().statusCode(200)
                .body("content.contactName", hasItem("Contatto-B-mt"))
                .body("content.contactName", not(hasItem("Contatto-A-mt")));
    }

    @Test
    void tenantIdInBodyIsIgnored() {
        String tokenA = TestTokens.withTenant(TENANT_A, "owner");
        String tokenB = TestTokens.withTenant(TENANT_B, "owner");

        // il body prova a forzare il tenant B: deve essere ignorato (tenant solo dal JWT)
        given().header("Authorization", "Bearer " + tokenA)
                .contentType(ContentType.JSON)
                .body(Map.of("contactName", "Contatto-override-mt", "tenant_id", TENANT_B, "tenantId", TENANT_B))
                .when().post(PATH)
                .then().statusCode(201);

        given().header("Authorization", "Bearer " + tokenA)
                .when().get(PATH + "?size=100")
                .then().body("content.contactName", hasItem("Contatto-override-mt"));
        given().header("Authorization", "Bearer " + tokenB)
                .when().get(PATH + "?size=100")
                .then().body("content.contactName", not(hasItem("Contatto-override-mt")));
    }

    @Test
    void missingTenantIsForbidden() {
        // token autenticato con ruolo ma senza claim tenant_id → fail-closed 403
        given().header("Authorization", "Bearer " + TestTokens.withRolesNoTenant("owner"))
                .when().get(PATH + "?size=100")
                .then().statusCode(403);
    }

    private static void create(String token, String contactName) {
        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("contactName", contactName))
                .when().post(PATH)
                .then().statusCode(201);
    }
}
