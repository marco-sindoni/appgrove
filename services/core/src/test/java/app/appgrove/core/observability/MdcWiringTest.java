package app.appgrove.core.observability;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.appgrove.core.TestTokens;
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
 * Wiring MDC di una richiesta autenticata (invariante #4, UC 0006): ogni record di log porta
 * tenant_id/app_id/user_id/correlation_id (+ trace_id/span_id dall'OTel strumentato); il
 * correlation_id riusa l'header {@code X-Correlation-Id} dell'edge, altrimenti il request id.
 */
@QuarkusTest
class MdcWiringTest {

    private static final String TENANT = "44444444-0000-0000-0000-0000000000a1";

    /** Cattura sincrona con snapshot dell'MDC alla pubblicazione (come fa il console handler). */
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
    void unaRichiestaAutenticataProduceUnLogConTuttiICampiMdc() {
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT, "owner"))
                .header("X-Correlation-Id", "corr-dal-gateway-123")
                .when().get("/test/mdc-probe")
                .then().statusCode(200);

        assertEquals(1, handler.mdcSnapshots.size());
        Map<String, String> mdc = handler.mdcSnapshots.get(0);
        assertEquals(TENANT, mdc.get("tenant_id"));
        // Guardia di regressione #08/5 (nessun dato personale nei log): user_id = claim `sub` (invariante #1),
        // NON l'upn — che nei token reali è l'email. Il token di test ha upn≠sub apposta.
        assertEquals(TestTokens.subjectFor(TENANT), mdc.get("user_id"),
                "user_id deve essere il sub (id opaco), mai upn/preferred_username (può essere l'email)");
        assertEquals("platform", mdc.get("app_id"), "app_id del core = platform (config appgrove.app-id via commons)");
        assertEquals("corr-dal-gateway-123", mdc.get("correlation_id"),
                "il correlation id dell'edge va riusato, non rigenerato");
        assertNotNull(mdc.get("request_id"));
        // strumentazione OTel attiva (export spento): trace/span nei log con nomi OTel (#08/3)
        assertNotNull(mdc.get("trace_id"), "trace_id atteso dallo span OTel della richiesta");
        assertNotNull(mdc.get("span_id"), "span_id atteso dallo span OTel della richiesta");
    }

    @Test
    void senzaHeaderIlCorrelationIdEGeneratoERiusaIlRequestId() {
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT, "owner"))
                .when().get("/test/mdc-probe")
                .then().statusCode(200);

        Map<String, String> mdc = handler.mdcSnapshots.get(0);
        assertNotNull(mdc.get("correlation_id"));
        assertEquals(mdc.get("request_id"), mdc.get("correlation_id"),
                "fallback: senza header dall'edge il correlation id coincide col request id");
    }
}
