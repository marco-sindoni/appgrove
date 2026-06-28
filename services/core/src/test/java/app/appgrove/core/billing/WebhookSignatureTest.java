package app.appgrove.core.billing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Firma HMAC formato Paddle ({@code ts=...;h1=...}) + anti-replay (UC 0025, #09 D18a). Round-trip,
 * rifiuto di manomissione, replay (ts fuori finestra) e header malformato.
 */
@QuarkusTest
class WebhookSignatureTest {

    private static final String BODY = "{\"event_id\":\"evt_sig\",\"event_type\":\"subscription.activated\"}";

    @Inject
    PaddleSignature signature;

    @Test
    void freshSignatureRoundTrips() {
        assertTrue(signature.verify(BODY, signature.sign(BODY)));
    }

    @Test
    void tamperedBodyIsRejected() {
        String header = signature.sign(BODY);
        assertFalse(signature.verify(BODY + " ", header), "body diverso → firma non valida");
    }

    @Test
    void replayedSignatureOutsideWindowIsRejected() {
        long oneHourAgo = Instant.now().getEpochSecond() - 3600;
        assertFalse(signature.verify(BODY, signature.sign(BODY, oneHourAgo)), "ts fuori finestra → replay");
    }

    @Test
    void malformedHeaderIsRejected() {
        assertFalse(signature.verify(BODY, "deadbeef"));
        assertFalse(signature.verify(BODY, "ts=abc;h1=xyz"), "ts non numerico");
        assertFalse(signature.verify(BODY, ""));
        assertFalse(signature.verify(BODY, null));
    }
}
