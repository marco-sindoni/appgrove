package app.appgrove.commons.access;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import java.lang.reflect.Method;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Il <b>varco</b> del ruolo per applicazione (UC 0099): interpreta {@link RequiresAppRole} e decide prima
 * che l'operazione annotata parta. È il gemello di {@code EntitlementGateFilter} e la ragione per cui
 * nessuna applicazione deve scrivere confronti fra ruoli.
 *
 * <p>L'applicazione è identificata dal proprio {@code quarkus.application.name} (= slug): ogni servizio
 * chiede il ruolo <b>su sé stesso</b>, e non potrebbe chiederlo su un altro nemmeno volendo.
 *
 * <p><b>Ordine rispetto al varco dei diritti d'accesso.</b> Gira <b>dopo</b> di quello (priorità più alta,
 * quindi più tardi): a un account senza abbonamento si risponde «serve l'abbonamento» (402), non «il tuo
 * ruolo non basta» — l'ordine delle due risposte è l'ordine in cui una persona può rimediare.
 *
 * <p><b>Perché è un filtro e non un controllo dentro l'operazione.</b> Perché l'operazione non deve poter
 * dimenticarselo, e perché il ruolo richiesto resta <b>leggibile</b> accanto alla firma dell'operazione:
 * chi legge la risorsa vede quanto potere serve senza entrare nel corpo dei metodi.
 */
@Provider
@RequiresAppRole(AppRole.viewer) // valore ininfluente: serve solo al legame per nome (name binding)
@Priority(Priorities.USER + 10)
public class AppRoleGateFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(AppRoleGateFilter.class);

    @Inject
    AppRoleService roles;

    @Context
    ResourceInfo resourceInfo;

    @ConfigProperty(name = "quarkus.application.name")
    String appSlug;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        RequiresAppRole required = requirement();
        if (required == null) {
            // Non dovrebbe capitare (il legame per nome garantisce l'annotazione), ma se capitasse la
            // risposta prudente è negare: un varco che non sa cosa deve chiedere non lascia passare.
            LOG.errorf("app_role.gate annotazione non risolvibile app_id=%s → si nega", appSlug);
            throw AppRoleRequiredException.unavailable(appSlug);
        }

        AppRoleOutcome outcome =
                required.fresh() ? roles.roleFresh(appSlug) : roles.roleOf(appSlug);

        if (outcome instanceof AppRoleOutcome.Granted granted) {
            if (granted.role().atLeast(required.value())) {
                return;
            }
            LOG.debugf(
                    "app_role.gate ruolo insufficiente app_id=%s richiesto=%s posseduto=%s",
                    appSlug, required.value(), granted.role());
            throw AppRoleRequiredException.insufficient(appSlug, required.value(), granted.role());
        }
        if (outcome instanceof AppRoleOutcome.NoAccess) {
            LOG.debugf("app_role.gate nessun accesso app_id=%s richiesto=%s", appSlug, required.value());
            throw AppRoleRequiredException.noAccess(appSlug, required.value());
        }
        // Unavailable: si nega, ma dicendo che è un guasto nostro.
        throw AppRoleRequiredException.unavailable(appSlug);
    }

    /**
     * L'annotazione che vale per questa richiesta: <b>vince il metodo</b> sulla classe, così una risorsa
     * può chiedere {@code viewer} per le letture e {@code editor} per le scritture senza spezzarsi in due.
     */
    private RequiresAppRole requirement() {
        Method method = resourceInfo == null ? null : resourceInfo.getResourceMethod();
        if (method != null && method.isAnnotationPresent(RequiresAppRole.class)) {
            return method.getAnnotation(RequiresAppRole.class);
        }
        Class<?> resource = resourceInfo == null ? null : resourceInfo.getResourceClass();
        return resource == null ? null : resource.getAnnotation(RequiresAppRole.class);
    }
}
