package app.appgrove.core.billing;

import app.appgrove.commons.entitlement.MeEntitlementsView;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

/**
 * Read-model entitlement del tenant corrente (UC 0027): {@code GET /api/platform/v1/me/entitlements}.
 * Aperto a <b>qualunque ruolo</b> autenticato del tenant (owner/admin/member): è la base sia per il
 * registry del frontend sia per il gate lato servizio delle app. Tenant dal JWT verificato (#1), letture
 * tenant-scoped (#2). I log portano {@code tenant_id}/{@code user_id} via MDC (commons, #4).
 */
@Path("/api/platform/v1/me")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class MeResource {

    private static final Logger LOG = Logger.getLogger(MeResource.class);

    @Inject
    EntitlementReadModel entitlements;

    @GET
    @Path("/entitlements")
    public MeEntitlementsView entitlements() {
        MeEntitlementsView view = entitlements.forCurrentTenant();
        LOG.debugf("entitlements risolti: %d app entitled", view.entitlements().size());
        return view;
    }
}
