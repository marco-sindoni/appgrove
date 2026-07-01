package app.appgrove.core.billing;

import app.appgrove.commons.entitlement.MetricLimit;
import app.appgrove.commons.web.ProblemDetail;
import app.appgrove.core.billing.SubscriptionDtos.ChangeTierRequest;
import app.appgrove.core.billing.SubscriptionDtos.ChangeTierResult;
import app.appgrove.core.billing.SubscriptionDtos.MySubscriptionsView;
import app.appgrove.core.catalog.App;
import app.appgrove.core.catalog.AppPrice;
import app.appgrove.core.catalog.AppPriceRepository;
import app.appgrove.core.catalog.AppRepository;
import app.appgrove.core.catalog.AppTier;
import app.appgrove.core.catalog.AppTierRepository;
import app.appgrove.core.catalog.BillingCycle;
import app.appgrove.core.platform.CallerContext;
import app.appgrove.core.platform.Roles;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * Portale cliente self-service (UC 0028): lettura degli abbonamenti del tenant e azioni di gestione
 * (upgrade/downgrade/disdici/riattiva) sulla <b>nostra</b> {@code subscription}. Modello
 * <b>command → provider → webhook → read-model</b>: le mutazioni <b>non</b> scrivono la subscription; chiamano
 * il {@link PaymentProvider} e fanno emettere il webhook (in dev dallo {@link StubSubscriptionActivation}),
 * così la fonte di verità resta la pipeline (invariante #09 C16).
 *
 * <p>Invarianti: tenant dal JWT ({@link CallerContext}, #1); letture/scritture tenant-scoped (#2); le
 * mutazioni sono OWNER-only (billing, UC 0028 §8); log strutturati {@code tenant_id}/{@code app_id}/{@code user_id}.
 */
@Path("/api/platform/v1/me/subscriptions")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class SubscriptionResource {

    private static final Logger LOG = Logger.getLogger(SubscriptionResource.class);

    @Inject
    CallerContext caller;

    @Inject
    AppRepository apps;

    @Inject
    AppTierRepository tiers;

    @Inject
    AppPriceRepository prices;

    @Inject
    SubscriptionRepository subscriptions;

    @Inject
    SubscriptionReadModel readModel;

    @Inject
    PaymentProvider provider;

    @Inject
    Event<SubscriptionChangeRequested> changeRequested;

    /** Elenco degli abbonamenti del tenant (anche non-attivi), per il pannello self-service. */
    @GET
    public MySubscriptionsView mySubscriptions() {
        MySubscriptionsView view = readModel.forCurrentTenant();
        LOG.debugf("subscriptions self-service: %d abbonamenti", view.subscriptions().size());
        return view;
    }

    /** Upgrade (immediato) o downgrade (schedulato a fine periodo) del piano. OWNER-only. */
    @POST
    @Path("/{appSlug}/change-tier")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed(Roles.OWNER)
    @Transactional
    public ChangeTierResult changeTier(
            @PathParam("appSlug") String appSlug, @Valid ChangeTierRequest body) {
        UUID appId = resolveApp(appSlug).getId();
        Subscription sub = requireActiveSubscription(appId);
        BillingCycle cycle = parseCycle(body.billingCycle());

        AppTier targetTier = tiers.findByAppAndKey(appId, body.targetTierKey())
                .orElseThrow(() -> new NotFoundException("Tier non trovato: " + body.targetTierKey()));
        if (targetTier.getId().equals(sub.getAppTierId())) {
            throw new BadRequestException("Già su questo piano");
        }

        int currentAmount = amountFor(sub.getAppTierId(), cycle);
        int targetAmount = amountFor(targetTier.getId(), cycle);
        TierChangePolicy.Direction direction = TierChangePolicy.direction(currentAmount, targetAmount);

        boolean immediate = direction != TierChangePolicy.Direction.DOWNGRADE;
        UUID resultTierId;
        UUID scheduledTierId;
        Instant scheduledChangeAt;
        Instant effectiveAt;
        if (immediate) {
            // upgrade / stesso prezzo: effetto subito, azzera un eventuale downgrade già programmato.
            resultTierId = targetTier.getId();
            scheduledTierId = null;
            scheduledChangeAt = null;
            effectiveAt = null;
        } else {
            // downgrade: gate stock (E23) + scheduling a fine periodo (resta sul tier corrente fin lì).
            TierChangePolicy.Decision decision =
                    TierChangePolicy.evaluateDowngrade(targetLimits(targetTier), currentUsage());
            if (decision.blocked()) {
                throw blocked(decision.remediation());
            }
            resultTierId = sub.getAppTierId();
            scheduledTierId = targetTier.getId();
            scheduledChangeAt = sub.getCurrentPeriodEnd();
            effectiveAt = sub.getCurrentPeriodEnd();
        }

        AppPrice targetPrice = priceFor(targetTier.getId(), cycle);
        provider.changeSubscriptionTier(new PaymentProvider.SubscriptionTierChange(
                caller.tenantId().toString(), appId, sub.getPaddleSubscriptionId(),
                targetPrice != null ? targetPrice.getPaddlePriceId() : null, immediate));

        LOG.infof("subscription.change-tier app_id=%s target=%s direction=%s", appId, targetTier.getKey(), direction);
        fire(sub, appId, resultTierId, sub.getCancelAt(), scheduledTierId, scheduledChangeAt);
        return new ChangeTierResult(direction.name(), effectiveAt);
    }

    /** Disdetta a fine periodo (imposta {@code cancel_at}). OWNER-only. */
    @POST
    @Path("/{appSlug}/cancel")
    @RolesAllowed(Roles.OWNER)
    @Transactional
    public ChangeTierResult cancel(@PathParam("appSlug") String appSlug) {
        UUID appId = resolveApp(appSlug).getId();
        Subscription sub = requireActiveSubscription(appId);
        if (sub.getCancelAt() != null) {
            throw new ClientErrorException("Disdetta già programmata", Response.Status.CONFLICT);
        }
        provider.cancelSubscription(new PaymentProvider.SubscriptionRef(
                caller.tenantId().toString(), appId, sub.getPaddleSubscriptionId()));
        LOG.infof("subscription.cancel app_id=%s cancel_at=%s", appId, sub.getCurrentPeriodEnd());
        // disdetta: resta sul tier corrente fino a fine periodo, cancel_at valorizzato, schedulazione azzerata.
        fire(sub, appId, sub.getAppTierId(), sub.getCurrentPeriodEnd(), null, null);
        return new ChangeTierResult("CANCEL", sub.getCurrentPeriodEnd());
    }

    /** Annulla una disdetta programmata (riattiva prima della scadenza). OWNER-only. */
    @POST
    @Path("/{appSlug}/resume")
    @RolesAllowed(Roles.OWNER)
    @Transactional
    public ChangeTierResult resume(@PathParam("appSlug") String appSlug) {
        UUID appId = resolveApp(appSlug).getId();
        Subscription sub = requireActiveSubscription(appId);
        if (sub.getCancelAt() == null) {
            throw new ClientErrorException("Nessuna disdetta da annullare", Response.Status.CONFLICT);
        }
        provider.resumeSubscription(new PaymentProvider.SubscriptionRef(
                caller.tenantId().toString(), appId, sub.getPaddleSubscriptionId()));
        LOG.infof("subscription.resume app_id=%s", appId);
        // riattiva: azzera cancel_at, preserva un eventuale downgrade schedulato.
        fire(sub, appId, sub.getAppTierId(), null, sub.getScheduledTierId(), sub.getScheduledChangeAt());
        return new ChangeTierResult("RESUME", null);
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private App resolveApp(String appSlug) {
        return apps.findBySlug(appSlug)
                .orElseThrow(() -> new NotFoundException("App non trovata: " + appSlug));
    }

    /** Subscription del tenant per l'app, che deve concedere accesso (altrimenti nessuna azione di cambio). */
    private Subscription requireActiveSubscription(UUID appId) {
        Subscription sub = subscriptions.findByApp(appId)
                .orElseThrow(() -> new NotFoundException("Nessun abbonamento per l'app"));
        if (!sub.getStatus().grantsAccess()) {
            throw new ClientErrorException(
                    "Abbonamento non attivo: riattiva o completa un nuovo acquisto", Response.Status.CONFLICT);
        }
        return sub;
    }

    private void fire(
            Subscription sub, UUID appId, UUID resultTierId, Instant cancelAt,
            UUID scheduledTierId, Instant scheduledChangeAt) {
        changeRequested.fire(new SubscriptionChangeRequested(
                caller.tenantId().toString(), appId, sub.getPaddleSubscriptionId(),
                resultTierId, sub.getCurrentPeriodStart(), sub.getCurrentPeriodEnd(),
                cancelAt, scheduledTierId, scheduledChangeAt, sub.getLastEventOccurredAt()));
    }

    private int amountFor(UUID tierId, BillingCycle cycle) {
        if (tierId == null) {
            return 0; // tier free (nessun prezzo)
        }
        AppPrice price = priceFor(tierId, cycle);
        return price != null ? price.getAmount() : 0;
    }

    /** Price del tier per il ciclo richiesto, con fallback al primo price disponibile; null se free. */
    private AppPrice priceFor(UUID tierId, BillingCycle cycle) {
        var all = prices.listByTier(tierId);
        return all.stream()
                .filter(p -> p.getBillingCycle() == cycle)
                .findFirst()
                .orElseGet(() -> all.isEmpty() ? null : all.get(0));
    }

    private Map<String, MetricLimit> targetLimits(AppTier tier) {
        if (tier.getLimits() == null || tier.getLimits().get("metric") == null) {
            return Map.of();
        }
        Map<String, Object> raw = tier.getLimits();
        long cap = raw.get("cap") instanceof Number n ? n.longValue() : -1L;
        String nature = raw.get("type") != null ? raw.get("type").toString() : null;
        String window = raw.get("window") != null ? raw.get("window").toString() : null;
        return Map.of(raw.get("metric").toString(), new MetricLimit(cap, nature, window));
    }

    /**
     * Uso corrente per metrica: <b>vuoto</b> per ora — la sorgente usage per-app è applicativa e non ancora
     * leggibile da core (UC 0028 §Punti aperti, differita). Finché non cablata, il gate stock non blocca a
     * runtime; la logica di blocco resta reale in {@link TierChangePolicy} e coperta da test.
     */
    private Map<String, Long> currentUsage() {
        return Map.of();
    }

    private BillingCycle parseCycle(String raw) {
        try {
            return BillingCycle.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("billingCycle non valido: " + raw);
        }
    }

    /** 409 problem+json con remediation azionabile (gate stock non superato, E23). */
    private ClientErrorException blocked(String remediation) {
        Response response = Response.status(Response.Status.CONFLICT)
                .type(ProblemDetail.MEDIA_TYPE)
                .entity(ProblemDetail.of(Response.Status.CONFLICT.getStatusCode(), "Downgrade bloccato", remediation))
                .build();
        return new ClientErrorException(response);
    }
}
