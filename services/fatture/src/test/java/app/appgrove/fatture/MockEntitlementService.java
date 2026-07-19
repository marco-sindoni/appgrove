package app.appgrove.fatture;

import app.appgrove.commons.entitlement.EntitlementService;
import app.appgrove.commons.entitlement.EntitlementView;
import app.appgrove.commons.entitlement.EntitlementViewSource;
import app.appgrove.commons.entitlement.MetricLimit;
import app.appgrove.commons.entitlement.SafetyNet;
import app.appgrove.commons.quota.QuotaNature;
import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sta al posto della <b>rete di sicurezza</b> (la chiamata REST reale a core) nei test di fatture:
 * offline e deterministico. Default = accesso pieno con tetto free (10 fatture/mese).
 *
 * <p><b>Perché è qualificato {@link SafetyNet} e non predefinito</b> (UC 0046): sostituendo il bean
 * predefinito, i test scavalcherebbero completamente la proiezione locale — cioè proprio il percorso
 * che gira in produzione. Qualificandolo come rete di sicurezza, l'intera suite attraversa
 * {@code ProjectedEntitlementService} → proiezione → (all'occorrenza) questa finta sorgente.
 *
 * <p>{@link #calls} conta gli accessi alla rete di sicurezza: è ciò che permette di dimostrare che
 * una proiezione fresca <b>non</b> genera traffico verso core. {@link #unreachable} simula core giù.
 */
@Mock
@SafetyNet
@ApplicationScoped
public class MockEntitlementService implements EntitlementService, EntitlementViewSource {

    static volatile boolean accessGranted = true;
    static volatile long cap = 10;
    /** {@code true} = core irraggiungibile: ogni accesso alla rete di sicurezza fallisce. */
    static volatile boolean unreachable = false;
    /** Accessi alla rete di sicurezza dall'ultimo {@link #reset()}. */
    static final AtomicInteger calls = new AtomicInteger();

    static void reset() {
        accessGranted = true;
        cap = 10;
        unreachable = false;
        calls.set(0);
    }

    private void record() {
        calls.incrementAndGet();
        if (unreachable) {
            throw new IllegalStateException("core irraggiungibile (simulato)");
        }
    }

    @Override
    public boolean hasAccess(String appSlug) {
        record();
        return accessGranted;
    }

    @Override
    public long capFor(String appSlug, String metric) {
        record();
        return cap;
    }

    @Override
    public QuotaNature natureOf(String appSlug, String metric) {
        record();
        return QuotaNature.FLOW;
    }

    @Override
    public Optional<EntitlementView> viewFor(String appSlug) {
        record();
        if (!accessGranted) {
            return Optional.empty();
        }
        return Optional.of(new EntitlementView(
                appSlug, "free", null, null, Map.of("fatture", new MetricLimit(cap, "flow", "month"))));
    }
}
