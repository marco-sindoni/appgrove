package app.appgrove.commons.entitlement;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Client REST verso il read-model entitlement di <b>core</b> ({@code GET /api/platform/v1/me/entitlements},
 * UC 0027). Vive in {@code commons} così ogni app lo riusa (lo erediteranno UC 0046/0054).
 *
 * <p><b>Nota architetturale (chiamata sincrona app→core).</b> È una chiamata server-to-server sincrona sul
 * path caldo: scelta pragmatica per sbloccare l'enforcement reale local-first, ma <b>antipattern a regime</b>.
 * Il target disaccoppiato (proiezione locale alimentata da eventi di lifecycle subscription, ossatura SQS di
 * UC 0025) è tracciato in {@code docs/_BACKLOG.md} e in UC 0046 — il seam qui è progettato per essere
 * sostituito senza toccare il codice di dominio dell'app.
 *
 * <p>L'URL di base è per-ambiente: {@code quarkus.rest-client.core-api.url}.
 */
@RegisterRestClient(configKey = "core-api")
@Produces(MediaType.APPLICATION_JSON)
public interface EntitlementClient {

    /**
     * Entitlement del tenant del JWT. Il token del chiamante è propagato esplicitamente nell'header
     * {@code Authorization} (invariante #1: {@code tenant_id} dal JWT verificato, mai da param/body).
     */
    @GET
    @Path("/api/platform/v1/me/entitlements")
    MeEntitlementsView getMyEntitlements(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorization);
}
