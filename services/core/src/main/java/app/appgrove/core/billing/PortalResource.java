package app.appgrove.core.billing;

import app.appgrove.core.billing.SubscriptionDtos.PortalSessionView;
import app.appgrove.core.platform.Account;
import app.appgrove.core.platform.AccountRepository;
import app.appgrove.core.platform.CallerContext;
import app.appgrove.core.platform.Roles;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

/**
 * Genera la sessione <b>Customer Portal</b> Paddle server-side (UC 0028 §4.5, #09 G33): delega a Paddle il
 * <b>metodo di pagamento</b> (PCI) e <b>fatture/ricevute</b> (MoR). Nessun dato di carta passa da noi.
 * OWNER-only (billing); tenant dal JWT (#1). L'URL viene aperto lato client.
 */
@Path("/api/platform/v1/me/portal-session")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class PortalResource {

    private static final Logger LOG = Logger.getLogger(PortalResource.class);

    @Inject
    CallerContext caller;

    @Inject
    AccountRepository accounts;

    @Inject
    PaymentProvider provider;

    @POST
    @RolesAllowed(Roles.OWNER)
    public PortalSessionView create() {
        Account account = accounts.findById(caller.tenantId());
        if (account == null || account.getPaddleCustomerId() == null) {
            // customer lazy (#09 C15): esiste solo dopo il primo acquisto → niente portal prima di allora.
            throw new ClientErrorException(
                    "Nessun customer Paddle: completa prima un acquisto", Response.Status.CONFLICT);
        }
        PaymentProvider.CustomerPortalSession session =
                provider.createCustomerPortalSession(account.getPaddleCustomerId());
        LOG.infof("portal-session generata (customer=%s)", account.getPaddleCustomerId());
        return new PortalSessionView(session.url());
    }
}
