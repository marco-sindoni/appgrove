package app.appgrove.commons.web;

import app.appgrove.commons.entitlement.EntitlementRequiredException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Entitlement non superato → <b>402 Payment Required</b> problem+json (#09 dec.30 gate 3, F31). */
@Provider
public class EntitlementRequiredMapper implements ExceptionMapper<EntitlementRequiredException> {

    private static final int PAYMENT_REQUIRED = 402;

    @Override
    public Response toResponse(EntitlementRequiredException exception) {
        ProblemDetail problem = ProblemDetail.of(
                PAYMENT_REQUIRED,
                "Abbonamento richiesto",
                exception.getMessage());
        return Response.status(PAYMENT_REQUIRED)
                .type(ProblemDetail.MEDIA_TYPE)
                .entity(problem)
                .build();
    }
}
