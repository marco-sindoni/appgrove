package app.appgrove.core.gdpr;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.appgrove.core.TestData;
import app.appgrove.core.TestTokens;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Limitazione del trattamento — art. 18 (UC 0034 §9, #13 D19): applica/rimuovi su utente e account
 * con causale dedicata e <b>prova nell'audit</b>; conflitti espliciti (già limitato, sospensione
 * amministrativa, rimozione senza limitazione); <b>security</b>: solo platform-admin.
 */
@QuarkusTest
class GdprRestrictionApiTest {

    private static final String ADMIN = "/api/platform/v1/admin/gdpr/restrictions";
    private static final String TENANT = "33333333-0000-0000-0000-0000000000d1";
    private static final String TENANT_SUSPENDED = "33333333-0000-0000-0000-0000000000d2";
    private static final String PLATFORM_TENANT = "a0000000-0000-4000-8000-000000000003";

    @Inject
    TestData data;

    private static String adminToken() {
        return TestTokens.withTenant(PLATFORM_TENANT, "owner", "platform-admin");
    }

    @BeforeEach
    void setup() {
        data.account(TENANT, "Restriction Tenant");
    }

    @Test
    void nonPlatformAdminIsForbidden() {
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT, "owner"))
                .contentType(ContentType.JSON)
                .body(Map.of("targetKind", "account", "targetId", TENANT))
                .when().post(ADMIN)
                .then().statusCode(403);
    }

    @Test
    void applyAndRemoveOnUserLeavesAuditProof() {
        UUID userId = data.user(TENANT, "sub-restrict-1", "restrict-1@example.test", "member");

        // applica: utente sospeso con causale dedicata + prova "applied" nell'audit
        given().header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("targetKind", "user", "targetId", userId.toString(),
                        "note", "richiesta via ticket"))
                .when().post(ADMIN)
                .then().statusCode(201)
                .body("outcome", equalTo("APPLIED"));
        assertEquals("suspended", data.userStatus(userId));

        given().header("Authorization", "Bearer " + adminToken())
                .when().get(ADMIN)
                .then().statusCode(200)
                .body("active.findAll { it.targetKind == 'user' }.targetId", hasItem(userId.toString()))
                .body("auditTrail.findAll { it.targetId == '" + userId + "' }.action", hasItem("applied"));

        // doppia applicazione → 409
        given().header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("targetKind", "user", "targetId", userId.toString()))
                .when().post(ADMIN)
                .then().statusCode(409);

        // rimozione: utente riattivato + prova "removed"
        given().header("Authorization", "Bearer " + adminToken())
                .when().delete(ADMIN + "/user/" + userId)
                .then().statusCode(200)
                .body("outcome", equalTo("REMOVED"));
        assertEquals("active", data.userStatus(userId));
        given().header("Authorization", "Bearer " + adminToken())
                .when().get(ADMIN)
                .then().statusCode(200)
                .body("auditTrail.findAll { it.targetId == '" + userId + "' }.action", hasItem("removed"));

        // rimozione senza limitazione → 409
        given().header("Authorization", "Bearer " + adminToken())
                .when().delete(ADMIN + "/user/" + userId)
                .then().statusCode(409);
    }

    @Test
    void applyOnAccountUsesDedicatedReasonAndRemoveRestoresIt() {
        given().header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("targetKind", "account", "targetId", TENANT))
                .when().post(ADMIN)
                .then().statusCode(201);
        assertEquals("suspended", data.accountStatus(TENANT));
        assertEquals("gdpr_restriction", data.accountSuspendedReason(TENANT));

        given().header("Authorization", "Bearer " + adminToken())
                .when().delete(ADMIN + "/account/" + TENANT)
                .then().statusCode(200);
        assertEquals("active", data.accountStatus(TENANT));
        assertNull(data.accountSuspendedReason(TENANT));
    }

    @Test
    void administrativeSuspensionIsNotTouched() {
        data.account(TENANT_SUSPENDED, "Sospeso Admin");
        data.suspendAccount(TENANT_SUSPENDED, "abuse");

        // non applicabile su un account già sospeso per altra causale
        given().header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("targetKind", "account", "targetId", TENANT_SUSPENDED))
                .when().post(ADMIN)
                .then().statusCode(409);
        // e la rimozione della limitazione non riattiva una sospensione amministrativa
        given().header("Authorization", "Bearer " + adminToken())
                .when().delete(ADMIN + "/account/" + TENANT_SUSPENDED)
                .then().statusCode(409);
        assertEquals("suspended", data.accountStatus(TENANT_SUSPENDED));
    }

    @Test
    void unknownTargetIs404() {
        given().header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("targetKind", "user", "targetId", UUID.randomUUID().toString()))
                .when().post(ADMIN)
                .then().statusCode(404);
    }
}
