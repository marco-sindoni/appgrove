package app.appgrove.fatture;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Gate entitlement (402) lato servizio (UC 0027): l'endpoint annotato {@code @RequiresEntitlement}
 * (creazione fattura) è bloccato con 402 quando il tenant non ha accesso. L'endpoint di stato quota,
 * <b>non annotato</b>, resta raggiungibile anche senza accesso: prova dell'opt-in del gate (e
 * dell'esenzione GDPR per costruzione, #09 F31).
 */
@QuarkusTest
class EntitlementGateTest {

    private static final String INVOICES = "/api/fatture/v1/invoices";
    private static final String QUOTA = "/api/fatture/v1/quota";
    private static final String TENANT = "66666666-0000-0000-0000-000000000027";

    @AfterEach
    void restore() {
        MockEntitlementService.reset();
    }

    @Test
    void gatedEndpointReturns402WhenNotEntitled() {
        MockEntitlementService.accessGranted = false;
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT, "owner"))
                .contentType(ContentType.JSON)
                .body(Map.of("customerName", "Senza accesso"))
                .when().post(INVOICES)
                .then().statusCode(402)
                .contentType("application/problem+json")
                .body("status", is(402));
    }

    @Test
    void gatedEndpointAllowsWhenEntitled() {
        MockEntitlementService.accessGranted = true;
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT, "owner"))
                .contentType(ContentType.JSON)
                .body(Map.of("customerName", "Con accesso"))
                .when().post(INVOICES)
                .then().statusCode(201);
    }

    @Test
    void ungatedQuotaStatusStaysReachableWithoutEntitlement() {
        // Endpoint NON annotato: niente gate → resta leggibile anche senza accesso (opt-in / esenzione GDPR).
        MockEntitlementService.accessGranted = false;
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT, "owner"))
                .when().get(QUOTA)
                .then().statusCode(200)
                .body("metric", is("fatture"));
    }
}
