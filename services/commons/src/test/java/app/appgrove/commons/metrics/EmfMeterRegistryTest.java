package app.appgrove.commons.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Bridge Micrometer→EMF (UC 0006, #08 8-9): forma del documento EMF (oggetto {@code _aws} radice,
 * namespace Appgrove) e whitelist delle dimensioni hard-enforced (regola dei due piani #08 30-31):
 * un tag {@code tenant_id} NON deve mai diventare dimensione né comparire nella riga.
 */
class EmfMeterRegistryTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Duration STEP = Duration.ofMinutes(1);

    private final MockClock clock = new MockClock();
    private final List<String> lines = new ArrayList<>();
    private final EmfMeterRegistry registry =
            new EmfMeterRegistry(EmfMeterRegistry.config(STEP), clock, lines::add);

    @Test
    void contatoreSerializzatoComeDocumentoEmfConNamespaceAppgrove() throws Exception {
        registry.counter("app.errori", "app_id", "fatture").increment(3);
        clock.add(STEP);

        registry.publish();

        assertEquals(1, lines.size());
        JsonNode root = MAPPER.readTree(lines.get(0));
        assertEquals("Appgrove", root.at("/_aws/CloudWatchMetrics/0/Namespace").asText());
        assertTrue(root.at("/_aws/Timestamp").isNumber());
        assertEquals("app_id", root.at("/_aws/CloudWatchMetrics/0/Dimensions/0/0").asText());
        assertEquals("app.errori", root.at("/_aws/CloudWatchMetrics/0/Metrics/0/Name").asText());
        assertEquals("Count", root.at("/_aws/CloudWatchMetrics/0/Metrics/0/Unit").asText());
        // dimensioni e valori come proprietà radice (contratto EMF)
        assertEquals("fatture", root.get("app_id").asText());
        assertEquals(3.0, root.get("app.errori").asDouble());
    }

    @Test
    void unTagTenantIdVieneScartatoDallaWhitelistDelleDimensioni() throws Exception {
        registry.counter("app.errori", "app_id", "fatture", "tenant_id", "t-secret", "user_id", "u-1")
                .increment();
        clock.add(STEP);

        registry.publish();

        String line = lines.get(0);
        assertFalse(line.contains("tenant_id"), "tenant_id non deve mai diventare dimensione");
        assertFalse(line.contains("t-secret"), "il valore del tenant non deve comparire nella riga");
        assertFalse(line.contains("user_id"), "user_id non deve mai diventare dimensione");
        JsonNode dims = MAPPER.readTree(line).at("/_aws/CloudWatchMetrics/0/Dimensions/0");
        assertEquals(1, dims.size());
        assertEquals("app_id", dims.get(0).asText());
    }

    @Test
    void timerPubblicaCountSumMaxComeMetricheSeparateInMillisecondi() throws Exception {
        Timer timer = registry.timer("op.durata", "status", "ok");
        timer.record(100, TimeUnit.MILLISECONDS);
        timer.record(300, TimeUnit.MILLISECONDS);
        clock.add(STEP);

        registry.publish();

        JsonNode root = MAPPER.readTree(lines.get(0));
        assertEquals(2.0, root.get("op.durata.count").asDouble());
        assertEquals(400.0, root.get("op.durata.sum").asDouble());
        assertEquals(300.0, root.get("op.durata.max").asDouble());
        // unità dichiarate nel metadata EMF: count in Count, sum/max in Milliseconds
        JsonNode metrics = root.at("/_aws/CloudWatchMetrics/0/Metrics");
        assertEquals("Count", unitOf(metrics, "op.durata.count"));
        assertEquals("Milliseconds", unitOf(metrics, "op.durata.sum"));
        assertEquals("Milliseconds", unitOf(metrics, "op.durata.max"));
    }

    @Test
    void ilTagUriDelleMetricheHttpVieneRimappatoSuEndpoint() throws Exception {
        // stessa forma dei meter http.server.requests di quarkus-micrometer
        registry.counter("http.server.requests", "uri", "/api/fatture/v1/invoices",
                "method", "GET", "status", "200").increment();
        clock.add(STEP);

        registry.publish();

        JsonNode root = MAPPER.readTree(lines.get(0));
        assertEquals("/api/fatture/v1/invoices", root.get("endpoint").asText());
        assertEquals("200", root.get("status").asText());
        assertFalse(lines.get(0).contains("method"), "i tag fuori whitelist si scartano");
        JsonNode dims = root.at("/_aws/CloudWatchMetrics/0/Dimensions/0");
        assertEquals(2, dims.size());
    }

    @Test
    void unMeterSenzaTagProduceDimensioniVuoteMaRestaValido() throws Exception {
        registry.counter("segnale").increment();
        clock.add(STEP);

        registry.publish();

        JsonNode root = MAPPER.readTree(lines.get(0));
        assertEquals(0, root.at("/_aws/CloudWatchMetrics/0/Dimensions/0").size());
        assertEquals(1.0, root.get("segnale").asDouble());
    }

    @Test
    void unMeterSenzaAttivitaNelloStepNonEmetteRigheAZero() {
        // Cost-min: un meter registrato resta per sempre — senza nuove osservazioni
        // nello step NON deve produrre righe (ingestione CloudWatch pagata per zeri).
        Timer timer = registry.timer("op.durata", "status", "ok");
        registry.counter("app.errori", "app_id", "fatture").increment();
        timer.record(100, TimeUnit.MILLISECONDS);
        clock.add(STEP);
        registry.publish();
        assertEquals(2, lines.size(), "primo step: entrambi i meter hanno attività");

        lines.clear();
        clock.add(STEP);
        registry.publish();
        assertEquals(0, lines.size(), "step senza attività: nessuna riga a zero");

        registry.counter("app.errori", "app_id", "fatture").increment();
        clock.add(STEP);
        registry.publish();
        assertEquals(1, lines.size(), "riparte solo il meter che ha osservato qualcosa");
    }

    private static String unitOf(JsonNode metrics, String name) {
        for (JsonNode metric : metrics) {
            if (name.equals(metric.get("Name").asText())) {
                return metric.get("Unit").asText();
            }
        }
        throw new AssertionError("metrica non trovata: " + name);
    }
}
