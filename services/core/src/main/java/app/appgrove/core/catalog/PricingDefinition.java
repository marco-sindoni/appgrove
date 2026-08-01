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

    /**
     * Una app del catalogo con i suoi tier. La chiave stabile dell'app è lo {@code slug}.
     *
     * <p>{@code category} e {@code descriptions} sono la <b>parte descrittiva</b> del listino (UC 0095):
     * la tinta/icona di categoria della vetrina e la descrizione breve nelle 5 lingue. Vivono qui perché
     * il catalogo è codice — devono esistere anche per un'app che non ha (ancora) un modulo impacchettato
     * nel frontend. Sono <b>facoltative</b>: un'app che non le dichiara resta presentabile (nome + tinta
     * derivata dallo slug), e non si sincronizzano nel database del catalogo (decisione 4 della change
     * 0076: contenuto di presentazione env-agnostico, non dato transazionale).
     */
    public record AppDef(
            String slug,
            String name,
            AppUserModel userModel,
            AppStatus status,
            List<TierDef> tiers,
            String category,
            Map<String, String> descriptions) {

        /** Definizione senza parte descrittiva (cataloghi sintetici dei test). */
        public AppDef(String slug, String name, AppUserModel userModel, AppStatus status, List<TierDef> tiers) {
            this(slug, name, userModel, status, tiers, null, null);
        }
    }

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
