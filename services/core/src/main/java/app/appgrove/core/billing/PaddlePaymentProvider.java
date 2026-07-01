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

    @Override
    public void changeSubscriptionTier(SubscriptionTierChange change) {
        throw new UnsupportedOperationException(
                "PaddlePaymentProvider.changeSubscriptionTier reale non implementato (gated #14): "
                        + "l'update subscription via API Paddle è tracciato nei Punti aperti di UC 0028");
    }

    @Override
    public void cancelSubscription(SubscriptionRef ref) {
        throw new UnsupportedOperationException(
                "PaddlePaymentProvider.cancelSubscription reale non implementato (gated #14, UC 0028)");
    }

    @Override
    public void resumeSubscription(SubscriptionRef ref) {
        throw new UnsupportedOperationException(
                "PaddlePaymentProvider.resumeSubscription reale non implementato (gated #14, UC 0028)");
    }

    @Override
    public CustomerPortalSession createCustomerPortalSession(String paddleCustomerId) {
        throw new UnsupportedOperationException(
                "PaddlePaymentProvider.createCustomerPortalSession reale non implementato (gated #14): "
                        + "la sessione Customer Portal via API Paddle è tracciata nei Punti aperti di UC 0028");
    }

    @Override
    public PricingSyncResult syncPricing(PricingSyncRequest request) {
        // UC 0022, slice offline: il motore di sync è esercitato contro lo stub. L'integrazione REALE
        // (REST Product/Price API di Paddle + secret per-ambiente da Secrets Manager, #09 I38) è BLOCCATA
        // da #14 (nessun account Paddle, sandbox incluso) → tracciata nei "Punti aperti" di UC 0022.
        throw new UnsupportedOperationException(
                "PaddlePaymentProvider.syncPricing reale non implementato (gated #14): "
                        + "il client Paddle è tracciato nei Punti aperti di UC 0022");
    }
}
