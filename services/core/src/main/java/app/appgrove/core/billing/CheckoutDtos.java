package app.appgrove.core.billing;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO del checkout (UC 0024). Catalogo lato cliente per la scelta tier, comando di avvio checkout
 * (server-initiated), token per l'overlay e stato minimale per il polling post-checkout.
 */
public final class CheckoutDtos {

    private CheckoutDtos() {}

    /** Catalogo di un'app per la scelta d'acquisto: anagrafica app + tier con prezzi per ciclo. */
    public record AppTiersView(UUID appId, String slug, String name, List<TierView> tiers) {}

    /** Un tier acquistabile: chiave/nome, limiti/feature (dal nostro DB), giorni di trial, prezzi per ciclo. */
    public record TierView(
            UUID tierId,
            String key,
            String name,
            Map<String, Object> limits,
            Map<String, Object> features,
            int trialDays,
            List<PriceView> prices) {}

    /** Prezzo di un tier per ciclo: importo in <b>minor units</b> + valuta (no ID Paddle lato client). */
    public record PriceView(String billingCycle, int amount, String currency) {}

    /**
     * Richiesta di avvio checkout: si indica il tier per {@code tierKey} <i>oppure</i> {@code appTierId}, più
     * il {@code billingCycle}. Il {@code tenant_id} <b>non</b> è nel body (viene dal JWT, invariante #1).
     */
    public record StartCheckoutRequest(String tierKey, UUID appTierId, @NotBlank String billingCycle) {}

    /** Esito: il solo {@code checkoutToken} per aprire l'overlay (gli ID Paddle restano server-side). */
    public record CheckoutTokenView(String checkoutToken) {}

    /** Stato minimale per il polling post-checkout: {@code status} corrente e {@code active} (mappa #09 E29). */
    public record SubscriptionStatusView(String status, boolean active) {}
}
