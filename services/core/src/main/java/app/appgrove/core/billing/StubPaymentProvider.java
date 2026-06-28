package app.appgrove.core.billing;

import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

/**
 * Provider di pagamento finto per dev/test (#09 I39): "API Paddle finta" che ritorna ID plausibili,
 * senza alcuna rete esterna né account Paddle. Attivo quando {@code appgrove.payments.provider=stub}
 * (default in dev/test). I webhook sintetici firmati che muovono la {@code subscription} sono prodotti
 * dallo {@code StubScenarioEmitter}.
 */
@ApplicationScoped
@IfBuildProperty(name = "appgrove.payments.provider", stringValue = "stub", enableIfMissing = true)
public class StubPaymentProvider implements PaymentProvider {

    @Override
    public CheckoutInit startCheckout(StartCheckoutCommand command) {
        return new CheckoutInit(
                "chk_" + token(),
                "ctm_" + token(),
                "txn_" + token(),
                "sub_" + token());
    }

    /** ID plausibile e opaco (stile Paddle), deterministicamente unico. */
    private static String token() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }
}
