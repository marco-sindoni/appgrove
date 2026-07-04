package app.appgrove.core.gdpr;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import app.appgrove.commons.gdpr.GdprExportWorker;
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
 * Console "Diritti GDPR" — aggregazione (UC 0034 §9, #13 L75): la tabella unificata convoglia
 * export, recessi per-app, eliminazioni account (grace) e ticket privacy, ciascuno con stato e
 * scadenza; il dettaglio export porta gli item per-servizio e il puntatore S3; il registro prove
 * di erasure risponde. In locale (nessuna regione AWS configurata) i deep-link sono assenti.
 */
@QuarkusTest
class AdminGdprConsoleTest {

    private static final String ADMIN = "/api/platform/v1/admin/gdpr";
    private static final String TENANT = "44444444-0000-0000-0000-0000000000c1";
    private static final String PLATFORM_TENANT = "a0000000-0000-4000-8000-000000000003";
    private static final UUID CONSOLE_APP_ID = UUID.fromString("99999999-2222-0000-0000-000000000002");
    private static final String CONSOLE_APP_SLUG = "consoleapp";

    @Inject
    TestData data;

    @Inject
    TestMessageQueues queues;

    @Inject
    TestExportStorage storage;

    @Inject
    GdprExportWorker platformWorker;

    @Inject
    GdprExportResultsConsumer resultsConsumer;

    private static String adminToken() {
        return TestTokens.withTenant(PLATFORM_TENANT, "owner", "platform-admin");
    }

    @BeforeEach
    void setup() {
        queues.clear();
        storage.clear();
        data.account(TENANT, "Console Tenant");
        data.app(CONSOLE_APP_ID, CONSOLE_APP_SLUG);
    }

    @Test
    void aggregatesExportsWithdrawalsDeletionsAndPrivacyTickets() {
        String userToken = TestTokens.withTenant(TENANT, "owner");

        // export completato (worker + consumer come UC 0032)
        String jobId = given().header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body(Map.of("kind", "account"))
                .when().post("/api/platform/v1/gdpr/exports")
                .then().statusCode(202)
                .extract().path("id");
        platformWorker.drain();
        resultsConsumer.drain();

        // recesso per-app: attivazione soft-deleted di un account vivo
        data.subscription(TENANT, CONSOLE_APP_ID, "canceled");
        data.softDeleteSubscriptions(TENANT, CONSOLE_APP_ID);

        // eliminazione account in grace
        given().header("Authorization", "Bearer " + userToken)
                .when().post("/api/platform/v1/accounts/me/deletion")
                .then().statusCode(202);

        // ticket privacy
        data.ticket(TENANT, "privacy", "Ticket console", "open");

        String token = adminToken();
        given().header("Authorization", "Bearer " + token)
                .when().get(ADMIN + "/requests")
                .then().statusCode(200)
                .body("findAll { it.type == 'export' }.refId", hasItem(jobId))
                .body("findAll { it.type == 'withdrawal' && it.tenantId == '" + TENANT + "' }.appId",
                        hasItem(CONSOLE_APP_SLUG))
                .body("findAll { it.type == 'account_deletion' && it.tenantId == '" + TENANT + "' }.status",
                        hasItem("GRACE_PENDING"))
                .body("findAll { it.type == 'account_deletion' && it.tenantId == '" + TENANT + "' }.dueAt",
                        hasItem(notNullValue()))
                .body("findAll { it.type == 'privacy_ticket' && it.tenantId == '" + TENANT + "' }.size()",
                        greaterThanOrEqualTo(1));

        // filtro per tipo
        given().header("Authorization", "Bearer " + token)
                .when().get(ADMIN + "/requests?type=export")
                .then().statusCode(200)
                .body("findAll { it.type != 'export' }.size()", equalTo(0));

        // dettaglio export: item per-servizio, chiave ZIP, scadenza del link (7gg); niente deep-link in locale
        given().header("Authorization", "Bearer " + token)
                .when().get(ADMIN + "/exports/" + jobId)
                .then().statusCode(200)
                .body("request.status", equalTo("COMPLETED"))
                .body("request.dueAt", notNullValue())
                .body("items[0].appId", equalTo("platform"))
                .body("zipKey", notNullValue())
                .body("s3ConsoleUrl", nullValue())
                .body("request.logsUrl", nullValue());

        // annulla l'eliminazione per non inquinare gli altri test (grace annullabile, UC 0033)
        given().header("Authorization", "Bearer " + userToken)
                .when().delete("/api/platform/v1/accounts/me/deletion")
                .then().statusCode(200);
    }

    @Test
    void purgeAuditRegistryResponds() {
        given().header("Authorization", "Bearer " + adminToken())
                .when().get(ADMIN + "/purge-audit")
                .then().statusCode(200)
                .body("size()", greaterThanOrEqualTo(0));
    }
}
