package app.appgrove.core.billing;

import app.appgrove.core.billing.CheckoutDtos.AppTiersView;
import app.appgrove.core.billing.CheckoutDtos.CheckoutTokenView;
import app.appgrove.core.billing.CheckoutDtos.PriceView;
import app.appgrove.core.billing.CheckoutDtos.StartCheckoutRequest;
import app.appgrove.core.billing.CheckoutDtos.SubscriptionStatusView;
import app.appgrove.core.billing.CheckoutDtos.TierView;
import app.appgrove.core.catalog.App;
import app.appgrove.core.catalog.AppPrice;
import app.appgrove.core.catalog.AppPriceRepository;
import app.appgrove.core.catalog.AppRepository;
import app.appgrove.core.catalog.AppTier;
import app.appgrove.core.catalog.AppTierRepository;
import app.appgrove.core.catalog.BillingCycle;
import app.appgrove.core.platform.Account;
import app.appgrove.core.platform.AccountRepository;
import app.appgrove.core.platform.CallerContext;
import app.appgrove.core.platform.Roles;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * Checkout d'acquisto di un'app (UC 0024): scelta tier (catalogo) → checkout <b>server-initiated</b> (token)
 * → polling stato. Tre endpoint, tutti autenticati:
 *
 * <ul>
 *   <li>{@code GET .../apps/{appId}/tiers} — catalogo lato cliente (tier + prezzi per ciclo);</li>
 *   <li>{@code POST .../apps/{appId}} (OWNER) — avvia il checkout: risolve il price, customer lazy,
 *       {@code custom_data={tenant_id, app_id}} <b>server-side</b> (invariante #1), ritorna il token. Non
 *       tocca {@code subscription}: l'attivazione è <b>solo</b> via webhook (#09 C16);</li>
 *   <li>{@code GET .../apps/{appId}/subscription} — stato minimale per il polling (tenant-scoped).</li>
 * </ul>
 *
 * <p>L'entitlement <b>completo</b> del tenant ({@code /me/entitlements}) è di UC 0027; qui si espone solo lo
 * stato per-app necessario alla UX post-checkout.
 */
@Path("/api/platform/v1/checkout")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CheckoutResource {

    private static final Logger LOG = Logger.getLogger(CheckoutResource.class);

    @Inject
    CallerContext caller;

    @Inject
    AppRepository apps;

    @Inject
    AppTierRepository tiers;

    @Inject
    AppPriceRepository prices;

    @Inject
    AccountRepository accounts;

    @Inject
    PaymentProvider provider;

    @Inject
    SubscriptionRepository subscriptions;

    @Inject
    Event<CheckoutStarted> checkoutStarted;

    /** Catalogo lato cliente: tier dell'app + prezzi per ciclo (read-only, contenuto uguale per tutti). */
    @GET
    @Path("/apps/{appSlug}/tiers")
    public AppTiersView tiers(@PathParam("appSlug") String appSlug) {
        App app = resolveApp(appSlug);
        UUID appId = app.getId();
        List<TierView> tierViews = tiers.listByApp(appId).stream().map(this::toTierView).toList();
        return new AppTiersView(appId, app.getSlug(), app.getName(), tierViews);
    }

    /** Avvia il checkout lato server e ritorna il token per l'overlay. OWNER-only. */
    @POST
    @Path("/apps/{appSlug}")
    @RolesAllowed(Roles.OWNER)
    @Transactional
    public CheckoutTokenView start(@PathParam("appSlug") String appSlug, @Valid StartCheckoutRequest body) {
        UUID tenantId = caller.tenantId(); // dal JWT verificato — MAI dal body (invariante #1)
        UUID appId = resolveApp(appSlug).getId();
        BillingCycle cycle = parseCycle(body.billingCycle());
        AppTier tier = resolveTier(appId, body);
        AppPrice price = prices.listByTier(tier.getId()).stream()
                .filter(p -> p.getBillingCycle() == cycle)
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "Nessun prezzo per tier " + tier.getKey() + " ciclo " + cycle));

        Account account = accounts.findById(tenantId);
        if (account == null) {
            throw new NotFoundException("Account non trovato");
        }
        String existingCustomerId = account.getPaddleCustomerId();

        PaymentProvider.CheckoutInit init = provider.startCheckout(new PaymentProvider.StartCheckoutCommand(
                tenantId.toString(),
                appId,
                tier.getId(),
                price.getPaddlePriceId(),
                cycle.name(),
                caller.email(),
                existingCustomerId));

        // Customer lazy (#09 C15): persiste l'id solo al primo acquisto (idempotente col webhook customer.*).
        if (existingCustomerId == null && init.paddleCustomerId() != null) {
            account.setPaddleCustomerId(init.paddleCustomerId());
        }

        // logging strutturato (invariante #4): tenant_id/user_id già in MDC (MdcRequestFilter); app_id nel messaggio.
        LOG.infof(
                "checkout.start app_id=%s app_tier_id=%s cycle=%s customer=%s",
                appId, tier.getId(), cycle, existingCustomerId == null ? "new" : "existing");

        // Evento di dominio: in prod nessun osservatore (attivazione solo via webhook reale); in dev/test
        // lo stub simula l'invio del webhook da parte di Paddle.
        checkoutStarted.fire(new CheckoutStarted(tenantId.toString(), appId, tier.getId(), cycle.name()));

        return new CheckoutTokenView(init.checkoutToken());
    }

    /** Stato minimale della subscription per (tenant, app) — per il solo polling post-checkout. */
    @GET
    @Path("/apps/{appSlug}/subscription")
    public SubscriptionStatusView subscription(@PathParam("appSlug") String appSlug) {
        UUID appId = resolveApp(appSlug).getId();
        return subscriptions.findByApp(appId)
                .map(s -> new SubscriptionStatusView(s.getStatus().name(), s.getStatus().grantsAccess()))
                .orElse(new SubscriptionStatusView(null, false));
    }

    // ── helper ───────────────────────────────────────────────────────────────

    /** Risolve l'app dal suo {@code slug} (chiave stabile lato cliente / registry FE) → 404 se assente. */
    private App resolveApp(String appSlug) {
        return apps.findBySlug(appSlug)
                .orElseThrow(() -> new NotFoundException("App non trovata: " + appSlug));
    }

    private AppTier resolveTier(UUID appId, StartCheckoutRequest body) {
        if (body.appTierId() != null) {
            AppTier tier = tiers.findById(body.appTierId());
            if (tier == null || !tier.getAppId().equals(appId)) {
                throw new NotFoundException("Tier non trovato per l'app: " + body.appTierId());
            }
            return tier;
        }
        if (body.tierKey() != null && !body.tierKey().isBlank()) {
            return tiers.findByAppAndKey(appId, body.tierKey())
                    .orElseThrow(() -> new NotFoundException("Tier non trovato: " + body.tierKey()));
        }
        throw new BadRequestException("Indicare tierKey o appTierId");
    }

    private BillingCycle parseCycle(String raw) {
        try {
            return BillingCycle.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("billingCycle non valido: " + raw);
        }
    }

    private TierView toTierView(AppTier tier) {
        List<PriceView> priceViews = prices.listByTier(tier.getId()).stream()
                .map(p -> new PriceView(p.getBillingCycle().name(), p.getAmount(), p.getCurrency()))
                .toList();
        return new TierView(
                tier.getId(), tier.getKey(), tier.getName(),
                tier.getLimits(), tier.getFeatures(), tier.getTrialDays(), priceViews);
    }
}
