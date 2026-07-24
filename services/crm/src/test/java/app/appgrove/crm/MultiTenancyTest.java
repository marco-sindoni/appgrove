package app.appgrove.crm;

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
 * (filtro automatico del discriminator). Ogni tenant si dà prima un posto: il dominio è gated dal posto.
 *
 * <p>Questa classe è il presidio dell'invariante #1/#2 dell'app: non va indebolita né resa condizionale.
 */
@QuarkusTest
class MultiTenancyTest {

    private static final String TENANT_A = "aaaaaaaa-0000-0000-0000-0000000000a1";
    private static final String TENANT_B = "bbbbbbbb-0000-0000-0000-0000000000b2";

    @Test
    void rowsAreIsolatedByTenant() {
        String tokenA = TestTokens.withTenant(TENANT_A, "owner");
        String tokenB = TestTokens.withTenant(TENANT_B, "owner");
        CrmApi.seatSelf(TENANT_A, "owner");
        CrmApi.seatSelf(TENANT_B, "owner");

        CrmApi.createContact(tokenA, "Contatto-A-mt");
        CrmApi.createContact(tokenB, "Contatto-B-mt");

        given().header("Authorization", "Bearer " + tokenA)
                .when().get(CrmApi.CONTACTS + "?size=100")
                .then().statusCode(200)
                .body("content.displayName", hasItem("Contatto-A-mt"))
                .body("content.displayName", not(hasItem("Contatto-B-mt")));

        given().header("Authorization", "Bearer " + tokenB)
                .when().get(CrmApi.CONTACTS + "?size=100")
                .then().statusCode(200)
                .body("content.displayName", hasItem("Contatto-B-mt"))
                .body("content.displayName", not(hasItem("Contatto-A-mt")));
    }

    @Test
    void tenantIdInBodyIsIgnored() {
        String tokenA = TestTokens.withTenant(TENANT_A, "owner");
        String tokenB = TestTokens.withTenant(TENANT_B, "owner");
        CrmApi.seatSelf(TENANT_A, "owner");
        CrmApi.seatSelf(TENANT_B, "owner");

        // il body prova a forzare il tenant B: deve essere ignorato (tenant solo dal JWT)
        given().header("Authorization", "Bearer " + tokenA)
                .contentType(ContentType.JSON)
                .body(Map.of("displayName", "Contatto-override-mt", "tenant_id", TENANT_B, "tenantId", TENANT_B))
                .when().post(CrmApi.CONTACTS)
                .then().statusCode(201);

        given().header("Authorization", "Bearer " + tokenA)
                .when().get(CrmApi.CONTACTS + "?size=100")
                .then().body("content.displayName", hasItem("Contatto-override-mt"));
        given().header("Authorization", "Bearer " + tokenB)
                .when().get(CrmApi.CONTACTS + "?size=100")
                .then().body("content.displayName", not(hasItem("Contatto-override-mt")));
    }

    @Test
    void missingTenantIsForbidden() {
        // token autenticato con ruolo ma senza claim tenant_id → fail-closed 403
        given().header("Authorization", "Bearer " + TestTokens.withRolesNoTenant("owner"))
                .when().get(CrmApi.CONTACTS + "?size=100")
                .then().statusCode(403);
    }
}
