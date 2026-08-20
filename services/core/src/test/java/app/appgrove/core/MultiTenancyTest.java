package app.appgrove.core;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import static org.junit.jupiter.api.Assertions.assertFalse;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
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

    // Account propri della prova «una persona, due account» (UC 0116): separati da TENANT_A/TENANT_B
    // per non far dipendere il collaudo dall'ordine con cui girano gli altri.
    private static final String TENANT_UNO = "aaaaaaaa-0000-0000-0000-0000000000f1";
    private static final String TENANT_DUE = "bbbbbbbb-0000-0000-0000-0000000000f2";

    @Inject
    TestData data;

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

    /**
     * UC 0116 — la prova che conta: la <b>stessa identità</b>, presente in due account, non raggiunge
     * i dati dell'uno dall'altro. Non è un caso di confine ma il caso che ha originato la storia (una
     * persona owner del proprio account e member di quello di un'azienda), e sta qui, dove vivono le
     * prove di separazione, non in un file nuovo.
     */
    @Test
    void sameIdentityInTwoTenantsDoesNotCrossTheBoundary() {
        data.account(TENANT_UNO, "Conto uno");
        data.account(TENANT_DUE, "Conto due");
        UUID person = data.identity("sub-mt-condivisa", "mt-condivisa@example.test", "Condivisa");
        data.membership(TENANT_UNO, person, "owner");
        data.membership(TENANT_DUE, person, "owner");

        String nelContoUno = TestTokens.withSubject("sub-mt-condivisa", TENANT_UNO, "owner");
        String nelContoDue = TestTokens.withSubject("sub-mt-condivisa", TENANT_DUE, "owner");

        // scrive nel conto uno
        invite(nelContoUno, "mt-solo-del-conto-uno@example.test");

        // legge dal conto due con la STESSA identità: non vede nulla del conto uno
        given().header("Authorization", "Bearer " + nelContoDue)
                .when().get(PATH + "?size=100")
                .then().statusCode(200)
                .body("content.email", not(hasItem("mt-solo-del-conto-uno@example.test")));

        // e l'elenco delle persone del conto due non nomina il conto uno da nessuna parte
        given().header("Authorization", "Bearer " + nelContoDue)
                .when().get("/api/platform/v1/users?size=100")
                .then().statusCode(200)
                .body("content.tenantId", everyItem(is(TENANT_DUE)));

        // né l'esportazione del proprio profilo fatta dal conto due contiene qualcosa del conto uno
        String exported = given().header("Authorization", "Bearer " + nelContoDue)
                .when().get("/api/platform/v1/users/me/export")
                .then().statusCode(200).extract().asString();
        assertFalse(exported.contains(TENANT_UNO), "l'export dentro il conto due non deve nominare il conto uno");
        assertFalse(exported.contains("mt-solo-del-conto-uno@example.test"),
                "l'export dentro il conto due non deve contenere dati del conto uno");
    }

    private static void invite(String token, String email) {
        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "role", "member"))
                .when().post(PATH)
                .then().statusCode(201);
    }
}
