package app.appgrove.core;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Console admin (UC 0021): superficie cross-tenant gated {@code platform-admin}. Asserisce sui dati del
 * seed deterministico (applicato in {@link #seed()}), che porta ≥2 tenant + app/subscription in stati vari.
 * Verifica: gating 403 per non-admin, liste cross-tenant, matrice entitlement derivata, disable-app.
 */
@QuarkusTest
class AdminApiTest {

    private static final String ADMIN = "/api/platform/v1/admin";
    private static final String PLATFORM_TENANT = "a0000000-0000-4000-8000-000000000003";
    // catalogo prodotto dal loader pricing-as-code (UC 0022): UUID deterministico CatalogIds('app:teams').
    private static final String TEAMS_APP = "1c4ea96d-bc57-3109-9c83-0933a3553779";

    @Inject
    AgroalDataSource ds;

    private static String adminToken() {
        return TestTokens.withTenant(PLATFORM_TENANT, "owner", "platform-admin");
    }

    @BeforeEach
    void seed() throws Exception {
        // identità + subscription (catalogo presente via loader allo startup → FK risolvono)
        Path root = Path.of(System.getProperty("user.dir")).getParent().getParent();
        String sql = Files.readString(root.resolve("dev/seed/seed.sql"))
                + "\n"
                + Files.readString(root.resolve("dev/seed/seed-subscriptions.sql"));
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute(sql);
        }
    }

    @Test
    void nonPlatformAdminIsForbidden() {
        given().header("Authorization", "Bearer " + TestTokens.withTenant(PLATFORM_TENANT, "owner"))
                .when().get(ADMIN + "/accounts")
                .then().statusCode(403);
    }

    @Test
    void accountsAreListedCrossTenant() {
        given().header("Authorization", "Bearer " + adminToken())
                .when().get(ADMIN + "/accounts")
                .then().statusCode(200)
                .body("name", hasItem("Acme Corp"))
                .body("name", hasItem("Bob Personal"))
                .body("name", hasItem("Appgrove Platform"));
    }

    @Test
    void usersAreListedCrossTenantWithTenantName() {
        given().header("Authorization", "Bearer " + adminToken())
                .when().get(ADMIN + "/users")
                .then().statusCode(200)
                .body("findAll { it.email == 'owner@acme.test' }.tenantName", hasItem("Acme Corp"))
                .body("email", hasItem("bob@bob.test"));
    }

    @Test
    void overviewReturnsBaseKpis() {
        given().header("Authorization", "Bearer " + adminToken())
                .when().get(ADMIN + "/overview")
                .then().statusCode(200)
                .body("accounts", greaterThanOrEqualTo(3))
                .body("disabledApps", greaterThanOrEqualTo(1)); // 'legacy' inactive
    }

    @Test
    void entitlementMatrixIsDerivedFromSubscriptionAndAppStatus() {
        given().header("Authorization", "Bearer " + adminToken())
                .when().get(ADMIN + "/entitlements")
                .then().statusCode(200)
                // Acme→teams: subscription active + app teams active → entitled
                .body("findAll { it.tenantName == 'Acme Corp' && it.appSlug == 'teams' }.entitled", hasItem(true))
                // Acme→legacy: subscription active MA app legacy inactive → NON entitled (gate 2)
                .body("findAll { it.tenantName == 'Acme Corp' && it.appSlug == 'legacy' }.entitled", hasItem(false))
                // Acme→notes: subscription past_due (dunning/grace) + app notes active → entitled (UC 0026, #09 E29)
                .body("findAll { it.tenantName == 'Acme Corp' && it.appSlug == 'notes' }.entitled", hasItem(true))
                // Bob→notes: subscription trialing + app notes active → entitled
                .body("findAll { it.tenantName == 'Bob Personal' && it.appSlug == 'notes' }.entitled", hasItem(true))
                // Bob→teams: subscription canceled → NON entitled
                .body("findAll { it.tenantName == 'Bob Personal' && it.appSlug == 'teams' }.entitled", hasItem(false));
    }

    @Test
    void disableAppTogglesStatusAndDropsEntitlement() {
        // disabilita teams
        given().header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON).body(Map.of("status", "inactive"))
                .when().patch(ADMIN + "/apps/" + TEAMS_APP)
                .then().statusCode(200)
                .body("slug", is("teams"))
                .body("status", is("inactive"));

        // ora Acme→teams non è più entitled (app disabilitata)
        given().header("Authorization", "Bearer " + adminToken())
                .when().get(ADMIN + "/entitlements")
                .then().statusCode(200)
                .body("findAll { it.tenantName == 'Acme Corp' && it.appSlug == 'teams' }.entitled", hasItem(false));

        // ripristina teams (idempotenza per le altre suite)
        given().header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON).body(Map.of("status", "active"))
                .when().patch(ADMIN + "/apps/" + TEAMS_APP)
                .then().statusCode(200)
                .body("status", is("active"));
    }
}
