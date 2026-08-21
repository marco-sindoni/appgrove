package app.appgrove.commons.access;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Client verso la lettura «dove posso entrare e con che ruolo» di <b>core</b>
 * ({@code GET /api/platform/v1/me/app-access}, UC 0099). Vive in {@code commons} così ogni applicazione la
 * riusa senza riscriverla: è il gemello di {@code EntitlementClient}, e come quello usa l'URL di base
 * per-ambiente {@code quarkus.rest-client.core-api.url}.
 *
 * <p>Non è la chiamata del percorso caldo: il percorso normale legge la <b>copia locale</b> del servizio
 * (UC 0099). Questa chiamata è il rinfresco — quando la copia manca, è scaduta o è stata invalidata — e la
 * rilettura obbligatoria delle operazioni irreversibili.
 */
@RegisterRestClient(configKey = "core-api")
@Produces(MediaType.APPLICATION_JSON)
public interface AppAccessClient {

    /**
     * Applicazioni in cui la persona del token può entrare, con il ruolo su ciascuna. Il token del
     * chiamante è propagato esplicitamente nell'header {@code Authorization} (invariante #1: account e
     * persona dal JWT verificato, mai da parametro o corpo).
     */
    @GET
    @Path("/api/platform/v1/me/app-access")
    List<MyAppAccessView> getMyAppAccess(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorization);
}
