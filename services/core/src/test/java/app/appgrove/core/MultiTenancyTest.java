package app.appgrove.core;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
                .body(Map.of("email", "mt-override@example.test", "tenant_id", TENANT_B))
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

    /**
     * Separazione fra account sulla tabella degli <b>accessi per applicazione</b> (UC 0098 §9: prova di
     * sicurezza dedicata). Sta qui, dove vivono già le prove di separazione, e non in un file nuovo.
     *
     * <p>Tre affermazioni, in ordine di gravità: l'owner del conto A non <b>legge</b> gli accessi del
     * conto B; non li <b>concede</b> a una persona del conto B (che per lui non esiste: «non trovato»,
     * non «vietato»); e non li <b>revoca</b> passando l'identificativo dell'accesso altrui.
     */
    @Test
    void appAccessesDoNotCrossTheAccountBoundary() {
        data.account(TENANT_A, "Conto A accessi");
        data.account(TENANT_B, "Conto B accessi");
        UUID appId = UUID.randomUUID();
        data.app(appId, "mt-app-0098-" + appId.toString().substring(0, 8));
        data.subscription(TENANT_A, appId, "active");
        data.subscription(TENANT_B, appId, "active");
        data.user(TENANT_A, TestTokens.subjectFor(TENANT_A), "mt-owner-a@example.test", "owner");
        data.user(TENANT_B, TestTokens.subjectFor(TENANT_B), "mt-owner-b@example.test", "owner");
        UUID personaDiB = data.user(TENANT_B, "sub-mt-b-0098", "mt-persona-b@example.test", "member");
        data.appAccess(TENANT_B, appId, personaDiB, "editor");

        String tokenA = TestTokens.withTenant(TENANT_A, "owner");
        String path = "/api/platform/v1/apps/" + appId + "/access";

        // 1. non legge: l'accesso del conto B non compare nell'elenco visto dal conto A
        given().header("Authorization", "Bearer " + tokenA)
                .when().get(path)
                .then().statusCode(200)
                .body("identityId", not(hasItem(personaDiB.toString())));

        // 2. non concede: la persona del conto B non esiste, per il conto A
        given().header("Authorization", "Bearer " + tokenA)
                .contentType(ContentType.JSON)
                .body(Map.of("identityId", personaDiB.toString(), "role", "admin"))
                .when().post(path)
                .then().statusCode(404);

        // 3. non revoca: l'accesso del conto B resta intatto (leak detector sulla riga vera)
        given().header("Authorization", "Bearer " + tokenA)
                .when().delete(path + "/" + personaDiB)
                .then().statusCode(404);
        assertEquals("editor", data.appAccessRole(TENANT_B, appId, personaDiB),
                "l'accesso del conto B non deve essere toccato da un'operazione del conto A");
    }

    private static void invite(String token, String email) {
        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("email", email))
                .when().post(PATH)
                .then().statusCode(201);
    }
}
