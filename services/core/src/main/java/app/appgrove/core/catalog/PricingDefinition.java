package app.appgrove.core.catalog;

import java.util.List;
import java.util.Map;

/**
 * Modello del <b>pricing-as-code</b> (UC 0022): la definizione versionata del "cosa si vende", letta dagli
 * YAML in {@code resources/pricing/}. Fonte di verità dei campi <b>env-agnostici</b> (slug/tier/limiti/
 * feature/ciclo/prezzi); gli ID Paddle (per-ambiente) NON stanno qui — li riempie la sync nel DB (#09 H37).
 */
public final class PricingDefinition {

    private PricingDefinition() {}

    /** Una app del catalogo con i suoi tier. La chiave stabile dell'app è lo {@code slug}. */
    public record AppDef(
            String slug, String name, AppUserModel userModel, AppStatus status, List<TierDef> tiers) {}

    /** Un tier: chiave interna stabile {@code key}, limiti/feature (JSON), eventuali price. */
    public record TierDef(
            String key,
            String name,
            int trialDays,
            Map<String, Object> limits,
            Map<String, Object> features,
            List<PriceDef> prices) {}

    /** Un price: (ciclo) + importo in minor units + valuta. Prezzo uguale in ogni ambiente. */
    public record PriceDef(BillingCycle billingCycle, long amount, String currency) {}
}
