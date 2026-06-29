package app.appgrove.fatture;

import app.appgrove.commons.entitlement.EntitlementService;
import app.appgrove.commons.quota.QuotaNature;
import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Mock dell'{@link EntitlementService} per i test di fatture: evita la chiamata REST reale a core
 * (offline, deterministico). Default = accesso pieno con tetto free (10 fatture/mese), così le suite
 * esistenti restano verdi; {@link #accessGranted}/{@link #cap} sono regolabili per esercitare il gate
 * 402 e la quota (vedi {@code EntitlementGateTest}).
 */
@Mock
@ApplicationScoped
public class MockEntitlementService implements EntitlementService {

    static volatile boolean accessGranted = true;
    static volatile long cap = 10;

    static void reset() {
        accessGranted = true;
        cap = 10;
    }

    @Override
    public boolean hasAccess(String appSlug) {
        return accessGranted;
    }

    @Override
    public long capFor(String appSlug, String metric) {
        return cap;
    }

    @Override
    public QuotaNature natureOf(String appSlug, String metric) {
        return QuotaNature.FLOW;
    }
}
