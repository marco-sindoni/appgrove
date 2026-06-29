package app.appgrove.core.billing;

import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Simulatore di attivazione <b>dev/test only</b> (gate {@code appgrove.payments.provider=stub}, attivo in
 * {@code %dev} e nei test): osserva {@link CheckoutStarted} ed emette — via la <b>stessa</b> pipeline webhook
 * firmata ({@link StubScenarioEmitter}, scenario {@code happy_path}) — gli eventi che Paddle invierebbe dopo
 * un pagamento riuscito. Così l'attivazione passa <b>sempre</b> dal webhook (invariante #09 C16); lo stub si
 * limita a recitare il ruolo di Paddle "che manda il webhook".
 *
 * <p>In prod questo bean non esiste (provider {@code paddle}) → nessun osservatore → {@code CheckoutResource}
 * non auto-attiva. I webhook emessi finiscono in coda: il consumer li drena (scheduler in {@code %dev};
 * {@code drain()} esplicito nei test), quindi il {@code POST /checkout} di per sé <b>non</b> attiva nulla.
 */
@ApplicationScoped
@IfBuildProperty(name = "appgrove.payments.provider", stringValue = "stub", enableIfMissing = true)
public class StubCheckoutActivation {

    private static final Logger LOG = Logger.getLogger(StubCheckoutActivation.class);

    @Inject
    StubScenarioEmitter emitter;

    void onCheckoutStarted(@Observes CheckoutStarted event) {
        LOG.infof(
                "stub.checkout.activation tenant_id=%s app_id=%s app_tier_id=%s → emit happy_path",
                event.tenantId(), event.appId(), event.appTierId());
        // happy_path: subscription.created(trialing) → subscription.activated(active), firmati e accodati.
        emitter.emit(
                LifecycleScenario.happy_path, event.tenantId(), event.appId(), event.appTierId(), null);
    }
}
