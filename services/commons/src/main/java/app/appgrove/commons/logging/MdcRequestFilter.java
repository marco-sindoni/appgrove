package app.appgrove.commons.logging;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.util.UUID;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logmanager.MDC;

/**
 * Logging strutturato (invariante #4): popola l'MDC a inizio richiesta con request id, {@code tenant_id}
 * e {@code user_id} dal JWT, così ogni log della richiesta li riporta. Pulisce l'MDC in risposta.
 * Il campo {@code app_id} è valorizzato dal singolo servizio.
 */
@Provider
@ApplicationScoped
public class MdcRequestFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String REQUEST_ID = "request_id";
    public static final String TENANT_ID = "tenant_id";
    public static final String USER_ID = "user_id";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Inject
    JsonWebToken jwt;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String requestId = requestContext.getHeaderString(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        MDC.put(REQUEST_ID, requestId);
        putIfPresent(TENANT_ID, claim("tenant_id"));
        putIfPresent(USER_ID, jwt != null ? safeName() : null);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        MDC.remove(REQUEST_ID);
        MDC.remove(TENANT_ID);
        MDC.remove(USER_ID);
    }

    private String claim(String name) {
        try {
            Object value = jwt == null ? null : jwt.getClaim(name);
            return value == null ? null : value.toString();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String safeName() {
        try {
            return jwt.getName();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void putIfPresent(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
        }
    }
}
