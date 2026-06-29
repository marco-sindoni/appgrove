package app.appgrove.commons.entitlement;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

/**
 * Entitlement effettivo di un tenant su <b>una</b> app (UC 0027). Compare nella risposta di
 * {@code GET /api/platform/v1/me/entitlements} solo per le app a cui il tenant <b>ha accesso</b>
 * (gate 2 "app abilitata" + gate 3 "entitled", #09 dec.30): la presenza in lista <b>è</b> l'accesso.
 *
 * <p>La chiave è lo <b>slug</b> dell'app (riconciliato da {@code subscription.app_id} UUID → {@code app.slug}):
 * è la chiave usata dal registry del frontend e dal gate lato servizio.
 *
 * @param appSlug slug stabile dell'app (chiave di entitlement)
 * @param tierKey tier effettivo (da subscription a pagamento, o tier free di baseline)
 * @param phase fase di lifecycle (TRIAL/ACTIVE/CANCELING/GRACE) per una subscription; {@code null} per
 *     la baseline free (nessuna subscription)
 * @param accessUntil fine accesso nota (cutoff/period end), {@code null} se non applicabile
 * @param limits tetti per metrica: {@code metric → {cap, nature, window}} (da {@code app_tier.limits})
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EntitlementView(
        String appSlug, String tierKey, String phase, Instant accessUntil, Map<String, MetricLimit> limits) {}
