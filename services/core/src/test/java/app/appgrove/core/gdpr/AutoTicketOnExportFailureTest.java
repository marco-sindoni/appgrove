package app.appgrove.core.gdpr;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import app.appgrove.commons.gdpr.ExportResultMessage;
import app.appgrove.commons.gdpr.GdprExportWorker;
import app.appgrove.commons.gdpr.GdprQueues;
import app.appgrove.core.TestData;
import app.appgrove.core.TestTokens;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Ticket privacy automatico su export FAILED (UC 0034 §9, #13 D21): il consumer risultati apre
 * <b>un solo</b> ticket per job fallito — idempotente sotto redelivery e con più item falliti —
 * con scadenza legale (art. 12), priorità alta e notifica alla casella di supporto.
 */
@QuarkusTest
class AutoTicketOnExportFailureTest {

    private static final String TENANT = "22222222-0000-0000-0000-0000000000e1";
    private static final UUID FAIL_APP_ID = UUID.fromString("99999999-3333-0000-0000-000000000003");
    private static final String FAIL_APP_SLUG = "failapp";

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

    @Inject
    ObjectMapper mapper;

    @Inject
    MockMailbox mailbox;

    @BeforeEach
    void setup() {
        queues.clear();
        storage.clear();
        mailbox.clear();
        data.account(TENANT, "Auto Ticket Tenant");
        data.app(FAIL_APP_ID, FAIL_APP_SLUG);
        data.subscription(TENANT, FAIL_APP_ID, "active");
    }

    @Test
    void failedExportOpensExactlyOnePrivacyTicket() throws Exception {
        String jobId = given()
                .header("Authorization", "Bearer " + TestTokens.withTenant(TENANT, "owner"))
                .contentType(ContentType.JSON)
                .body(Map.of("kind", "account"))
                .when().post("/api/platform/v1/gdpr/exports")
                .then().statusCode(202)
                .extract().path("id");
        platformWorker.drain();
        resultsConsumer.drain();

        // l'app risponde FAILED → job FAILED + auto-ticket
        queues.send(GdprQueues.EXPORT_RESULTS, mapper.writeValueAsString(
                ExportResultMessage.failed(jobId, FAIL_APP_SLUG, "errore simulato")));
        resultsConsumer.drain();
        assertEquals(1, data.ticketCountForExportJob(UUID.fromString(jobId)),
                "un export fallito deve aprire un ticket privacy");
        assertFalse(mailbox.getMailsSentTo("support@appgrove.app").isEmpty(),
                "l'auto-ticket deve notificare la casella di supporto");

        // redelivery dello stesso esito → nessun duplicato (indice unico su export_job_id)
        queues.send(GdprQueues.EXPORT_RESULTS, mapper.writeValueAsString(
                ExportResultMessage.failed(jobId, FAIL_APP_SLUG, "errore simulato (redelivery)")));
        resultsConsumer.drain();
        assertEquals(1, data.ticketCountForExportJob(UUID.fromString(jobId)),
                "il redelivery non deve duplicare il ticket");

        // il ticket è privacy, con scadenza legale e priorità alta, visibile in console
        given().header("Authorization",
                        "Bearer " + TestTokens.withTenant("a0000000-0000-4000-8000-000000000003",
                                "owner", "platform-admin"))
                .when().get("/api/platform/v1/admin/gdpr/tickets?type=privacy")
                .then().statusCode(200)
                .body("findAll { it.exportJobId == '" + jobId + "' }.size()",
                        org.hamcrest.Matchers.equalTo(1))
                .body("findAll { it.exportJobId == '" + jobId + "' }.priority",
                        org.hamcrest.Matchers.hasItem("high"))
                .body("findAll { it.exportJobId == '" + jobId + "' }.dueAt",
                        org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.notNullValue()));
    }
}
