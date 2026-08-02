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
     * @param appDisabled l'app è stata <b>sospesa dalla piattaforma</b> (UC 0076: {@code app.status}
     *     diverso da {@code active}). L'abbonamento resta elencato e valido e i dati restano intatti,
     *     ma l'app non è raggiungibile: senza questo flag il pannello mostrerebbe "attivo" per un'app
     *     che è sparita dalla barra laterale, che è la incoerenza annotata nello use case 0076
     * @param usage uso corrente <b>a giacenza</b> ({@code metrica → valore}) riportato dall'app e
     *     proiettato in {@code platform.app_usage_stock} (UC 0054). Serve alla card per dire "8 su 10"
     *     invece del solo tetto del piano (UC 0067 §4.6). Vuoto se l'app non ha ancora riportato nulla,
     *     o se le sue metriche sono <b>a finestra</b>: di quelle core non conosce il consumo corrente
     * @param blockedTiers piani dell'app che la regola di riduzione ({@link TierChangePolicy})
     *     <b>rifiuterebbe adesso</b>, {@code chiave piano → spiegazione rimediale}. Esposti perché la
     *     finestra "cambia piano" possa disabilitarli spiegando il perché, invece di lasciarli cliccabili
     *     e farli fallire con un 409: la regola resta in un solo posto e i due lati non divergono
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
            boolean portalAvailable,
            boolean appDisabled,
            Map<String, Long> usage,
            Map<String, String> blockedTiers) {}

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
