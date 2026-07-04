package app.appgrove.core.gdpr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.commons.gdpr.DataManifest;
import app.appgrove.commons.gdpr.ExportResult;
import app.appgrove.commons.gdpr.GdprScope;
import app.appgrove.commons.gdpr.PurgeResult;
import app.appgrove.core.TestData;
import io.quarkus.test.junit.QuarkusTest;
import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

/**
 * Contratto GDPR della piattaforma (#13 L74, UC 0032 §9 compliance): l'export copre OGNI entità con
 * campi {@code @PersonalData} (metro: il manifesto derivato) e la purge è fisica, scoped al tenant e
 * senza orfani. Una futura entità col manifesto ma fuori dall'export fa fallire questo test.
 */
@QuarkusTest
class PlatformGdprContractTest {

    private static final String TENANT_A = "77777777-0000-0000-0000-0000000000c1";
    private static final String TENANT_B = "77777777-0000-0000-0000-0000000000c2";

    @Inject
    PlatformDataContract contract;

    @Inject
    TestData data;

    @Test
    void exportCoversEveryManifestEntity() {
        data.account(TENANT_A, "Acme GDPR");
        data.user(TENANT_A, "sub-gdpr-a", "gdpr-a@example.test", "owner");
        data.invitation(TENANT_A, "invitato-a@example.test", "member");

        ExportResult export = contract.exportData(new GdprScope(TENANT_A));
        assertEquals("platform", export.appId());
        assertFalse(export.steps().isEmpty(), "il contratto deve dichiarare gli step di progress");

        DataManifest manifest = contract.manifest();
        assertFalse(manifest.entries().isEmpty());
        for (DataManifest.Entry entry : manifest.entries()) {
            List<Map<String, Object>> rows = export.entities().get(entry.entity());
            assertTrue(rows != null && !rows.isEmpty(),
                    "entità del manifesto assente dall'export: " + entry.entity());
            assertTrue(rows.get(0).containsKey(entry.field()),
                    "campo personale non coperto dall'export: " + entry.entity() + "." + entry.field());
        }
    }

    @Test
    void purgeIsPhysicalScopedToTenantAndLeavesNoOrphans() {
        data.account(TENANT_A, "Da cancellare");
        data.user(TENANT_A, "sub-purge-a", "purge-a@example.test", "owner");
        data.invitation(TENANT_A, "invitato-purge-a@example.test", "member");
        data.account(TENANT_B, "Da preservare");
        data.user(TENANT_B, "sub-purge-b", "purge-b@example.test", "owner");

        PurgeResult result = contract.purgeData(new GdprScope(TENANT_A));
        assertTrue(result.total() > 0, "la purge deve cancellare almeno una riga");

        // A: nessun dato residuo in nessuna entità esportabile
        ExportResult afterA = contract.exportData(new GdprScope(TENANT_A));
        afterA.entities().forEach((entity, rows) ->
                assertTrue(rows.isEmpty(), "dati residui per il tenant A in " + entity));

        // B: intatto
        ExportResult afterB = contract.exportData(new GdprScope(TENANT_B));
        assertFalse(afterB.entities().get("accounts").isEmpty(), "l'account di B non va toccato");
        assertFalse(afterB.entities().get("users").isEmpty(), "gli utenti di B non vanno toccati");
    }
}
