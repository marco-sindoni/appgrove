package app.appgrove.core.billing;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.core.TestTokens;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Coerenza fra le <b>due viste</b> sullo stesso fatto (UC 0077): ciò che l'utente vede nel backoffice
 * ({@code GET /me/entitlements}) e ciò che la console admin dice di lui (matrice cross-account) devono
 * combaciare, perché derivano dalla stessa regola ({@link EntitlementAccess}). Prima di questa change le
 * due divergevano: la matrice partiva dalle righe di subscription e non vedeva mai un'app abilitata dal
 * tier free di baseline.
 *
 * <p>Copre anche il terzo consumatore della regola, lo stato usato dal polling post-checkout (UC 0024):
 * un'app disabilitata dalla piattaforma non deve risultargli "attiva".
 */
@QuarkusTest
class EntitlementCoherenceTest {

    private static final String ME = "/api/platform/v1/me/entitlements";
    private static final String ADMIN = "/api/platform/v1/admin";
    private static final String ACME = "a0000000-0000-4000-8000-000000000001";
    private static final String BOB = "a0000000-0000-4000-8000-000000000002";
    private static final String PLATFORM_TENANT = "a0000000-0000-4000-8000-000000000003";
    // catalogo dal loader pricing-as-code (UC 0022): UUID deterministico CatalogIds('app:teams').
    private static final String TEAMS_APP = "1c4ea96d-bc57-3109-9c83-0933a3553779";

    @Inject
    AgroalDataSource ds;

    private static String adminToken() {
        return TestTokens.withTenant(PLATFORM_TENANT, "owner", "platform-admin");
    }

    @BeforeEach
    void seed() throws Exception {
        Path root = Path.of(System.getProperty("user.dir")).getParent().getParent();
        String sql = Files.readString(root.resolve("dev/seed/seed.sql"))
                + "\n"
                + Files.readString(root.resolve("dev/seed/seed-subscriptions.sql"));
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute(sql);
        }
    }

    /** App entitled secondo il read-model del tenant (quello che alimenta il menu del backoffice). */
    private Set<String> entitledPerIlTenant(String tenantId) {
        List<String> slugs = given().header("Authorization", "Bearer " + TestTokens.withTenant(tenantId, "owner"))
                .when().get(ME)
                .then().statusCode(200)
                .extract().jsonPath().getList("entitlements.appSlug", String.class);
        return new TreeSet<>(slugs);
    }

    /** App entitled secondo la matrice della console admin, per lo stesso account. */
    private Set<String> entitledPerLAdmin(String tenantId) {
        List<String> slugs = given().header("Authorization", "Bearer " + adminToken())
                .when().get(ADMIN + "/entitlements")
                .then().statusCode(200)
                .extract().jsonPath()
                .getList("findAll { it.tenantId == '" + tenantId + "' && it.entitled }.appSlug", String.class);
        return new TreeSet<>(slugs);
    }

    private boolean pollingDiceAttiva(String tenantId, String appSlug) {
        return given().header("Authorization", "Bearer " + TestTokens.withTenant(tenantId, "owner"))
                .when().get("/api/platform/v1/checkout/apps/" + appSlug + "/subscription")
                .then().statusCode(200)
                .extract().jsonPath().getBoolean("active");
    }

    private void setAccountStatus(String tenantId, String status) throws Exception {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("update platform.accounts set status = '" + status + "' where id = '" + tenantId + "'");
        }
    }

    private void setAppStatus(String appId, String status) {
        given().header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON).body(Map.of("status", status))
                .when().patch(ADMIN + "/apps/" + appId)
                .then().statusCode(200)
                .body("status", is(status));
    }

    @Test
    void leDueVisteCombacianoSugliStessiDati() {
        // Acme ha subscription in stati vari + almeno un'app abilitata dalla baseline free (fatture):
        // è esattamente il caso su cui le due viste divergevano.
        assertEquals(entitledPerIlTenant(ACME), entitledPerLAdmin(ACME));
        assertEquals(entitledPerIlTenant(BOB), entitledPerLAdmin(BOB));
    }

    @Test
    void laBaselineGratuitaEVisibileAncheAllAdmin() {
        // fatture ha un tier senza prezzo: nessuna subscription, eppure l'utente la vede nel menu.
        // Prima di UC 0077 la matrice admin non la mostrava affatto.
        assertTrue(entitledPerIlTenant(ACME).contains("fatture"), "il tenant vede la baseline free");
        assertTrue(entitledPerLAdmin(ACME).contains("fatture"), "anche l'admin la vede");
    }

    @Test
    void disabilitareUnAppLaToglieDaEntrambeLeVisteEDalPolling() {
        assertTrue(entitledPerIlTenant(ACME).contains("teams"));
        assertTrue(pollingDiceAttiva(ACME, "teams"));

        setAppStatus(TEAMS_APP, "inactive");
        try {
            assertFalse(entitledPerIlTenant(ACME).contains("teams"), "sparisce dal menu");
            assertFalse(entitledPerLAdmin(ACME).contains("teams"), "sparisce dalla matrice admin");
            // Il difetto chiuso da UC 0077: prima il polling guardava solo lo stato della subscription
            // e avrebbe detto "attiva" su un'app disabilitata dalla piattaforma.
            assertFalse(pollingDiceAttiva(ACME, "teams"), "il polling non dichiara attiva un'app disabilitata");
            assertEquals(entitledPerIlTenant(ACME), entitledPerLAdmin(ACME));
        } finally {
            setAppStatus(TEAMS_APP, "active"); // le altre suite condividono il database
        }
    }

    @Test
    void accountInAttesaDiEliminazioneEVuotoInEntrambeLeViste() throws Exception {
        setAccountStatus(BOB, "pending_deletion");
        try {
            assertTrue(entitledPerIlTenant(BOB).isEmpty(), "il tenant non ha più entitlement");
            assertTrue(entitledPerLAdmin(BOB).isEmpty(), "e l'admin lo vede allo stesso modo");
        } finally {
            setAccountStatus(BOB, "active");
        }
    }
}
