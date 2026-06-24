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
 * Matrice multi-tenancy (UC 0012 §9): righe isolate per tenant (leak detector) e anti-override del tenant.
 * I tenant sono determinati esclusivamente dal JWT firmato dall'harness. L'isolamento è verificato
 * rileggendo dal DB (GET), che esercita il filtro automatico del discriminator.
 */
@QuarkusTest
class MultiTenancyTest {

    private static final String PATH = "/api/_demo/widgets";

    @Test
    void rowsAreIsolatedByTenant() {
        String tokenA = TestTokens.withTenant("tenant-a");
        String tokenB = TestTokens.withTenant("tenant-b");

        given().header("Authorization", "Bearer " + tokenA)
                .contentType(ContentType.JSON).body(Map.of("name", "widget-a"))
                .when().post(PATH)
                .then().statusCode(200);

        given().header("Authorization", "Bearer " + tokenB)
                .contentType(ContentType.JSON).body(Map.of("name", "widget-b"))
                .when().post(PATH)
                .then().statusCode(200);

        // A vede solo le proprie righe, tutte col proprio tenant
        given().header("Authorization", "Bearer " + tokenA)
                .when().get(PATH)
                .then().statusCode(200)
                .body("name", hasItem("widget-a"))
                .body("name", not(hasItem("widget-b")))
                .body("tenantId", everyItem(is("tenant-a")));

        // B vede solo le proprie righe
        given().header("Authorization", "Bearer " + tokenB)
                .when().get(PATH)
                .then().statusCode(200)
                .body("name", hasItem("widget-b"))
                .body("name", not(hasItem("widget-a")))
                .body("tenantId", everyItem(is("tenant-b")));
    }

    @Test
    void tenantIdInBodyIsIgnored() {
        String tokenA = TestTokens.withTenant("tenant-a");
        String tokenB = TestTokens.withTenant("tenant-b");

        // il body prova a forzare tenant-b: deve essere ignorato (tenant solo dal JWT)
        given().header("Authorization", "Bearer " + tokenA)
                .contentType(ContentType.JSON).body(Map.of("name", "override-attempt", "tenantId", "tenant-b"))
                .when().post(PATH)
                .then().statusCode(200).body("tenantId", is("tenant-a"));

        // la riga appartiene ad A, non a B
        given().header("Authorization", "Bearer " + tokenA)
                .when().get(PATH)
                .then().body("name", hasItem("override-attempt"));
        given().header("Authorization", "Bearer " + tokenB)
                .when().get(PATH)
                .then().body("name", not(hasItem("override-attempt")));
    }
}
