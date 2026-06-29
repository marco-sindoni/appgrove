package app.appgrove.commons.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.appgrove.commons.entitlement.EntitlementRequiredException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

/** Il mapper traduce {@link EntitlementRequiredException} in 402 problem+json (UC 0027). */
class EntitlementRequiredMapperTest {

    @Test
    void mapsTo402ProblemJson() {
        try (Response response =
                new EntitlementRequiredMapper().toResponse(new EntitlementRequiredException("fatture"))) {
            assertEquals(402, response.getStatus());
            assertEquals(ProblemDetail.MEDIA_TYPE, response.getMediaType().toString());
            ProblemDetail problem = (ProblemDetail) response.getEntity();
            assertEquals(402, problem.status());
            assertEquals("Abbonamento richiesto", problem.title());
        }
    }
}
