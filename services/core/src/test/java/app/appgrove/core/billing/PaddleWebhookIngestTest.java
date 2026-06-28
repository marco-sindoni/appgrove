package app.appgrove.core.billing;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Ingest webhook (#09 D18a/D19): firma valida → 200 + accodato; firma errata → 401 e <b>nessun
 * processing</b> (niente in coda → niente scrittura su subscription). Endpoint pubblico (HMAC, non JWT).
 */
@QuarkusTest
class PaddleWebhookIngestTest {

    private static final String PATH = "/api/platform/v1/webhooks/paddle";
    private static final String TENANT = "11111111-0000-0000-0000-000000000001";

    @Inject
    PaddleSignature signature;

    @Inject
    InMemoryWebhookQueue queue;

    @BeforeEach
    void reset() {
        queue.clear();
    }

    @Test
    void validSignatureIsAcceptedAndEnqueued() {
        String body = webhook(UUID.randomUUID());
        given().contentType(ContentType.JSON)
                .header("Paddle-Signature", signature.sign(body))
                .body(body)
                .when().post(PATH)
                .then().statusCode(200);
        assertEquals(1, queue.size(), "il webhook valido viene accodato");
    }

    @Test
    void invalidSignatureIsRejectedWithoutProcessing() {
        String body = webhook(UUID.randomUUID());
        given().contentType(ContentType.JSON)
                .header("Paddle-Signature", "deadbeef-firma-errata")
                .body(body)
                .when().post(PATH)
                .then().statusCode(401);
        assertEquals(0, queue.size(), "firma errata → niente accodamento, niente processing");
    }

    @Test
    void missingSignatureIsRejected() {
        String body = webhook(UUID.randomUUID());
        given().contentType(ContentType.JSON)
                .body(body)
                .when().post(PATH)
                .then().statusCode(401);
        assertEquals(0, queue.size());
    }

    @Test
    void replayedSignatureIsRejected() {
        String body = webhook(UUID.randomUUID());
        long oneHourAgo = java.time.Instant.now().getEpochSecond() - 3600;
        given().contentType(ContentType.JSON)
                .header("Paddle-Signature", signature.sign(body, oneHourAgo))
                .body(body)
                .when().post(PATH)
                .then().statusCode(401);
        assertEquals(0, queue.size(), "firma replay (ts fuori finestra) → niente processing");
    }

    private static String webhook(UUID appId) {
        return """
                {"event_id":"evt_test","event_type":"subscription.activated","occurred_at":"2026-06-27T10:00:00Z",
                 "data":{"paddle_subscription_id":"sub_test","status":"active",
                 "current_period_start":"2026-06-27T10:00:00Z","current_period_end":"2027-06-27T10:00:00Z",
                 "cancel_at":null,"trial_end":null,
                 "custom_data":{"tenant_id":"%s","app_id":"%s"}}}"""
                .formatted(TENANT, appId);
    }
}
