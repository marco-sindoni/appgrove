package app.appgrove.core.catalog;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import app.appgrove.core.TestData;
import app.appgrove.core.TestTokens;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Leva reversibile di disponibilità di un'app (UC 0076), lato API: transizione con motivazione, riga di
 * registro per ogni transizione <b>effettiva</b>, idempotenza senza registro sporco, errori tipizzati e
 * gating {@code platform-admin}. Ogni test lavora su un'app di catalogo <b>propria</b> (UUID casuale):
 * il registro è di piattaforma e va interrogato senza dipendere dalle altre suite.
 */
@QuarkusTest
class AppDisableApiTest {

    private static final String ADMIN = "/api/platform/v1/admin";
    private static final String PLATFORM_TENANT = "a0000000-0000-4000-8000-000000000076";
    private static final String ACCOUNT_TENANT = "a0000000-0000-4000-8000-000000000077";

    @Inject
    TestData data;

    private UUID appId;

    @BeforeEach
    void setup() {
        appId = UUID.randomUUID();
        data.app(appId, "uc0076-" + appId.toString().substring(0, 8));
    }

    @Test
    void transitionsAreRecordedOnceEachWithActorAndReason() {
        // disabilita con motivazione → una riga di registro
        patchStatus(adminToken(), appId, "inactive", "manutenzione straordinaria")
                .statusCode(200)
                .body("status", is("inactive"));

        entry(0)
                .body("fromStatus", is("active"))
                .body("toStatus", is("inactive"))
                .body("reason", is("manutenzione straordinaria"))
                .body("actor", is(TestTokens.subjectFor(PLATFORM_TENANT)))
                .body("executedAt", notNullValue());
        assertEntries(1);

        // riabilita → riga simmetrica, la più recente in testa
        patchStatus(adminToken(), appId, "active", null).statusCode(200).body("status", is("active"));

        entry(0).body("fromStatus", is("inactive")).body("toStatus", is("active")).body("reason", nullValue());
        assertEntries(2);
    }

    @Test
    void repeatingTheCurrentStatusIsIdempotentAndLeavesNoAuditRow() {
        patchStatus(adminToken(), appId, "inactive", "primo comando").statusCode(200);
        assertEntries(1);

        // stesso stato richiesto due volte: risposta normale, nessuna riga in più (niente prove false)
        patchStatus(adminToken(), appId, "inactive", "secondo comando")
                .statusCode(200)
                .body("status", is("inactive"));
        assertEntries(1);
        // e la motivazione del comando a vuoto non è finita nel registro
        entry(0).body("reason", is("primo comando"));
    }

    @Test
    void invalidStatusIsRejectedAsProblemJson() {
        patchStatus(adminToken(), appId, "dismessa", null)
                .statusCode(400)
                .body("status", is(400))
                .body("title", notNullValue());
        assertEntries(0);
        org.junit.jupiter.api.Assertions.assertEquals("active", data.appStatus(appId));
    }

    @Test
    void unknownAppIsNotFound() {
        patchStatus(adminToken(), UUID.randomUUID(), "inactive", null)
                .statusCode(404)
                .body("status", is(404))
                .body("title", notNullValue());
    }

    @Test
    void oversizedReasonIsRejected() {
        patchStatus(adminToken(), appId, "inactive", "x".repeat(AppStatusService.REASON_MAX_LENGTH + 1))
                .statusCode(400);
        assertEntries(0);
        org.junit.jupiter.api.Assertions.assertEquals("active", data.appStatus(appId));
    }

    @Test
    void accountRolesCannotChangeAppStatusNorReadTheRegister() {
        String owner = TestTokens.withTenant(ACCOUNT_TENANT, "owner");

        patchStatus(owner, appId, "inactive", "provo io").statusCode(403);
        given().header("Authorization", "Bearer " + owner)
                .when().get(ADMIN + "/apps/audit")
                .then().statusCode(403);

        org.junit.jupiter.api.Assertions.assertEquals("active", data.appStatus(appId), "nessun cambio di stato");
        assertEntries(0);
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private static String adminToken() {
        return TestTokens.withTenant(PLATFORM_TENANT, "owner", "platform-admin");
    }

    private static ValidatableResponse patchStatus(String token, UUID id, String status, String reason) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status);
        if (reason != null) {
            body.put("reason", reason);
        }
        return given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .patch(ADMIN + "/apps/" + id)
                .then();
    }

    /** Le voci di registro della sola app di questo test, più recenti prima. */
    private ValidatableResponse register() {
        return given().header("Authorization", "Bearer " + adminToken())
                .when()
                .get(ADMIN + "/apps/audit")
                .then()
                .statusCode(200);
    }

    private ValidatableResponse entry(int index) {
        return register().rootPath("findAll { it.appId == '" + appId + "' }[" + index + "]");
    }

    private void assertEntries(int expected) {
        register().body("findAll { it.appId == '" + appId + "' }.size()", is(expected));
    }
}
