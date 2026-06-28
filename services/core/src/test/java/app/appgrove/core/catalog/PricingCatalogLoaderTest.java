package app.appgrove.core.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.core.catalog.PricingDefinition.AppDef;
import app.appgrove.core.catalog.PricingDefinition.PriceDef;
import app.appgrove.core.catalog.PricingDefinition.TierDef;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Loader del pricing-as-code (UC 0022): legge gli YAML in {@code resources/pricing/}, in modo deterministico. */
@QuarkusTest
class PricingCatalogLoaderTest {

    @Inject
    PricingCatalogLoader loader;

    private static AppDef bySlug(List<AppDef> defs, String slug) {
        return defs.stream().filter(a -> a.slug().equals(slug)).findFirst().orElseThrow();
    }

    @Test
    void loadsRealAppsPlusFixturesInTest() {
        // in %test include-fixtures=true → app reali (fatture) + fixture (notes/teams/legacy)
        List<AppDef> defs = loader.load();
        assertEquals(
                java.util.Set.of("fatture", "notes", "teams", "legacy"),
                defs.stream().map(AppDef::slug).collect(java.util.stream.Collectors.toSet()));
        // l'app reale viene prima delle fixture (indice reale caricato per primo)
        assertEquals("fatture", defs.get(0).slug());

        AppDef notes = bySlug(defs, "notes");
        assertEquals(AppUserModel.single_user, notes.userModel());
        assertEquals(AppStatus.active, notes.status());

        TierDef pro = notes.tiers().stream().filter(t -> t.key().equals("pro")).findFirst().orElseThrow();
        assertEquals(2, pro.prices().size(), "Notes Pro ha mensile + annuale");
        PriceDef monthly =
                pro.prices().stream().filter(p -> p.billingCycle() == BillingCycle.monthly).findFirst().orElseThrow();
        assertEquals(900, monthly.amount());
        assertEquals("EUR", monthly.currency());
        assertEquals("notes", pro.limits().get("metric"), "i limiti sono JSON nel nostro DB, non in Paddle");

        AppDef legacy = defs.stream().filter(a -> a.slug().equals("legacy")).findFirst().orElseThrow();
        assertEquals(AppStatus.inactive, legacy.status(), "legacy è disabilitata");
    }

    @Test
    void loadIsDeterministic() {
        List<AppDef> a = loader.load();
        List<AppDef> b = loader.load();
        assertEquals(a.stream().map(AppDef::slug).toList(), b.stream().map(AppDef::slug).toList());
        assertTrue(a.equals(b), "stessi YAML → stessa definizione (record equality)");
    }
}
