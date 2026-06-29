package app.appgrove.core.billing;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import app.appgrove.core.TestTokens;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Read-model entitlement {@code GET /api/platform/v1/me/entitlements} (UC 0027). Usa il seed multi-stato
 * (UC 0011): Acme→teams active, Acme→notes past_due (grace), Acme→legacy active (app inactive),
 * Bob→notes trialing, Bob→teams canceled. Verifica: derivazione accesso (grantsAccess + app abilitata),
 * baseline free, natura flow/stock nei limiti, isolamento per tenant, fail-closed senza tenant.
 */
@QuarkusTest
class EntitlementsApiTest {

    private static final String PATH = "/api/platform/v1/me/entitlements";
    private static final String ACME = "a0000000-0000-4000-8000-000000000001";
    private static final String BOB = "a0000000-0000-4000-8000-000000000002";
    private static final String FRESH = "a0000000-0000-4000-8000-0000000000ff";

    @Inject
    AgroalDataSource ds;

    @BeforeEach
    void seed() throws Exception {
        // catalogo già presente (loader allo startup); il seed identità+subscription è idempotente.
        Path root = Path.of(System.getProperty("user.dir")).getParent().getParent();
        String sql = Files.readString(root.resolve("dev/seed/seed.sql"))
                + "\n"
                + Files.readString(root.resolve("dev/seed/seed-subscriptions.sql"));
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute(sql);
        }
    }

    @Test
    void entitledAppsRespectGrantsAccessAndAppEnabled() {
        // Acme: teams (active) + notes (past_due → grace) entitled; legacy NON (app inactive, gate 2).
        given().header("Authorization", "Bearer " + TestTokens.withTenant(ACME, "owner"))
                .when().get(PATH)
                .then().statusCode(200)
                .body("entitlements.appSlug", hasItem("teams"))
                .body("entitlements.appSlug", hasItem("notes"))
                .body("entitlements.appSlug", not(hasItem("legacy")));
    }

    @Test
    void stockAndFlowNaturesSurfaceInLimits() {
        // teams: metrica seats di natura STOCK (cap 10); fatture (baseline free): flow, finestra month.
        given().header("Authorization", "Bearer " + TestTokens.withTenant(ACME, "owner"))
                .when().get(PATH)
                .then().statusCode(200)
                .body("entitlements.find { it.appSlug == 'teams' }.limits.seats.nature", is("stock"))
                .body("entitlements.find { it.appSlug == 'teams' }.limits.seats.cap", is(10))
                .body("entitlements.find { it.appSlug == 'fatture' }.limits.fatture.nature", is("flow"))
                .body("entitlements.find { it.appSlug == 'fatture' }.limits.fatture.window", is("month"));
    }

    @Test
    void freshTenantGetsFreeTierBaseline() {
        // Nessuna subscription: l'entitlement effettivo è il tier free (fatture cap 10, phase assente).
        given().header("Authorization", "Bearer " + TestTokens.withTenant(FRESH, "owner"))
                .when().get(PATH)
                .then().statusCode(200)
                .body("entitlements.appSlug", hasItem("fatture"))
                .body("entitlements.appSlug", not(hasItem("teams"))) // teams: nessun tier free
                .body("entitlements.find { it.appSlug == 'fatture' }.tierKey", is("free"))
                .body("entitlements.find { it.appSlug == 'fatture' }.phase", is(nullValue()))
                .body("entitlements.find { it.appSlug == 'fatture' }.limits.fatture.cap", is(10));
    }

    @Test
    void entitlementsAreScopedToTenant() {
        // Bob→teams è canceled e teams non ha free tier → Bob NON è entitled a teams (Acme sì).
        given().header("Authorization", "Bearer " + TestTokens.withTenant(BOB, "owner"))
                .when().get(PATH)
                .then().statusCode(200)
                .body("entitlements.appSlug", hasItem("notes")) // Bob→notes trialing
                .body("entitlements.appSlug", not(hasItem("teams")));
    }

    @Test
    void missingTenantIsForbidden() {
        // Token autenticato ma senza tenant_id → fail-closed del resolver → 403 (invariante #1).
        given().header("Authorization", "Bearer " + TestTokens.withRolesNoTenant("owner"))
                .when().get(PATH)
                .then().statusCode(403);
    }

    @Test
    void unauthenticatedIsRejected() {
        given().when().get(PATH).then().statusCode(401);
    }
}
