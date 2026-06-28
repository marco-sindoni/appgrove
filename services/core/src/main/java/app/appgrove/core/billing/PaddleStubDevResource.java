package app.appgrove.core.billing;

import app.appgrove.core.platform.CallerContext;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
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
import java.util.List;
import java.util.UUID;

/**
 * Endpoint <b>dev/test only</b> dello stub Paddle (gate {@code appgrove.payments.dev-endpoints=true},
 * attivo solo in {@code %dev} e nei test — assente in prod). Permette di:
 * <ul>
 *   <li>scatenare uno scenario lifecycle ({@code POST /scenarios/{scenario}}) → emette i webhook firmati;</li>
 *   <li>leggere la subscription del tenant corrente ({@code GET /subscriptions...}), per osservare l'esito
 *       dopo che il consumer ha drenato la coda.</li>
 * </ul>
 * Autenticato: il {@code tenant_id} arriva dal JWT verificato (invariante #1) e popola i
 * {@code custom_data} server-side dei webhook emessi; le letture sono tenant-scoped (discriminator).
 * In dev sostituisce — per il solo debug locale — checkout (UC 0024) e portal (UC 0028).
 */
@Path("/api/platform/v1/dev/paddle")
@IfBuildProperty(name = "appgrove.payments.dev-endpoints", stringValue = "true")
@Authenticated
public class PaddleStubDevResource {

    @Inject
    CallerContext caller;

    @Inject
    StubScenarioEmitter emitter;

    @Inject
    SubscriptionRepository subscriptions;

    public record EmitRequest(@NotNull UUID appId, UUID appTierId, UUID targetTierId) {}

    public record SubscriptionView(
            UUID appId,
            UUID appTierId,
            String status,
            Instant currentPeriodStart,
            Instant currentPeriodEnd,
            Instant cancelAt,
            Instant trialEnd,
            String paddleSubscriptionId) {
        static SubscriptionView from(Subscription s) {
            return new SubscriptionView(
                    s.getAppId(), s.getAppTierId(), s.getStatus().name(),
                    s.getCurrentPeriodStart(), s.getCurrentPeriodEnd(), s.getCancelAt(),
                    s.getTrialEnd(), s.getPaddleSubscriptionId());
        }
    }

    @POST
    @Path("/scenarios/{scenario}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response emit(@PathParam("scenario") LifecycleScenario scenario, EmitRequest request) {
        if (request == null || request.appId() == null) {
            throw new BadRequestException("appId richiesto");
        }
        try {
            List<StubScenarioEmitter.EmittedEvent> emitted = emitter.emit(
                    scenario, caller.tenantId().toString(),
                    request.appId(), request.appTierId(), request.targetTierId());
            return Response.accepted(emitted).build();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @GET
    @Path("/subscriptions")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SubscriptionView> mySubscriptions() {
        return subscriptions.listAll().stream().map(SubscriptionView::from).toList();
    }

    @GET
    @Path("/subscriptions/{appId}")
    @Produces(MediaType.APPLICATION_JSON)
    public SubscriptionView byApp(@PathParam("appId") UUID appId) {
        return subscriptions.findByApp(appId)
                .map(SubscriptionView::from)
                .orElseThrow(() -> new NotFoundException("Nessuna subscription per l'app " + appId));
    }
}
