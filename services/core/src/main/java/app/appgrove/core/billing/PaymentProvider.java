package app.appgrove.core.billing;

import java.util.List;
import java.util.Map;
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

    /**
     * Comando di avvio checkout (UC 0024). Il {@code tenantId} arriva dal JWT verificato (invariante #1) e
     * finisce nei {@code custom_data} server-side; {@code paddlePriceId} è il price risolto da
     * {@code (app, tier, ciclo)}; {@code existingPaddleCustomerId} (nullable) abilita il <b>customer
     * lazy</b> (#09 C15): se l'account ne ha già uno lo si riusa, altrimenti il provider lo crea.
     */
    record StartCheckoutCommand(
            String tenantId,
            UUID appId,
            UUID appTierId,
            String paddlePriceId,
            String billingCycle,
            String customerEmail,
            String existingPaddleCustomerId) {}

    /** Esito: token per l'overlay + ID plausibili (customer/transaction/subscription). */
    record CheckoutInit(
            String checkoutToken,
            String paddleCustomerId,
            String paddleTransactionId,
            String paddleSubscriptionId) {}

    /**
     * Sincronizza il <b>catalogo pricing-as-code</b> verso il provider (UC 0022, #09 H37): per ogni
     * Product/Price <b>crea il mancante</b> e ritorna l'ID Paddle <b>per-ambiente</b>; per gli esistenti
     * (con ID già noto) lo riconferma. L'immutabilità (mai mutare l'importo di un price vivo), l'archiviazione
     * dei rimossi e il grandfathering sono applicati a monte dal {@code PricingSyncService}: qui il provider
     * si limita ad assegnare/riconfermare gli ID. Idempotente: stessa richiesta → stessi ID.
     */
    PricingSyncResult syncPricing(PricingSyncRequest request);

    /** Richiesta di sync: i Product desiderati con i loro Price (chiavi interne stabili + eventuale ID già noto). */
    record PricingSyncRequest(List<ProductSync> products) {}

    /** Un Product (= app): {@code productKey} = slug; {@code existingProductId} non-null se già su Paddle. */
    record ProductSync(
            String productKey, String productName, String existingProductId, List<PriceSync> prices) {}

    /** Un Price (= tier × ciclo): {@code priceKey} = {@code slug:tierKey:cycle}; importo in minor units. */
    record PriceSync(
            String priceKey,
            long amountMinorUnits,
            String currency,
            String billingCycle,
            String existingPriceId) {}

    /** Esito: chiave interna stabile → ID Paddle dell'ambiente, per Product e per Price. */
    record PricingSyncResult(Map<String, String> productIdByKey, Map<String, String> priceIdByKey) {}
}
