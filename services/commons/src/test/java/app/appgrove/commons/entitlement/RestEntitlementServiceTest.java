package app.appgrove.commons.entitlement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.commons.quota.QuotaNature;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Logica di lettura dell'entitlement (UC 0027): risoluzione di accesso, cap e natura per (app, metrica),
 * con metriche <b>flow</b> e <b>stock</b>. Non tocca HTTP: il read-model è iniettato da una sottoclasse
 * che sovrascrive {@code entitlements()}.
 */
class RestEntitlementServiceTest {

    /** Stub che salta la chiamata REST e ritorna un read-model fisso. */
    private static final class Stub extends RestEntitlementService {
        private final MeEntitlementsView view;

        Stub(MeEntitlementsView view) {
            this.view = view;
        }

        @Override
        protected MeEntitlementsView entitlements() {
            return view;
        }
    }

    private static EntitlementService service() {
        // fatture: metrica flow (fatture, cap 10, finestra month); teams: metrica stock (seats, cap 5).
        EntitlementView fatture = new EntitlementView(
                "fatture", "free", null, null, Map.of("fatture", new MetricLimit(10, "flow", "month")));
        EntitlementView teams = new EntitlementView(
                "teams", "team", "ACTIVE", null, Map.of("seats", new MetricLimit(5, "stock", null)));
        return new Stub(new MeEntitlementsView(List.of(fatture, teams)));
    }

    @Test
    void hasAccessReflectsPresenceInReadModel() {
        EntitlementService svc = service();
        assertTrue(svc.hasAccess("fatture"));
        assertTrue(svc.hasAccess("teams"));
        assertFalse(svc.hasAccess("legacy"), "app assente dal read-model = nessun accesso");
    }

    @Test
    void capForResolvesFlowAndStockAndUnknown() {
        EntitlementService svc = service();
        assertEquals(10, svc.capFor("fatture", "fatture"), "metrica flow");
        assertEquals(5, svc.capFor("teams", "seats"), "metrica stock");
        assertEquals(-1, svc.capFor("fatture", "altra"), "metrica sconosciuta → nessun limite");
        assertEquals(-1, svc.capFor("legacy", "x"), "app non entitled → nessun limite");
    }

    @Test
    void natureOfDistinguishesFlowFromStock() {
        EntitlementService svc = service();
        assertEquals(QuotaNature.FLOW, svc.natureOf("fatture", "fatture"));
        assertEquals(QuotaNature.STOCK, svc.natureOf("teams", "seats"));
        assertNull(svc.natureOf("fatture", "altra"));
    }
}
