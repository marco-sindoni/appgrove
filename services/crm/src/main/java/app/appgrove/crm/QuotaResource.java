package app.appgrove.crm;

import app.appgrove.commons.quota.QuotaLimitSource;
import app.appgrove.crm.QuotaDtos.QuotaStatusView;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Stato di quota dell'app Mini-CRM (sola lettura). Espone uso/tetto della metrica
 * {@code seats} per il tenant del JWT (invariante #1): alimenta il banner consumo/limite del
 * modulo frontend. L'enforcement vero resta sulla creazione ({@link ItemResource}, 429); questo
 * endpoint è informativo e volutamente fuori dal gate entitlement.
 *
 * <p><b>Esente dai ruoli, di proposito</b> (UC 0101): non porta nemmeno il varco del ruolo, e l'esenzione è
 * <b>dichiarata col suo motivo</b> in {@link CrmOperationsContract}. Il consumo va letto anche da chi non è
 * ancora stato abilitato all'applicazione: un banner che diventa un rifiuto non informa nessuno. Chi
 * aggiungesse qui un {@code @RequiresAppRole} «per coerenza» renderebbe rossa la suite, ed è il
 * comportamento voluto.
 */
@Path("/api/crm/v1/quota")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class QuotaResource {

    @Inject
    CrmQuotaService quota;

    @Inject
    QuotaLimitSource limits;

    @Inject
    CallerContext caller;

    @GET
    @RolesAllowed({Roles.OWNER, Roles.ADMIN, Roles.MEMBER})
    public QuotaStatusView status() {
        String metric = CrmQuotaService.METRIC;
        long used = quota.currentUsage(metric);
        long cap = limits.capFor(caller.tenantId().toString(), metric);
        return QuotaStatusView.of(metric, used, cap);
    }
}
