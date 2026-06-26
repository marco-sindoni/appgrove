package app.appgrove.authlocal;

import app.appgrove.commons.web.ProblemDetail;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Token assente/scaduto/forgiato → 401 problem+json (fail-closed). */
@Provider
public class InvalidTokenMapper implements ExceptionMapper<InvalidTokenException> {

    @Override
    public Response toResponse(InvalidTokenException exception) {
        ProblemDetail problem = ProblemDetail.of(
                Response.Status.UNAUTHORIZED.getStatusCode(), "Non autorizzato", "Token non valido o scaduto.");
        return Response.status(Response.Status.UNAUTHORIZED)
                .type(ProblemDetail.MEDIA_TYPE)
                .entity(problem)
                .build();
    }
}
