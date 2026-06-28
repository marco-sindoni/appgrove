package app.appgrove.core.billing;

import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Provider di pagamento reale verso Paddle (test/prod). Attivo quando
 * {@code appgrove.payments.provider=paddle} (default in {@code %prod}).
 *
 * <p><b>Fuori scope UC 0023</b>: l'integrazione reale è bloccata da #14 (nessun account Paddle senza
 * sito + ToU/PP, sandbox incluso). L'implementazione concreta dei metodi sarà riempita per-metodo dagli
 * UC consumatori: {@code startCheckout} → UC 0024, customer portal → UC 0028, sync pricing → UC 0022
 * (vedi "Punti aperti" di UC 0023). Finché non implementato, fallisce esplicitamente.
 */
@ApplicationScoped
@IfBuildProperty(name = "appgrove.payments.provider", stringValue = "paddle")
public class PaddlePaymentProvider implements PaymentProvider {

    @Override
    public CheckoutInit startCheckout(StartCheckoutCommand command) {
        throw new UnsupportedOperationException(
                "PaddlePaymentProvider reale non implementato (fuori scope UC 0023, gated #14): "
                        + "startCheckout è di UC 0024");
    }
}
