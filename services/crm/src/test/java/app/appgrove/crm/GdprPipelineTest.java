package app.appgrove.crm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.commons.gdpr.ExportRequestMessage;
import app.appgrove.commons.gdpr.ExportResult;
import app.appgrove.commons.gdpr.ExportResultMessage;
import app.appgrove.commons.gdpr.GdprExportWorker;
import app.appgrove.commons.gdpr.GdprQueues;
import app.appgrove.commons.gdpr.GdprScope;
import app.appgrove.commons.gdpr.TenantPurgeConsumer;
import app.appgrove.commons.gdpr.TenantPurgeMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pipeline GDPR lato app (UC 0032): il worker consuma la coda export dell'app, carica il frammento e
 * notifica l'esito; il consumer purge cancella lo schema e registra l'audit (#13 L70).
 * Code/storage in-memory, drain() esplicito (scheduler spento nei test).
 */
@QuarkusTest
class GdprPipelineTest {

    private static final String TENANT = "55555555-0000-0000-0000-0000000000e5";

    @Inject
    GdprExportWorker worker;

    @Inject
    TenantPurgeConsumer purgeConsumer;

    @Inject
    CrmDataContract contract;

    @Inject
    TestMessageQueues queues;

    @Inject
    TestExportStorage storage;

    @Inject
    ObjectMapper mapper;

    @Inject
    AgroalDataSource ds;

    @BeforeEach
    void reset() {
        queues.clear();
        storage.clear();
    }

    @Test
    void exportWorkerUploadsFragmentAndReportsResult() throws Exception {
        seedContact();
        String jobId = UUID.randomUUID().toString();

        queues.send(GdprQueues.exportQueue("crm"),
                mapper.writeValueAsString(new ExportRequestMessage(jobId, TENANT, "crm")));
        assertEquals(1, worker.drain());

        // frammento nello storage, con i dati del tenant
        String fragmentKey = GdprQueues.fragmentKey(jobId, "crm");
        assertTrue(storage.contains(fragmentKey), "il frammento deve stare nello storage export");
        ExportResult fragment = mapper.readValue(storage.get(fragmentKey), ExportResult.class);
        assertEquals("crm", fragment.appId());
        assertTrue(fragment.entities().get("contact").size() >= 1);

        // esito COMPLETED sulla coda risultati (aggregata dal core)
        List<String> results = queues.bodies(GdprQueues.EXPORT_RESULTS);
        assertEquals(1, results.size());
        ExportResultMessage result = mapper.readValue(results.get(0), ExportResultMessage.class);
        assertTrue(result.success());
        assertEquals(jobId, result.jobId());
        assertEquals(fragmentKey, result.fragmentKey());
        assertEquals(contract.exportData(new GdprScope(TENANT)).steps(), result.steps());
    }

    @Test
    void purgeConsumerErasesTenantAndWritesAudit() throws Exception {
        seedContact();

        queues.send(GdprQueues.purgeQueue("crm"),
                mapper.writeValueAsString(new TenantPurgeMessage(TENANT, TenantPurgeMessage.REASON_OFFBOARDED)));
        assertEquals(1, purgeConsumer.drain());

        // erasure fisica, nessun orfano
        ExportResult after = contract.exportData(new GdprScope(TENANT));
        assertTrue(after.entities().get("contact").isEmpty(), "nessun contatto residuo");
        assertTrue(after.entities().get("interaction").isEmpty(), "nessuna interazione orfana");
        assertTrue(after.entities().get("seat").isEmpty(), "nessun posto residuo");

        // audit registrato nello schema dell'app (prova, #13 L70)
        assertTrue(auditCount(TENANT) >= 1, "la purge deve lasciare la riga di audit");
    }

    private int auditCount(String tenantId) throws Exception {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "select count(*) from app_crm.gdpr_purge_audit"
                                + " where tenant_id = ? and app_id = 'crm'")) {
            ps.setString(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static void seedContact() {
        CrmApi.seatSelf(TENANT, "owner");
        CrmApi.createContact(TestTokens.withTenant(TENANT, "owner"), "Contatto Pipeline");
    }
}
