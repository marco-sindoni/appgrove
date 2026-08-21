package app.appgrove.commons.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.appgrove.commons.access.AppRole;
import app.appgrove.commons.access.AppRoleRequiredException;
import app.appgrove.commons.web.AppRoleRequiredMapper.AppRoleProblem;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

/**
 * I tre rifiuti del varco del ruolo diventano tre corpi problem+json distinguibili <b>senza interpretare
 * un messaggio</b> (UC 0099): il testo del server è in italiano, l'interfaccia parla cinque lingue, quindi
 * l'identificativo nel campo {@code type} è contratto.
 */
class AppRoleRequiredMapperTest {

    private static AppRoleProblem map(AppRoleRequiredException exception) {
        try (Response response = new AppRoleRequiredMapper().toResponse(exception)) {
            assertEquals(ProblemDetail.MEDIA_TYPE, response.getMediaType().toString());
            assertEquals(exception.kind().status(), response.getStatus());
            return (AppRoleProblem) response.getEntity();
        }
    }

    @Test
    void noAccessIs403WithItsOwnStableIdentifier() {
        AppRoleProblem problem = map(AppRoleRequiredException.noAccess("crm", AppRole.viewer));
        assertEquals(403, problem.status());
        assertEquals(AppRoleRequiredException.TYPE_NO_ACCESS, problem.type());
        assertEquals("crm", problem.appSlug());
        assertEquals("viewer", problem.requiredRole());
        assertNull(problem.role(), "chi non ha accesso non ha alcun ruolo da dichiarare");
    }

    @Test
    void anInsufficientRoleIs403WithADifferentIdentifierAndBothRoles() {
        AppRoleProblem problem =
                map(AppRoleRequiredException.insufficient("crm", AppRole.editor, AppRole.viewer));
        assertEquals(403, problem.status());
        assertEquals(AppRoleRequiredException.TYPE_INSUFFICIENT, problem.type());
        assertEquals("editor", problem.requiredRole());
        assertEquals("viewer", problem.role());
        assertNotEquals(
                AppRoleRequiredException.TYPE_NO_ACCESS,
                problem.type(),
                "«non entri» e «non puoi fare questo» non devono essere lo stesso rifiuto");
    }

    @Test
    void anUndecidableOutcomeIs503AndNotAPermissionProblem() {
        AppRoleProblem problem = map(AppRoleRequiredException.unavailable("crm"));
        assertEquals(503, problem.status());
        assertEquals(AppRoleRequiredException.TYPE_UNAVAILABLE, problem.type());
        assertNull(problem.requiredRole());
        assertNull(problem.role());
    }
}
