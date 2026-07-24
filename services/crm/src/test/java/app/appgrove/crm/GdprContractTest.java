package app.appgrove.crm;

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

    private static final String TENANT_A = "33333333-0000-0000-0000-0000000000a3";
    private static final String TENANT_B = "44444444-0000-0000-0000-0000000000b4";

    @Inject
    CrmDataContract contract;

    @Test
    void exportCoversContactsAndInteractionsAlignedWithManifest() {
        String token = TestTokens.withTenant(TENANT_A, "owner");
        seedContactWithInteraction(TENANT_A, token);

        ExportResult export = contract.exportData(new GdprScope(TENANT_A));

        assertEquals("crm", export.appId());
        assertFalse(export.entities().get("contact").isEmpty(), "l'export deve includere i contatti");
        assertFalse(export.entities().get("interaction").isEmpty(), "l'export deve includere le interazioni");

        // ogni campo personale del manifesto è coperto dall'export dell'entità corrispondente
        DataManifest manifest = contract.manifest();
        Map<String, Object> firstContact = export.entities().get("contact").get(0);
        for (DataManifest.Entry entry : manifest.entries()) {
            if (entry.entity().equals("contact")) {
                assertTrue(firstContact.containsKey(entry.field()),
                        "campo personale non coperto dall'export: contact." + entry.field());
            }
        }
    }

    @Test
    void purgeIsPhysicalScopedToTenantAndLeavesNoOrphans() {
        String tokenA = TestTokens.withTenant(TENANT_A, "owner");
        String tokenB = TestTokens.withTenant(TENANT_B, "owner");
        seedContactWithInteraction(TENANT_A, tokenA);
        seedContactWithInteraction(TENANT_B, tokenB);

        PurgeResult result = contract.purgeData(new GdprScope(TENANT_A));
        assertTrue(result.total() > 0, "la purge deve cancellare almeno una riga");

        // A: nessun dato residuo (contatti, interazioni orfane, posti)
        ExportResult afterA = contract.exportData(new GdprScope(TENANT_A));
        assertTrue(afterA.entities().get("contact").isEmpty(), "nessun contatto residuo per A");
        assertTrue(afterA.entities().get("interaction").isEmpty(), "nessuna interazione orfana per A");
        assertTrue(afterA.entities().get("seat").isEmpty(), "nessun posto residuo per A");

        // B: intatto (la purge di A non tocca B)
        ExportResult afterB = contract.exportData(new GdprScope(TENANT_B));
        assertFalse(afterB.entities().get("contact").isEmpty(), "i dati di B non devono essere toccati");
    }

    /** Dà un posto al tenant, crea un contatto e vi aggiunge un'interazione. */
    private static void seedContactWithInteraction(String tenant, String token) {
        CrmApi.seatSelf(tenant, "owner");
        String id = CrmApi.createContact(token, "Contatto GDPR");
        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("kind", "note", "note", "Nota di prova"))
                .when().post(CrmApi.CONTACTS + "/" + id + "/interactions")
                .then().statusCode(201);
    }
}
