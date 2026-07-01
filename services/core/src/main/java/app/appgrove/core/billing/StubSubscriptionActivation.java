package app.appgrove.core.billing;

import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Simulatore <b>dev/test only</b> delle mutazioni self-service (gate {@code appgrove.payments.provider=stub}):
 * osserva {@link SubscriptionChangeRequested} ed emette — via la <b>stessa</b> pipeline webhook firmata
 * ({@link StubScenarioEmitter}) — il {@code subscription.updated} che Paddle invierebbe dopo un cambio piano,
 * una disdetta o una riattivazione. Così l'effetto passa <b>sempre</b> dal webhook (invariante #09 C16).
 *
 * <p>In prod questo bean non esiste (provider {@code paddle}) → nessun osservatore → il resource non muta
 * nulla da sé: l'effetto arriva dal webhook reale di Paddle. Speculare a {@link StubCheckoutActivation}.
 */
@ApplicationScoped
@IfBuildProperty(name = "appgrove.payments.provider", stringValue = "stub", enableIfMissing = true)
public class StubSubscriptionActivation {

    private static final Logger LOG = Logger.getLogger(StubSubscriptionActivation.class);

    @Inject
    StubScenarioEmitter emitter;

    void onSubscriptionChangeRequested(@Observes SubscriptionChangeRequested event) {
        LOG.infof(
                "stub.subscription.change tenant_id=%s app_id=%s result_tier=%s scheduled_tier=%s cancel_at=%s → emit subscription.updated",
                event.tenantId(), event.appId(), event.resultTierId(),
                event.scheduledTierId(), event.cancelAt());
        emitter.emitSubscriptionUpdate(
                event.tenantId(), event.appId(), event.resultTierId(), event.paddleSubscriptionId(),
                event.currentPeriodStart(), event.currentPeriodEnd(), event.cancelAt(),
                event.scheduledTierId(), event.scheduledChangeAt(), event.afterOccurredAt());
    }
}
