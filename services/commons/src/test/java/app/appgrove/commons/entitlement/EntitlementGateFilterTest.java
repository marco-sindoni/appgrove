package app.appgrove.commons.entitlement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.appgrove.commons.quota.QuotaNature;
import org.junit.jupiter.api.Test;

/**
 * Gate 402 (UC 0027): l'endpoint annotato {@code @RequiresEntitlement} è bloccato (402) quando il
 * tenant non ha accesso all'app, e passa quando ce l'ha. La natura opt-in del gate (endpoint non
 * annotati = nessun filtro) è verificata a livello HTTP nell'app (es. fatture).
 */
class EntitlementGateFilterTest {

    /** EntitlementService a verdetto fisso. */
    private static final class FixedAccess implements EntitlementService {
        private final boolean access;

        FixedAccess(boolean access) {
            this.access = access;
        }

        @Override
        public boolean hasAccess(String appSlug) {
            return access;
        }

        @Override
        public long capFor(String appSlug, String metric) {
            return -1;
        }

        @Override
        public QuotaNature natureOf(String appSlug, String metric) {
            return null;
        }
    }

    private static EntitlementGateFilter filterWith(boolean access) {
        EntitlementGateFilter filter = new EntitlementGateFilter();
        filter.entitlements = new FixedAccess(access);
        filter.appSlug = "fatture";
        return filter;
    }

    @Test
    void blocksWhenNoAccess() {
        assertThrows(EntitlementRequiredException.class, () -> filterWith(false).filter(null));
    }

    @Test
    void passesWhenAccess() {
        assertDoesNotThrow(() -> filterWith(true).filter(null));
    }
}
