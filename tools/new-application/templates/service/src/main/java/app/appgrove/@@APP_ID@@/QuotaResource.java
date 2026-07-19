package app.appgrove.@@APP_ID@@;

import app.appgrove.commons.quota.QuotaLimitSource;
import app.appgrove.@@APP_ID@@.QuotaDtos.QuotaStatusView;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Stato di quota dell'app @@APP_NAME@@ (sola lettura). Espone uso/tetto della metrica
 * {@code @@METRIC@@} per il tenant del JWT (invariante #1): alimenta il banner consumo/limite del
 * modulo frontend. L'enforcement vero resta sulla creazione ({@link ItemResource}, 429); questo
 * endpoint è informativo e volutamente fuori dal gate entitlement.
 */
@Path("/api/@@APP_ID@@/v1/quota")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class QuotaResource {

    @Inject
    @@APP_CLASS@@QuotaService quota;

    @Inject
    QuotaLimitSource limits;

    @Inject
    CallerContext caller;

    @GET
    @RolesAllowed({@@ROLES_ALLOWED@@})
    public QuotaStatusView status() {
        String metric = @@APP_CLASS@@QuotaService.METRIC;
        long used = quota.currentUsage(metric);
        long cap = limits.capFor(caller.tenantId().toString(), metric);
        return QuotaStatusView.of(metric, used, cap);
    }
}
