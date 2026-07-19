package app.appgrove.@@APP_ID@@;

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
 * Contratto GDPR (#13 L74): l'export copre ogni entità con dati personali (allineato al manifesto),
 * la purge cancella tutto senza orfani e resta limitata al tenant (A non tocca B).
 *
 * <p>È il test che rende vera la regola "no contratto = no produzione": se l'app aggiunge un campo
 * personale e non lo esporta, o se la purge lascia residui, qui diventa rosso.
 */
@QuarkusTest
class GdprContractTest {

    private static final String PATH = "/api/@@APP_ID@@/v1/items";
    private static final String TENANT_A = "33333333-0000-0000-0000-0000000000a3";
    private static final String TENANT_B = "44444444-0000-0000-0000-0000000000b4";

    @Inject
    @@APP_CLASS@@DataContract contract;

    @Test
    void exportCoversRecordsAndLinesAlignedWithManifest() {
        String token = TestTokens.withTenant(TENANT_A, "owner");
        createRecordWithLine(token, "Contatto Export", "export@example.test");

        ExportResult export = contract.exportData(new GdprScope(TENANT_A));

        assertEquals("@@APP_ID@@", export.appId());
        assertFalse(export.entities().get("item").isEmpty(), "l'export deve includere i record");
        assertFalse(export.entities().get("item_line").isEmpty(), "l'export deve includere le righe");

        // ogni campo del manifesto è coperto dall'export (allineamento manifesto ↔ export)
        Map<String, Object> firstItem = export.entities().get("item").get(0);
        DataManifest manifest = contract.manifest();
        for (DataManifest.Entry entry : manifest.entries()) {
            if (entry.entity().equals("item")) {
                assertTrue(firstItem.containsKey(entry.field()),
                        "campo personale non coperto dall'export: " + entry.field());
            }
        }
    }

    @Test
    void purgeIsPhysicalScopedToTenantAndLeavesNoOrphans() {
        String tokenA = TestTokens.withTenant(TENANT_A, "owner");
        String tokenB = TestTokens.withTenant(TENANT_B, "owner");
        createRecordWithLine(tokenA, "Contatto Purge A", "purge-a@example.test");
        createRecordWithLine(tokenB, "Contatto Purge B", "purge-b@example.test");

        PurgeResult result = contract.purgeData(new GdprScope(TENANT_A));
        assertTrue(result.total() > 0, "la purge deve cancellare almeno una riga");

        // A: nessun dato residuo (né record né righe orfane)
        ExportResult afterA = contract.exportData(new GdprScope(TENANT_A));
        assertTrue(afterA.entities().get("item").isEmpty(), "nessun record residuo per A");
        assertTrue(afterA.entities().get("item_line").isEmpty(), "nessuna riga orfana per A");

        // B: intatto (la purge di A non tocca B)
        ExportResult afterB = contract.exportData(new GdprScope(TENANT_B));
        assertFalse(afterB.entities().get("item").isEmpty(), "i dati di B non devono essere toccati");
    }

    private static void createRecordWithLine(String token, String contactName, String email) {
        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "contactName", contactName,
                        "contactEmail", email,
                        "lines", List.of(Map.of("description", "Voce", "quantity", 1, "unitAmount", 100))))
                .when().post(PATH)
                .then().statusCode(201);
    }
}
