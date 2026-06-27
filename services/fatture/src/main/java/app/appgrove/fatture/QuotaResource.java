package app.appgrove.fatture;

import app.appgrove.commons.quota.QuotaLimitSource;
import app.appgrove.fatture.QuotaDtos.QuotaStatusView;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Stato di quota dell'app fatture (sola lettura). Espone uso/tetto della metrica {@code fatture} per
 * il tenant del JWT (invariante #1): alimenta il banner consumo/limite del modulo frontend (UC 0052).
 * L'enforcement vero resta sulla creazione ({@link InvoiceResource}, 429); questo endpoint è informativo.
 */
@Path("/api/fatture/v1/quota")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class QuotaResource {

    @Inject
    FattureQuotaService quota;

    @Inject
    QuotaLimitSource limits;

    @Inject
    CallerContext caller;

    @GET
    @RolesAllowed({Roles.OWNER, Roles.ADMIN})
    public QuotaStatusView fatture() {
        String metric = FattureQuotaService.METRIC;
        long used = quota.currentUsage(metric);
        long cap = limits.capFor(caller.tenantId().toString(), metric);
        return QuotaStatusView.of(metric, used, cap);
    }
}
