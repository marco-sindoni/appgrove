package app.appgrove.core.gdpr;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.appgrove.commons.gdpr.ExportResultMessage;
import app.appgrove.commons.gdpr.GdprQueues;
import app.appgrove.core.TestData;
import app.appgrove.core.TestTokens;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Recesso per-app (UC 0033 §9, #13 D19/E23): esporta → conferma → cancella immediata. La conferma
 * esige l'export per-app COMPLETED (409 altrimenti); alla conferma l'attivazione sparisce
 * (entitlement/export account) e la purge parte sulla coda dell'app. Ownership: l'export di un
 * altro tenant è un 404.
 */
@QuarkusTest
class GdprWithdrawalApiTest {

    private static final String EXPORTS = "/api/platform/v1/gdpr/exports";
    private static final String TENANT = "77777777-0000-0000-0000-0000000000e1";
    private static final String TENANT_OTHER = "77777777-0000-0000-0000-0000000000e2";
    private static final UUID APP_ID = UUID.fromString("99999999-1111-0000-0000-000000000e33");
    private static final String APP_SLUG = "recessoapp";

    @Inject
    TestData data;

    @Inject
    TestMessageQueues queues;

    @Inject
    TestExportStorage storage;

    @Inject
    GdprExportResultsConsumer resultsConsumer;

    @Inject
    ObjectMapper mapper;

    @BeforeEach
    void reset() {
        queues.clear();
        storage.clear();
        data.account(TENANT, "Tenant recesso");
        data.account(TENANT_OTHER, "Tenant estraneo");
        data.app(APP_ID, APP_SLUG);
    }

    private static String withdrawalPath(String slug) {
        return "/api/platform/v1/gdpr/apps/" + slug + "/withdrawal";
    }

    @Test
    void withdrawalRequiresCompletedPerAppExportThenPurges() {
        data.subscription(TENANT, APP_ID, "canceled"); // F31: il diritto vale anche senza accesso
        String token = TestTokens.withTenant(TENANT, "owner");

        // export per-app (passo "esporta")
        String jobId = given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("kind", "app", "appId", APP_SLUG))
                .when().post(EXPORTS)
                .then().statusCode(202)
                .extract().path("id");

        // conferma PRIMA del completamento → 409 (il passo esporta non è concluso)
        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("exportJobId", jobId))
                .when().post(withdrawalPath(APP_SLUG))
                .then().statusCode(409);

        // il worker dell'app non esiste nel test del core: simula l'esito COMPLETED via coda risultati
        completeAppExport(jobId);

        // conferma → 202, purge sulla coda dell'app
        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("exportJobId", jobId))
                .when().post(withdrawalPath(APP_SLUG))
                .then().statusCode(202)
                .body("appId", equalTo(APP_SLUG))
                .body("status", equalTo("PURGE_REQUESTED"));
        assertEquals(1, queues.size(GdprQueues.purgeQueue(APP_SLUG)),
                "la purge dell'app deve partire sulla sua coda");

        // attivazione rimossa: l'app non è più attivata (subscription soft-deleted)
        assertEquals(0, data.subscriptionCount(TENANT, APP_ID));
        // e un nuovo export per-app della stessa app ora è un 404 (app non più attivata)
        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("kind", "app", "appId", APP_SLUG))
                .when().post(EXPORTS)
                .then().statusCode(404);
    }

    @Test
    void withdrawalValidatesExportJobKindAndTenant() {
        data.subscription(TENANT, APP_ID, "active");
        String token = TestTokens.withTenant(TENANT, "owner");

        // export ACCOUNT (kind sbagliato come prova del recesso per-app) → 409
        String accountJob = given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("kind", "account"))
                .when().post(EXPORTS)
                .then().statusCode(202)
                .extract().path("id");
        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("exportJobId", accountJob))
                .when().post(withdrawalPath(APP_SLUG))
                .then().statusCode(409);

        // export job di un ALTRO tenant → 404 (nessun information leak)
        data.subscription(TENANT_OTHER, APP_ID, "active");
        String otherToken = TestTokens.withTenant(TENANT_OTHER, "owner");
        String otherJob = given().header("Authorization", "Bearer " + otherToken)
                .contentType(ContentType.JSON)
                .body(Map.of("kind", "app", "appId", APP_SLUG))
                .when().post(EXPORTS)
                .then().statusCode(202)
                .extract().path("id");
        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("exportJobId", otherJob))
                .when().post(withdrawalPath(APP_SLUG))
                .then().statusCode(404);

        // app mai attivata dal tenant → 404
        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("exportJobId", accountJob))
                .when().post(withdrawalPath("app-inesistente"))
                .then().statusCode(404);

        // nessuna purge partita da questi tentativi
        assertEquals(0, queues.size(GdprQueues.purgeQueue(APP_SLUG)));
    }

    @Test
    void withdrawalIsForbiddenToMembers() {
        data.subscription(TENANT, APP_ID, "active");
        String member = TestTokens.withTenant(TENANT, "member");
        given().header("Authorization", "Bearer " + member)
                .contentType(ContentType.JSON)
                .body(Map.of("exportJobId", UUID.randomUUID().toString()))
                .when().post(withdrawalPath(APP_SLUG))
                .then().statusCode(403);
    }

    /**
     * Porta a COMPLETED l'export per-app: consuma la richiesta rimasta sulla coda dell'app
     * simulando l'esito positivo del worker (come farebbe il servizio dell'app).
     */
    private void completeAppExport(String jobId) {
        // consuma la richiesta pendente sulla coda export dell'app
        queues.receive(GdprQueues.exportQueue(APP_SLUG), 10)
                .forEach(m -> queues.delete(GdprQueues.exportQueue(APP_SLUG), m));
        // carica il frammento come farebbe il worker dell'app, poi notifica l'esito COMPLETED
        String fragmentKey = "jobs/" + jobId + "/" + APP_SLUG + ".json";
        storage.put(fragmentKey, "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8), "application/json");
        try {
            queues.send(GdprQueues.EXPORT_RESULTS, mapper.writeValueAsString(
                    ExportResultMessage.completed(jobId, APP_SLUG, List.of("step"), fragmentKey)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        resultsConsumer.drain();
    }
}
