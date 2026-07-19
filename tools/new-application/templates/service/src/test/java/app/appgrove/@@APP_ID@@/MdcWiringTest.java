package app.appgrove.@@APP_ID@@;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.test.junit.QuarkusTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Wiring del contesto di log di una richiesta autenticata (invariante #4, UC 0006): i campi arrivano
 * dal framework (commons) e {@code app_id=@@APP_ID@@} dalla config del servizio; il correlation id
 * riusa l'header dell'edge, altrimenti il request id.
 */
@QuarkusTest
class MdcWiringTest {

    private static final String TENANT = "55555555-0000-0000-0000-0000000000b1";

    private static final class CapturingHandler extends Handler {
        final List<Map<String, String>> mdcSnapshots = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            mdcSnapshots.add(((ExtLogRecord) record).getMdcCopy());
        }

        @Override
        public void flush() {}

        @Override
        public void close() {}
    }

    private final CapturingHandler handler = new CapturingHandler();

    @BeforeEach
    void attaccaHandler() {
        Logger.getLogger(MdcProbeResource.LOGGER_NAME).addHandler(handler);
    }

    @AfterEach
    void staccaHandler() {
        Logger.getLogger(MdcProbeResource.LOGGER_NAME).removeHandler(handler);
    }

    @Test
    void unaRichiestaAutenticataProduceUnLogConTuttiICampi() {
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT, "owner"))
                .header("X-Correlation-Id", "corr-edge-456")
                .when().get("/test/mdc-probe")
                .then().statusCode(200);

        assertEquals(1, handler.mdcSnapshots.size());
        Map<String, String> mdc = handler.mdcSnapshots.get(0);
        assertEquals(TENANT, mdc.get("tenant_id"));
        // Guardia di regressione #08/5 (nessun dato personale nei log): user_id = claim `sub`
        // (invariante #1), NON l'upn — che nei token reali è l'email. Il token di test ha upn≠sub apposta.
        assertEquals("sub-" + TENANT, mdc.get("user_id"),
                "user_id deve essere il sub (id opaco), mai upn/preferred_username (può essere l'email)");
        assertEquals("@@APP_ID@@", mdc.get("app_id"));
        assertEquals("corr-edge-456", mdc.get("correlation_id"));
        assertNotNull(mdc.get("request_id"));
        assertNotNull(mdc.get("trace_id"));
        assertNotNull(mdc.get("span_id"));
    }

    @Test
    void senzaHeaderIlCorrelationIdRicadeSulRequestId() {
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT, "owner"))
                .when().get("/test/mdc-probe")
                .then().statusCode(200);

        Map<String, String> mdc = handler.mdcSnapshots.get(0);
        assertEquals(mdc.get("request_id"), mdc.get("correlation_id"));
    }
}
