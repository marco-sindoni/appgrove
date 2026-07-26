package app.appgrove.core.platform;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.time.Instant;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Timbra l'ultima attività dell'account su ogni richiesta autenticata (UC 0035). Legge il tenant
 * dal claim {@code tenant_id} del JWT verificato (invariante #1: mai da body/param) e delega ad
 * {@link AccountActivityTracker} (throttlato, best-effort). Filtro di <b>risposta</b>: non aggiunge
 * latenza prima dell'elaborazione e, essendo best-effort, non altera mai l'esito della richiesta —
 * le richieste non autenticate (nessun {@code tenant_id}) sono ignorate.
 */
@Provider
public class ActivityStampFilter implements ContainerResponseFilter {

    @Inject
    JsonWebToken jwt;

    @Inject
    AccountActivityTracker tracker;

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        try {
            Object claim = jwt.getClaim("tenant_id");
            String tenantId = claim == null ? null : claim.toString();
            if (tenantId == null || tenantId.isBlank()) {
                return;
            }
            tracker.touch(tenantId, Instant.now());
        } catch (RuntimeException e) {
            // best-effort: il timbro di attività non deve mai influenzare la risposta.
        }
    }
}
