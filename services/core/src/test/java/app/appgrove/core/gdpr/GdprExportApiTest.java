package app.appgrove.core.gdpr;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.commons.gdpr.ExportResultMessage;
import app.appgrove.commons.gdpr.GdprExportWorker;
import app.appgrove.commons.gdpr.GdprQueues;
import app.appgrove.core.TestData;
import app.appgrove.core.TestTokens;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Ciclo di vita dell'export job (#13 D22, UC 0032 §9 integration): richiesta → worker per-servizio →
 * aggregazione ZIP → presigned 7gg; fan-out alle app attivate; FAILED su esito negativo; ownership
 * (un tenant non vede i job altrui). Code/storage in-memory; worker e consumer guidati via drain().
 */
@QuarkusTest
class GdprExportApiTest {

    private static final String PATH = "/api/platform/v1/gdpr/exports";
    private static final String TENANT_A = "77777777-0000-0000-0000-0000000000a7";
    private static final String TENANT_B = "88888888-0000-0000-0000-0000000000b8";
    private static final UUID GDPR_APP_ID = UUID.fromString("99999999-1111-0000-0000-000000000001");
    private static final String GDPR_APP_SLUG = "gdprapp";

    @Inject
    TestMessageQueues queues;

    @Inject
    TestExportStorage storage;

    @Inject
    GdprExportWorker platformWorker;

    @Inject
    GdprExportResultsConsumer resultsConsumer;

    @Inject
    TestData data;

    @Inject
    ObjectMapper mapper;

    @BeforeEach
    void reset() {
        queues.clear();
        storage.clear();
        data.account(TENANT_A, "Tenant A GDPR");
        data.account(TENANT_B, "Tenant B GDPR");
        data.app(GDPR_APP_ID, GDPR_APP_SLUG);
    }

    @Test
    void accountExportCompletesWithZipAndPresignedLink() throws Exception {
        // tenant A senza app attivate → unico servizio coinvolto: platform
        String token = TestTokens.withTenant(TENANT_A, "owner");
        String jobId = given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("kind", "account"))
                .when().post(PATH)
                .then().statusCode(202)
                .body("status", equalTo("RUNNING"))
                .body("progress.total", equalTo(1))
                .extract().path("id");

        assertEquals(1, platformWorker.drain(), "il worker platform deve consumare la richiesta");
        assertEquals(1, resultsConsumer.drain(), "il consumer deve applicare l'esito");

        given().header("Authorization", "Bearer " + token)
                .when().get(PATH + "/" + jobId)
                .then().statusCode(200)
                .body("status", equalTo("COMPLETED"))
                .body("progress.completed", equalTo(1))
                .body("items[0].appId", equalTo("platform"))
                .body("items[0].steps.size()", equalTo(4));

        // ZIP aggregato nello storage, con il frammento platform.json
        String zipKey = GdprQueues.zipKey(jobId);
        assertTrue(storage.contains(zipKey), "lo ZIP finale deve stare nello storage export");
        List<String> entries = zipEntries(storage.get(zipKey));
        assertEquals(List.of("platform.json"), entries);

        // link firmato con scadenza 7 giorni (#13 D22)
        String expires = given().header("Authorization", "Bearer " + token)
                .when().get(PATH + "/" + jobId + "/download")
                .then().statusCode(200)
                .body("url", notNullValue())
                .extract().path("expiresAt");
        Instant expiresAt = Instant.parse(expires);
        assertTrue(expiresAt.isAfter(Instant.now().plus(Duration.ofDays(6)))
                        && expiresAt.isBefore(Instant.now().plus(Duration.ofDays(8))),
                "il link deve scadere a ~7 giorni, non " + expiresAt);
    }

    @Test
    void accountExportFansOutToActivatedAppsAndFailsOnFailedResult() throws Exception {
        // app attivata (subscription anche canceled: i diritti valgono per tutta la retention, F31)
        data.subscription(TENANT_B, GDPR_APP_ID, "canceled");
        String token = TestTokens.withTenant(TENANT_B, "owner");

        String jobId = given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("kind", "account"))
                .when().post(PATH)
                .then().statusCode(202)
                .body("progress.total", equalTo(2))
                .extract().path("id");

        // fan-out: una richiesta anche sulla coda export dell'app
        assertEquals(1, queues.size(GdprQueues.exportQueue(GDPR_APP_SLUG)),
                "la richiesta deve arrivare sulla coda export dell'app attivata");

        platformWorker.drain();
        resultsConsumer.drain();

        // l'app risponde FAILED → job FAILED (#13 D22; il ticket privacy automatico è UC 0034)
        queues.send(GdprQueues.EXPORT_RESULTS, mapper.writeValueAsString(
                ExportResultMessage.failed(jobId, GDPR_APP_SLUG, "errore simulato")));
        resultsConsumer.drain();

        given().header("Authorization", "Bearer " + token)
                .when().get(PATH + "/" + jobId)
                .then().statusCode(200)
                .body("status", equalTo("FAILED"));

        // download non disponibile su job non COMPLETED
        given().header("Authorization", "Bearer " + token)
                .when().get(PATH + "/" + jobId + "/download")
                .then().statusCode(409);
    }

    @Test
    void jobsAreInvisibleToOtherTenants() {
        String tokenA = TestTokens.withTenant(TENANT_A, "owner");
        String jobId = given().header("Authorization", "Bearer " + tokenA)
                .contentType(ContentType.JSON)
                .body(Map.of("kind", "account"))
                .when().post(PATH)
                .then().statusCode(202)
                .extract().path("id");

        // il tenant B non vede il job di A (discriminator → 404, non 403: nessun information leak)
        String tokenB = TestTokens.withTenant(TENANT_B, "owner");
        given().header("Authorization", "Bearer " + tokenB)
                .when().get(PATH + "/" + jobId)
                .then().statusCode(404);
        given().header("Authorization", "Bearer " + tokenB)
                .when().get(PATH + "/" + jobId + "/download")
                .then().statusCode(404);
    }

    @Test
    void singleAppExportValidatesActivation() {
        String token = TestTokens.withTenant(TENANT_A, "owner");

        // kind=app senza appId → 400
        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("kind", "app"))
                .when().post(PATH)
                .then().statusCode(400);

        // app mai attivata dal tenant → 404
        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("kind", "app", "appId", GDPR_APP_SLUG))
                .when().post(PATH)
                .then().statusCode(404);
    }

    private static List<String> zipEntries(byte[] zip) throws Exception {
        List<String> names = new ArrayList<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            for (ZipEntry entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()) {
                names.add(entry.getName());
            }
        }
        return names;
    }
}
