package app.appgrove.commons.web;

import app.appgrove.commons.quota.QuotaExceededException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Quota esaurita → <b>429 Too Many Requests</b> problem+json (#09 A6). */
@Provider
public class QuotaExceededMapper implements ExceptionMapper<QuotaExceededException> {

    private static final int TOO_MANY_REQUESTS = 429;

    @Override
    public Response toResponse(QuotaExceededException exception) {
        ProblemDetail problem = ProblemDetail.of(
                TOO_MANY_REQUESTS,
                "Quota esaurita",
                exception.getMessage());
        return Response.status(TOO_MANY_REQUESTS)
                .type(ProblemDetail.MEDIA_TYPE)
                .entity(problem)
                .build();
    }
}
