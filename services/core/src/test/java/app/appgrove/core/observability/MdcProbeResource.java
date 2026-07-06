package app.appgrove.core.observability;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import org.jboss.logging.Logger;

/**
 * Endpoint di test (solo classpath di test): emette un log INFO dentro una richiesta autenticata,
 * così {@code MdcWiringTest} può verificare che l'MDC del framework (commons, incluso app_id da config)
 * arrivi davvero su ogni record di log.
 */
@Path("/test/mdc-probe")
@Authenticated
public class MdcProbeResource {

    public static final String LOGGER_NAME = "appgrove.test.mdc-probe";

    private static final Logger LOG = Logger.getLogger(LOGGER_NAME);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> probe() {
        LOG.info("mdc-probe");
        return Map.of("ok", "true");
    }
}
