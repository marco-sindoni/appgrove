package app.appgrove.core.billing;

import app.appgrove.core.billing.PaymentDtos.PaymentsView;
import app.appgrove.core.platform.Roles;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

/**
 * Storico pagamenti e ricevute del conto corrente (UC 0096):
 * {@code GET /api/platform/v1/me/payments}.
 *
 * <p><b>Ruoli</b>: {@code owner} e {@code admin}. UC 0096 §2 assegna a loro pagamenti e ricevute e lascia
 * al {@code member} il solo accesso alla pagina: quanto il workspace ha pagato non è informazione di cui
 * un membro abbia bisogno per lavorare. Il frontend nasconde la sezione ai membri; il divieto vero è qui.
 *
 * <p>Invarianti: nessun parametro consente al chiamante di indicare un conto — il tenant viene dal token
 * verificato (#1) e la lettura è tenant-scoped dal discriminator (#2). I log portano
 * {@code tenant_id}/{@code user_id} via MDC (commons, #4).
 */
@Path("/api/platform/v1/me/payments")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class PaymentsResource {

    private static final Logger LOG = Logger.getLogger(PaymentsResource.class);

    @Inject
    PaymentReadModel readModel;

    /** Lo storico del conto, dalla transazione più recente. */
    @GET
    @RolesAllowed({Roles.OWNER, Roles.ADMIN})
    public PaymentsView payments() {
        PaymentsView view = readModel.forCurrentTenant();
        LOG.debugf("payments.read transazioni=%d", view.payments().size());
        return view;
    }
}
