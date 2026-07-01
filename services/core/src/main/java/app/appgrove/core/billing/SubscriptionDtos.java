package app.appgrove.core.billing;

import app.appgrove.commons.entitlement.MetricLimit;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTO del portale cliente self-service (UC 0028). Read-model dedicato {@code /me/subscriptions} — elenca
 * <b>tutte</b> le subscription del tenant (anche non-attive), distinto da {@code /me/entitlements} (gate);
 * comandi di cambio piano; esito sessione portal.
 */
public final class SubscriptionDtos {

    private SubscriptionDtos() {}

    /** Lista degli abbonamenti del tenant (owner-facing). */
    public record MySubscriptionsView(List<SubscriptionView> subscriptions) {}

    /**
     * Un abbonamento del tenant su un'app, con dettaglio lifecycle completo e flag azionabili. A differenza
     * dell'entitlement view, include le subscription <b>senza accesso</b> (canceled/paused) per offrire
     * riattiva + diritti GDPR (UC 0028 §5).
     *
     * @param phase fase lifecycle (TRIAL/ACTIVE/CANCELING/GRACE/ENDED) da {@link SubscriptionLifecycle}
     * @param scheduledTierKey tier di destinazione del downgrade schedulato, o {@code null}
     * @param limits tetti del tier corrente ({@code metric → {cap, nature, window}})
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SubscriptionView(
            String appSlug,
            String appName,
            String status,
            String tierKey,
            String tierName,
            Instant currentPeriodStart,
            Instant currentPeriodEnd,
            Instant cancelAt,
            Instant trialEnd,
            String scheduledTierKey,
            Instant scheduledChangeAt,
            String phase,
            Map<String, MetricLimit> limits,
            boolean canUpgrade,
            boolean canDowngrade,
            boolean canCancel,
            boolean canResume,
            boolean canReactivate,
            boolean portalAvailable) {}

    /**
     * Richiesta di cambio piano: tier di destinazione (chiave interna) + ciclo. Il {@code tenant_id}
     * <b>non</b> è nel body (dal JWT, invariante #1). Upgrade = immediato, downgrade = schedulato (deciso
     * server-side da {@link TierChangePolicy}, non dal client).
     */
    public record ChangeTierRequest(@NotBlank String targetTierKey, @NotBlank String billingCycle) {}

    /** Esito del cambio piano: direzione applicata e (per downgrade) l'istante di efficacia. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ChangeTierResult(String direction, Instant effectiveAt) {}

    /** Esito della generazione sessione portal: URL da aprire lato client. */
    public record PortalSessionView(String url) {}
}
