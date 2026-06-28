package app.appgrove.core.billing;

import java.util.UUID;

/**
 * Port verso il provider di pagamento (Paddle), dietro la stessa interfaccia per dev e prod (#09 I39):
 * in {@code %dev}/test = {@link StubPaymentProvider} (deterministico, offline, nessun account Paddle);
 * in prod = {@code PaddlePaymentProvider} reale (placeholder finché non attivabile, gated #14).
 *
 * <p>Selezione per profilo via {@code appgrove.payments.provider} (build property). Il port nasce con
 * UC 0023 con il solo {@link #startCheckout} (la "API Paddle finta che ritorna ID plausibili");
 * gli altri metodi reali (customer portal, sync pricing) saranno aggiunti dai rispettivi UC
 * consumatori (0024/0028/0022) — vedi "Punti aperti" di UC 0023.
 */
public interface PaymentProvider {

    /**
     * Avvia un checkout lato server (#09 C14): il backend risolve il price e imposta i
     * {@code custom_data={tenant_id, app_id}} server-side (non manomettibili dal client), ritornando
     * un token + gli ID Paddle (finti nello stub).
     */
    CheckoutInit startCheckout(StartCheckoutCommand command);

    /** Comando di avvio checkout. Il {@code tenantId} arriva dal JWT verificato (invariante #1). */
    record StartCheckoutCommand(
            String tenantId, UUID appId, UUID appTierId, String billingCycle, String customerEmail) {}

    /** Esito: token per l'overlay + ID plausibili (customer/transaction/subscription). */
    record CheckoutInit(
            String checkoutToken,
            String paddleCustomerId,
            String paddleTransactionId,
            String paddleSubscriptionId) {}
}
