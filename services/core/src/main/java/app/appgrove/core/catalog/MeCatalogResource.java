package app.appgrove.core.catalog;

import app.appgrove.core.catalog.CatalogDtos.CatalogView;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

/**
 * Vetrina del catalogo per il tenant corrente (UC 0095): {@code GET /api/platform/v1/me/catalog}.
 *
 * <p>Aperta a <b>qualunque ruolo</b> autenticato dell'account — è la vetrina, e vedere che cosa esiste
 * non richiede alcun diritto d'uso. Il divieto vero resta a valle: avviare l'acquisto è riservato al
 * ruolo {@code owner} da {@code CheckoutResource}, e nessuna delle informazioni servite qui lo aggira.
 *
 * <p>Nessun parametro: il tenant viene dal JWT verificato (#1) e le letture tenant-scoped passano dal
 * discriminator (#2). I log portano {@code tenant_id}/{@code user_id}/{@code app_id} via MDC (#4).
 */
@Path("/api/platform/v1/me/catalog")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class MeCatalogResource {

    private static final Logger LOG = Logger.getLogger(MeCatalogResource.class);

    @Inject
    CatalogReadModel catalog;

    @GET
    public CatalogView catalog() {
        CatalogView view = catalog.forCurrentTenant();
        LOG.infof("catalog.read apps=%d", view.apps().size());
        return view;
    }
}
