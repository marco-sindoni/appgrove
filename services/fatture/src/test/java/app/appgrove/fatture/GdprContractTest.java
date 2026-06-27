package app.appgrove.fatture;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.commons.gdpr.DataManifest;
import app.appgrove.commons.gdpr.ExportResult;
import app.appgrove.commons.gdpr.GdprScope;
import app.appgrove.commons.gdpr.PurgeResult;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Contratto GDPR (UC 0051 §9, #13 L74): export copre ogni entità con dati personali (allineato al
 * manifesto), purge cancella tutto senza orfani e resta scoped al tenant (A non tocca B).
 */
@QuarkusTest
class GdprContractTest {

    private static final String PATH = "/api/fatture/v1/invoices";
    private static final String TENANT_A = "33333333-0000-0000-0000-0000000000a3";
    private static final String TENANT_B = "44444444-0000-0000-0000-0000000000b4";

    @Inject
    FattureDataContract contract;

    @Test
    void exportCoversInvoicesAndLinesAlignedWithManifest() {
        String token = TestTokens.withTenant(TENANT_A, "owner");
        createInvoiceWithLine(token, "Cliente Export", "export@example.test");

        ExportResult export = contract.exportData(new GdprScope(TENANT_A));

        assertEquals("fatture", export.appId());
        assertFalse(export.entities().get("invoice").isEmpty(), "l'export deve includere le fatture");
        assertFalse(export.entities().get("invoice_line").isEmpty(), "l'export deve includere le righe");

        // ogni campo del manifesto è coperto dall'export (allineamento manifesto ↔ export)
        Map<String, Object> firstInvoice = export.entities().get("invoice").get(0);
        DataManifest manifest = contract.manifest();
        for (DataManifest.Entry entry : manifest.entries()) {
            if (entry.entity().equals("invoice")) {
                assertTrue(firstInvoice.containsKey(entry.field()),
                        "campo personale non coperto dall'export: " + entry.field());
            }
        }
    }

    @Test
    void purgeIsPhysicalScopedToTenantAndLeavesNoOrphans() {
        String tokenA = TestTokens.withTenant(TENANT_A, "owner");
        String tokenB = TestTokens.withTenant(TENANT_B, "owner");
        createInvoiceWithLine(tokenA, "Cliente Purge A", "purge-a@example.test");
        createInvoiceWithLine(tokenB, "Cliente Purge B", "purge-b@example.test");

        PurgeResult result = contract.purgeData(new GdprScope(TENANT_A));
        assertTrue(result.total() > 0, "la purge deve cancellare almeno una riga");

        // A: nessun dato residuo (né fatture né righe orfane)
        ExportResult afterA = contract.exportData(new GdprScope(TENANT_A));
        assertTrue(afterA.entities().get("invoice").isEmpty(), "nessuna fattura residua per A");
        assertTrue(afterA.entities().get("invoice_line").isEmpty(), "nessuna riga orfana per A");

        // B: intatto (la purge di A non tocca B)
        ExportResult afterB = contract.exportData(new GdprScope(TENANT_B));
        assertFalse(afterB.entities().get("invoice").isEmpty(), "i dati di B non devono essere toccati");
    }

    private static void createInvoiceWithLine(String token, String customerName, String email) {
        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "customerName", customerName,
                        "customerEmail", email,
                        "lines", List.of(Map.of("description", "Servizio", "quantity", 1, "unitAmount", 100))))
                .when().post(PATH)
                .then().statusCode(201);
    }
}
