package app.appgrove.commons.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.MDC;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * AuditLogger (UC 0006): il record porta {@code log_type=audit} nell'MDC (pattern del
 * subscription filter Terraform {@code { $.mdc.log_type = "audit" }}) e l'MDC viene
 * ripristinato dopo la chiamata.
 */
class AuditLoggerTest {

    /** Cattura sincrona dei record con snapshot dell'MDC al momento della pubblicazione. */
    private static final class CapturingHandler extends Handler {
        record Captured(ExtLogRecord record, Map<String, String> mdc) {}

        final List<Captured> captured = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            ExtLogRecord ext = (ExtLogRecord) record;
            captured.add(new Captured(ext, ext.getMdcCopy()));
        }

        @Override
        public void flush() {}

        @Override
        public void close() {}
    }

    private final AuditLogger audit = new AuditLogger();
    private final CapturingHandler handler = new CapturingHandler();

    @BeforeEach
    void attaccaHandler() {
        Logger.getLogger(AuditLogger.LOGGER_NAME).addHandler(handler);
    }

    @AfterEach
    void staccaHandler() {
        Logger.getLogger(AuditLogger.LOGGER_NAME).removeHandler(handler);
        MDC.remove(AuditLogger.LOG_TYPE_KEY);
    }

    @Test
    void ilRecordPortaLogTypeAuditNellMdcEilMdcVieneRipulitoDopo() {
        audit.log("gdpr.export.requested", AuditOutcome.SUCCESS, Map.of("job_id", "j-1"));

        assertEquals(1, handler.captured.size());
        assertEquals("audit", handler.captured.get(0).mdc().get(AuditLogger.LOG_TYPE_KEY));
        assertNull(MDC.get(AuditLogger.LOG_TYPE_KEY), "log_type non deve sopravvivere alla chiamata");
    }

    @Test
    void unLogTypePreesistenteVieneRipristinato() {
        MDC.put(AuditLogger.LOG_TYPE_KEY, "altro");

        audit.log("tenant.offboarded", AuditOutcome.SUCCESS, Map.of());

        assertEquals("audit", handler.captured.get(0).mdc().get(AuditLogger.LOG_TYPE_KEY));
        assertEquals("altro", MDC.get(AuditLogger.LOG_TYPE_KEY), "il valore precedente va ripristinato");
    }

    @Test
    void messaggioConAzioneEsitoEDettagliOrdinatiInModoDeterministico() {
        audit.log("member.updated", AuditOutcome.DENIED, Map.of("role", "admin", "actor", "sub-1"));

        String message = handler.captured.get(0).record().getMessage();
        assertEquals("member.updated outcome=DENIED actor=sub-1 role=admin", message);
    }

    @Test
    void dettagliNulliONonPresentiNonRomponoIlMessaggio() {
        audit.log("member.removed", AuditOutcome.SUCCESS, null);

        assertEquals("member.removed outcome=SUCCESS",
                handler.captured.get(0).record().getMessage());
    }
}
