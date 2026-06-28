package app.appgrove.core.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.appgrove.core.catalog.PricingDefinition.AppDef;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Guardia di sicurezza prod (UC 0022): con {@code include-fixtures=false} (come in produzione) il loader
 * carica <b>solo le app reali</b> ({@code pricing/index.yaml}), MAI le fixture sintetiche di
 * {@code pricing/fixtures/} → la sync di produzione non crea Product Paddle per app finte.
 */
@QuarkusTest
@TestProfile(PricingCatalogRealOnlyTest.RealOnlyProfile.class)
class PricingCatalogRealOnlyTest {

    public static class RealOnlyProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            // come in prod: niente fixture. sync-on-startup off per non sincronizzare al boot in questo profilo.
            return Map.of(
                    "appgrove.pricing.include-fixtures", "false",
                    "appgrove.pricing.sync-on-startup", "false");
        }
    }

    @Inject
    PricingCatalogLoader loader;

    @Test
    void prodLoadsOnlyRealAppsNotFixtures() {
        List<String> slugs = loader.load().stream().map(AppDef::slug).toList();
        assertEquals(List.of("fatture"), slugs, "in prod il catalogo è solo le app reali, niente fixture");
    }
}
