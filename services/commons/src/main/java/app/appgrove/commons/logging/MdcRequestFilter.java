package app.appgrove.commons.logging;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.util.Optional;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logmanager.MDC;

/**
 * Logging strutturato (invariante #4): popola l'MDC a inizio richiesta con request id,
 * {@code correlation_id}, {@code tenant_id} e {@code user_id} dal JWT, più {@code trace_id}/
 * {@code span_id} dal contesto OpenTelemetry (nomi secondo le semantic conventions OTel, #08/3),
 * così ogni log della richiesta li riporta. Pulisce l'MDC in risposta.
 * Il campo {@code app_id} arriva dalla config {@code appgrove.app-id} del servizio — la STESSA
 * chiave usata dalle metriche EMF: un'unica fonte, log e metriche non possono divergere.
 *
 * <p>Il {@code correlation_id} arriva dall'header {@code X-Correlation-Id} iniettato all'edge
 * (API Gateway, #08/4); in sua assenza (chiamate dirette, sviluppo locale) si riusa il request id.
 */
@Provider
@ApplicationScoped
public class MdcRequestFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String REQUEST_ID = "request_id";
    public static final String CORRELATION_ID = "correlation_id";
    public static final String TENANT_ID = "tenant_id";
    public static final String USER_ID = "user_id";
    public static final String APP_ID = "app_id";
    public static final String TRACE_ID = "trace_id";
    public static final String SPAN_ID = "span_id";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Inject
    JsonWebToken jwt;

    @Inject
    @ConfigProperty(name = "appgrove.app-id")
    Optional<String> appId;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String requestId = requestContext.getHeaderString(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        MDC.put(REQUEST_ID, requestId);

        String correlationId = requestContext.getHeaderString(CORRELATION_ID_HEADER);
        MDC.put(CORRELATION_ID, correlationId == null || correlationId.isBlank() ? requestId : correlationId);

        putIfPresent(APP_ID, appId.orElse(null));
        putIfPresent(TENANT_ID, claim("tenant_id"));
        // user_id = claim `sub` (invariante #1), MAI jwt.getName(): quel metodo risolve
        // upn/preferred_username, che può essere l'email (dati personali nei log, vietati da #08/5).
        putIfPresent(USER_ID, safeSubject());

        // trace_id/span_id dal contesto OTel corrente (strumentazione attiva, export spento #08/11):
        // presenti solo se il servizio ha l'estensione OpenTelemetry e lo span è valido.
        SpanContext span = Span.current().getSpanContext();
        if (span.isValid()) {
            MDC.put(TRACE_ID, span.getTraceId());
            MDC.put(SPAN_ID, span.getSpanId());
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        MDC.remove(REQUEST_ID);
        MDC.remove(CORRELATION_ID);
        MDC.remove(TENANT_ID);
        MDC.remove(USER_ID);
        MDC.remove(APP_ID);
        MDC.remove(TRACE_ID);
        MDC.remove(SPAN_ID);
    }

    private String claim(String name) {
        try {
            Object value = jwt == null ? null : jwt.getClaim(name);
            return value == null ? null : value.toString();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String safeSubject() {
        try {
            return jwt == null ? null : jwt.getSubject();
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
