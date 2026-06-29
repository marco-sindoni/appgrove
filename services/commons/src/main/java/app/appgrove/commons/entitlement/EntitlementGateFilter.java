package app.appgrove.commons.entitlement;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Gate entitlement (402) lato servizio: name-bound a {@link RequiresEntitlement}, gira <b>prima</b>
 * dell'endpoint annotato e, se il tenant del JWT non ha accesso all'app corrente, lancia
 * {@link EntitlementRequiredException} (→ 402). È la difesa in profondità del servizio, complementare
 * al gate grossolano dell'authorizer edge (UC 0014, ☁); i ruoli (403) restano su {@code @RolesAllowed},
 * la quota (429) sulla SPI.
 *
 * <p>L'app è identificata dal proprio {@code quarkus.application.name} (= slug): ogni servizio gatekeepa
 * sul proprio entitlement.
 */
@Provider
@RequiresEntitlement
public class EntitlementGateFilter implements ContainerRequestFilter {

    @Inject
    EntitlementService entitlements;

    @ConfigProperty(name = "quarkus.application.name")
    String appSlug;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (!entitlements.hasAccess(appSlug)) {
            throw new EntitlementRequiredException(appSlug);
        }
    }
}
