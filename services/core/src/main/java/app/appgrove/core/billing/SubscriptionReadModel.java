package app.appgrove.core.billing;

import app.appgrove.commons.entitlement.MetricLimit;
import app.appgrove.core.billing.SubscriptionDtos.MySubscriptionsView;
import app.appgrove.core.billing.SubscriptionDtos.SubscriptionView;
import app.appgrove.core.catalog.App;
import app.appgrove.core.catalog.AppRepository;
import app.appgrove.core.catalog.AppTier;
import app.appgrove.core.catalog.AppTierRepository;
import app.appgrove.core.platform.Account;
import app.appgrove.core.platform.AccountRepository;
import app.appgrove.core.platform.CallerContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Read-model dedicato del portale cliente (UC 0028): elenca <b>tutte</b> le subscription del tenant corrente
 * — incluse le non-attive (canceled/paused) — con il dettaglio lifecycle completo (fase, cambio schedulato)
 * e i flag azionabili. Distinto da {@link EntitlementReadModel} (che è il read-model del <b>gate</b> e
 * restituisce solo le app con accesso): qui servono anche le subscription scadute per offrire riattiva +
 * diritti GDPR (UC 0028 §5).
 *
 * <p>Invarianti: le subscription sono lette tenant-scoped (discriminator, #1/#2); il catalogo
 * ({@link App}/{@link AppTier}) è platform-level. Riusa {@link SubscriptionLifecycle} per la fase.
 */
@ApplicationScoped
public class SubscriptionReadModel {

    @Inject
    SubscriptionRepository subscriptions;

    @Inject
    AppRepository apps;

    @Inject
    AppTierRepository tiers;

    @Inject
    AccountRepository accounts;

    @Inject
    CallerContext caller;

    /** Tutte le subscription del tenant del JWT, con fase/cambio schedulato/limiti/flag. */
    public MySubscriptionsView forCurrentTenant() {
        Account account = accounts.findById(caller.tenantId());
        boolean portalAvailable = account != null && account.getPaddleCustomerId() != null;

        List<SubscriptionView> views = new ArrayList<>();
        for (Subscription sub : subscriptions.listAll()) {
            App app = apps.findById(sub.getAppId());
            if (app == null) {
                continue; // subscription orfana (app rimossa dal catalogo) → non mostrata
            }
            AppTier tier = sub.getAppTierId() != null ? tiers.findById(sub.getAppTierId()) : null;
            AppTier scheduledTier =
                    sub.getScheduledTierId() != null ? tiers.findById(sub.getScheduledTierId()) : null;
            SubscriptionLifecycle lifecycle = SubscriptionLifecycle.of(sub);
            SubscriptionLifecycle.Phase phase = lifecycle.phase();

            boolean changeable = phase == SubscriptionLifecycle.Phase.TRIAL
                    || phase == SubscriptionLifecycle.Phase.ACTIVE
                    || phase == SubscriptionLifecycle.Phase.CANCELING;
            boolean canCancel = (phase == SubscriptionLifecycle.Phase.TRIAL
                            || phase == SubscriptionLifecycle.Phase.ACTIVE)
                    && sub.getCancelAt() == null;
            boolean canResume = phase == SubscriptionLifecycle.Phase.CANCELING;
            boolean canReactivate = phase == SubscriptionLifecycle.Phase.ENDED;

            views.add(new SubscriptionView(
                    app.getSlug(),
                    app.getName(),
                    sub.getStatus().name(),
                    tier != null ? tier.getKey() : null,
                    tier != null ? tier.getName() : null,
                    sub.getCurrentPeriodStart(),
                    sub.getCurrentPeriodEnd(),
                    sub.getCancelAt(),
                    sub.getTrialEnd(),
                    scheduledTier != null ? scheduledTier.getKey() : null,
                    sub.getScheduledChangeAt(),
                    phase.name(),
                    limitsOf(tier),
                    changeable,
                    changeable,
                    canCancel,
                    canResume,
                    canReactivate,
                    portalAvailable));
        }
        return new MySubscriptionsView(views);
    }

    /** Converte {@code app_tier.limits} ({@code {metric, cap, type, window}}) in {@code metric → MetricLimit}. */
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
