package app.appgrove.core.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Contratto degli ID deterministici del catalogo (UC 0022). Questi valori sono <b>cablati</b> nelle FK
 * delle subscription del seed ({@code dev/seed/seed.sql}): se l'algoritmo {@link CatalogIds} cambiasse, il
 * seed fallirebbe le FK. Questo test è la rete di sicurezza che lega loader e seed.
 */
class CatalogIdsTest {

    @Test
    void appAndTierIdsMatchTheSeedFkContract() {
        assertEquals(UUID.fromString("e8b95b18-4b67-3943-aa28-5544c737f9eb"), CatalogIds.appId("notes"));
        assertEquals(UUID.fromString("1c4ea96d-bc57-3109-9c83-0933a3553779"), CatalogIds.appId("teams"));
        assertEquals(UUID.fromString("52fbfc15-5970-3d3c-9d61-5ab3ac37b232"), CatalogIds.appId("legacy"));

        assertEquals(UUID.fromString("6f7a0317-17b2-3bbd-b2b1-6644d9a9186e"), CatalogIds.tierId("notes", "free"));
        assertEquals(UUID.fromString("491687be-df2b-344c-b99d-8c3a601fa7c5"), CatalogIds.tierId("notes", "pro"));
        assertEquals(UUID.fromString("e075f588-c33b-35c5-af41-285c1d006f8e"), CatalogIds.tierId("teams", "team"));
        assertEquals(UUID.fromString("a70ee7e4-d0ae-37e6-aa7d-2f2380833d5f"), CatalogIds.tierId("legacy", "std"));
    }

    @Test
    void idsAreDeterministicAndDistinctPerKey() {
        assertEquals(CatalogIds.priceId("notes", "pro", "monthly"), CatalogIds.priceId("notes", "pro", "monthly"));
        assertNotEquals(
                CatalogIds.priceId("notes", "pro", "monthly"), CatalogIds.priceId("notes", "pro", "annual"));
        assertNotEquals(CatalogIds.appId("notes"), CatalogIds.appId("teams"));
        assertNotEquals(CatalogIds.tierId("notes", "free"), CatalogIds.tierId("notes", "pro"));
    }
}
