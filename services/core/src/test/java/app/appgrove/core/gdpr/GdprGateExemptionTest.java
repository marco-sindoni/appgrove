package app.appgrove.core.gdpr;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;

import app.appgrove.commons.entitlement.RequiresEntitlement;
import app.appgrove.core.TestData;
import app.appgrove.core.TestTokens;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.lang.reflect.Method;
import java.util.Map;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

/**
 * Guardia dell'esenzione dai gate di enforcement (#09 F31) — il test rimandato da UC 0027: i diritti
 * GDPR devono restare esercitabili con solo authN+ownership, MAI bloccati da entitlement/quota, per
 * tutta la retention (subscription canceled/paused o inesistente).
 */
@QuarkusTest
class GdprGateExemptionTest {

    private static final String PATH = "/api/platform/v1/gdpr/exports";
    private static final String TENANT = "77777777-0000-0000-0000-0000000000f3";

    @Inject
    TestData data;

    /** Statica (per costruzione): gli endpoint dei diritti GDPR non portano {@code @RequiresEntitlement}. */
    @Test
    void gdprEndpointsCarryNoEntitlementGate() {
        // UC 0032 (export account) + UC 0033 (eliminazione, recesso per-app, export profilo)
        for (Class<?> resource : new Class<?>[] {
                GdprResource.class,
                AccountDeletionResource.class,
                GdprWithdrawalResource.class,
                ProfileExportResource.class}) {
            assertFalse(resource.isAnnotationPresent(RequiresEntitlement.class),
                    resource.getSimpleName() + " non deve essere gateata (#09 F31)");
            for (Method method : resource.getDeclaredMethods()) {
                assertFalse(method.isAnnotationPresent(RequiresEntitlement.class),
                        "endpoint GDPR gateato in violazione di #09 F31: "
                                + resource.getSimpleName() + "." + method.getName());
            }
        }
    }

    /** Funzionale: tenant SENZA alcuna subscription (nessun entitlement) → l'export risponde comunque. */
    @Test
    void exportRespondsWithoutAnySubscription() {
        data.account(TENANT, "Tenant senza abbonamenti");
        String token = TestTokens.withTenant(TENANT, "owner");
        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("kind", "account"))
                .when().post(PATH)
                .then().statusCode(202);
    }
}
