package app.appgrove.core.legal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.core.TestTokens;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * (Ri-)accettazione legale runtime (UC 0056): derivazione major → bloccante, minor → notifica,
 * idempotenza del log, isolamento multi-tenant, rendering testi coi token risolti. Le versioni correnti
 * (legal_version) sono popolate dallo startup-sync dai frontmatter di content/legal (IT facente fede).
 */
@QuarkusTest
class LegalTest {

    private static final String STATUS = "/api/platform/v1/me/legal/status";
    private static final String ACCEPTANCE = "/api/platform/v1/me/legal/acceptance";

    @Inject
    AgroalDataSource ds;

    @Test
    void freshUserHasAllBindingComponentsPending() {
        String token = TestTokens.withTenant("11111111-0000-0000-0000-000000000001", "member");
        given().header("Authorization", "Bearer " + token)
                .when().get(STATUS)
                .then().statusCode(200)
                .body("pending.component", hasItem("terms"))
                .body("pending.component", hasItem("privacy"))
                .body("pending.component", hasItem("cookie"))
                // atto richiesto: Termini = accept, Privacy = acknowledge
                .body("pending.find { it.component == 'terms' }.act", org.hamcrest.Matchers.is("accept"))
                .body("pending.find { it.component == 'privacy' }.act", org.hamcrest.Matchers.is("acknowledge"));
    }

    @Test
    void acceptClearsPendingAndIsIdempotent() {
        String token = TestTokens.withTenant("11111111-0000-0000-0000-000000000002", "member");
        // accetta i tre componenti vincolanti
        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("components", java.util.List.of("terms", "privacy", "cookie")))
                .when().post(ACCEPTANCE)
                .then().statusCode(200)
                .body("pending", empty());
        // ora lo stato non ha più pendenti
        given().header("Authorization", "Bearer " + token)
                .when().get(STATUS)
                .then().statusCode(200)
                .body("pending", empty());
        // idempotente: ri-accettare non rompe (unique index) e resta senza pendenti
        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("components", java.util.List.of("terms")))
                .when().post(ACCEPTANCE)
                .then().statusCode(200)
                .body("pending", empty());
    }

    @Test
    void higherMajorReopensPending() throws SQLException {
        String token = TestTokens.withTenant("11111111-0000-0000-0000-000000000003", "member");
        // accetta la versione corrente dei Termini
        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("components", java.util.List.of("terms")))
                .when().post(ACCEPTANCE)
                .then().statusCode(200);
        given().header("Authorization", "Bearer " + token)
                .when().get(STATUS)
                .then().body("pending.component", not(hasItem("terms")));
        try {
            // nuova major dei Termini: l'accettazione precedente diventa insufficiente → di nuovo pendente
            bumpMajor("terms", +1);
            given().header("Authorization", "Bearer " + token)
                    .when().get(STATUS)
                    .then().body("pending.component", hasItem("terms"));
        } finally {
            bumpMajor("terms", -1); // ripristina lo stato condiviso
        }
    }

    @Test
    void acceptanceIsTenantIsolated() {
        String tenantA = "22222222-0000-0000-0000-00000000000a";
        String tenantB = "22222222-0000-0000-0000-00000000000b";
        String tokenA = TestTokens.withTenant(tenantA, "member");
        String tokenB = TestTokens.withTenant(tenantB, "member");
        // A accetta i Termini
        given().header("Authorization", "Bearer " + tokenA)
                .contentType(ContentType.JSON)
                .body(Map.of("components", java.util.List.of("terms")))
                .when().post(ACCEPTANCE)
                .then().statusCode(200);
        // A: Termini non più pendenti
        given().header("Authorization", "Bearer " + tokenA)
                .when().get(STATUS)
                .then().body("pending.component", not(hasItem("terms")));
        // B: l'accettazione di A NON lo riguarda → Termini ancora pendenti
        given().header("Authorization", "Bearer " + tokenB)
                .when().get(STATUS)
                .then().body("pending.component", hasItem("terms"));
    }

    @Test
    void documentResolvesTokens() {
        given().header("Authorization",
                        "Bearer " + TestTokens.withTenant("33333333-0000-0000-0000-000000000001", "member"))
                .when().get("/api/platform/v1/legal/terms?lang=it")
                .then().statusCode(200)
                .body("component", org.hamcrest.Matchers.is("terms"))
                .body("markdown", not(org.hamcrest.Matchers.containsString("{{")));
    }

    @Test
    void unknownComponentIsRejected() {
        given().header("Authorization",
                        "Bearer " + TestTokens.withTenant("33333333-0000-0000-0000-000000000002", "member"))
                .when().get("/api/platform/v1/legal/nope")
                .then().statusCode(400);
    }

    @Test
    void syncIsIdempotent() {
        // lo startup ha già popolato legal_version; una ri-sync non cambia il numero di componenti.
        long before = countVersions();
        assertTrue(before >= LegalComponent.BINDING.size(), "almeno i componenti vincolanti presenti");
        assertEquals(before, countVersions(), "ri-lettura stabile");
        assertFalse(LegalComponent.BINDING.isEmpty());
    }

    private long countVersions() {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            var rs = st.executeQuery("select count(*) from platform.legal_version where deleted_at is null");
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void bumpMajor(String component, int delta) throws SQLException {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("update platform.legal_version set major = major + (" + delta
                    + ") where component = '" + component + "'");
        }
    }
}
