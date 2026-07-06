package app.appgrove.core.gdpr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.commons.audit.AuditLogger;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.MDC;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Strumentazione audit degli eventi GDPR esistenti (UC 0006): l'offboarding di un tenant emette
 * l'evento {@code tenant.offboarded} sul logger {@code appgrove.audit} con {@code log_type=audit}
 * nell'MDC (instradabile dal subscription filter), senza sporcare l'MDC dopo la chiamata.
 */
@QuarkusTest
class GdprAuditEventsTest {

    private static final class CapturingHandler extends Handler {
        record Captured(String message, String logType) {}

        final List<Captured> captured = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            ExtLogRecord ext = (ExtLogRecord) record;
            captured.add(new Captured(ext.getMessage(), ext.getMdcCopy().get(AuditLogger.LOG_TYPE_KEY)));
        }

        @Override
        public void flush() {}

        @Override
        public void close() {}
    }

    @Inject
    TenantOffboarding offboarding;

    private final CapturingHandler handler = new CapturingHandler();

    @BeforeEach
    void attaccaHandler() {
        Logger.getLogger(AuditLogger.LOGGER_NAME).addHandler(handler);
    }

    @AfterEach
    void staccaHandler() {
        Logger.getLogger(AuditLogger.LOGGER_NAME).removeHandler(handler);
    }

    @Test
    void lOffboardingEmetteLEventoAuditTenantOffboarded() {
        String tenant = UUID.randomUUID().toString();

        offboarding.offboard(tenant, "test-audit");

        assertEquals(1, handler.captured.size(), "un solo evento audit atteso");
        CapturingHandler.Captured event = handler.captured.get(0);
        assertEquals("audit", event.logType(), "log_type=audit nell'MDC del record");
        assertTrue(event.message().startsWith("tenant.offboarded outcome=SUCCESS"), event.message());
        assertTrue(event.message().contains("tenant_id=" + tenant),
                "il tenant va nei details: l'offboarding gira anche fuori richiesta");
        assertTrue(event.message().contains("reason=test-audit"), event.message());
        assertNull(MDC.get(AuditLogger.LOG_TYPE_KEY), "l'MDC va ripristinato dopo l'evento");
    }
}
