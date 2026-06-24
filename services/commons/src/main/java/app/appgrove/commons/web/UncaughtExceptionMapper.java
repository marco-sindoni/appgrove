package app.appgrove.commons.web;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/** Rete di sicurezza: qualsiasi eccezione non gestita → 500 problem+json senza esporre dettagli interni. */
@Provider
public class UncaughtExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(UncaughtExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        LOG.error("Eccezione non gestita", exception);
        ProblemDetail problem = ProblemDetail.of(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Errore interno",
                "Si è verificato un errore imprevisto.");
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(ProblemDetail.MEDIA_TYPE)
                .entity(problem)
                .build();
    }
}
