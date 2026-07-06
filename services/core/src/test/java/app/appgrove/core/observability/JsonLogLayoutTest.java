package app.appgrove.core.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.json.runtime.JsonFormatter;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.MDC;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Forma REALE dell'evento log JSON prodotto da quarkus-logging-json (config default, come in
 * test/prod): {@code level} è un campo top-level e l'MDC è annidato sotto {@code mdc} — è il
 * contratto su cui poggiano i pattern dei filtri Terraform:
 * errori {@code { $.level = "ERROR" }}, audit {@code { $.mdc.log_type = "audit" }}.
 */
class JsonLogLayoutTest {

    @AfterEach
    void pulisciMdc() {
        MDC.clear();
    }

    @Test
    void livelloTopLevelEMdcAnnidatoSottoLaChiaveMdc() throws Exception {
        // stesso formatter che l'estensione installa sul console handler (config default)
        JsonFormatter formatter = new JsonFormatter();
        MDC.put("tenant_id", "t-1");
        MDC.put("app_id", "platform");
        MDC.put("log_type", "audit");
        ExtLogRecord record = new ExtLogRecord(Level.ERROR, "boom", JsonLogLayoutTest.class.getName());
        record.setLoggerName("app.appgrove.core.Prova");

        JsonNode root = new ObjectMapper().readTree(formatter.format(record));

        assertEquals("ERROR", root.get("level").asText(), "pattern errori: { $.level = \"ERROR\" }");
        assertEquals("audit", root.at("/mdc/log_type").asText(),
                "pattern audit: { $.mdc.log_type = \"audit\" }");
        assertEquals("t-1", root.at("/mdc/tenant_id").asText());
        assertEquals("platform", root.at("/mdc/app_id").asText());
        assertEquals("boom", root.get("message").asText());
        assertEquals("app.appgrove.core.Prova", root.get("loggerName").asText());
        assertTrue(root.has("timestamp"), "timestamp top-level (formato ISO con offset)");
    }
}
