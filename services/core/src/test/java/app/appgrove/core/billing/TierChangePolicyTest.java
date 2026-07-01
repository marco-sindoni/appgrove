package app.appgrove.core.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.commons.entitlement.MetricLimit;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit puro del gating cambio piano (UC 0028 §4.2, #09 E23): direzione per importo e blocco stock sul
 * downgrade. Nessuna persistenza — logica di dominio isolata.
 */
class TierChangePolicyTest {

    @Test
    void directionByAmount() {
        assertEquals(TierChangePolicy.Direction.DOWNGRADE, TierChangePolicy.direction(900, 500));
        assertEquals(TierChangePolicy.Direction.UPGRADE, TierChangePolicy.direction(500, 900));
        assertEquals(TierChangePolicy.Direction.SAME, TierChangePolicy.direction(500, 500));
        // dal tier free (0) verso un tier a pagamento = upgrade
        assertEquals(TierChangePolicy.Direction.UPGRADE, TierChangePolicy.direction(0, 500));
    }

    @Test
    void downgradeBlockedWhenStockUsageAboveTargetCap() {
        Map<String, MetricLimit> target = Map.of("projects", new MetricLimit(3, "stock", null));
        TierChangePolicy.Decision decision =
                TierChangePolicy.evaluateDowngrade(target, Map.of("projects", 5L));
        assertTrue(decision.blocked(), "5 progetti sopra il cap 3 stock → bloccato");
        assertTrue(decision.remediation().contains("projects"));
    }

    @Test
    void downgradeAllowedWhenStockUsageWithinTargetCap() {
        Map<String, MetricLimit> target = Map.of("projects", new MetricLimit(3, "stock", null));
        assertFalse(TierChangePolicy.evaluateDowngrade(target, Map.of("projects", 2L)).blocked());
    }

    @Test
    void flowMetricNeverBlocksDowngrade() {
        // le metriche flow si applicano dal periodo successivo → non bloccano il cambio anche se sopra il cap
        Map<String, MetricLimit> target = Map.of("api_calls", new MetricLimit(100, "flow", "month"));
        assertFalse(TierChangePolicy.evaluateDowngrade(target, Map.of("api_calls", 500L)).blocked());
    }

    @Test
    void emptyUsageAllowsDowngrade() {
        // sorgente usage per-app non ancora disponibile (differita) → nessun blocco a runtime
        Map<String, MetricLimit> target = Map.of("projects", new MetricLimit(3, "stock", null));
        assertFalse(TierChangePolicy.evaluateDowngrade(target, Map.of()).blocked());
    }
}
