package app.appgrove.core.billing;

import app.appgrove.commons.entitlement.EntitlementView;
import app.appgrove.commons.entitlement.MeEntitlementsView;
import app.appgrove.commons.entitlement.MetricLimit;
import app.appgrove.core.catalog.App;
import app.appgrove.core.catalog.AppPriceRepository;
import app.appgrove.core.catalog.AppRepository;
import app.appgrove.core.catalog.AppStatus;
import app.appgrove.core.catalog.AppTier;
import app.appgrove.core.catalog.AppTierRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Deriva il read-model degli entitlement del <b>tenant corrente</b> (UC 0027). È la fonte unica
 * consumata sia dal frontend (registry) sia dalle app (gate 402 + cap quota via {@code EntitlementService}).
 *
 * <p>Regola di accesso (#09 dec.30): {@code access = app.status==active && (subscription.grantsAccess()
 * ‖ baseline free tier)}. La subscription a pagamento <b>sovrascrive</b> la baseline; in assenza di
 * subscription l'entitlement effettivo è il <b>tier free</b> dell'app (tier senza prezzo). L'accesso e
 * la fase di lifecycle riusano i building block esistenti ({@link SubscriptionStatus#grantsAccess()},
 * {@link SubscriptionLifecycle}) — questo modello li <b>consuma</b>, non li ri-deriva.
 *
 * <p>Invarianti: le subscription sono lette tenant-scoped (discriminator, #1/#2); il catalogo
 * ({@link App}/{@link AppTier}) è platform-level. La view espone lo <b>slug</b> (riconciliazione
 * {@code subscription.app_id} UUID → {@code app.slug}).
 */
@ApplicationScoped
public class EntitlementReadModel {

    @Inject
    SubscriptionRepository subscriptions;

    @Inject
    AppRepository apps;

    @Inject
    AppTierRepository tiers;

    @Inject
    AppPriceRepository prices;

    /** Entitlement effettivi del tenant del JWT (solo le app a cui ha accesso). */
    public MeEntitlementsView forCurrentTenant() {
        Map<UUID, Subscription> byApp = new HashMap<>();
        for (Subscription s : subscriptions.listAll()) {
            byApp.put(s.getAppId(), s);
        }

        List<EntitlementView> entitled = new ArrayList<>();
        for (App app : apps.listAll()) {
            if (app.getStatus() != AppStatus.active) {
                continue; // gate 2: app abilitata
            }
            Subscription sub = byApp.get(app.getId());

            AppTier tier;
            String phase;
            Instant accessUntil;
            if (sub != null) {
                if (!sub.getStatus().grantsAccess()) {
                    continue; // gate 3: subscription presente ma non concede accesso (canceled/paused)
                }
                SubscriptionLifecycle lifecycle = SubscriptionLifecycle.of(sub);
                phase = lifecycle.phase().name();
                accessUntil = lifecycle.accessUntil();
                tier = sub.getAppTierId() != null ? tiers.findById(sub.getAppTierId()) : null;
            } else {
                Optional<AppTier> free = freeTier(app.getId());
                if (free.isEmpty()) {
                    continue; // nessuna subscription e nessun tier free → niente accesso
                }
                tier = free.get();
                phase = null; // baseline free (nessuna subscription)
                accessUntil = null;
            }

            entitled.add(new EntitlementView(
                    app.getSlug(),
                    tier != null ? tier.getKey() : null,
                    phase,
                    accessUntil,
                    limitsOf(tier)));
        }
        return new MeEntitlementsView(entitled);
    }

    /** Tier free di baseline: il primo tier dell'app <b>senza alcun prezzo</b> (freemium). */
    private Optional<AppTier> freeTier(UUID appId) {
        return tiers.listByApp(appId).stream()
                .filter(t -> prices.listByTier(t.getId()).isEmpty())
                .findFirst();
    }

    /**
     * Converte {@code app_tier.limits} (descrittore single-metrica {@code {metric, cap, type, window}})
     * nella mappa {@code metric → {cap, nature, window}} della view. Forma forward-compatible: assenza
     * della chiave {@code metric} → nessun limite.
     */
    private Map<String, MetricLimit> limitsOf(AppTier tier) {
        if (tier == null || tier.getLimits() == null) {
            return Map.of();
        }
        Map<String, Object> raw = tier.getLimits();
        Object metric = raw.get("metric");
        if (metric == null) {
            return Map.of();
        }
        long cap = raw.get("cap") instanceof Number n ? n.longValue() : -1L;
        String nature = raw.get("type") != null ? raw.get("type").toString() : null;
        String window = raw.get("window") != null ? raw.get("window").toString() : null;
        return Map.of(metric.toString(), new MetricLimit(cap, nature, window));
    }
}
