package app.appgrove.core;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * Sicurezza dell'access token (UC 0016): il filtro condiviso {@code AccessTokenGuardFilter} +
 * la verifica smallrye devono rifiutare i token non-access, con client_id errato, scaduti o
 * malformati, letti SOLO dal JWT verificato. Endpoint di comodo: la console admin
 * ({@code platform-admin}); qui interessa lo status 401 del gate, non il corpo.
 */
@QuarkusTest
class AccessTokenGuardTest {

    private static final String ADMIN = "/api/platform/v1/admin";
    private static final String PLATFORM_TENANT = "a0000000-0000-4000-8000-000000000003";

    private static io.restassured.specification.RequestSpecification withBearer(String token) {
        return given().header("Authorization", "Bearer " + token);
    }

    @Test
    void accessTokenValidoNonVieneRifiutatoDalFiltro() {
        // token access valido (token_use=access, client_id atteso, ruolo platform-admin): NON 401.
        withBearer(TestTokens.withTenant(PLATFORM_TENANT, "owner", "platform-admin"))
                .when().get(ADMIN + "/overview")
                .then().statusCode(200);
    }

    @Test
    void idTokenVieneRifiutato() {
        // firma valida ma token_use=id → 401 (i servizi accettano solo access token).
        withBearer(TestTokens.idToken(PLATFORM_TENANT, "owner", "platform-admin"))
                .when().get(ADMIN + "/overview")
                .then().statusCode(401);
    }

    @Test
    void clientIdInattesoVieneRifiutato() {
        withBearer(TestTokens.withWrongClientId(PLATFORM_TENANT, "owner", "platform-admin"))
                .when().get(ADMIN + "/overview")
                .then().statusCode(401);
    }

    @Test
    void tokenScadutoVieneRifiutato() {
        withBearer(TestTokens.expired(PLATFORM_TENANT, "owner", "platform-admin"))
                .when().get(ADMIN + "/overview")
                .then().statusCode(401);
    }

    @Test
    void tokenMalformatoVieneRifiutato() {
        withBearer("non-e-un-jwt")
                .when().get(ADMIN + "/overview")
                .then().statusCode(401);
    }

    @Test
    void richiestaSenzaTokenNonPassaIlGateRuoli() {
        // nessun token: il filtro lascia passare, ma @RolesAllowed nega (401/403). Mai 200.
        given().when().get(ADMIN + "/overview")
                .then().statusCode(org.hamcrest.Matchers.anyOf(
                        org.hamcrest.Matchers.is(401), org.hamcrest.Matchers.is(403)));
    }
}
