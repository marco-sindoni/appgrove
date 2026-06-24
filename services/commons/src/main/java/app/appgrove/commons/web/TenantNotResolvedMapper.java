package app.appgrove.commons.web;

import app.appgrove.commons.tenancy.TenantNotResolvedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Fail-closed: tenant non risolvibile dal JWT → 403 problem+json. */
@Provider
public class TenantNotResolvedMapper implements ExceptionMapper<TenantNotResolvedException> {

    @Override
    public Response toResponse(TenantNotResolvedException exception) {
        ProblemDetail problem = ProblemDetail.of(
                Response.Status.FORBIDDEN.getStatusCode(),
                "Accesso negato",
                "Tenant non determinabile dal token.");
        return Response.status(Response.Status.FORBIDDEN)
                .type(ProblemDetail.MEDIA_TYPE)
                .entity(problem)
                .build();
    }
}
