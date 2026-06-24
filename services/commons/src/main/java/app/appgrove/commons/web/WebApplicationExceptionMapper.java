package app.appgrove.commons.web;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Normalizza le WebApplicationException (404/405/…) in problem+json mantenendo lo status. */
@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException exception) {
        int status = exception.getResponse() != null
                ? exception.getResponse().getStatus()
                : Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        ProblemDetail problem = ProblemDetail.of(status, reason(status), exception.getMessage());
        return Response.status(status)
                .type(ProblemDetail.MEDIA_TYPE)
                .entity(problem)
                .build();
    }

    private static String reason(int status) {
        Response.Status s = Response.Status.fromStatusCode(status);
        return s != null ? s.getReasonPhrase() : "Errore";
    }
}
