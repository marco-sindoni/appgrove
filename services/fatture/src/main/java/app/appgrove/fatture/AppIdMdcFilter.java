package app.appgrove.fatture;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logmanager.MDC;

/**
 * Completa il logging strutturato (invariante #4) aggiungendo {@code app_id} all'MDC: il commons
 * valorizza {@code tenant_id}/{@code user_id}/{@code request_id}, l'app dichiara il proprio id.
 */
@Provider
@ApplicationScoped
public class AppIdMdcFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String APP_ID = "app_id";
    public static final String APP_ID_VALUE = "fatture";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        MDC.put(APP_ID, APP_ID_VALUE);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        MDC.remove(APP_ID);
    }
}
