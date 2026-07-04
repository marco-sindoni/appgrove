package app.appgrove.core.gdpr;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.commons.gdpr.GdprQueues;
import app.appgrove.core.TestData;
import app.appgrove.core.TestTokens;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Eliminazione account con grace 14gg (UC 0033 §9, #13 E25): richiesta → disattivazione immediata
 * (zero entitlement) e grace visibile; annullo entro la grace → riattivazione; a grace scaduta lo
 * sweeper invoca l'offboarding (fan-out purge) una sola volta. Security: OWNER-only; F31: il
 * diritto risponde anche senza subscription attiva.
 */
@QuarkusTest
class AccountDeletionApiTest {

    private static final String PATH = "/api/platform/v1/accounts/me/deletion";
    private static final String TENANT = "77777777-0000-0000-0000-0000000033d1";
    private static final String TENANT_SWEEP = "77777777-0000-0000-0000-0000000033d2";
    private static final UUID APP_ID = UUID.fromString("99999999-1111-0000-0000-000000000d33");
    private static final String APP_SLUG = "graceapp";

    @Inject
    TestData data;

    @Inject
    TestMessageQueues queues;

    @Inject
    AccountDeletionSweeper sweeper;

    @Inject
    AgroalDataSource ds;

    @BeforeEach
    void reset() {
        queues.clear();
        data.account(TENANT, "Tenant grace");
        data.account(TENANT_SWEEP, "Tenant sweep");
        data.app(APP_ID, APP_SLUG);
        resetAccount(TENANT);
        resetAccount(TENANT_SWEEP);
    }

    @Test
    void requestDeactivatesImmediatelyAndCancelRestores() {
        String owner = TestTokens.withTenant(TENANT, "owner");

        // richiesta → 202, stato pending_deletion, scadenza grace = richiesta + 14gg
        String requestedAt = given().header("Authorization", "Bearer " + owner)
                .when().post(PATH)
                .then().statusCode(202)
                .body("status", equalTo("pending_deletion"))
                .body("deletionEffectiveAt", notNullValue())
                .extract().path("deletionRequestedAt");
        assertTrue(Instant.parse(requestedAt).isAfter(Instant.now().minusSeconds(60)));

        // disattivazione immediata: zero entitlement durante la grace (#13 E25)
        given().header("Authorization", "Bearer " + owner)
                .when().get("/api/platform/v1/me/entitlements")
                .then().statusCode(200)
                .body("entitlements.size()", equalTo(0));

        // doppia richiesta → 409
        given().header("Authorization", "Bearer " + owner)
                .when().post(PATH)
                .then().statusCode(409);

        // annullo entro la grace → account riattivato, marcatori azzerati
        given().header("Authorization", "Bearer " + owner)
                .when().delete(PATH)
                .then().statusCode(200)
                .body("status", equalTo("active"))
                .body("deletionRequestedAt", equalTo(null));

        // annullo senza eliminazione in corso → 409
        given().header("Authorization", "Bearer " + owner)
                .when().delete(PATH)
                .then().statusCode(409);
    }

    @Test
    void deletionIsOwnerOnly() {
        for (String role : List.of("admin", "member")) {
            String token = TestTokens.withTenant(TENANT, role);
            given().header("Authorization", "Bearer " + token)
                    .when().post(PATH)
                    .then().statusCode(403);
            given().header("Authorization", "Bearer " + token)
                    .when().delete(PATH)
                    .then().statusCode(403);
        }
    }

    /** F31: il diritto di eliminazione risponde anche con subscription canceled (nessun gate). */
    @Test
    void deletionRespondsWithCanceledSubscription() {
        data.subscription(TENANT, APP_ID, "canceled");
        String owner = TestTokens.withTenant(TENANT, "owner");
        given().header("Authorization", "Bearer " + owner)
                .when().post(PATH)
                .then().statusCode(202);
        given().header("Authorization", "Bearer " + owner)
                .when().delete(PATH)
                .then().statusCode(200);
    }

    @Test
    void sweeperOffboardsOnlyExpiredGraceAndOnlyOnce() {
        data.subscription(TENANT_SWEEP, APP_ID, "active");
        String owner = TestTokens.withTenant(TENANT_SWEEP, "owner");
        given().header("Authorization", "Bearer " + owner)
                .when().post(PATH)
                .then().statusCode(202);

        // grace NON scaduta → lo sweep non tocca nulla
        assertEquals(List.of(), sweeper.sweep(Instant.now()));
        assertEquals(0, queues.size(GdprQueues.purgeQueue(APP_SLUG)));

        // retrodata la richiesta oltre i 14 giorni → offboard: purge piattaforma + app attivata
        backdateRequest(TENANT_SWEEP, Instant.now().minusSeconds(15L * 24 * 3600));
        assertEquals(List.of(TENANT_SWEEP), sweeper.sweep(Instant.now()));
        assertEquals(1, queues.size(GdprQueues.purgeQueue(PlatformDataContract.APP_ID)));
        assertEquals(1, queues.size(GdprQueues.purgeQueue(APP_SLUG)));

        // secondo sweep: account già marcato eliminato → nessun nuovo fan-out (idempotenza)
        assertEquals(List.of(), sweeper.sweep(Instant.now()));
        assertEquals(1, queues.size(GdprQueues.purgeQueue(PlatformDataContract.APP_ID)));
    }

    /** Riporta l'account allo stato attivo e non eliminato (le fixture sono idempotenti on conflict). */
    private void resetAccount(String tenantId) {
        exec("update platform.accounts set status = 'active', deletion_requested_at = null,"
                + " deleted_at = null where id = ?::uuid", tenantId);
    }

    private void backdateRequest(String tenantId, Instant requestedAt) {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "update platform.accounts set deletion_requested_at = ? where id = ?::uuid")) {
            ps.setTimestamp(1, Timestamp.from(requestedAt));
            ps.setString(2, tenantId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void exec(String sql, String tenantId) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
